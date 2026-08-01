use crate::defs;
use crate::utils::*;
use anyhow::{Context, Result, bail, ensure};
use log::{info, warn};
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};

pub const PLUGIN_MANIFEST: &str = "plugin.json";
/// Default entry file name.
pub const PLUGIN_ENTRY: &str = "main.lua";

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct PluginManifest {
    pub id: String,
    #[serde(default)]
    pub name: String,
    #[serde(default)]
    pub author: String,
    #[serde(default)]
    pub version: String,
    #[serde(default)]
    pub description: String,
    #[serde(default)]
    pub license: String,
    /// Minimum FolkPatch/APatch version code required.
    #[serde(default)]
    pub min_version: Option<u32>,
    /// Other plugin ids this plugin depends on.
    #[serde(default)]
    pub depends: Vec<String>,
    /// Entry Lua file, defaults to `main.lua`.
    #[serde(default)]
    pub entry: Option<String>,
    /// Declared configuration fields shown in the manager UI.
    #[serde(default)]
    pub config: Vec<ConfigField>,
    /// Optional quick action: a button shown in the manager UI that runs a
    /// single callback with one tap.
    #[serde(default)]
    pub quick_action: Option<QuickAction>,
}

/// A quick action: a one-tap button that runs a plugin callback.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct QuickAction {
    /// Callback function name to run.
    pub function: String,
    /// Button label (default / English).
    #[serde(default)]
    pub label: String,
    /// Localized labels keyed by language code (e.g. "zh", "ja", "tr").
    #[serde(default)]
    pub labels: std::collections::HashMap<String, String>,
}

/// A user-editable configuration field, shown in the manager UI.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ConfigField {
    /// Config key, used by `get_config`/`set_config`.
    pub key: String,
    /// Human-readable label (default / English).
    #[serde(default)]
    pub label: String,
    /// Localized labels keyed by language code (e.g. "zh", "ja", "tr").
    #[serde(default)]
    pub labels: std::collections::HashMap<String, String>,
    /// Field type: "text" | "number" | "bool" | "select".
    #[serde(default = "default_config_type")]
    pub r#type: String,
    /// Default value.
    #[serde(default)]
    pub default: Option<serde_json::Value>,
    /// Options for "select" type.
    #[serde(default)]
    pub options: Vec<String>,
}

fn default_config_type() -> String {
    "text".to_string()
}

/// User configuration file inside a plugin directory.
pub const PLUGIN_CONFIG_FILE: &str = "config.json";

#[derive(Debug, Serialize)]
pub struct PluginInfo {
    pub id: String,
    pub name: String,
    pub author: String,
    pub version: String,
    pub description: String,
    pub license: String,
    pub enabled: bool,
    pub has_manifest: bool,
    pub has_action: bool,
    pub quick_action: Option<QuickAction>,
    pub config: Vec<ConfigField>,
}

pub fn plugin_path(id: &str) -> Result<PathBuf> {
    anyhow::ensure!(
        !id.is_empty() && id != "." && id != ".." && !id.contains('/') && !id.contains('\\'),
        "Invalid plugin id"
    );
    Ok(Path::new(defs::PLUGIN_DIR).join(id))
}

pub fn read_manifest(id: &str) -> Result<PluginManifest> {
    let path = plugin_path(id)?.join(PLUGIN_MANIFEST);
    if !path.exists() {
        bail!("Plugin {id} has no {PLUGIN_MANIFEST}");
    }
    let content = fs::read_to_string(&path)
        .with_context(|| format!("Failed to read manifest for {id}"))?;
    let manifest: PluginManifest =
        serde_json::from_str(&content).with_context(|| format!("Invalid manifest for {id}"))?;
    if manifest.id != id {
        warn!("Manifest id '{}' does not match directory '{id}'", manifest.id);
    }
    Ok(manifest)
}

pub fn read_manifest_optional(id: &str) -> Option<PluginManifest> {
    read_manifest(id).ok()
}

pub fn plugin_entry_name(manifest: &PluginManifest) -> &str {
    manifest.entry.as_deref().unwrap_or(PLUGIN_ENTRY)
}

