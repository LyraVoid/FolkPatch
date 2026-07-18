use std::{
    collections::BTreeMap,
    ffi::CString,
    fs,
    fs::{DirEntry, FileType, create_dir, create_dir_all, read_dir, read_link},
    os::unix::fs::{FileTypeExt, symlink},
    path::{Path, PathBuf},
    time::{SystemTime, UNIX_EPOCH},
};

use anyhow::{Context, Result, bail};
use extattr::lgetxattr;
use libc;
use rustix::{
    fd::AsFd,
    fs::{CWD, Gid, MetadataExt, Mode, Uid, chmod, chown},
    mount::{
        FsMountFlags, FsOpenFlags, MountAttrFlags, MountFlags, MountPropagationFlags, UnmountFlags,
        fsconfig_create, fsconfig_set_string, fsmount, fsopen,
        mount, mount_bind, mount_change, unmount,
    },
};

use crate::{
    defs::{
        AP_MAGIC_MOUNT_SOURCE, DISABLE_FILE_NAME, MODULE_DIR, REMOVE_FILE_NAME,
        REPLACE_DIR_FILE_NAME, REPLACE_DIR_XATTR, SKIP_MOUNT_FILE_NAME,
    },
    magic_mount::NodeFileType::{Directory, RegularFile, Symlink, Whiteout},
    restorecon::{lgetfilecon, lsetfilecon},
    utils::ensure_dir_exists,
};

const MAX_LAYERS: usize = 64;

#[derive(PartialEq, Eq, Hash, Clone, Debug)]
enum NodeFileType {
    RegularFile,
    Directory,
    Symlink,
    Whiteout,
}

impl NodeFileType {
    fn from_file_type(file_type: FileType) -> Self {
        if file_type.is_file() {
            RegularFile
        } else if file_type.is_dir() {
            Directory
        } else if file_type.is_symlink() {
            Symlink
        } else {
            Whiteout
        }
    }

    fn needs_overlay_vs_real(&self, real_path: &Path) -> bool {
        match self {
            Symlink => true,
            Whiteout => real_path.exists(),
            _ => match real_path.symlink_metadata() {
                Ok(metadata) => {
                    let real_type = Self::from_file_type(metadata.file_type());
                    real_type != *self || real_type == Symlink
                }
                Err(_) => true,
            },
        }
    }
}

#[derive(Debug, Clone)]
struct Node {
    name: String,
    file_type: NodeFileType,
    children: BTreeMap<String, Node>,
    module_path: Option<PathBuf>,
    replace: bool,
    skip: bool,
}

impl Node {
    fn collect_module_files<P>(&mut self, module_dir: P) -> Result<bool>
    where
        P: AsRef<Path>,
    {
        let dir = module_dir.as_ref();
        let mut has_file = false;
        for entry in dir.read_dir()?.flatten() {
            let name = entry.file_name().to_string_lossy().to_string();

            let node = match self.children.entry(name.clone()) {
                std::collections::btree_map::Entry::Occupied(o) => Some(o.into_mut()),
                std::collections::btree_map::Entry::Vacant(v) => {
                    Self::new_module(&name, &entry).map(|it| v.insert(it))
                }
            };

            if let Some(node) = node {
                has_file |= if node.file_type == NodeFileType::Directory {
                    node.collect_module_files(dir.join(&node.name))? || node.replace
                } else {
                    true
                };
            }
        }

        Ok(has_file)
    }

    fn dir_is_replace<P>(path: P) -> bool
    where
        P: AsRef<Path>,
    {
        if let Ok(v) = lgetxattr(&path, REPLACE_DIR_XATTR)
            && String::from_utf8_lossy(&v) == "y"
        {
            return true;
        }

        path.as_ref().join(REPLACE_DIR_FILE_NAME).exists()
    }

    fn new_root<T: ToString>(name: T) -> Self {
        Node {
            name: name.to_string(),
            file_type: Directory,
            children: Default::default(),
            module_path: None,
            replace: false,
            skip: false,
        }
    }

