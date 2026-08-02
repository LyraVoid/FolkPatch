local M = {}

local TEST_FILE = "/sdcard/folkpatch_tick_test.txt"

-- Start daemon at service stage (survives reboot)
function M.service()
    start_daemon("tick", 5)
    info("Daemon started: checking every 5s")
end

-- Called every 5 seconds by the daemon
function M.tick()
    if file_exists(TEST_FILE) then
        info("File already exists, skip: " .. TEST_FILE)
    else
        write_file(TEST_FILE, "Created by file-tick-test plugin at " .. os.date("%Y-%m-%d %H:%M:%S") .. "\n")
        info("File created: " .. TEST_FILE)
    end
end

-- Quick action: delete the test file so next tick recreates it
function M.clean_now()
    if file_exists(TEST_FILE) then
        rm(TEST_FILE)
        info("Test file deleted: " .. TEST_FILE)
    else
        info("No test file to delete")
    end
end

return M
