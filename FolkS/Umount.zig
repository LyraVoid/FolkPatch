const std = @import("std");
const linux = std.os.linux;

const MNT_DETACH = 2;

pub fn main() !void {
    const config_file = linux.openat(linux.AT.FDCWD, "UmountPATH", .{ .ACCMODE = .RDONLY }, 0);
    if (linux.errno(config_file) != .SUCCESS) return;
    defer _ = linux.close(@intCast(config_file));

    var file_content: [8192]u8 = undefined;
    const bytes_read = linux.read(@intCast(config_file), &file_content, file_content.len);
    if (linux.errno(bytes_read) != .SUCCESS) return;

    var path_buffer: [512]u8 = undefined;
    var lines = std.mem.splitScalar(u8, file_content[0..bytes_read], '\n');

    while (lines.next()) |line| {
        const mount_path = std.mem.trim(u8, line, " \r\n\t");

        if (mount_path.len == 0) continue;
        if (mount_path.len >= path_buffer.len) continue;

        @memcpy(path_buffer[0..mount_path.len], mount_path);
        path_buffer[mount_path.len] = 0;
        const path_as_c_string: [:0]const u8 = path_buffer[0..mount_path.len :0];

        const unmount_result = linux.umount2(path_as_c_string, MNT_DETACH);
        const error_code = linux.errno(unmount_result);

        if (error_code == .SUCCESS) {
            print("卸载成功: {s}\n", .{path_as_c_string});
        } else {
            print("卸载失败: {s}\n", .{path_as_c_string});
        }
    }
}

fn print(comptime format: []const u8, args: anytype) void {
    var output_buffer: [512]u8 = undefined;
    const text = std.fmt.bufPrint(&output_buffer, format, args) catch return;
    _ = linux.write(linux.STDOUT_FILENO, text.ptr, text.len);
}
