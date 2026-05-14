use std::{
    ffi::{CStr, CString},
    fs::File,
    io::{self, Read},
    process,
    sync::{Arc, Mutex},
};

use libc::{c_long, c_void, syscall, uid_t, EINVAL};
use log::{error, info, warn};

use crate::package::{read_ap_package_config, synchronize_package_uid};

const MAJOR: c_long = 0;
const MINOR: c_long = 13;
const PATCH: c_long = 1;

const KSTORAGE_EXCLUDE_LIST_GROUP: i32 = 1;
const KSTORAGE_AUTO_EXCLUDE_GROUP: i32 = 3;

const __NR_SUPERCALL: c_long = 45;
const SUPERCALL_SU: c_long = 0x1010;
const SUPERCALL_KSTORAGE_WRITE: c_long = 0x1041;
const SUPERCALL_KSTORAGE_READ: c_long = 0x1042;
const SUPERCALL_SU_GRANT_UID: c_long = 0x1100;
const SUPERCALL_SU_REVOKE_UID: c_long = 0x1101;
const SUPERCALL_SU_NUMS: c_long = 0x1102;
const SUPERCALL_SU_LIST: c_long = 0x1103;
const SUPERCALL_SU_RESET_PATH: c_long = 0x1111;
const SUPERCALL_SU_GET_SAFEMODE: c_long = 0x1112;

const SUPERCALL_KPM_LOAD: c_long = 0x1020;

const SUPERCALL_UTS_SET: c_long = 0x1050;
const SUPERCALL_UTS_RESET: c_long = 0x1051;

const SUPERCALL_PATHHIDE_ENABLE: c_long = 0x1064;
const SUPERCALL_PATHHIDE_ADD: c_long = 0x1060;
const SUPERCALL_PATHHIDE_CLEAR: c_long = 0x1063;
const SUPERCALL_PATHHIDE_UID_MODE: c_long = 0x106A;
const SUPERCALL_PATHHIDE_UID_ADD: c_long = 0x1066;
const SUPERCALL_PATHHIDE_UID_CLEAR: c_long = 0x1069;
const SUPERCALL_PATHHIDE_FILTER_SYSTEM: c_long = 0x106B;

const SUPERCALL_NETISOLATE_ENABLE: c_long = 0x1070;
const SUPERCALL_NETISOLATE_UID_ADD: c_long = 0x1072;
const SUPERCALL_NETISOLATE_UID_REMOVE: c_long = 0x1073;
const SUPERCALL_NETISOLATE_UID_LIST: c_long = 0x1074;
const SUPERCALL_NETISOLATE_UID_CLEAR: c_long = 0x1075;

const SUPERCALL_SCONTEXT_LEN: usize = 0x60;

#[repr(C)]
struct SuProfile {
    uid: i32,
    to_uid: i32,
    scontext: [u8; SUPERCALL_SCONTEXT_LEN],
}

fn ver_and_cmd(cmd: c_long) -> c_long {
    let version_code: u32 = ((MAJOR << 16) + (MINOR << 8) + PATCH).try_into().unwrap();
    ((version_code as c_long) << 32) | (0x1158 << 16) | (cmd & 0xFFFF)
}

fn sc_su_revoke_uid(key: &CStr, uid: uid_t) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_SU_REVOKE_UID),
            uid,
        ) as c_long
    }
}

fn sc_su_grant_uid(key: &CStr, profile: &SuProfile) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_SU_GRANT_UID),
            profile,
        ) as c_long
    }
}

fn sc_kstorage_write(
    key: &CStr,
    gid: i32,
    did: i64,
    data: *mut c_void,
    offset: i32,
    dlen: i32,
) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_KSTORAGE_WRITE),
            gid as c_long,
            did as c_long,
            data,
            (((offset as i64) << 32) | (dlen as i64)) as c_long,
        ) as c_long
    }
}

fn sc_kstorage_read(
    key: &CStr,
    gid: i32,
    did: i64,
    out_data: *mut c_void,
    offset: i32,
    dlen: i32,
) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_KSTORAGE_READ),
            gid as c_long,
            did as c_long,
            out_data,
            (((offset as i64) << 32) | (dlen as i64)) as c_long,
        ) as c_long
    }
}