/// Check that all declared dependencies are installed and enabled.
pub fn check_dependencies(manifest: &PluginManifest) -> Result<()> {
    for dep in &manifest.depends {
        let dep_path = plugin_path(dep)?;
        ensure!(dep_path.is_dir(), "Missing dependency: plugin '{dep}'");
        ensure!(
            !dep_path.join(defs::DISABLE_FILE_NAME).exists(),
            "Dependency plugin '{dep}' is disabled"
        );
    }
    Ok(())
}

fn validate_manifest(manifest: &PluginManifest) -> Result<()> {
    let id = manifest.id.trim();
    anyhow::ensure!(
        !id.is_empty() && id != "." && id != ".." && !id.contains('/') && !id.contains('\\'),
        "Invalid plugin id in manifest"
    );
    if let Some(min) = manifest.min_version {
        let current: u32 = defs::VERSION_CODE.parse().unwrap_or(0);
        ensure!(
            current >= min,
            "Requires version >= {min}, current is {current}"
        );
    }
    let entry = plugin_entry_name(manifest);
    ensure!(
        !entry.contains('/') && !entry.contains('\\') && !entry.contains(".."),
        "Invalid plugin entry name: {entry}"
    );
    Ok(())
}

/// Install a plugin from a zip package.
pub fn install_plugin(zip: &str) -> Result<()> {
    let realpath = fs::canonicalize(zip).with_context(|| format!("realpath: {zip} failed"))?;
    ensure_dir_exists(defs::PLUGIN_DIR)?;
    ensure_dir_exists(defs::PLUGIN_STAGE_DIR)?;

    let file = fs::File::open(&realpath)?;
    let mut archive = zip::ZipArchive::new(file)?;

    // Read manifest from zip first.
    let mut manifest_bytes = Vec::new();
    {
        use std::io::Read;
        let mut entry = archive
            .by_name(PLUGIN_MANIFEST)
            .with_context(|| format!("{PLUGIN_MANIFEST} not found in zip"))?;
        entry.read_to_end(&mut manifest_bytes)?;
    }
    let manifest: PluginManifest = serde_json::from_slice(&manifest_bytes)
        .with_context(|| format!("Invalid {PLUGIN_MANIFEST}"))?;
    validate_manifest(&manifest)?;
    check_dependencies(&manifest)?;

    let id = manifest.id.clone();
    let target = plugin_path(&id)?;

    if target.exists() {
        fs::remove_dir_all(&target)?;
    }

    let entry = plugin_entry_name(&manifest);
    // The zip may store files under a root folder or at the archive root.
    // Search for the entry file anywhere in the archive.
    let mut entry_path: Option<String> = None;
    for name in archive.file_names() {
        if name.ends_with(&format!("/{entry}")) || name == entry {
            entry_path = Some(name.to_string());
            break;
        }
    }
    let Some(entry_path) = entry_path else {
        bail!("Entry file '{entry}' not found in zip");
    };
    let entry_dir = Path::new(&entry_path)
        .parent()
        .map(|p| p.to_string_lossy().into_owned())
        .unwrap_or_default();

    // Extract only the root directory that contains the entry file.
    fs::create_dir_all(&target)?;
    for i in 0..archive.len() {
        let mut file = archive.by_index(i)?;
        let Some(name) = file.enclosed_name().map(|p| p.to_string_lossy().into_owned()) else {
            continue;
        };
        let relative = if entry_dir.is_empty() {
            name.clone()
        } else {
            match name.strip_prefix(&format!("{entry_dir}/")) {
                Some(rest) => rest.to_string(),
                None => continue,
            }
        };
        if relative.is_empty() {
            continue;
        }
        let out_path = target.join(&relative);
        if file.is_dir() {
            fs::create_dir_all(&out_path)?;
            continue;
        }
        if let Some(parent) = out_path.parent() {
            fs::create_dir_all(parent)?;
        }
        let mut out = fs::File::create(&out_path)?;
        std::io::copy(&mut file, &mut out)?;
    }

    // Ensure the entry file actually landed.
    ensure!(
        target.join(entry).exists(),
        "Failed to extract entry file '{entry}'"
    );

    // Clean up stage dir.
    let _ = fs::remove_dir_all(defs::PLUGIN_STAGE_DIR);

    info!("Installed plugin {id}");
    println!("- Installed plugin {id}");
    Ok(())
}