    fn new_module<S>(name: &S, entry: &DirEntry) -> Option<Self>
    where
        S: ToString,
    {
        if let Ok(metadata) = entry.metadata() {
            let path = entry.path();
            let file_type = if metadata.file_type().is_char_device() && metadata.rdev() == 0 {
                NodeFileType::Whiteout
            } else {
                NodeFileType::from_file_type(metadata.file_type())
            };
            let replace = file_type == NodeFileType::Directory && Self::dir_is_replace(&path);
            if replace {
                log::debug!("{} need replace", path.display());
            }
            return Some(Self {
                name: name.to_string(),
                file_type,
                children: BTreeMap::default(),
                module_path: Some(path),
                replace,
                skip: false,
            });
        }

        None
    }
}

const MODULE_PARTITIONS: &[(&str, bool)] = &[
    ("system", false),
    ("vendor", true),
    ("system_ext", true),
    ("product", true),
    ("odm", false),
    ("oem", false),
    ("apex", true),
    ("mi_ext", true),
    ("my_bigball", true),
    ("my_carrier", true),
    ("my_company", true),
    ("my_engineering", true),
    ("my_heytap", true),
    ("my_manifest", true),
    ("my_preload", true),
    ("my_product", true),
    ("my_region", true),
    ("my_reserve", true),
    ("my_stock", true),
    ("optics", true),
    ("prism", true),
];

fn collect_module_files() -> Result<Option<Node>> {
    let mut root = Node::new_root("");
    let module_root = Path::new(MODULE_DIR);
    let mut has_file = false;

    log::debug!("begin collect module files: {}", module_root.display());

    for entry in module_root.read_dir()?.flatten() {
        if !entry.file_type()?.is_dir() {
            continue;
        }

        let id = entry.file_name().to_str().unwrap().to_string();
        log::debug!("processing new module: {id}");

        let prop = entry.path().join("module.prop");
        if !prop.exists() {
            log::debug!("skipped module {id}, because not found module.prop");
            continue;
        }

        if entry.path().join(DISABLE_FILE_NAME).exists()
            || entry.path().join(REMOVE_FILE_NAME).exists()
            || entry.path().join(SKIP_MOUNT_FILE_NAME).exists()
        {
            log::debug!("skipped module {id}, due to disable/remove/skip_mount");
            continue;
        }

        log::debug!("collecting {}", entry.path().display());

        for &(partition, require_symlink) in MODULE_PARTITIONS {
            let module_partition_dir = entry.path().join(partition);

            if !module_partition_dir.is_dir() {
                continue;
            }

            if require_symlink {
                let path_of_root = Path::new("/").join(partition);
                let path_of_system = Path::new("/system").join(partition);
                if !path_of_root.is_dir() || !path_of_system.is_symlink() {
                    continue;
                }
            } else {
                let path_of_root = Path::new("/").join(partition);
                if !path_of_root.is_dir() {
                    continue;
                }
            }

            let partition_node = root
                .children
                .entry(partition.to_string())
                .or_insert_with(|| Node::new_root(partition));

            let collected = partition_node.collect_module_files(&module_partition_dir)?;
            has_file |= collected;
        }
    }

    if has_file {
        Ok(Some(root))
    } else {
        Ok(None)
    }
}

fn clone_symlink<Src: AsRef<Path>, Dst: AsRef<Path>>(src: Src, dst: Dst) -> Result<()> {
    let src_symlink = read_link(src.as_ref())?;
    symlink(&src_symlink, dst.as_ref())?;
    lsetfilecon(dst.as_ref(), lgetfilecon(src.as_ref())?.as_str())?;
    log::debug!(
        "clone symlink {} -> {}({})",
        dst.as_ref().display(),
        dst.as_ref().display(),
        src_symlink.display()
    );
    Ok(())
}

fn escape_mount_option_value(value: &str) -> String {
    let mut escaped = String::with_capacity(value.len());
    for ch in value.chars() {
        if matches!(ch, '\\' | ',' | ':') {
            escaped.push('\\');
        }
        escaped.push(ch);
    }
    escaped
}