fn sc_set_ap_mod_exclude(key: &CStr, uid: i64, exclude: i32) -> c_long {
    sc_kstorage_write(
        key,
        KSTORAGE_EXCLUDE_LIST_GROUP,
        uid,
        &exclude as *const i32 as *mut c_void,
        0,
        size_of::<i32>() as i32,
    )
}

pub fn get_new_app_profile_mode() -> i32 {
    let key = CStr::from_bytes_with_nul(b"su\0").expect("auto exclude key init failed");
    let mut enabled = 0_i32;
    let rc = sc_kstorage_read(
        key,
        KSTORAGE_AUTO_EXCLUDE_GROUP,
        0,
        &mut enabled as *mut i32 as *mut c_void,
        0,
        size_of::<i32>() as i32,
    );
    if rc < 0 {
        return 0;
    }
    enabled
}

pub fn sc_su_get_safemode(key: &CStr) -> c_long {
    if key.to_bytes().is_empty() {
        warn!("[sc_su_get_safemode] null superkey, tell apd we are not in safemode!");
        return 0;
    }

    let key_ptr = key.as_ptr();
    if key_ptr.is_null() {
        warn!("[sc_su_get_safemode] superkey pointer is null!");
        return 0;
    }

    unsafe {
        syscall(
            __NR_SUPERCALL,
            key_ptr,
            ver_and_cmd(SUPERCALL_SU_GET_SAFEMODE),
        ) as c_long
    }
}

fn sc_su(key: &CStr, profile: &SuProfile) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_SU),
            profile,
        ) as c_long
    }
}

fn sc_su_reset_path(key: &CStr, path: &CStr) -> c_long {
    if key.to_bytes().is_empty() || path.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_SU_RESET_PATH),
            path.as_ptr(),
        ) as c_long
    }
}

fn sc_kpm_load(key: &CStr, path: &CStr, args: &CStr) -> c_long {
    if key.to_bytes().is_empty() || path.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_KPM_LOAD),
            path.as_ptr(),
            args.as_ptr(),
            std::ptr::null::<c_void>(),
        ) as c_long
    }
}

fn sc_su_uid_nums(key: &CStr) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe { syscall(__NR_SUPERCALL, key.as_ptr(), ver_and_cmd(SUPERCALL_SU_NUMS)) as c_long }
}

fn sc_su_allow_uids(key: &CStr, buf: &mut [uid_t]) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    if buf.is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_SU_LIST),
            buf.as_mut_ptr(),
            buf.len() as i32,
        ) as c_long
    }
}

fn read_file_to_string(path: &str) -> io::Result<String> {
    let mut file = File::open(path)?;
    let mut content = String::new();
    file.read_to_string(&mut content)?;
    Ok(content)
}

fn convert_string_to_u8_array(s: &str) -> [u8; SUPERCALL_SCONTEXT_LEN] {
    let mut u8_array = [0u8; SUPERCALL_SCONTEXT_LEN];
    let bytes = s.as_bytes();
    let len = usize::min(SUPERCALL_SCONTEXT_LEN, bytes.len());
    u8_array[..len].copy_from_slice(&bytes[..len]);
    u8_array
}

fn convert_superkey(s: &Option<String>) -> Option<CString> {
    s.as_ref().and_then(|s| CString::new(s.clone()).ok())
}

pub fn refresh_ap_package_list(skey: &CStr, mutex: &Arc<Mutex<()>>) {
    let _lock = mutex.lock().unwrap();

    if let Err(e) = synchronize_package_uid() {
        error!("Failed to synchronize package UIDs: {}", e);
    }

    let package_configs = read_ap_package_config();

    let num = sc_su_uid_nums(skey);
    if num < 0 {
        error!("[refresh_su_list] Error getting number of UIDs: {}", num);
        return;
    }
    let num = num as usize;
    let mut uids = vec![0 as uid_t; num];
    let n = sc_su_allow_uids(skey, &mut uids);
    if n < 0 {
        error!("[refresh_su_list] Error getting su list");
        return;
    }

    let granted_uids: std::collections::HashSet<uid_t> = package_configs
        .iter()
        .filter(|c| c.allow == 1 && c.exclude == 0)
        .map(|c| c.uid as uid_t)
        .collect();

    for uid in &uids {
        if *uid == 0 || *uid == 2000 {
            continue;
        }
        if granted_uids.contains(uid) {
            continue;
        }
        info!(
            "[refresh_ap_package_list] Revoking {} root permission...",
            uid
        );
        let rc = sc_su_revoke_uid(skey, *uid);
        if rc != 0 {
            error!("[refresh_ap_package_list] Error revoking UID: {}", rc);
        }
    }

    for config in &package_configs {
        if config.allow == 1 && config.exclude == 0 {
            let profile = SuProfile {
                uid: config.uid,
                to_uid: config.to_uid,
                scontext: convert_string_to_u8_array(&config.sctx),
            };
            let result = sc_su_grant_uid(skey, &profile);
            info!(
                "[refresh_ap_package_list] Loading {}: result = {}",
                config.pkg, result
            );
        }
        if config.allow == 0 && config.exclude == 1 {
            let result = sc_set_ap_mod_exclude(skey, config.uid as i64, 1);
            info!(
                "[refresh_ap_package_list] Loading exclude {}: result = {}",
                config.pkg, result
            );
        }
    }
}

