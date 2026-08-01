-- Cache Cleaner
--
-- Periodically clears app caches and /data/cache.
-- Runs as a background daemon (declared via `main`), auto-started at the
-- `service` stage.
--
-- Configuration is stored in config.json inside the plugin directory and can
-- be edited from the manager UI or via:
--   apd plugin config --id cache-cleaner set <key> <value>

local config = {
    clean_hour = 4,   -- hour of day to run (0-23)
    clear_all = false,
    interval = 60,    -- daemon check interval in seconds (fixed)
}

local function load_config()
    -- read saved user config, falling back to defaults declared in plugin.json
    local saved = {
        clean_hour = get_config("clean_hour"),
        clear_all = get_config("clear_all"),
    }
    local function num(v, d)
        local n = tonumber(v)
        if n == nil then return d end
        return n
    end
    local function bool(v, d)
        if v == "" then return d end
        return v == "true"
    end
    config.clean_hour = math.floor(num(saved.clean_hour, config.clean_hour))
    config.clear_all = bool(saved.clear_all, config.clear_all)
end

-- Paths to clean. Each entry: { label, glob }
local function cache_paths()
    local paths = {
        { "system cache",        "/data/cache/*" },
        { "dalvik cache",        "/data/dalvik-cache/*" },
        { "app caches (user 0)", "/data/user/0/*/cache/*" },
    }
    if config.clear_all then
        table.insert(paths, { "app caches (/data/data)", "/data/data/*/cache/*" })
    end
    return paths
end

local function clean()
    local total_before = 0
    local freed = 0
    for _, p in ipairs(cache_paths()) do
        local label, glob = p[1], p[2]
        local probe = exec("sh", "-c", "du -sk " .. glob .. " 2>/dev/null | awk '{s+=$1} END{print s+0}'")
        local kb = tonumber(probe.stdout) or 0
        total_before = total_before + kb

        -- remove file contents but keep parent dirs, ignore errors
        exec("sh", "-c", "rm -rf " .. glob .. " 2>/dev/null")
        freed = freed + kb
        info(string.format("[%s] cleaned %.1f MB", label, kb / 1024))
    end
    info(string.format("cache clean done: %.1f MB scanned, %.1f MB freed", total_before / 1024, freed / 1024))
end

local function run_clean_if_due()
    local now = os.date("*t")
    -- run only during the configured hour
    if now.hour ~= config.clean_hour then
        return
    end

    -- run once per day: track last run date
    local stamp_file = PLUGIN_DIR .. "/last_run"
    local ok, last = pcall(read_file, stamp_file)
    if not ok then last = "" end
    local today = os.date("%Y-%m-%d")
    if last == today then
        return
    end

    clean()
    write_file(stamp_file, today)
end

load_config()

return {
    post_fs_data = function()
        info("cache-cleaner loaded, scheduled at hour " .. config.clean_hour)
    end,

    service = function()
        info("cache-cleaner service started")
    end,

    boot_completed = function()
        info("cache-cleaner ready")
    end,

    -- background daemon: check every `interval` seconds whether it is time to clean
    main = function()
        while true do
            run_clean_if_due()
            os.execute("sleep " .. config.interval)
        end
    end,

    -- action: run from the manager UI or  apd plugin action cache-cleaner
    action = function()
        info("manual clean triggered")
        clean()
    end,

    -- manual trigger via:  apd plugin run cache-cleaner clean_now
    clean_now = function()
        clean()
    end,
}
