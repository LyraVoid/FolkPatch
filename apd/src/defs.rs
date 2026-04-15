use const_format::concatcp;

pub const ADB_DIR: &str = "/data/adb/";
pub const WORKING_DIR: &str = concatcp!(ADB_DIR, "ap/");
pub const BINARY_DIR: &str = concatcp!(WORKING_DIR, "bin/");
pub const APATCH_LOG_FOLDER: &str = concatcp!(WORKING_DIR, "log/");

pub const AP_RC_PATH: &str = concatcp!(WORKING_DIR, ".aprc");
pub const GLOBAL_NAMESPACE_FILE: &str = concatcp!(ADB_DIR, ".global_namespace_enable");
pub const MAGIC_MOUNT_FILE: &str = concatcp!(ADB_DIR, ".magic_mount_enable");
pub const HIDE_SERVICE_FILE: &str = concatcp!(ADB_DIR, ".hide_service_enable");
pub const HIDE_BINARY_PATH: &str = concatcp!(ADB_DIR, "fp/bin/fpd");
pub const UMOUNT_SERVICE_FILE: &str = concatcp!(ADB_DIR, ".umount_service_enable");
pub const UMOUNT_BINARY_PATH: &str = concatcp!(ADB_DIR, "fp/bin/fpd");
pub const DAEMON_PATH: &str = concatcp!(ADB_DIR, "apd");

pub const MODULE_DIR: &str = concatcp!(ADB_DIR, "modules/");
pub const AP_MAGIC_MOUNT_SOURCE: &str = concatcp!(WORKING_DIR, "magic_mount");

// warning: this directory should not change, or you need to change the code in module_installer.sh!!!
pub const MODULE_UPDATE_DIR: &str = concatcp!(ADB_DIR, "modules_update/");

pub const TEMP_DIR: &str = "/debug_ramdisk";
pub const TEMP_DIR_LEGACY: &str = "/sbin";

pub const MODULE_WEB_DIR: &str = "webroot";
pub const MODULE_ACTION_SH: &str = "action.sh";
pub const DISABLE_FILE_NAME: &str = "disable";
pub const SKIP_MOUNT_FILE_NAME: &str = "skip_mount";
pub const UPDATE_FILE_NAME: &str = "update";
pub const REMOVE_FILE_NAME: &str = "remove";

// Metamodule support
pub const METAMODULE_MOUNT_SCRIPT: &str = "metamount.sh";
pub const METAMODULE_METAINSTALL_SCRIPT: &str = "metainstall.sh";
pub const METAMODULE_METAUNINSTALL_SCRIPT: &str = "metauninstall.sh";
pub const METAMODULE_DIR: &str = concatcp!(ADB_DIR, "metamodule/");

pub const FP_KPMS_DIR: &str = concatcp!(ADB_DIR, "fp/kpms/");
pub const FP_KPMS_AUTOLOAD_DIR: &str = concatcp!(ADB_DIR, "fp/kpms/autoload/");
pub const KPM_AUTOLOAD_CONFIG: &str = concatcp!(ADB_DIR, "fp/kpms/kpm_autoload_config.json");

pub const PTS_NAME: &str = "pts";

pub const VERSION_CODE: &str = include_str!(concat!(env!("OUT_DIR"), "/VERSION_CODE"));
pub const VERSION_NAME: &str = include_str!(concat!(env!("OUT_DIR"), "/VERSION_NAME"));
