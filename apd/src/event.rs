use crate::sepolicy::get_policy_main;
use anyhow::{Context, Result};
use libc::SIGPWR;
use log::{info, warn};
use notify::{
    event::{ModifyKind, RenameMode},
    Config, Event, EventKind, INotifyWatcher, RecursiveMode, Watcher,
};
use signal_hook::{consts::signal::*, iterator::Signals};
use std::process::Stdio;
use std::{
    env,
    ffi::CStr,
    fs,
    os::unix::{fs::PermissionsExt, process::CommandExt},
    path::{Path, PathBuf},
    process::Command,
    sync::{Arc, Mutex},
    thread,
    time::Duration,
};

use crate::{
    assets, defs, lua, metamodule, module, restorecon, supercall,
    supercall::{init_load_su_path, refresh_ap_package_list},
    utils::{self, switch_cgroups},
};

pub fn report_kernel(superkey: Option<String>, event: &str, state: &str) -> Result<()> {
    let args = vec![
        superkey.unwrap_or_default(),
        "event".to_string(),
        event.to_string(),
        state.to_string(),
    ];
    let args_ref: Vec<&str> = args.iter().map(|s| s.as_str()).collect();
    let _ = utils::run_command("truncate", &args_ref, None)?.wait()?;
    Ok(())
}

