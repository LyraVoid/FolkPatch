use std::{
    cmp::PartialEq,
    collections::{HashMap, hash_map::Entry},
    fs,
    fs::{DirEntry, FileType, create_dir, create_dir_all, read_dir, read_link},
    os::unix::fs::{FileTypeExt, symlink},
    path::{Path, PathBuf},
};

use anyhow::{Context, Result, bail};
use extattr::lgetxattr;
use rustix::{
    fs::{Gid, MetadataExt, Mode, Uid, chmod, chown},
    mount::{
        MountFlags, MountPropagationFlags, UnmountFlags, mount, mount_bind, mount_change,
        mount_move, unmount,
    },
};

use crate::{
    defs::{
        AP_MAGIC_MOUNT_SOURCE, DISABLE_FILE_NAME, MODULE_DIR, REMOVE_FILE_NAME,
        SKIP_MOUNT_FILE_NAME,
    },
    magic_mount::NodeFileType::{Directory, RegularFile, Symlink, Whiteout},
    restorecon::{lgetfilecon, lsetfilecon},
    utils::ensure_dir_exists,
};

const REPLACE_DIR_FILE_NAME: &str = ".replace";
const REPLACE_DIR_XATTR: &str = "trusted.overlay.opaque";

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

    /// Check if mounting this node type over `real_path` requires a tmpfs overlay
    /// due to type mismatch or missing file.
    fn needs_tmpfs_vs_real(&self, real_path: &Path) -> bool {
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
    children: HashMap<String, Node>,
    // the module that owned this node
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
                Entry::Occupied(o) => Some(o.into_mut()),
                Entry::Vacant(v) => Self::new_module(&name, &entry).map(|it| v.insert(it)),
            };

            if let Some(node) = node {
                has_file |= if node.file_type == NodeFileType::Directory {
                    node.collect_module_files(dir.join(&node.name))? || node.replace
                } else {
                    true
                }
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
                children: HashMap::default(),
                module_path: Some(path),
                replace,
                skip: false,
            });
        }

        None
    }
}

