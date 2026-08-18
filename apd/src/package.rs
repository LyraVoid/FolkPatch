use std::{
    collections::{HashMap, HashSet},
    fs::File,
    io::{self, BufRead},
    path::Path,
    process::Command,
    thread,
    time::Duration,
};

use log::{info, warn};
use serde::{Deserialize, Serialize};

use crate::defs;

const DEFAULT_SCONTEXT: &str = "u:r:untrusted_app:s0";
const MAGISK_SCONTEXT: &str = "u:r:magisk:s0";

#[derive(Deserialize, Serialize, Clone)]
pub struct PackageConfig {
    pub pkg: String,
    pub exclude: i32,
    pub allow: i32,
    pub uid: i32,
    pub to_uid: i32,
    pub sctx: String,
}

fn read_known_user_packages() -> HashSet<String> {
    std::fs::read_to_string(defs::AUTO_EXCLUDE_KNOWN_PACKAGES_FILE)
        .map(|content| {
            content
                .lines()
                .map(str::trim)
                .filter(|line| !line.is_empty())
                .map(ToOwned::to_owned)
                .collect()
        })
        .unwrap_or_default()
}

fn write_known_user_packages(packages: &HashSet<String>) -> io::Result<()> {
    let mut sorted_packages: Vec<_> = packages.iter().cloned().collect();
    sorted_packages.sort();
    let mut content = sorted_packages.join("\n");
    if !content.is_empty() {
        content.push('\n');
    }
    std::fs::write(defs::AUTO_EXCLUDE_KNOWN_PACKAGES_FILE, content)
}

fn manager_package() -> Option<String> {
    std::fs::read_to_string(defs::MANAGER_PACKAGE_FILE)
        .ok()
        .map(|pkg| pkg.trim().to_owned())
        .filter(|pkg| !pkg.is_empty())
}

fn list_user_packages() -> HashSet<String> {
    let commands: [(&str, &[&str]); 2] = [
        ("cmd", &["package", "list", "packages", "-3"]),
        ("pm", &["list", "packages", "-3"]),
    ];

    for (program, args) in commands {
        let output = match Command::new(program).args(args).output() {
            Ok(output) if output.status.success() => output,
            Ok(output) => {
                warn!("User package query {} {:?} failed: {:?}", program, args, output.status.code());
                continue;
            }
            Err(e) => {
                warn!("User package query {} {:?} failed: {}", program, args, e);
                continue;
            }
        };

        return String::from_utf8_lossy(&output.stdout)
            .lines()
            .filter_map(|line| line.strip_prefix("package:"))
            .map(str::trim)
            .filter(|pkg| !pkg.is_empty())
            .map(ToOwned::to_owned)
            .collect();
    }

    HashSet::new()
}

pub fn sync_auto_exclude_new_apps(
    package_configs: &mut Vec<PackageConfig>,
    uid_map: &HashMap<String, i32>,
    mode: i32,
) -> io::Result<bool> {
    let current_user_packages = list_user_packages();
    if current_user_packages.is_empty() {
        return Ok(false);
    }

    let known_user_packages = read_known_user_packages();
    let known_initialized = Path::new(defs::AUTO_EXCLUDE_KNOWN_PACKAGES_FILE).exists();
    let manager_pkg = manager_package();
    let mut changed = false;

    if mode != 0 && known_initialized {
        let new_packages: Vec<_> = current_user_packages
            .difference(&known_user_packages)
            .cloned()
            .collect();

        for pkg in new_packages {
            if manager_pkg.as_deref() == Some(pkg.as_str()) {
                info!("[auto_exclude] Skipping manager package {}", pkg);
                continue;
            }
            let Some(&uid) = uid_map.get(&pkg) else {
                warn!("[auto_exclude] Missing uid for package {}, skip", pkg);
                continue;
            };

            let exists = package_configs.iter().any(|config| config.pkg == pkg || config.uid == uid);
            if exists {
                continue;
            }

            let (allow, exclude, sctx, mode_name) = match mode {
                1 => (1, 0, MAGISK_SCONTEXT.to_string(), "root"),
                2 => (0, 1, DEFAULT_SCONTEXT.to_string(), "exclude"),
                _ => continue,
            };

            info!(
                "[new_app_profile] New package detected, apply {} by default: {} ({})",
                mode_name, pkg, uid
            );
            package_configs.push(PackageConfig {
                pkg,
                exclude,
                allow,
                uid,
                to_uid: 0,
                sctx,
            });
            changed = true;
        }
    }

    write_known_user_packages(&current_user_packages)?;
    Ok(changed || !known_initialized)
}