pub fn privilege_apd_profile(superkey: &Option<String>) {
    let key = convert_superkey(superkey);

    let all_allow_ctx = "u:r:magisk:s0";
    let profile = SuProfile {
        uid: process::id().try_into().expect("PID conversion failed"),
        to_uid: 0,
        scontext: convert_string_to_u8_array(all_allow_ctx),
    };
    if let Some(ref key) = key {
        let result = sc_su(key, &profile);
        info!("[privilege_apd_profile] result = {}", result);
    }
}

pub fn init_load_su_path(superkey: &Option<String>) {
    let su_path_file = "/data/adb/ap/su_path";

    match read_file_to_string(su_path_file) {
        Ok(su_path) => {
            let superkey_cstr = convert_superkey(superkey);

            match superkey_cstr {
                Some(superkey_cstr) => match CString::new(su_path.trim()) {
                    Ok(su_path_cstr) => {
                        let result = sc_su_reset_path(&superkey_cstr, &su_path_cstr);
                        if result == 0 {
                            info!("suPath load successfully");
                        } else {
                            warn!("Failed to load su path, error code: {}", result);
                        }
                    }
                    Err(e) => {
                        warn!("Failed to convert su_path: {}", e);
                    }
                },
                _ => {
                    warn!("Superkey is None, skipping...");
                }
            }
        }
        Err(e) => {
            warn!("Failed to read su_path file: {}", e);
        }
    }
}
pub fn autoload_kpm_modules(superkey: &Option<String>, event_filter: &str) {
    use serde::Deserialize;

    #[derive(Deserialize, Default)]
    struct KpmAutoLoadEntry {
        path: String,
        #[serde(default = "default_event")]
        event: String,
        #[serde(default)]
        args: String,
    }

    fn default_event() -> String {
        "service".to_string()
    }

    #[derive(Deserialize, Default)]
    struct KpmAutoLoadConfig {
        enabled: bool,
        #[serde(default, rename = "kpmEntries")]
        kpm_entries: Vec<KpmAutoLoadEntry>,
        #[serde(default, rename = "kpmPaths")]
        legacy_kpm_paths: Vec<String>,
    }

    let config_path = crate::defs::KPM_AUTOLOAD_CONFIG;
    let content = match std::fs::read_to_string(config_path) {
        Ok(c) => c,
        Err(e) => {
            info!("[kpm_autoload] config not found or unreadable ({}): {}", config_path, e);
            return;
        }
    };

    let config: KpmAutoLoadConfig = match serde_json::from_str(&content) {
        Ok(c) => c,
        Err(e) => {
            warn!("[kpm_autoload] failed to parse config: {}", e);
            return;
        }
    };

    let KpmAutoLoadConfig {
        enabled,
        mut kpm_entries,
        legacy_kpm_paths,
    } = config;

    if kpm_entries.is_empty() && !legacy_kpm_paths.is_empty() {
        info!("[kpm_autoload] using legacy kpmPaths config");
        kpm_entries = legacy_kpm_paths
            .into_iter()
            .map(|path| KpmAutoLoadEntry {
                path,
                event: default_event(),
                args: String::new(),
            })
            .collect();
    }

    if !enabled || kpm_entries.is_empty() {
        info!("[kpm_autoload] disabled or no entries configured, skipping");
        return;
    }

    let key = convert_superkey(superkey);
    let key = match key {
        Some(k) => k,
        None => {
            warn!("[kpm_autoload] no superkey available");
            return;
        }
    };

    const MAX_KPM_MODULES: usize = 64;

    if kpm_entries.len() > MAX_KPM_MODULES {
        warn!(
            "[kpm_autoload] too many entries ({}), truncating to {}",
            kpm_entries.len(),
            MAX_KPM_MODULES
        );
    }

    let mut success = 0u32;
    let mut fail = 0u32;
    for entry in kpm_entries.iter().take(MAX_KPM_MODULES) {
        if entry.event != event_filter {
            info!("[kpm_autoload] skipping '{}' (event='{}', expected='{}')", entry.path, entry.event, event_filter);
            continue;
        }

        let path_str = &entry.path;

        if !std::path::Path::new(path_str).exists() {
            warn!("[kpm_autoload] file not found: {}", path_str);
            fail += 1;
            continue;
        }

        let canonical = match std::fs::canonicalize(path_str) {
            Ok(p) => p,
            Err(e) => {
                warn!("[kpm_autoload] cannot canonicalize '{}': {}", path_str, e);
                fail += 1;
                continue;
            }
        };

        let allowed_dir = std::path::Path::new(crate::defs::FP_KPMS_AUTOLOAD_DIR);
        if !canonical.starts_with(allowed_dir) {
            warn!(
                "[kpm_autoload] path '{}' outside allowed directory '{}', skipping",
                path_str,
                crate::defs::FP_KPMS_AUTOLOAD_DIR
            );
            fail += 1;
            continue;
        }

        let path_cstr = match CString::new(canonical.to_string_lossy().into_owned()) {
            Ok(c) => c,
            Err(e) => {
                warn!("[kpm_autoload] invalid canonical path: {}", e);
                fail += 1;
                continue;
            }
        };
        let args_cstr = match CString::new(entry.args.clone()) {
            Ok(c) => c,
            Err(e) => {
                warn!("[kpm_autoload] invalid args for '{}': {}", path_str, e);
                fail += 1;
                continue;
            }
        };
        info!("[kpm_autoload] loading '{}' with event='{}' args='{}'", path_str, entry.event, entry.args);
        let rc = sc_kpm_load(&key, &path_cstr, &args_cstr);
        if rc == 0 {
            success += 1;
            info!("[kpm_autoload] loaded: {}", path_str);
        } else {
            fail += 1;
            warn!("[kpm_autoload] failed to load '{}', rc={}", path_str, rc);
        }
    }
    info!("[kpm_autoload] done: success={}, fail={}", success, fail);
}