fn mount_overlay_core(
    lower_dirs: &[String],
    upperdir: Option<&Path>,
    workdir: Option<&Path>,
    dest: &Path,
    mount_source: &str,
) -> Result<()> {
    let lowerdir_config = lower_dirs
        .iter()
        .map(|p| escape_mount_option_value(p))
        .collect::<Vec<_>>()
        .join(":");

    log::debug!(
        "overlayfs core mount: dest={}, layers={}, source={}",
        dest.display(),
        lower_dirs.len(),
        mount_source
    );

    let upperdir_s = upperdir
        .filter(|up| up.exists())
        .map(|e| e.display().to_string());
    let workdir_s = workdir
        .filter(|wd| wd.exists())
        .map(|e| e.display().to_string());

    let fs = fsopen("overlay", FsOpenFlags::FSOPEN_CLOEXEC).context("Failed to fsopen overlay")?;
    let fs = fs.as_fd();
    fsconfig_set_string(fs, "lowerdir", &lowerdir_config)
        .with_context(|| format!("Failed to fsconfig set lowerdir with {lowerdir_config}"))?;

    if let (Some(upperdir), Some(workdir)) = (&upperdir_s, &workdir_s) {
        fsconfig_set_string(fs, "upperdir", upperdir)
            .with_context(|| format!("Failed to fsconfig set upperdir with {upperdir}"))?;
        fsconfig_set_string(fs, "workdir", workdir)
            .with_context(|| format!("Failed to fsconfig set workdir with {workdir}"))?;
    }

    let source_s = mount_source.to_string();
    fsconfig_set_string(fs, "source", &source_s)
        .with_context(|| format!("Failed to fsconfig set source with {source_s}"))?;
    fsconfig_create(fs).context("Failed to fsconfig create new fs")?;

    let mount_fd = fsmount(fs, FsMountFlags::FSMOUNT_CLOEXEC, MountAttrFlags::empty())
        .context("Failed to mount")?;
    move_mount(
        mount_fd.as_fd(),
        "",
        CWD,
        dest,
        MoveMountFlags::MOVE_MOUNT_F_EMPTY_PATH,
    )?;

    log::debug!("overlayfs mount success: {}", dest.display());
    Ok(())
}

fn mount_overlayfs(
    lower_dirs: &[String],
    lowest: &str,
    upperdir: Option<PathBuf>,
    workdir: Option<PathBuf>,
    dest: impl AsRef<Path>,
    mount_source: &str,
) -> Result<()> {
    let mut current_layers: Vec<String> = lower_dirs.to_vec();
    current_layers.push(lowest.to_string());

    while current_layers.len() > MAX_LAYERS {
        let split_idx = current_layers.len().saturating_sub(MAX_LAYERS - 1);
        let bottom_chunk: Vec<String> = current_layers.drain(split_idx..).collect();

        let timestamp = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_nanos();
        let staging_dir = Path::new("/dev/ap_magic_mount").join(format!(
            "staging_{}_{}",
            timestamp,
            current_layers.len()
        ));

        ensure_dir_exists(&staging_dir)?;

        mount_overlay_core(&bottom_chunk, None, None, &staging_dir, mount_source)?;
        log::debug!(
            "staging layer created: path={}, input_layers={}",
            staging_dir.display(),
            bottom_chunk.len()
        );

        current_layers.push(staging_dir.to_string_lossy().into_owned());
    }

    mount_overlay_core(
        &current_layers,
        upperdir.as_deref(),
        workdir.as_deref(),
        dest.as_ref(),
        mount_source,
    )
}

fn prepare_overlay_dir(
    path: &Path,
    work_dir_path: &Path,
    module_path: Option<&PathBuf>,
) -> Result<()> {
    log::debug!(
        "creating overlay dir for {} at {}",
        path.display(),
        work_dir_path.display()
    );
    create_dir_all(work_dir_path)?;
    let source: &Path = if path.exists() {
        path
    } else if let Some(mp) = module_path {
        mp
    } else {
        bail!("cannot mount root dir {}!", path.display());
    };
    let metadata = source.metadata()?;
    chmod(work_dir_path, Mode::from_raw_mode(metadata.mode()))?;
    chown(
        work_dir_path,
        Some(Uid::from_raw(metadata.uid())),
        Some(Gid::from_raw(metadata.gid())),
    )?;
    lsetfilecon(work_dir_path, lgetfilecon(source)?.as_str())?;
    Ok(())
}

fn handle_mount_result(result: Result<()>, path: &Path, name: &str, has_overlay: bool) -> Result<()> {
    if let Err(e) = result {
        if has_overlay {
            return Err(e);
        }
        log::error!("mount child {}/{} failed: {}", path.display(), name, e);
    }
    Ok(())
}

