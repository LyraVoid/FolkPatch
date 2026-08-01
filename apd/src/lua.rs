use crate::defs;
use crate::module::*;
use crate::utils::*;
use anyhow::Result;
use log::{info, warn};
use mlua::{Function, Lua, Result as LuaResult, Table};
use std::{fs, path::Path};

pub fn save_text<P: AsRef<Path>>(filename: P, content: &str) -> std::io::Result<()> {
    let _ = ensure_dir_exists("/data/adb/config");
    let path = format!("/data/adb/config/{}", filename.as_ref().display());
    fs::write(&path, content)?;
    Ok(())
}

pub fn load_text<P: AsRef<Path>>(filename: P) -> std::io::Result<String> {
    let _ = ensure_dir_exists("/data/adb/config");
    let path = format!("/data/adb/config/{}", filename.as_ref().display());
    fs::read_to_string(path)
}

pub fn load_all_lua_modules(lua: &Lua) -> LuaResult<()> {
    let modules_dir = Path::new("/data/adb/modules");

    let modules: Table = match lua.globals().get("modules") {
        Ok(t) => t,
        Err(_) => {
            let t = lua.create_table()?;
            lua.globals().set("modules", t.clone())?;
            t
        }
    };

    if modules_dir.exists() {
        for entry in
            fs::read_dir(modules_dir).unwrap_or_else(|_| fs::read_dir("/dev/null").unwrap())
        {
            if let Ok(entry) = entry {
                let path = entry.path();
                if path.is_dir() {
                    let id = path.file_name().unwrap().to_string_lossy().to_string();
                    let package: Table = lua.globals().get("package")?;
                    let old_cpath: String = package.get("cpath")?;
                    let new_cpath = format!("{}/?.so;{}", path.to_string_lossy(), old_cpath);
                    package.set("cpath", new_cpath)?;

                    let lua_file = path.join(format!("{}.lua", id));

                    if lua_file.exists() {
                        match fs::read_to_string(&lua_file) {
                            Ok(code) => {
                                match lua
                                    .load(&code)
                                    .set_name(&*lua_file.to_string_lossy())
                                    .eval::<Table>()
                                {
                                    Ok(module) => {
                                        modules.set(id.clone(), module.clone())?;
                                    }
                                    Err(e) => {
                                        eprintln!(
                                            "Failed to eval Lua {}: {}",
                                            lua_file.display(),
                                            e
                                        );
                                    }
                                }
                            }
                            Err(e) => {
                                eprintln!("Failed to read Lua {}: {}", lua_file.display(), e);
                            }
                        }
                    }
                }
            }
        }
    }

    Ok(())
}

pub fn info_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|lua, msg: String| {
        info!("[Lua] {}", msg);
        println!("[Lua] {msg}");
        append_plugin_log(lua, &format!("[Lua] {msg}"));
        Ok(())
    })
}

pub fn warn_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|lua, msg: String| {
        warn!("[Lua] {}", msg);
        eprintln!("[Lua] {msg}");
        append_plugin_log(lua, &format!("[Lua] {msg}"));
        Ok(())
    })
}

/// Append a line to the plugin's last_output.log if running in plugin context.
fn append_plugin_log(lua: &Lua, line: &str) {
    use std::io::Write;
    let Ok(dir) = lua.globals().get::<String>("PLUGIN_DIR") else {
        return;
    };
    let log_path = Path::new(&dir).join("last_output.log");
    if let Ok(mut file) = fs::OpenOptions::new().create(true).append(true).open(&log_path) {
        let _ = writeln!(file, "{line}");
    }
}

pub fn install_module_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|_, zip: String| {
        install_module(&zip)
            .map_err(|e| mlua::Error::external(format!("install_module failed: {}", e)))
    })
}
pub fn save_text_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|_, (filename, content): (String, String)| {
        save_text(&filename, &content)
            .map_err(|e| mlua::Error::external(format!("save filed: {}", e)))?;
        Ok(())
    })
}
pub fn read_text_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|_, filename: String| {
        let content = match load_text(&filename) {
            Ok(s) => s,
            Err(ref e) if e.kind() == std::io::ErrorKind::NotFound => String::new(),
            Err(e) => return Err(mlua::Error::external(format!("read failed: {}", e))),
        };
        Ok(content)
    })
}

/// `getprop(name)` — read an Android system property.
pub fn getprop_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|_, name: String| {
        Ok(crate::utils::getprop(&name).unwrap_or_default())
    })
}