fn collect_module_files() -> Result<Option<Node>> {
    let mut root = Node::new_root("");
    let mut system = Node::new_root("system");
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

        let mod_system = entry.path().join("system");

        if !mod_system.is_dir() {
            continue;
        }

        log::debug!("collecting {}", entry.path().display());

        has_file |= system.collect_module_files(mod_system)?;
    }

    if has_file {
        const BUILTIN_PARTITIONS: [(&str, bool); 5] = [
            ("vendor", true),
            ("system_ext", true),
            ("product", true),
            ("odm", false),
            ("oem", false),
        ];

        for (partition, require_symlink) in BUILTIN_PARTITIONS {
            let path_of_root = Path::new("/").join(partition);
            let path_of_system = Path::new("/system").join(partition);
            if path_of_root.is_dir() && (!require_symlink || path_of_system.is_symlink()) {
                let name = partition.to_string();
                if let Some(node) = system.children.remove(&name) {
                    root.children.insert(name, node);
                }
            }
        }

        root.children.insert("system".to_string(), system);
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

fn should_create_tmpfs(path: &Path, current: &mut Node, has_tmpfs: bool) -> bool {
    if has_tmpfs {
        return false;
    }
    if current.replace && current.module_path.is_some() {
        return true;
    }
    for (name, node) in &mut current.children {
        let real_path = path.join(name);
        if node.file_type.needs_tmpfs_vs_real(&real_path) {
            if current.module_path.is_none() {
                log::error!("cannot create tmpfs on {}, ignore: {name}", path.display());
                node.skip = true;
                continue;
            }
            return true;
        }
    }
    false
}

fn prepare_tmpfs_skeleton(
    path: &Path,
    work_dir_path: &Path,
    module_path: Option<&PathBuf>,
) -> Result<()> {
    log::debug!(
        "creating tmpfs skeleton for {} at {}",
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

fn handle_mount_result(result: Result<()>, path: &Path, name: &str, has_tmpfs: bool) -> Result<()> {
    if let Err(e) = result {
        if has_tmpfs {
            return Err(e);
        }
        log::error!("mount child {}/{} failed: {}", path.display(), name, e);
    }
    Ok(())
}

fn process_existing_entries(
    path: &Path,
    work_dir_path: &Path,
    children: &mut HashMap<String, Node>,
    has_tmpfs: bool,
) -> Result<()> {
    for entry in path.read_dir()?.flatten() {
        let name = entry.file_name().to_string_lossy().to_string();
        let result = if let Some(node) = children.remove(&name) {
            if node.skip {
                continue;
            }
            do_magic_mount(path, work_dir_path, node, has_tmpfs)
                .with_context(|| format!("magic mount {}/{name}", path.display()))
        } else if has_tmpfs {
            mount_mirror(path, work_dir_path, &entry)
                .with_context(|| format!("mount mirror {}/{name}", path.display()))
        } else {
            Ok(())
        };
        handle_mount_result(result, path, &name, has_tmpfs)?;
    }
    Ok(())
}

fn process_remaining_children(
    path: &Path,
    work_dir_path: &Path,
    children: HashMap<String, Node>,
    has_tmpfs: bool,
) -> Result<()> {
    for (name, node) in children {
        if node.skip {
            continue;
        }
        let result = do_magic_mount(path, work_dir_path, node, has_tmpfs)
            .with_context(|| format!("magic mount {}/{name}", path.display()));
        handle_mount_result(result, path, &name, has_tmpfs)?;
    }
    Ok(())
}

fn move_tmpfs_to_target(work_dir_path: &Path, target: &Path) -> Result<()> {
    log::debug!(
        "moving tmpfs {} -> {}",
        work_dir_path.display(),
        target.display()
    );
    mount_move(work_dir_path, target).context("move self")?;
    mount_change(target, MountPropagationFlags::PRIVATE).context("make self private")?;
    Ok(())
}

fn do_magic_mount<P: AsRef<Path>, WP: AsRef<Path>>(
    path: P,
    work_dir_path: WP,
    mut current: Node,
    has_tmpfs: bool,
) -> Result<()> {
    let path = path.as_ref().join(&current.name);
    let work_dir_path = work_dir_path.as_ref().join(&current.name);
    match current.file_type {
        RegularFile => {
            let target_path = if has_tmpfs {
                fs::File::create(&work_dir_path)?;
                &work_dir_path
            } else {
                &path
            };
            if let Some(module_path) = &current.module_path {
                log::debug!(
                    "mount module file {} -> {}",
                    module_path.display(),
                    work_dir_path.display()
                );
                mount_bind(module_path, target_path)?;
            } else {
                bail!("cannot mount root file {}!", path.display());
            }
        }
        Symlink => {
            if let Some(module_path) = &current.module_path {
                log::debug!(
                    "create module symlink {} -> {}",
                    module_path.display(),
                    work_dir_path.display()
                );
                clone_symlink(module_path, &work_dir_path)?;
            } else {
                bail!("cannot mount root symlink {}!", path.display());
            }
        }
        Directory => {
            let create_tmpfs = should_create_tmpfs(&path, &mut current, has_tmpfs);
            let has_tmpfs = has_tmpfs || create_tmpfs;

            if has_tmpfs {
                prepare_tmpfs_skeleton(&path, &work_dir_path, current.module_path.as_ref())?;
            }
            if create_tmpfs {
                log::debug!(
                    "creating tmpfs for {} at {}",
                    path.display(),
                    work_dir_path.display()
                );
                mount_bind(&work_dir_path, &work_dir_path).context("bind self")?;
            }
            if path.exists() && !current.replace {
                process_existing_entries(
                    &path,
                    &work_dir_path,
                    &mut current.children,
                    has_tmpfs,
                )?;
            }
            if current.replace {
                if current.module_path.is_none() {
                    bail!("dir {} is declared as replaced but it is root!", path.display());
                }
                log::debug!("dir {} is replaced", path.display());
            }
            process_remaining_children(&path, &work_dir_path, current.children, has_tmpfs)?;
            if create_tmpfs {
                move_tmpfs_to_target(&work_dir_path, &path)?;
            }
        }
        Whiteout => {
            log::debug!("file {} is removed", path.display());
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
        let result = do_magic_mount("/", &tmp_dir, root, false);
        if let Err(e) = unmount(&tmp_dir, UnmountFlags::DETACH) {
            log::error!("failed to unmount tmp {}", e);
        }
        fs::remove_dir(tmp_dir).ok();
        result
    } else {
        log::info!("no modules to mount, skipping!");
        Ok(())
    }
}