fn sc_pathhide_enable(key: &CStr, enable: bool) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_PATHHIDE_ENABLE),
            if enable { 1i64 } else { 0i64 },
        ) as c_long
    }
}

fn sc_pathhide_add(key: &CStr, path: &CStr) -> c_long {
    if key.to_bytes().is_empty() || path.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_PATHHIDE_ADD),
            path.as_ptr(),
        ) as c_long
    }
}

fn sc_pathhide_clear(key: &CStr) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_PATHHIDE_CLEAR),
        ) as c_long
    }
}

fn sc_pathhide_uid_mode(key: &CStr, enable: bool) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_PATHHIDE_UID_MODE),
            if enable { 1i64 } else { 0i64 },
        ) as c_long
    }
}

fn sc_pathhide_filter_system(key: &CStr, enable: bool) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_PATHHIDE_FILTER_SYSTEM),
            if enable { 1i64 } else { 0i64 },
        ) as c_long
    }
}

fn sc_pathhide_uid_add(key: &CStr, uid: i32) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_PATHHIDE_UID_ADD),
            uid,
        ) as c_long
    }
}

fn sc_pathhide_uid_clear(key: &CStr) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_PATHHIDE_UID_CLEAR),
        ) as c_long
    }
}

fn sc_netisolate_enable(key: &CStr, enable: bool) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_NETISOLATE_ENABLE),
            if enable { 1i64 } else { 0i64 },
        ) as c_long
    }
}

fn sc_netisolate_uid_add(key: &CStr, uid: i32) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_NETISOLATE_UID_ADD),
            uid,
        ) as c_long
    }
}

fn sc_netisolate_uid_clear(key: &CStr) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_NETISOLATE_UID_CLEAR),
        ) as c_long
    }
}