/// `setprop(name, value)` — set an Android system property (bypasses read-only).
pub fn setprop_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|_, (name, value): (String, String)| {
        match crate::resetprop::set_prop(&name, &value) {
            Ok(()) => Ok(true),
            Err(e) => {
                warn!("setprop {name} failed: {e}");
                Ok(false)
            }
        }
    })
}

/// `exec(cmd, ...)` — run a command and capture its output.
/// Accepts a single command string (`exec("sh -c ...")`) or a varargs list.
pub fn exec_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|lua, args: mlua::Variadic<String>| {
        let program = args
            .first()
            .map(String::as_str)
            .unwrap_or_default()
            .to_string();
        let rest: Vec<&str> = args.iter().skip(1).map(String::as_str).collect();
        let output = std::process::Command::new(&program)
            .args(&rest)
            .output()
            .map_err(|e| mlua::Error::external(format!("exec failed: {e}")))?;
        let table = lua.create_table()?;
        table.set("ok", output.status.success())?;
        table.set("code", output.status.code().unwrap_or(-1))?;
        table.set("stdout", String::from_utf8_lossy(&output.stdout).into_owned())?;
        table.set("stderr", String::from_utf8_lossy(&output.stderr).into_owned())?;
        Ok(table)
    })
}

/// `write_file(path, content)` — write text content to a file.
pub fn write_file_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|_, (path, content): (String, String)| {
        if let Some(parent) = Path::new(&path).parent() {
            let _ = ensure_dir_exists(parent);
        }
        match fs::write(&path, content) {
            Ok(()) => Ok(true),
            Err(e) => {
                warn!("write_file {path} failed: {e}");
                Ok(false)
            }
        }
    })
}

/// `read_file(path)` — read a file's text content.
pub fn read_file_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|_, path: String| {
        match fs::read_to_string(&path) {
            Ok(s) => Ok(s),
            Err(e) => Err(mlua::Error::external(format!("read_file failed: {e}"))),
        }
    })
}

/// `sysctl(key, value)` — write a sysctl parameter.
pub fn sysctl_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|_, (key, value): (String, String)| {
        let safe_key = key.replace('.', "/");
        let path = format!("/proc/sys/{safe_key}");
        match fs::write(&path, value) {
            Ok(()) => Ok(true),
            Err(e) => {
                warn!("sysctl {key} failed: {e}");
                Ok(false)
            }
        }
    })
}

/// `chmod(path, mode)` — set a file's permission bits (mode like 0755 or "0755").
pub fn chmod_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|_, (path, mode): (String, mlua::Value)| {
        let bits = match &mode {
            mlua::Value::Integer(i) => *i as u32,
            mlua::Value::String(s) => {
                let s = s.to_string_lossy();
                u32::from_str_radix(s.trim_start_matches('0'), 8).map_err(|e| {
                    mlua::Error::external(format!("invalid mode '{s}': {e}"))
                })?
            }
            _ => return Err(mlua::Error::external("mode must be number or string")),
        };
        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            match fs::set_permissions(&path, fs::Permissions::from_mode(bits)) {
                Ok(()) => Ok(true),
                Err(e) => {
                    warn!("chmod {path} failed: {e}");
                    Ok(false)
                }
            }
        }
        #[cfg(not(unix))]
        {
            let _ = (path, bits);
            Ok(false)
        }
    })
}

/// `mkdir(path, recursive)` — create a directory.
pub fn mkdir_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|_, (path, recursive): (String, bool)| {
        let result = if recursive {
            ensure_dir_exists(&path)
        } else {
            fs::create_dir(&path).map_err(anyhow::Error::from)
        };
        match result {
            Ok(()) => Ok(true),
            Err(e) => {
                warn!("mkdir {path} failed: {e}");
                Ok(false)
            }
        }
    })
}

/// `rm(path)` — remove a file or empty directory.
pub fn rm_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|_, path: String| {
        let result = fs::remove_file(&path).or_else(|_| fs::remove_dir(&path));
        match result {
            Ok(()) => Ok(true),
            Err(e) => {
                warn!("rm {path} failed: {e}");
                Ok(false)
            }
        }
    })
}

/// `get_config(key)` — read the plugin's saved user config value (string).
pub fn get_config_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|lua, key: String| {
        let id: String = lua.globals().get("PLUGIN_ID")?;
        let map = crate::plugin::read_user_config(&id).unwrap_or_default();
        match map.get(&key) {
            Some(serde_json::Value::String(s)) => Ok(s.clone()),
            Some(v) => Ok(v.to_string()),
            None => Ok(String::new()),
        }
    })
}

