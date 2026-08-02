local M = {}

-- Wait up to N seconds for a path to become ready (e.g. /sdcard after unlock)
local function wait_for_path(path, timeout_sec)
    local waited = 0
    while waited < timeout_sec do
        local r = exec("test", "-d", path)
        if r.ok then
            info("Path ready after " .. waited .. "s: " .. path)
            return true
        end
        exec("sleep", "1")
        waited = waited + 1
    end
    warn("Timeout waiting for path (" .. timeout_sec .. "s): " .. path)
    return false
end

local function run_commands()
    local cmds = get_config("commands")
    if cmds == "" then
        info("No commands configured")
        return
    end
    for line in cmds:gmatch("[^\n]+") do
        line = line:match("^%s*(.-)%s*$")
        if line ~= "" and line:sub(1, 1) ~= "#" then
            info("Running: " .. line)
            local r = exec("sh", "-c", line)
            if r.ok then
                if r.stdout ~= "" then info("  stdout: " .. r.stdout) end
            else
                warn("  failed (code " .. r.code .. "): " .. r.stderr)
            end
        end
    end
    info("All commands executed")
end

local function try_run(stage)
    local target = get_config("stage")
    if target == "" then target = "boot_completed" end
    if stage == target then
        info("Stage [" .. stage .. "] matched, executing...")
        -- If configured, wait for a path to be ready first
        local wait_path = get_config("wait_path")
        local wait_time = tonumber(get_config("wait_time")) or 0
        if wait_path ~= "" and wait_time > 0 then
            wait_for_path(wait_path, wait_time)
        end
        run_commands()
    end
end

function M.post_fs_data()
    try_run("post_fs_data")
end

function M.service()
    try_run("service")
end

function M.boot_completed()
    try_run("boot_completed")
end

-- Quick action: run manually regardless of stage
function M.run_now()
    run_commands()
end

return M