pub fn on_post_data_fs(superkey: Option<String>) -> Result<()> {
    utils::umask(0);
    report_kernel(superkey.clone(), "post-fs-data", "before")?;

    // Create /data/adb/fp/bin directory for fpd binary
    let fp_dir = Path::new("/data/adb/fp");
    let fp_bin_dir = Path::new("/data/adb/fp/bin");
    let fp_kpms_dir = Path::new(defs::FP_KPMS_DIR);
    let fp_kpms_autoload_dir = Path::new(defs::FP_KPMS_AUTOLOAD_DIR);
    if !fp_bin_dir.exists() {
        fs::create_dir_all(fp_bin_dir)
            .with_context(|| "Failed to create /data/adb/fp/bin directory")?;
        let permissions = fs::Permissions::from_mode(0o755);
        fs::set_permissions(fp_dir, permissions.clone())
            .with_context(|| "Failed to set permissions for /data/adb/fp")?;
        fs::set_permissions(fp_bin_dir, permissions)
            .with_context(|| "Failed to set permissions for /data/adb/fp/bin")?;
        info!("Created directory: /data/adb/fp/bin");
    }
    if !fp_kpms_autoload_dir.exists() {
        fs::create_dir_all(fp_kpms_autoload_dir)
            .with_context(|| "Failed to create /data/adb/fp/kpms/autoload directory")?;
        let permissions = fs::Permissions::from_mode(0o755);
        fs::set_permissions(fp_kpms_dir, permissions.clone())
            .with_context(|| "Failed to set permissions for /data/adb/fp/kpms")?;
        fs::set_permissions(fp_kpms_autoload_dir, permissions)
            .with_context(|| "Failed to set permissions for /data/adb/fp/kpms/autoload")?;
        info!("Created directory: /data/adb/fp/kpms/autoload");
    }

    init_load_su_path(&superkey);

    let mut sepol = get_policy_main(&["magiskpolicy".to_string(), "--live".to_string()])?;
    sepol.magisk_rules();
    sepol
        .to_file("/sys/fs/selinux/load")
        .context("Cannot apply policy")?;

    info!("Re-privilege apd profile after injecting sepolicy");
    supercall::privilege_apd_profile(&superkey);

    if utils::has_magisk() {
        warn!("Magisk detected, skip post-fs-data!");
        report_kernel(superkey.clone(), "post-fs-data", "after")?;
        return Ok(());
    }

    // Create log environment
    if !Path::new(defs::APATCH_LOG_FOLDER).exists() {
        fs::create_dir(defs::APATCH_LOG_FOLDER).expect("Failed to create log folder");
        let permissions = fs::Permissions::from_mode(0o700);
        fs::set_permissions(defs::APATCH_LOG_FOLDER, permissions)
            .expect("Failed to set permissions");
    }
    let command_string = format!(
        "cd {}; rm -f *.last; [ -f dmesg.log ] && mv dmesg.log dmesg.last; [ -f logcat.log ] && mv logcat.log logcat.last; [ -f locat.log ] && mv locat.log logcat.last; rm -f *.log *.old.log",
        defs::APATCH_LOG_FOLDER
    );
    let mut args = vec!["-c", &command_string];
    // for all file to .old
    let result = utils::run_command("sh", &args, None)?.wait()?;
    if result.success() {
        info!("Successfully rotated logs.");
    } else {
        info!("Failed to rotate logs.");
    }
    let logcat_path = format!("{}logcat.log", defs::APATCH_LOG_FOLDER);
    let dmesg_path = format!("{}dmesg.log", defs::APATCH_LOG_FOLDER);
    let bootlog = fs::File::create(dmesg_path)?;
    args = vec![
        "-s",
        "9",
        "45s",
        "logcat",
        "-b",
        "main,system,crash",
        "DrmLibFs:S",
        "-f",
        &logcat_path,
        "logcatcher-bootlog:S",
        "&",
    ];
    let _ = unsafe {
        Command::new("timeout")
            .process_group(0)
            .pre_exec(|| {
                switch_cgroups();
                Ok(())
            })
            .args(args)
            .spawn()
    };
    args = vec!["-s", "9", "120s", "dmesg", "-w"];
    let _result = unsafe {
        Command::new("timeout")
            .process_group(0)
            .pre_exec(|| {
                switch_cgroups();
                Ok(())
            })
            .args(args)
            .stdout(Stdio::from(bootlog))
            .spawn()
    };

    let key = "KERNELPATCH_VERSION";
    match env::var(key) {
        Ok(value) => println!("{}: {}", key, value),
        Err(_) => println!("{} not found", key),
    }

    let key = "KERNEL_VERSION";
    match env::var(key) {
        Ok(value) => println!("{}: {}", key, value),
        Err(_) => println!("{} not found", key),
    }

    let safe_mode = utils::is_safe_mode(superkey.clone());

    if safe_mode {
        // we should still mount modules.img to `/data/adb/modules` in safe mode
        // becuase we may need to operate the module dir in safe mode
        warn!("safe mode, skip common post-fs-data.d scripts");
        if let Err(e) = module::disable_all_modules() {
            warn!("disable all modules failed: {}", e);
        }
    } else {
        // Then exec common post-fs-data scripts
        if let Err(e) = module::exec_common_scripts("post-fs-data.d", true) {
            warn!("exec common post-fs-data scripts failed: {}", e);
        }
    }
    let module_update_dir = defs::MODULE_UPDATE_DIR; //save module place
    let module_dir = defs::MODULE_DIR; // run modules place
    let module_update_flag = Path::new(defs::WORKING_DIR).join(defs::UPDATE_FILE_NAME); // if update ,there will be renewed modules file
    assets::ensure_binaries().with_context(|| "binary missing")?;

    if Path::new(defs::MODULE_UPDATE_DIR).exists() {
        module::handle_updated_modules()?;
        fs::remove_dir_all(module_update_dir)?;
    }

    if safe_mode {
        warn!("safe mode, skip post-fs-data scripts and disable all modules!");
        if let Err(e) = module::disable_all_modules() {
            warn!("disable all modules failed: {}", e);
        }
        return Ok(());
    }

    if let Err(e) = module::prune_modules() {
        warn!("prune modules failed: {}", e);
    }

    if let Err(e) = restorecon::restorecon() {
        warn!("restorecon failed: {}", e);
    }

    // load sepolicy.rule
    if module::load_sepolicy_rule().is_err() {
        warn!("load sepolicy.rule failed");
    }
    if Path::new(defs::MAGIC_MOUNT_FILE).exists() {
        info!("Magic Mount mode enabled");
        if let Err(e) = crate::magic_mount::magic_mount() {
            log::error!("Magic Mount failed: {}", e);
        }
    } else {
        info!("Magic Mount disabled");
        if let Err(e) = metamodule::exec_mount_script(module_dir) {
            warn!("execute metamodule mount failed: {e}");
        }
    }

    // Execute Hide Service if enabled
    if Path::new(defs::HIDE_SERVICE_FILE).exists() {
        info!("Hide Service enabled, executing fpd -hide...");
        if Path::new(defs::HIDE_BINARY_PATH).exists() {
            let result = Command::new(defs::HIDE_BINARY_PATH).arg("-hide").status();
            match result {
                Ok(status) => {
                    if status.success() {
                        info!("fpd -hide executed successfully");
                    } else {
                        warn!("fpd -hide exited with status: {:?}", status.code());
                    }
                }
                Err(e) => {
                    warn!("Failed to execute fpd -hide: {}", e);
                }
            }
        } else {
            warn!(
                "fpd binary not found at {}, please copy it manually",
                defs::HIDE_BINARY_PATH
            );
        }
    } else {
        info!("Hide Service disabled");
    }

    // exec modules post-fs-data scripts
    // TODO: Add timeout
    if let Err(e) = module::exec_stage_script("post-fs-data", true) {
        warn!("exec post-fs-data scripts failed: {}", e);
    }
    if let Err(e) = lua::exec_stage_lua("post-fs-data", true, superkey.as_deref().unwrap_or("")) {
        warn!("Failed to exec post-fs-data lua: {}", e);
    }
    // load system.prop
    if let Err(e) = module::load_system_prop() {
        warn!("load system.prop failed: {}", e);
    }

    info!("remove update flag");
    let _ = fs::remove_file(module_update_flag);

    run_stage("post-mount", superkey.clone(), true);

    report_kernel(superkey, "post-fs-data", "after")?;

    env::set_current_dir("/").with_context(|| "failed to chdir to /")?;

    Ok(())
}

