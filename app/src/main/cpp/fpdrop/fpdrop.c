/*
 * fpdrop - 真正的 shell 级降权工具
 *
 * 背景：APatch/KernelSU 的 `su 2000 -c cmd` 只降低 uid/gid，进程仍保留全部
 * capability（CAP_DAC_OVERRIDE 等）且 SELinux context 为 magisk 特权域，
 * 因此仍能读取 /data 下其他应用的私有数据，达不到官方 Shizuku（shell 身份）
 * 的权限隔离效果。
 *
 * 本工具以 root 身份被 server 调用：先把 uid/gid 降为 shell(2000) 并清空
 * 全部 capability（与 adb shell 进程能力一致），再 exec 目标命令。capability
 * 清零后，DAC 权限检查不再被绕过，MT 等应用通过 Shizuku 将无法读取受保护目录。
 *
 * 用法: fpdrop [uid] [gid] [--] cmd [args...]    (默认 uid=gid=2000)
 */
#include <errno.h>
#include <grp.h>
#include <linux/capability.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/prctl.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include <unistd.h>

#ifndef _LINUX_CAPABILITY_U32S_3
#define _LINUX_CAPABILITY_U32S_3 2
#endif

/* 清空 bounding set（uid 已非 0，满足 PR_CAPBSET_DROP 的要求） */
static void drop_bset(void) {
    for (int cap = 0; cap < 64; ++cap) {
        if (prctl(PR_CAPBSET_DROP, (unsigned long)cap, 0UL, 0UL, 0UL) != 0) {
            if (errno == EINVAL) break; /* 超出系统支持的 cap 范围 */
        }
    }
}

/* 清空 effective/permitted/inheritable capability */
static int drop_caps(void) {
    struct __user_cap_header_struct hdr;
    struct __user_cap_data_struct data[_LINUX_CAPABILITY_U32S_3];
    memset(&hdr, 0, sizeof(hdr));
    memset(data, 0, sizeof(data));
    hdr.version = _LINUX_CAPABILITY_VERSION_3;
    hdr.pid = 0;
    return (int)syscall(SYS_capset, &hdr, data);
}

int main(int argc, char **argv) {
    uid_t uid = 2000;
    gid_t gid = 2000;
    int idx = 1;

    if (argc >= 3) {
        char *end;
        errno = 0;
        unsigned long u = strtoul(argv[1], &end, 10);
        if (errno == 0 && *end == '\0') {
            unsigned long g = strtoul(argv[2], &end, 10);
            if (errno == 0 && *end == '\0') {
                uid = (uid_t)u;
                gid = (gid_t)g;
                idx = 3;
            }
        }
    }
    if (idx < argc && strcmp(argv[idx], "--") == 0) {
        idx++;
    }
    if (idx >= argc) {
        fprintf(stderr, "fpdrop: no command\n");
        return 127;
    }

    if (setgroups(0, NULL) != 0) {
        perror("fpdrop: setgroups");
        return 126;
    }
    if (setgid(gid) != 0) {
        perror("fpdrop: setgid");
        return 126;
    }
    if (setuid(uid) != 0) {
        perror("fpdrop: setuid");
        return 126;
    }

    drop_bset();
    if (drop_caps() != 0) {
        perror("fpdrop: capset");
        return 126;
    }

    execvp(argv[idx], &argv[idx]);
    fprintf(stderr, "fpdrop: execvp %s: %s\n", argv[idx], strerror(errno));
    return 127;
}