pub fn read_ap_package_config() -> Vec<PackageConfig> {
    let max_retry = 5;
    for _ in 0..max_retry {
        let file = match File::open("/data/adb/ap/package_config") {
            Ok(file) => file,
            Err(e) => {
                warn!("Error opening file: {}", e);
                thread::sleep(Duration::from_secs(1));
                continue;
            }
        };

        let mut reader = csv::Reader::from_reader(file);
        let mut package_configs = Vec::new();
        let mut success = true;

        for record in reader.deserialize() {
            match record {
                Ok(config) => package_configs.push(config),
                Err(e) => {
                    warn!("Error deserializing record: {}", e);
                    success = false;
                    break;
                }
            }
        }

        if success {
            return package_configs;
        }
        thread::sleep(Duration::from_secs(1));
    }
    Vec::new()
}

pub fn write_ap_package_config(package_configs: &[PackageConfig]) -> io::Result<()> {
    let max_retry = 5;
    for _ in 0..max_retry {
        let temp_path = "/data/adb/ap/package_config.tmp";
        let file = match File::create(temp_path) {
            Ok(file) => file,
            Err(e) => {
                warn!("Error creating temp file: {}", e);
                thread::sleep(Duration::from_secs(1));
                continue;
            }
        };

        let mut writer = csv::Writer::from_writer(file);
        let mut success = true;

        for config in package_configs {
            if let Err(e) = writer.serialize(config) {
                warn!("Error serializing record: {}", e);
                success = false;
                break;
            }
        }

        if !success {
            thread::sleep(Duration::from_secs(1));
            continue;
        }

        if let Err(e) = writer.flush() {
            warn!("Error flushing writer: {}", e);
            thread::sleep(Duration::from_secs(1));
            continue;
        }

        if let Err(e) = std::fs::rename(temp_path, "/data/adb/ap/package_config") {
            warn!("Error renaming temp file: {}", e);
            thread::sleep(Duration::from_secs(1));
            continue;
        }
        return Ok(());
    }
    Err(io::Error::other("Failed after max retries"))
}

fn read_lines<P>(filename: P) -> io::Result<io::Lines<io::BufReader<File>>>
where
    P: AsRef<Path>,
{
    File::open(filename).map(|file| io::BufReader::new(file).lines())
}

pub fn synchronize_package_uid() -> io::Result<()> {
    info!("[synchronize_package_uid] Start synchronizing root list with system packages...");

    let max_retry = 5;
    for _ in 0..max_retry {
        match read_lines("/data/system/packages.list") {
            Ok(lines) => {
                // Skip bad lines instead of `map_while(Result::ok)`: truncating
                // at the first error would drop the tail of the list, and the
                // retain() below would then revoke grants for those packages.
                #[allow(clippy::lines_filter_map_ok)]
                let lines: Vec<_> = lines.filter_map(|line| line.ok()).collect();

                let mut package_configs = read_ap_package_config();
                let uid_map: HashMap<String, i32> = lines
                    .iter()
                    .filter_map(|line| {
                        let words: Vec<&str> = line.split_whitespace().collect();
                        if words.len() < 2 {
                            return None;
                        }
                        words[1]
                            .parse::<i32>()
                            .ok()
                            .map(|uid| (words[0].to_string(), uid))
                    })
                    .collect();

                let system_packages: Vec<String> = lines
                    .iter()
                    .filter_map(|line| line.split_whitespace().next())
                    .map(|pkg| pkg.to_string())
                    .collect();

                let original_len = package_configs.len();
                package_configs.retain(|config| system_packages.contains(&config.pkg));
                let removed_count = original_len - package_configs.len();

                if removed_count > 0 {
                    info!(
                        "Removed {} uninstalled package configurations",
                        removed_count
                    );
                }

                let mut updated = false;

                let new_app_profile_mode = crate::supercall::get_new_app_profile_mode();
                if sync_auto_exclude_new_apps(&mut package_configs, &uid_map, new_app_profile_mode)? {
                    updated = true;
                }

                for line in &lines {
                    let words: Vec<&str> = line.split_whitespace().collect();
                    if words.len() >= 2 {
                        let pkg_name = words[0];
                        if let Ok(uid) = words[1].parse::<i32>() {
                            if let Some(config) = package_configs
                                .iter_mut()
                                .find(|config| config.pkg == pkg_name)
                            {
                                if config.uid % 100000 != uid % 100000 {
                                    let uid = config.uid / 100000 * 100000 + uid % 100000;
                                    info!(
                                        "Updating uid for package {}: {} -> {}",
                                        pkg_name, config.uid, uid
                                    );
                                    config.uid = uid;
                                    updated = true;
                                }
                            }
                        } else {
                            warn!("Error parsing uid: {}", words[1]);
                        }
                    }
                }

                if updated || removed_count > 0 {
                    write_ap_package_config(&package_configs)?;
                }
                return Ok(());
            }
            Err(e) => {
                warn!("Error reading packages.list: {}", e);
                thread::sleep(Duration::from_secs(1));
            }
        }
    }
    Err(io::Error::other("Failed after max retries"))
}