pub fn apply_netisolate(superkey: &Option<String>) {
    if !std::path::Path::new(crate::defs::NETISOLATE_ENABLE_FILE).exists() {
        info!("[netisolate] disabled, skipping");
        return;
    }

    let key = convert_superkey(superkey);
    let key = match key {
        Some(k) => k,
        None => {
            warn!("[netisolate] no superkey available");
            return;
        }
    };

    // Step 1: Clear and populate UID blocklist (netisolate not yet enabled)
    match std::fs::read_to_string(crate::defs::NETISOLATE_UIDS_FILE) {
        Ok(uids) => {
            sc_netisolate_uid_clear(&key);
            let mut count = 0u32;
            for uid_str in uids.lines() {
                let uid_str = uid_str.trim();
                if uid_str.is_empty() {
                    continue;
                }
                match uid_str.parse::<i32>() {
                    Ok(uid) => {
                        let rc = sc_netisolate_uid_add(&key, uid);
                        if rc < 0 {
                            warn!("[netisolate] add uid {} failed: {}", uid, rc);
                        } else {
                            count += 1;
                        }
                    }
                    Err(_) => {
                        warn!("[netisolate] invalid uid: '{}'", uid_str);
                    }
                }
            }
            info!("[netisolate] {} uids restored", count);
        }
        Err(_) => {
            info!("[netisolate] no uids file");
        }
    }

    // Step 2: Enable netisolate LAST
    let rc = sc_netisolate_enable(&key, true);
    if rc < 0 {
        warn!("[netisolate] enable failed: {}", rc);
        return;
    }

    info!("[netisolate] auto-apply completed");
}

pub fn apply_pathhide(superkey: &Option<String>) {
    if !std::path::Path::new(crate::defs::PATHHIDE_ENABLE_FILE).exists() {
        info!("[pathhide] disabled, skipping");
        return;
    }

    let key = convert_superkey(superkey);
    let key = match key {
        Some(k) => k,
        None => {
            warn!("[pathhide] no superkey available");
            return;
        }
    };

    // Step 1: Clear and populate blocklist (pathhide not yet enabled, hooks are no-ops)
    match std::fs::read_to_string(crate::defs::PATHHIDE_PATHS_FILE) {
        Ok(paths) => {
            sc_pathhide_clear(&key);
            let mut count = 0u32;
            for path in paths.lines() {
                let path = path.trim();
                if path.is_empty() {
                    continue;
                }
                match CString::new(path) {
                    Ok(path_cstr) => {
                        let rc = sc_pathhide_add(&key, &path_cstr);
                        if rc < 0 {
                            warn!("[pathhide] add path '{}' failed: {}", path, rc);
                        } else {
                            count += 1;
                        }
                    }
                    Err(e) => {
                        warn!("[pathhide] invalid path '{}': {}", path, e);
                    }
                }
            }
            info!("[pathhide] {} paths restored", count);
        }
        Err(_) => {
            info!("[pathhide] no paths file, clearing blocklist");
            sc_pathhide_clear(&key);
        }
    }

    // Step 2: Configure UID whitelist BEFORE enabling (so filters are ready)
    if std::path::Path::new(crate::defs::PATHHIDE_UID_MODE_FILE).exists() {
        match std::fs::read_to_string(crate::defs::PATHHIDE_UIDS_FILE) {
            Ok(uids) => {
                sc_pathhide_uid_clear(&key);
                let mut count = 0u32;
                for uid_str in uids.lines() {
                    let uid_str = uid_str.trim();
                    if uid_str.is_empty() {
                        continue;
                    }
                    match uid_str.parse::<i32>() {
                        Ok(uid) => {
                            let rc = sc_pathhide_uid_add(&key, uid);
                            if rc < 0 {
                                warn!("[pathhide] add uid {} failed: {}", uid, rc);
                            } else {
                                count += 1;
                            }
                        }
                        Err(_) => {
                            warn!("[pathhide] invalid uid: '{}'", uid_str);
                        }
                    }
                }
                info!("[pathhide] {} uids restored", count);
            }
            Err(_) => {
                info!("[pathhide] no uids file");
            }
        }

        let rc = sc_pathhide_uid_mode(&key, true);
        if rc < 0 {
            warn!("[pathhide] uid mode enable failed: {}", rc);
        }
    }

    // Step 2.5: Configure filter_system (allow hiding from system/root UIDs)
    if std::path::Path::new(crate::defs::PATHHIDE_FILTER_SYSTEM_FILE).exists() {
        let rc = sc_pathhide_filter_system(&key, true);
        if rc < 0 {
            warn!("[pathhide] filter_system enable failed: {}", rc);
        }
    }

    // Step 3: Enable pathhide LAST (all config is now in place)
    let rc = sc_pathhide_enable(&key, true);
    if rc < 0 {
        warn!("[pathhide] enable failed: {}", rc);
        return;
    }

    info!("[pathhide] auto-apply completed");
}