/// `set_config(key, value)` — save a user config value for this plugin.
pub fn set_config_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|lua, (key, value): (String, String)| {
        let id: String = lua.globals().get("PLUGIN_ID")?;
        match crate::plugin::set_user_config(&id, &key, &value) {
            Ok(()) => Ok(true),
            Err(e) => {
                warn!("set_config {key} failed: {e}");
                Ok(false)
            }
        }
    })
}

pub fn bind_plugin_api(lua: &Lua) -> LuaResult<()> {
    lua.globals().set("info", info_lua(lua)?)?;
    lua.globals().set("warn", warn_lua(lua)?)?;
    lua.globals().set("getprop", getprop_lua(lua)?)?;
    lua.globals().set("setprop", setprop_lua(lua)?)?;
    lua.globals().set("exec", exec_lua(lua)?)?;
    lua.globals().set("write_file", write_file_lua(lua)?)?;
    lua.globals().set("read_file", read_file_lua(lua)?)?;
    lua.globals().set("sysctl", sysctl_lua(lua)?)?;
    lua.globals().set("chmod", chmod_lua(lua)?)?;
    lua.globals().set("mkdir", mkdir_lua(lua)?)?;
    lua.globals().set("rm", rm_lua(lua)?)?;
    lua.globals().set("start_daemon", start_daemon_lua(lua)?)?;
    lua.globals().set("get_config", get_config_lua(lua)?)?;
    lua.globals().set("set_config", set_config_lua(lua)?)?;
    Ok(())
}

pub fn exec_stage_lua(stage: &str, wait: bool, superkey: &str) -> Result<()> {
    let stage_safe = stage.replace('-', "_");
    run_lua(&superkey, &stage_safe, true, wait).map_err(|e| anyhow::anyhow!("{}", e))?;
    Ok(())
}

/// Check whether a plugin declares the named callback without executing it.
pub fn plugin_has_callback(id: &str, function: &str) -> bool {
    let Ok(path) = crate::plugin::plugin_path(id) else {
        return false;
    };
    let entry = crate::plugin::read_manifest_optional(id)
        .map(|m| crate::plugin::plugin_entry_name(&m).to_string())
        .unwrap_or_else(|| crate::plugin::PLUGIN_ENTRY.to_string());
    let script_path = path.join(&entry);
    let Ok(code) = fs::read_to_string(&script_path) else {
        return false;
    };
    let lua = unsafe { Lua::unsafe_new() };
    let Ok(plugin) = lua
        .load(&code)
        .set_name(script_path.to_string_lossy())
        .eval::<Table>()
    else {
        return false;
    };
    plugin.get::<Function>(function).is_ok()
}

/// Run the optional callback for each enabled standalone Lua plugin.
pub fn exec_plugin_stage(stage: &str) -> Result<()> {
    let plugins_dir = Path::new(defs::PLUGIN_DIR);
    if !plugins_dir.exists() {
        return Ok(());
    }

    let callback = stage.replace('-', "_");
    for entry in fs::read_dir(plugins_dir)? {
        let path = entry?.path();
        if !path.is_dir() || path.join("disable").exists() {
            continue;
        }
        let Some(id) = path.file_name().and_then(|name| name.to_str()) else {
            continue;
        };
        if let Err(error) = run_plugin(id, &callback) {
            warn!("Plugin {id} stage {stage} failed: {error}");
        }

        // If the plugin declares a `main` callback, spawn it as a background
        // daemon after the boot sequence so it runs continuously.
        if stage == "service" && plugin_has_callback(id, "main") {
            let mut cmd = std::process::Command::new(defs::DAEMON_PATH);
            cmd.args(["plugin", "daemon", id, "main", "1"])
                .stdout(std::process::Stdio::null())
                .stderr(std::process::Stdio::null());
            #[cfg(unix)]
            unsafe {
                use std::os::unix::process::CommandExt;
                cmd.pre_exec(|| {
                    libc::setsid();
                    Ok(())
                });
            }
            match cmd.spawn() {
                Ok(_) => info!("Spawned plugin daemon {id}::main"),
                Err(e) => warn!("Failed to spawn plugin daemon {id}::main: {e}"),
            }
        }
    }
    Ok(())
}