fn mount_mirror<P: AsRef<Path>, WP: AsRef<Path>>(
    path: P,
    work_dir_path: WP,
    entry: &DirEntry,
) -> Result<()> {
    let path = path.as_ref().join(entry.file_name());
    let work_dir_path = work_dir_path.as_ref().join(entry.file_name());
    let file_type = entry.file_type()?;

    if file_type.is_file() {
        log::debug!(
            "mount mirror file {} -> {}",
            path.display(),
            work_dir_path.display()
        );
        fs::File::create(&work_dir_path)?;
        mount_bind(&path, &work_dir_path)?;
    } else if file_type.is_dir() {
        log::debug!(
            "mount mirror dir {} -> {}",
            path.display(),
            work_dir_path.display()
        );
        create_dir(&work_dir_path)?;
        let metadata = entry.metadata()?;
        chmod(&work_dir_path, Mode::from_raw_mode(metadata.mode()))?;
        chown(
            &work_dir_path,
            Some(Uid::from_raw(metadata.uid())),
            Some(Gid::from_raw(metadata.gid())),
        )?;
        lsetfilecon(&work_dir_path, lgetfilecon(&path)?.as_str())?;
        for entry in read_dir(&path)?.flatten() {
            mount_mirror(&path, &work_dir_path, &entry)?;
        }
    } else if file_type.is_symlink() {
        log::debug!(
            "create mirror symlink {} -> {}",
            path.display(),
            work_dir_path.display()
        );
        clone_symlink(&path, &work_dir_path)?;
    }

    Ok(())
}

fn should_create_overlay(path: &Path, current: &mut Node, has_overlay: bool) -> bool {
    if has_overlay {
        return false;
    }
    if current.replace && current.module_path.is_some() {
        return true;
    }
    for (name, node) in &mut current.children {
        let real_path = path.join(name);
        if node.file_type.needs_overlay_vs_real(&real_path) {
            if current.module_path.is_none() {
                log::error!("cannot create overlay on {}, ignore: {name}", path.display());
                node.skip = true;
                continue;
            }
            return true;
        }
    }
    false
}

fn create_whiteout(upper_dir: &Path, name: &str) -> Result<()> {
    let whiteout_path = upper_dir.join(name);
    log::debug!("creating whiteout: {}", whiteout_path.display());
    #[cfg(any(target_os = "linux", target_os = "android"))]
    {
        let c_path = CString::new(whiteout_path.to_string_lossy().as_bytes())?;
        let result = unsafe {
            libc::mknod(
                c_path.as_ptr(),
                libc::S_IFCHR | 0o600,
                libc::makedev(0, 0),
            )
        };
        if result != 0 {
            let err = std::io::Error::last_os_error();
            bail!("create whiteout {}: {}", whiteout_path.display(), err);
        }
    }
    #[cfg(not(any(target_os = "linux", target_os = "android")))]
    {
        bail!("whiteout creation only supported on linux/android");
    }
    Ok(())
}

fn set_opaque_xattr(dir: &Path) -> Result<()> {
    #[cfg(any(target_os = "linux", target_os = "android"))]
    {
        use extattr::{Flags as XattrFlags, lsetxattr};
        lsetxattr(dir, "trusted.overlay.opaque", b"y", XattrFlags::empty())
            .with_context(|| format!("set opaque xattr on {}", dir.display()))?;
    }
    #[cfg(not(any(target_os = "linux", target_os = "android")))]
    {
        bail!("opaque xattr only supported on linux/android");
    }
    Ok(())
}

fn process_existing_entries(
    path: &Path,
    upper_dir: &Path,
    work_dir: &Path,
    children: &mut BTreeMap<String, Node>,
    has_overlay: bool,
) -> Result<()> {
    for entry in path.read_dir()?.flatten() {
        let name = entry.file_name().to_string_lossy().to_string();
        let result = if let Some(node) = children.remove(&name) {
            if node.skip {
                continue;
            }
            do_overlay_mount(path, upper_dir, work_dir, node, has_overlay)
                .with_context(|| format!("overlay mount {}/{name}", path.display()))
        } else if has_overlay {
            mount_mirror(path, work_dir, &entry)
                .with_context(|| format!("mount mirror {}/{name}", path.display()))
        } else {
            Ok(())
        };
        handle_mount_result(result, path, &name, has_overlay)?;
    }
    Ok(())
}

