use crate::{defs, event, insmod, late_load, lua, magica, module, module_config, plugin, supercall, utils};
#[cfg(target_os = "android")]
use android_logger::Config;
use anyhow::{Context, Result};
use clap::Parser;
#[cfg(target_os = "android")]
use log::LevelFilter;
use std::path::PathBuf;

/// APatch cli
#[derive(Parser, Debug)]
#[command(author, version = defs::VERSION_CODE, about, long_about = None)]
struct Args {
    #[arg(
        short,
        long,
        value_name = "KEY",
        help = "Super key for authentication root"
    )]
    superkey: Option<String>,
    #[command(subcommand)]
    command: Commands,
}

#[derive(clap::Subcommand, Debug)]
enum Commands {
    /// Manage APatch modules
    Module {
        #[command(subcommand)]
        command: Module,
    },

    /// Manage lightweight Lua plugins
    Plugin {
        #[command(subcommand)]
        command: Plugin,
    },

    /// Trigger `post-fs-data` event
    PostFsData,

    /// Trigger `service` event
    Services,

    /// Trigger `boot-complete` event
    BootCompleted,

    /// Apply boot-time patch features from manager boot fallback
    ManagerBootCompleted,

    /// Start uid listener for synchronizing root list
    UidListener,

    /// Load a kernel module (.ko) without version check (jailbreak mode)
    Insmod {
        /// kernel module path
        module: PathBuf,
        /// module load parameters (e.g. key=val key2=val2)
        #[arg(trailing_var_arg = true, allow_hyphen_values = true, num_args = 0..)]
        params: Vec<String>,
    },

    /// Emulate system reboot (keep runtime-loaded modules active)
    SoftReboot,

    /// Jailbreak mode: load the KernelPatch module for this kernel and apply Magisk policy
    LateLoad {
        /// kernel module path (auto-detects KMI if omitted)
        #[arg(long)]
        module: Option<PathBuf>,
        /// kernel KMI (e.g. android14-5.15), auto-detected if omitted
        #[arg(long)]
        kmi: Option<String>,
        /// enable adb-root escalation on this tcp port, then run late-load via adb shell
        #[arg(long)]
        magica: Option<u16>,
        /// restore adb properties after a magica jailbreak (used by the adb shell step)
        #[arg(long)]
        post_magica: bool,
        /// manager package name to restart after a successful jailbreak
        #[arg(long)]
        package_name: Option<String>,
    },

    /// Resetprop - Magisk-compatible system property tool
    #[command(disable_help_flag = true)]
    Resetprop {
        /// Arguments passed to resetprop
        #[arg(trailing_var_arg = true, allow_hyphen_values = true, num_args = 0..)]
        args: Vec<String>,
    },

    /// MagiskPolicy - SELinux Policy Patch Tool
    Sepolicy(crate::sepolicy::Args),
}

#[derive(clap::Subcommand, Debug)]
enum Module {
    /// Install module <ZIP>
    Install {
        /// module zip file path
        zip: String,
    },

    /// Uninstall module <id>
    Uninstall {
        /// module id
        id: String,
    },

    /// Undo uninstall module <id>
    UndoUninstall {
        /// module id
        id: String,
    },

    /// enable module <id>
    Enable {
        /// module id
        id: String,
    },

    /// disable module <id>
    Disable {
        // module id
        id: String,
    },

    /// run action for module <id>
    Action {
        // module id
        id: String,
    },
    /// module lua runner
    Lua {
        // module id
        id: String,
        // lua function
        function: String,
    },
    /// list all modules
    List,

    /// manage module configuration
    Config {
        /// target internal module name (resolved as internal.<name>)
        #[arg(long)]
        internal: Option<String>,
        #[command(subcommand)]
        command: ModuleConfigCmd,
    },
}

#[derive(clap::Subcommand, Debug)]
enum Plugin {
    /// List installed plugins
    List,
    /// Install plugin from <ZIP>
    Install {
        /// plugin zip file path
        zip: String,
    },
    /// Uninstall plugin <id>
    Uninstall {
        /// plugin id
        id: String,
    },
    /// Enable plugin <id>
    Enable { id: String },
    /// Disable plugin <id>
    Disable { id: String },
    /// Run a plugin callback
    Run { id: String, function: String },
    /// Run a plugin callback as a background daemon loop
    Daemon {
        /// plugin id
        id: String,
        /// callback function name
        function: String,
        /// interval between runs in seconds
        interval: u64,
    },
    /// Run the plugin's action callback
    Action { id: String },
    /// Show the last execution log of a plugin
    Log { id: String },
    /// Clear the log of a plugin (or all plugins if id is "all")
    ClearLog { id: String },
    /// Manage plugin user configuration
    Config {
        /// target plugin id
        #[arg(long)]
        id: Option<String>,
        #[command(subcommand)]
        command: PluginConfigCmd,
    },
}

#[derive(clap::Subcommand, Debug)]
enum PluginConfigCmd {
    /// List all config entries of the current plugin
    List,
    /// Get a config value
    Get { key: String },
    /// Set a config value
    Set { key: String, value: String },
    /// Delete a config entry
    Delete { key: String },
}