fn sc_uts_set(key: &CStr, release: Option<&CStr>, version: Option<&CStr>) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    let release_ptr = match release {
        Some(r) => r.as_ptr(),
        None => std::ptr::null(),
    };
    let version_ptr = match version {
        Some(v) => v.as_ptr(),
        None => std::ptr::null(),
    };
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_UTS_SET),
            release_ptr,
            version_ptr,
        ) as c_long
    }
}

fn sc_uts_reset(key: &CStr) -> c_long {
    if key.to_bytes().is_empty() {
        return (-EINVAL).into();
    }
    unsafe {
        syscall(
            __NR_SUPERCALL,
            key.as_ptr(),
            ver_and_cmd(SUPERCALL_UTS_RESET),
        ) as c_long
    }
}

pub fn apply_uts_spoof(superkey: &Option<String>) {
    use std::path::Path;

    const MAX_BOOT_RETRIES: u32 = 3;

    if !Path::new(crate::defs::UTS_SPOOF_ENABLE_FILE).exists() {
        info!("[uts_spoof] disabled, skipping");
        return;
    }

    let config_content = match std::fs::read_to_string(crate::defs::UTS_SPOOF_CONFIG_FILE) {
        Ok(c) => c,
        Err(e) => {
            warn!("[uts_spoof] failed to read config: {}", e);
            return;
        }
    };

    let config: serde_json::Value = match serde_json::from_str(&config_content) {
        Ok(v) => v,
        Err(e) => {
            warn!("[uts_spoof] failed to parse config: {}", e);
            return;
        }
    };

    let release = config.get("release").and_then(|v| v.as_str()).unwrap_or("");
    let version = config.get("version").and_then(|v| v.as_str()).unwrap_or("");

    let key = convert_superkey(superkey);
    let key = match key {
        Some(k) => k,
        None => {
            warn!("[uts_spoof] no superkey available");
            return;
        }
    };

    let _ = sc_uts_reset(&key);

    let retries = match std::fs::read_to_string(crate::defs::UTS_SPOOF_BOOT_PENDING) {
        Ok(s) => s.trim().parse::<u32>().unwrap_or(0),
        Err(_) => 0,
    };

    if retries >= MAX_BOOT_RETRIES {
        warn!(
            "[uts_spoof] boot pending retries ({}) >= max ({}), skipping spoof to prevent bootloop",
            retries, MAX_BOOT_RETRIES
        );
        let _ = std::fs::remove_file(crate::defs::UTS_SPOOF_BOOT_PENDING);
        return;
    }

    if let Err(e) = std::fs::write(crate::defs::UTS_SPOOF_BOOT_PENDING, (retries + 1).to_string()) {
        warn!("[uts_spoof] failed to write boot pending flag: {}", e);
    }

    // Only set if we have values to spoof
    let release_cstr = if !release.is_empty() {
        match CString::new(release) {
            Ok(c) => Some(c),
            Err(e) => {
                warn!("[uts_spoof] invalid release string: {}", e);
                None
            }
        }
    } else {
        None
    };
    let version_cstr = if !version.is_empty() {
        match CString::new(version) {
            Ok(c) => Some(c),
            Err(e) => {
                warn!("[uts_spoof] invalid version string: {}", e);
                None
            }
        }
    } else {
        None
    };

    if release_cstr.is_some() || version_cstr.is_some() {
        let rc = sc_uts_set(
            &key,
            release_cstr.as_deref(),
            version_cstr.as_deref(),
        );
        if rc == 0 {
            info!("[uts_spoof] applied: release='{}' version='{}'", release, version);
        } else {
            warn!("[uts_spoof] set failed: {}", rc);
        }
    } else {
        info!("[uts_spoof] config has empty values, skipping set");
    }
}