fn run_plugin(id: &str, function: &str) -> LuaResult<()> {
    let path = crate::plugin::plugin_path(id)
        .map_err(|error| mlua::Error::external(error.to_string()))?;
    let entry = crate::plugin::read_manifest_optional(id)
        .map(|m| crate::plugin::plugin_entry_name(&m).to_string())
        .unwrap_or_else(|| crate::plugin::PLUGIN_ENTRY.to_string());
    let script_path = path.join(&entry);

    let lua = unsafe { Lua::unsafe_new() };
    bind_plugin_api(&lua)?;
    lua.globals().set("PLUGIN_ID", id)?;
    lua.globals().set("PLUGIN_DIR", path.to_string_lossy())?;
    let code = fs::read_to_string(&script_path).map_err(mlua::Error::external)?;
    let plugin: Table = lua
        .load(&code)
        .set_name(script_path.to_string_lossy())
        .eval()?;
    if let Ok(callback) = plugin.get::<Function>(function) {
        callback.call::<()>(())?;
    }
    Ok(())
}

pub fn run_plugin_callback(id: &str, function: &str) -> Result<()> {
    run_plugin(id, function).map_err(|error| anyhow::anyhow!(error.to_string()))
}

/// Run a plugin callback in a loop with a fixed interval.
/// This is the target of `apd plugin daemon`.
pub fn run_plugin_daemon(id: &str, function: &str, interval: u64) -> Result<()> {
    let interval = interval.max(1);
    info!("Starting plugin daemon {id}::{function} every {interval}s");
    loop {
        if let Err(e) = run_plugin(id, function) {
            warn!("Plugin {id} daemon iteration {function} failed: {e}");
        }
        std::thread::sleep(std::time::Duration::from_secs(interval));
    }
}

/// `start_daemon(function, interval_secs)` — spawn a background daemon process
/// that calls the named callback every `interval_secs` seconds.
/// The daemon keeps running after the current apd invocation exits.
pub fn start_daemon_lua(lua: &Lua) -> LuaResult<Function> {
    lua.create_function(|lua, (function, interval): (String, u64)| {
        let id: String = lua.globals().get("PLUGIN_ID")?;
        let interval = interval.max(1);
        #[cfg(unix)]
        {
            let mut cmd = std::process::Command::new(defs::DAEMON_PATH);
            cmd.args(["plugin", "daemon", &id, &function, &interval.to_string()])
                .stdout(std::process::Stdio::null())
                .stderr(std::process::Stdio::null());
            unsafe {
                use std::os::unix::process::CommandExt;
                cmd.pre_exec(|| {
                    libc::setsid();
                    Ok(())
                });
            }
            match cmd.spawn() {
                Ok(_) => Ok(true),
                Err(e) => {
                    warn!("start_daemon spawn failed: {e}");
                    Ok(false)
                }
            }
        }
        #[cfg(not(unix))]
        {
            let _ = (function, interval);
            Ok(false)
        }
    })
}

pub fn set_plugin_state(id: &str, enabled: bool) -> Result<()> {
    let path = crate::plugin::plugin_path(id)?;
    anyhow::ensure!(path.is_dir(), "Plugin {id} not found");
    let disable = path.join(defs::DISABLE_FILE_NAME);
    if enabled {
        if disable.exists() {
            fs::remove_file(disable)?;
        }
    } else if !disable.exists() {
        fs::File::create(disable)?;
    }
    Ok(())
}

pub fn run_lua(id: &str, function: &str, on_each_module: bool, _wait: bool) -> mlua::Result<()> {
    let lua = unsafe { Lua::unsafe_new() };

    let func = install_module_lua(&lua)?;
    lua.globals().set("install_module", func)?;
    lua.globals().set("info", info_lua(&lua)?)?;
    lua.globals().set("warn", warn_lua(&lua)?)?;
    lua.globals().set("setConfig", save_text_lua(&lua)?)?;
    lua.globals().set("getConfig", read_text_lua(&lua)?)?;

    load_all_lua_modules(&lua)?;

    let modules: mlua::Table = lua.globals().get("modules")?;
    if on_each_module {
        for pair in modules.pairs::<String, mlua::Table>() {
            let (_, module_table) = pair?;
            if let Ok(func_obj) = module_table.get::<mlua::Function>(function) {
                func_obj.call::<()>(id)?;
            }
        }
    } else {
        let module_table: mlua::Table = modules.get(id)?;
        let func_obj: mlua::Function = module_table.get(function)?;
        func_obj.call::<()>(())?;
    }

    Ok(())
}