fn process_remaining_children(
    path: &Path,
    upper_dir: &Path,
    work_dir: &Path,
    children: BTreeMap<String, Node>,
    has_overlay: bool,
) -> Result<()> {
    for (name, node) in children {
        if node.skip {
            continue;
        }
        let result = do_overlay_mount(path, upper_dir, work_dir, node, has_overlay)
            .with_context(|| format!("overlay mount {}/{name}", path.display()));
        handle_mount_result(result, path, &name, has_overlay)?;
    }
    Ok(())
}

fn do_overlay_mount<P: AsRef<Path>, UP: AsRef<Path>, WP: AsRef<Path>>(
    path: P,
    upper_dir: UP,
    work_dir: WP,
    mut current: Node,
    has_overlay: bool,
) -> Result<()> {
    let path = path.as_ref().join(&current.name);
    let upper_dir = upper_dir.as_ref().join(&current.name);
    let work_dir = work_dir.as_ref().join(&current.name);

    match current.file_type {
        RegularFile => {
            if let Some(module_path) = &current.module_path {
                log::debug!(
                    "mount module file {} -> {}",
                    module_path.display(),
                    upper_dir.display()
                );
                fs::File::create(&upper_dir)?;
                mount_bind(module_path, &upper_dir)?;
            } else {
                bail!("cannot mount root file {}!", path.display());
            }
        }
        Symlink => {
            if let Some(module_path) = &current.module_path {
                log::debug!(
                    "create module symlink {} -> {}",
                    module_path.display(),
                    upper_dir.display()
                );
                clone_symlink(module_path, &upper_dir)?;
            } else {
                bail!("cannot mount root symlink {}!", path.display());
            }
        }
        Directory => {
            let create_overlay = should_create_overlay(&path, &mut current, has_overlay);
            let has_overlay = has_overlay || create_overlay;

            if has_overlay {
                prepare_overlay_dir(&path, &work_dir, current.module_path.as_ref())?;
            }

            if create_overlay {
                log::debug!(
                    "creating overlay for {} at {}",
                    path.display(),
                    work_dir.display()
                );
                let upper_for_overlay = upper_dir.clone();
                let work_for_overlay = work_dir.clone();
                mount_overlayfs(
                    &[],
                    path.to_str().unwrap_or(""),
                    Some(upper_for_overlay),
                    Some(work_for_overlay),
                    &path,
                    "overlay",
                )?;
            }

            if path.exists() && !current.replace {
                process_existing_entries(
                    &path,
                    &upper_dir,
                    &work_dir,
                    &mut current.children,
                    has_overlay,
                )?;
            }

            if current.replace {
                if current.module_path.is_none() {
                    bail!(
                        "dir {} is declared as replaced but it is root!",
                        path.display()
                    );
                }
                log::debug!("dir {} is replaced", path.display());
                set_opaque_xattr(&upper_dir)?;
            }

            process_remaining_children(&path, &upper_dir, &work_dir, current.children, has_overlay)?;
        }
        Whiteout => {
            log::debug!("file {} is removed (whiteout)", path.display());
            create_whiteout(&upper_dir.parent().unwrap_or(&path), &current.name)?;
        }
    }
    Ok(())
}

pub fn magic_mount() -> Result<()> {
    if let Some(root) = collect_module_files()? {
        log::debug!("collected: {:#?}", root);
        let tmp_dir = PathBuf::from(AP_MAGIC_MOUNT_SOURCE);
        ensure_dir_exists(&tmp_dir)?;
        mount("tmpfs", &tmp_dir, "tmpfs", MountFlags::empty(), None).context("mount tmp")?;
        mount_change(&tmp_dir, MountPropagationFlags::PRIVATE).context("make tmp private")?;

        let upper_root = tmp_dir.join("upper");
        let work_root = tmp_dir.join("work");
        create_dir_all(&upper_root)?;
        create_dir_all(&work_root)?;

        let result = do_overlay_mount("/", &upper_root, &work_root, root, false);

        if let Err(e) = unmount(&tmp_dir, UnmountFlags::DETACH) {
            log::error!("failed to unmount tmp {}", e);
        }
        fs::remove_dir_all(tmp_dir).ok();
        result
    } else {
        log::info!("no modules to mount, skipping!");
        Ok(())
    }
}