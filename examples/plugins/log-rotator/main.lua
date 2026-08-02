local M = {}

local LOG_DIR = "/data/adb/log"
local STAMP_FILE = "/data/adb/plugins/log-rotator/.last_clean"

local function do_clean()
    if not file_exists(LOG_DIR) then
        info("Log directory not found, skip: " .. LOG_DIR)
        return
    end
    local days = tonumber(get_config("max_days")) or 7
    info("Cleaning logs older than " .. days .. " days in " .. LOG_DIR)
    local r = exec("find", LOG_DIR, "-type", "f", "-name", "*.log", "-mtime", "+" .. days, "-delete", "-print")
    if r.ok then
        local count = 0
        for _ in r.stdout:gmatch("[^\n]+") do count = count + 1 end
        info("Deleted " .. count .. " old log file(s)")
    else
        warn("Clean failed: " .. r.stderr)
    end
    -- Also clean rotated logs (*.last, *.old)
    exec("find", LOG_DIR, "-type", "f", "(", "-name", "*.last", "-o", "-name", "*.old", ")", "-mtime", "+" .. days, "-delete")
    -- Write stamp
    write_file(STAMP_FILE, os.date("%Y-%m-%d %H:%M:%S"))
end

local function should_clean_today()
    if not file_exists(STAMP_FILE) then return true end
    local last = read_file(STAMP_FILE)
    return last:sub(1, 10) ~= os.date("%Y-%m-%d")
end

-- Daemon: check every 60s, clean once at configured hour
function M.check()
    local target_hour = tonumber(get_config("clean_hour")) or 3
    local hour = tonumber(os.date("%H"))
    if hour == target_hour and should_clean_today() then
        do_clean()
    end
end

-- Start daemon at boot
function M.service()
    start_daemon("check", 60)
    info("Log rotator daemon started (target hour: " .. (get_config("clean_hour") or "3") .. ")")
end

-- Quick action: clean immediately
function M.clean_now()
    do_clean()
end

return M