fn run_stage(stage: &str, superkey: Option<String>, block: bool) {
    utils::umask(0);

    if utils::has_magisk() {
        warn!("Magisk detected, skip {stage}");
        return;
    }

    if utils::is_safe_mode(superkey.clone()) {
        warn!("safe mode, skip {stage} scripts");
        if let Err(e) = module::disable_all_modules() {
            warn!("disable all modules failed: {}", e);
        }
        return;
    }

    // execute metamodule stage script first (priority)
    if let Err(e) = metamodule::exec_stage_script(stage, block) {
        warn!("Failed to exec metamodule {stage} script: {e}");
    }

    if let Err(e) = module::exec_common_scripts(&format!("{stage}.d"), block) {
        warn!("Failed to exec common {stage} scripts: {e}");
    }
    if let Err(e) = module::exec_stage_script(stage, block) {
        warn!("Failed to exec {stage} scripts: {e}");
    }
    if let Err(e) = lua::exec_stage_lua(stage, block, superkey.as_deref().unwrap_or("")) {
        warn!("Failed to exec {stage} lua: {e}");
    }
}

pub fn on_services(superkey: Option<String>) -> Result<()> {
    info!("on_services triggered!");

    supercall::autoload_kpm_modules(&superkey);

    run_stage("service", superkey, false);

    Ok(())
}

fn run_uid_monitor() {
    info!("Trigger run_uid_monitor!");

    let mut command = &mut Command::new("/data/adb/apd");
    {
        command = command.process_group(0);
        command = unsafe {
            command.pre_exec(|| {
                // ignore the error?
                switch_cgroups();
                Ok(())
            })
        };
    }
    command = command.arg("uid-listener");

    command
        .spawn()
        .map(|_| ())
        .expect("[run_uid_monitor] Failed to run uid monitor");
}

