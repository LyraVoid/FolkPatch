-- Hello Plugin
-- Example of the FolkPatch plugin API.
-- Run a callback manually:  apd plugin run hello-plugin post_fs_data

local function demo_api()
    -- Log helpers
    info("hello-plugin post_fs_data")
    warn("this is a warning")

    -- System properties
    local version = getprop("ro.build.version.release")
    info("Android version: " .. version)

    -- exec returns a table with ok / code / stdout / stderr
    local res = exec("getprop", "ro.product.model")
    info("Model: " .. res.stdout)

    -- exec with a single shell command string
    local sh = exec("sh", "-c", "echo hello from shell; uname -r")
    info("Shell: " .. sh.stdout)

    -- File I/O
    write_file("/data/adb/plugins/hello-plugin/hello.txt", "hello from plugin\n")
    info("read back: " .. read_file("/data/adb/plugins/hello-plugin/hello.txt"))

    -- sysctl (write kernel parameter)
    -- sysctl("vm/swappiness", "60")

    -- mkdir / chmod / rm
    -- mkdir("/data/adb/plugins/hello-plugin/tmp", true)
    -- chmod("/data/adb/plugins/hello-plugin/hello.txt", 0o644)
    -- rm("/data/adb/plugins/hello-plugin/hello.txt")
end

return {
    post_fs_data = function()
        demo_api()
    end,

    service = function()
        info("hello-plugin service")
    end,

    boot_completed = function()
        info("hello-plugin boot_completed")
    end,
}