#[derive(clap::Subcommand, Debug)]
enum ModuleConfigCmd {
    /// Get a config value
    Get {
        /// config key
        key: String,
    },

    /// Set a config value
    Set {
        /// config key
        key: String,
        /// config value (omit to read from stdin)
        value: Option<String>,
        /// read value from stdin (default if value not provided)
        #[arg(long)]
        stdin: bool,
        /// use temporary config (cleared on reboot)
        #[arg(short, long)]
        temp: bool,
    },

    /// List all config entries
    List,

    /// Delete a config entry
    Delete {
        /// config key
        key: String,
        /// delete from temporary config
        #[arg(short, long)]
        temp: bool,
    },

    /// Clear all config entries
    Clear {
        /// clear temporary config
        #[arg(short, long)]
        temp: bool,
    },
}

pub fn run() -> Result<()> {
    #[cfg(target_os = "android")]
    android_logger::init_once(
        Config::default()
            .with_max_level(LevelFilter::Trace) // limit log level
            .with_tag("APatchD")
            .with_filter(
                android_logger::FilterBuilder::new()
                    .filter_level(LevelFilter::Trace)
                    .filter_module("notify", LevelFilter::Warn)
                    .build(),
            ),
    );

    #[cfg(not(target_os = "android"))]
    env_logger::init();

    // the kernel executes su with argv[0] = "/system/bin/kp" or "/system/bin/su" or "su" or "kp" and replace it with us
    let arg0 = std::env::args().next().unwrap_or_default();
    if arg0.ends_with("kp") || arg0.ends_with("su") {
        return crate::apd::root_shell();
    }
    if arg0.ends_with("resetprop") {
        let all_args: Vec<String> = std::env::args().collect();
        crate::resetprop::resetprop_main(&all_args)
    }
    if arg0.ends_with("magiskpolicy") {
        let all_args: Vec<String> = std::env::args().collect();
        crate::sepolicy::policy_main(&all_args)
    }

    let cli = Args::parse();

    log::info!("command: {:?}", cli.command);

    if let Some(ref _superkey) = cli.superkey {
        supercall::privilege_apd_profile(&cli.superkey);
    }

    let result = match cli.command {
        Commands::PostFsData => event::on_post_data_fs(cli.superkey),

        Commands::BootCompleted => event::on_boot_completed(cli.superkey),

        Commands::ManagerBootCompleted => event::on_manager_boot_completed(cli.superkey),

        Commands::UidListener => event::start_uid_listener(),

        Commands::Insmod { module, params } => insmod::insmod(&module, &params),

        Commands::SoftReboot => event::soft_reboot(cli.superkey),

        Commands::LateLoad {
            module,
            kmi,
            magica,
            post_magica,
            package_name,
        } => {
            if let Some(port) = magica {
                return magica::run(port, &module, &kmi, &package_name);
            }
            let result = late_load::run(module, kmi, package_name);
            if post_magica && let Err(e) = magica::disable_adb_root() {
                log::error!("disable adb root failed: {e:#}");
            }
            result
        }

        Commands::Module { command } => {
            #[cfg(any(target_os = "linux", target_os = "android"))]
            {
                utils::switch_mnt_ns(1)?;
            }
            match command {
                Module::Install { zip } => module::install_module(&zip),
                Module::Uninstall { id } => module::uninstall_module(&id),
                Module::UndoUninstall { id } => module::undo_uninstall_module(&id),
                Module::Action { id } => module::run_action(&id),
                Module::Lua { id, function } => {
                    lua::run_lua(&id, &function, false, true).map_err(|e| anyhow::anyhow!("{}", e))
                }
                Module::Enable { id } => module::enable_module(&id),
                Module::Disable { id } => module::disable_module(&id),
                Module::List => module::list_modules(),
                Module::Config { internal, command } => {
                    let module_id = match internal {
                        Some(internal_name) => format!("internal.{internal_name}"),
                        None => std::env::var("AP_MODULE").map_err(|_| {
                            anyhow::anyhow!(
                                "This command must be run in the context of a module or passed --internal <name>"
                            )
                        })?,
                    };

                    match command {
                        ModuleConfigCmd::Get { key } => {
                            // Use merge_configs to respect priority (temp overrides persist)
                            let config = module_config::merge_configs(&module_id)?;
                            match config.get(&key) {
                                Some(value) => {
                                    println!("{value}");
                                    Ok(())
                                }
                                None => anyhow::bail!("Key '{key}' not found"),
                            }
                        }
                        ModuleConfigCmd::Set {
                            key,
                            value,
                            stdin,
                            temp,
                        } => {
                            // Validate key at CLI layer for better user experience
                            module_config::validate_config_key(&key)?;

                            // Read value from stdin or argument
                            let value_str = match value {
                                Some(v) if !stdin => v,
                                _ => {
                                    // Read from stdin
                                    use std::io::Read;
                                    let mut buffer = String::new();
                                    std::io::stdin()
                                        .read_to_string(&mut buffer)
                                        .context("Failed to read from stdin")?;
                                    buffer
                                }
                            };

                            // Validate value
                            module_config::validate_config_value(&value_str)?;

                            let config_type = if temp {
                                module_config::ConfigType::Temp
                            } else {
                                module_config::ConfigType::Persist
                            };
                            module_config::set_config_value(
                                &module_id,
                                &key,
                                &value_str,
                                config_type,
                            )
                        }
                        ModuleConfigCmd::List => {
                            let config = module_config::merge_configs(&module_id)?;
                            if config.is_empty() {
                                println!("No config entries found");
                            } else {
                                for (key, value) in config {
                                    println!("{key}={value}");
                                }
                            }
                            Ok(())
                        }
                        ModuleConfigCmd::Delete { key, temp } => {
                            let config_type = if temp {
                                module_config::ConfigType::Temp
                            } else {
                                module_config::ConfigType::Persist
                            };
                            module_config::delete_config_value(&module_id, &key, config_type)
                        }
                        ModuleConfigCmd::Clear { temp } => {
                            let config_type = if temp {
                                module_config::ConfigType::Temp
                            } else {
                                module_config::ConfigType::Persist
                            };
                            module_config::clear_config(&module_id, config_type)
                        }
                    }
                }
            }
        }

        Commands::Plugin { command } => {
            #[cfg(any(target_os = "linux", target_os = "android"))]
            utils::switch_mnt_ns(1)?;
            match command {
                Plugin::List => plugin::list_plugins_json(),
                Plugin::Install { zip } => plugin::install_plugin(&zip),
                Plugin::Uninstall { id } => plugin::uninstall_plugin(&id),
                Plugin::Enable { id } => lua::set_plugin_state(&id, true),
                Plugin::Disable { id } => lua::set_plugin_state(&id, false),
                Plugin::Run { id, function } => lua::run_plugin_callback(&id, &function),
                Plugin::Daemon {
                    id,
                    function,
                    interval,
                } => lua::run_plugin_daemon(&id, &function, interval),
                Plugin::Action { id } => lua::run_plugin_callback(&id, "action"),
                Plugin::Log { id } => {
                    let log_path = plugin::plugin_path(&id)?.join("last_output.log");
                    if log_path.exists() {
                        let content = std::fs::read_to_string(&log_path)?;
                        print!("{content}");
                    } else {
                        println!("No log found for plugin '{id}'");
                    }
                    Ok(())
                }
                Plugin::ClearLog { id } => {
                    if id == "all" {
                        let plugins_dir = std::path::Path::new(defs::PLUGIN_DIR);
                        if plugins_dir.exists() {
                            for entry in std::fs::read_dir(plugins_dir)? {
                                let path = entry?.path();
                                if path.is_dir() {
                                    let log = path.join("last_output.log");
                                    if log.exists() {
                                        let _ = std::fs::remove_file(&log);
                                    }
                                }
                            }
                        }
                        println!("All plugin logs cleared");
                    } else {
                        let log_path = plugin::plugin_path(&id)?.join("last_output.log");
                        if log_path.exists() {
                            std::fs::remove_file(&log_path)?;
                        }
                        println!("Log cleared for plugin '{id}'");
                    }
                    Ok(())
                }
                Plugin::Config { id, command } => {
                    let plugin_id = match id {
                        Some(id) => id,
                        None => std::env::var("AP_PLUGIN").map_err(|_| {
                            anyhow::anyhow!(
                                "This command must be run in the context of a plugin or passed --id <id>"
                            )
                        })?,
                    };
                    match command {
                        PluginConfigCmd::List => {
                            let map = plugin::read_user_config(&plugin_id)?;
                            if map.is_empty() {
                                println!("No config entries found");
                            } else {
                                for (key, value) in map {
                                    let raw = match &value {
                                        serde_json::Value::String(s) => s.clone(),
                                        other => other.to_string(),
                                    };
                                    println!("{key}={raw}");
                                }
                            }
                            Ok(())
                        }
                        PluginConfigCmd::Get { key } => {
                            let map = plugin::read_user_config(&plugin_id)?;
                            match map.get(&key) {
                                Some(v) => {
                                    // Print raw string without JSON quotes
                                    let raw = match v {
                                        serde_json::Value::String(s) => s.clone(),
                                        other => other.to_string(),
                                    };
                                    println!("{raw}");
                                    Ok(())
                                }
                                None => anyhow::bail!("Key '{key}' not found"),
                            }
                        }
                        PluginConfigCmd::Set { key, value } => {
                            plugin::set_user_config(&plugin_id, &key, &value)
                        }
                        PluginConfigCmd::Delete { key } => {
                            plugin::delete_user_config(&plugin_id, &key)
                        }
                    }
                }
            }
        }

        Commands::Services => event::on_services(cli.superkey),

        Commands::Resetprop { args } => {
            let mut full_args = vec!["resetprop".to_string()];
            full_args.extend(args);
            crate::resetprop::resetprop_main(&full_args)
        }

        Commands::Sepolicy(sepolicy_args) => crate::sepolicy::execute(&sepolicy_args),
    };

    if let Err(e) = &result {
        log::error!("Error: {:?}", e);
    }
    result
}
