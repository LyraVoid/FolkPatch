const std = @import("std");

const PropItem = struct {
    key: []const u8,
    value: []const u8,
};

fn setProp(gpa: std.mem.Allocator, io: std.Io, key: []const u8, value: []const u8) !void {
    const result = try std.process.run(gpa, io, .{
        .argv = &[_][]const u8{ "resetprop", "-n", key, value },
    });
    gpa.free(result.stdout);
    gpa.free(result.stderr);
}

fn getProp(gpa: std.mem.Allocator, io: std.Io, key: []const u8) !?[]const u8 {
    const result = try std.process.run(gpa, io, .{
        .argv = &[_][]const u8{ "resetprop", key },
        .stdout_limit = .limited(1024),
        .stderr_limit = .limited(1024),
    });
    gpa.free(result.stderr);
    errdefer gpa.free(result.stdout);

    const output = std.mem.trim(u8, result.stdout, " \n\r\t");
    if (output.len == 0) {
        gpa.free(result.stdout);
        return null;
    }
    return output;
}

pub fn main(init: std.process.Init) !void {
    const gpa = init.gpa;
    const io = init.io;

    const core_list = [_]PropItem{
        .{ .key = "ro.boot.vbmeta.device_state", .value = "locked" },
        .{ .key = "ro.boot.verifiedbootstate", .value = "green" },
        .{ .key = "ro.boot.flash.locked", .value = "1" },
        .{ .key = "ro.boot.veritymode", .value = "enforcing" },
        .{ .key = "ro.boot.warranty_bit", .value = "0" },
        .{ .key = "ro.warranty_bit", .value = "0" },
        .{ .key = "ro.debuggable", .value = "0" },
        .{ .key = "ro.force.debuggable", .value = "0" },
        .{ .key = "ro.secure", .value = "1" },
        .{ .key = "ro.adb.secure", .value = "1" },
        .{ .key = "ro.build.type", .value = "user" },
        .{ .key = "ro.build.tags", .value = "release-keys" },
        .{ .key = "ro.vendor.boot.warranty_bit", .value = "0" },
        .{ .key = "ro.vendor.warranty_bit", .value = "0" },
        .{ .key = "vendor.boot.vbmeta.device_state", .value = "locked" },
        .{ .key = "vendor.boot.verifiedbootstate", .value = "green" },
        .{ .key = "sys.oem_unlock_allowed", .value = "0" },
        .{ .key = "ro.secureboot.lockstate", .value = "locked" },
        .{ .key = "ro.boot.realmebootstate", .value = "green" },
        .{ .key = "ro.boot.realme.lockstate", .value = "1" },
    };

    for (core_list) |item| {
        try setProp(gpa, io, item.key, item.value);
    }

    const boot_keys = [_][]const u8{ "ro.bootmode", "ro.boot.bootmode", "vendor.boot.bootmode" };
    for (boot_keys) |key| {
        if (try getProp(gpa, io, key)) |val| {
            defer gpa.free(val);
            if (std.mem.indexOf(u8, val, "recovery") != null) {
                try setProp(gpa, io, key, "unknown");
            }
        }
    }

    const patch_list = [_]PropItem{
        .{ .key = "ro.boot.vbmeta.device_state", .value = "locked" },
        .{ .key = "ro.boot.vbmeta.invalidate_on_error", .value = "yes" },
        .{ .key = "ro.boot.vbmeta.avb_version", .value = "1.0" },
        .{ .key = "ro.boot.vbmeta.hash_alg", .value = "sha256" },
        .{ .key = "ro.boot.vbmeta.size", .value = "4096" },
    };

    for (patch_list) |item| {
        if (try getProp(gpa, io, item.key)) |val| {
            gpa.free(val);
        } else {
            try setProp(gpa, io, item.key, item.value);
        }
    }
}