pub fn on_boot_completed(superkey: Option<String>) -> Result<()> {
    info!("on_boot_completed triggered!");

    run_stage("boot-completed", superkey, false);

    // Execute Umount Service if enabled
    // Run at boot-completed (latest possible stage) to ensure all mount
    // points — including those created by system_server and Zygote hooks
    // (e.g. ReZygisk module.prop bind mounts) — are fully established.
    if Path::new(defs::UMOUNT_SERVICE_FILE).exists() {
        info!("Umount Service enabled, executing fpd -umount...");
        if Path::new(defs::UMOUNT_BINARY_PATH).exists() {
            let result = unsafe {
                Command::new(defs::UMOUNT_BINARY_PATH)
                    .arg("-umount")
                    .stdout(Stdio::piped())
                    .stderr(Stdio::piped())
                    .pre_exec(|| {
                        let _ = utils::switch_mnt_ns(1);
                        Ok(())
                    })
                    .output()
            };
            match result {
                Ok(output) => {
                    let stdout = String::from_utf8_lossy(&output.stdout);
                    let stderr = String::from_utf8_lossy(&output.stderr);
                    if output.status.success() {
                        info!("fpd -umount executed successfully");
                    } else {
                        warn!("fpd -umount exited with status: {:?}", output.status.code());
                    }
                    if !stdout.trim().is_empty() {
                        info!("fpd -umount stdout: {}", stdout.trim());
                    }
                    if !stderr.trim().is_empty() {
                        info!("fpd -umount stderr: {}", stderr.trim());
                    }
                }
                Err(e) => {
                    warn!("Failed to execute fpd -umount: {}", e);
                }
            }
        } else {
            warn!(
                "fpd binary not found at {}, please copy it manually",
                defs::UMOUNT_BINARY_PATH
            );
        }
    } else {
        info!("Umount Service disabled");
    }

    run_uid_monitor();
    Ok(())
}

pub fn start_uid_listener() -> Result<()> {
    info!("start_uid_listener triggered!");
    println!("[start_uid_listener] Registering...");

    // create inotify instance
    const SYS_PACKAGES_LIST_TMP: &str = "/data/system/packages.list.tmp";
    let sys_packages_list_tmp = PathBuf::from(&SYS_PACKAGES_LIST_TMP);
    let dir: PathBuf = sys_packages_list_tmp.parent().unwrap().into();

    let (tx, rx) = std::sync::mpsc::channel();
    let tx_clone = tx.clone();
    let mutex = Arc::new(Mutex::new(()));

    {
        let mutex_clone = mutex.clone();
        thread::spawn(move || {
            let mut signals = Signals::new(&[SIGTERM, SIGINT, SIGPWR]).unwrap();
            for sig in signals.forever() {
                log::warn!("[shutdown] Caught signal {sig}, refreshing package list...");
                let skey = CStr::from_bytes_with_nul(b"su\0")
                    .expect("[shutdown_listener] CStr::from_bytes_with_nul failed");
                refresh_ap_package_list(&skey, &mutex_clone);
                break; // 执行一次后退出线程
            }
        });
    }

    let mut watcher = INotifyWatcher::new(
        move |ev: notify::Result<Event>| match ev {
            Ok(Event {
                kind: EventKind::Modify(ModifyKind::Name(RenameMode::Both)),
                paths,
                ..
            }) => {
                if paths.contains(&sys_packages_list_tmp) {
                    info!("[uid_monitor] System packages list changed, sending to tx...");
                    tx_clone.send(false).unwrap()
                }
            }
            Err(err) => warn!("inotify error: {err}"),
            _ => (),
        },
        Config::default(),
    )?;

    watcher.watch(dir.as_ref(), RecursiveMode::NonRecursive)?;

    let mut debounce = false;
    while let Ok(delayed) = rx.recv() {
        if delayed {
            debounce = false;
            let skey = CStr::from_bytes_with_nul(b"su\0")
                .expect("[start_uid_listener] CStr::from_bytes_with_nul failed");
            refresh_ap_package_list(&skey, &mutex);
        } else if !debounce {
            thread::sleep(Duration::from_secs(1));
            debounce = true;
            tx.send(true)?;
        }
    }

    Ok(())
}