/// Remove a plugin entirely.
pub fn uninstall_plugin(id: &str) -> Result<()> {
    let path = plugin_path(id)?;
    ensure!(path.is_dir(), "Plugin {id} not found");
    fs::remove_dir_all(&path)?;
    info!("Removed plugin {id}");
    Ok(())
}

/// Path to a plugin's config.json (user configuration).
pub fn config_path(id: &str) -> Result<PathBuf> {
    Ok(plugin_path(id)?.join(PLUGIN_CONFIG_FILE))
}

/// Read the user config.json for a plugin as a map of key -> value.
pub fn read_user_config(id: &str) -> Result<std::collections::HashMap<String, serde_json::Value>> {
    let path = config_path(id)?;
    if !path.exists() {
        return Ok(std::collections::HashMap::new());
    }
    let content = fs::read_to_string(&path)
        .with_context(|| format!("Failed to read config for {id}"))?;
    let map: std::collections::HashMap<String, serde_json::Value> =
        serde_json::from_str(&content).unwrap_or_default();
    Ok(map)
}

/// Write a single config value for a plugin, merging with existing entries.
pub fn set_user_config(id: &str, key: &str, value: &str) -> Result<()> {
    anyhow::ensure!(
        !key.is_empty() && key.len() <= 64 && !key.contains('/') && !key.contains('\\'),
        "Invalid config key"
    );
    let mut map = read_user_config(id)?;
    map.insert(
        key.to_string(),
        serde_json::Value::String(value.to_string()),
    );
    let path = config_path(id)?;
    if let Some(parent) = path.parent() {
        ensure_dir_exists(parent)?;
    }
    let content = serde_json::to_string_pretty(&map)?;
    fs::write(&path, content)?;
    info!("Set config {id}::{key} = {value}");
    Ok(())
}

/// Delete a config key for a plugin.
pub fn delete_user_config(id: &str, key: &str) -> Result<()> {
    let mut map = read_user_config(id)?;
    map.remove(key);
    let path = config_path(id)?;
    let content = serde_json::to_string_pretty(&map)?;
    fs::write(&path, content)?;
    Ok(())
}

/// List plugins with metadata as a JSON array.
pub fn list_plugins_json() -> Result<()> {
    let plugins_dir = Path::new(defs::PLUGIN_DIR);
    let mut list: Vec<PluginInfo> = Vec::new();
    if plugins_dir.exists() {
        for entry in fs::read_dir(plugins_dir)? {
            let path = entry?.path();
            if !path.is_dir() {
                continue;
            }
            let Some(id) = path.file_name().and_then(|n| n.to_str()) else {
                continue;
            };
            let enabled = !path.join(defs::DISABLE_FILE_NAME).exists();
            let manifest = read_manifest_optional(id);
            let has_action = manifest
                .as_ref()
                .map(|_| crate::lua::plugin_has_callback(id, "action"))
                .unwrap_or(false);
            let (name, author, version, description, license, has_manifest, config, quick_action) =
                match manifest {
                    Some(m) => (
                        m.name,
                        m.author,
                        m.version,
                        m.description,
                        m.license,
                        true,
                        m.config,
                        m.quick_action,
                    ),
                    None => (
                        String::new(),
                        String::new(),
                        String::new(),
                        String::new(),
                        String::new(),
                        false,
                        Vec::new(),
                        None,
                    ),
                };
            list.push(PluginInfo {
                id: id.to_string(),
                name,
                author,
                version,
                description,
                license,
                enabled,
                has_manifest,
                has_action,
                quick_action,
                config,
            });
        }
    }
    println!("{}", serde_json::to_string(&list)?);
    Ok(())
}
