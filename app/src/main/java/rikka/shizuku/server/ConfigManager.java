package rikka.shizuku.server;

import androidx.annotation.Nullable;

import java.util.List;

import rikka.shizuku.server.util.Logger;

public abstract class ConfigManager {

    protected static final Logger LOGGER = new Logger("ConfigManager");

    public static final int FLAG_ALLOWED = 1 << 1;
    public static final int FLAG_DENIED = 1 << 2;
    public static final int MASK_PERMISSION = FLAG_ALLOWED | FLAG_DENIED;

    @Nullable
    public abstract ConfigPackageEntry find(int uid);

    public abstract void update(int uid, List<String> packages, int mask, int values);

    public abstract void remove(int uid);

    /**
     * 分权控制：该 uid 的应用通过 Shizuku 执行命令时是否应降级为 shell (uid 2000)。
     * 仅当 server 以 root (uid 0) 运行时生效；shell 模式下所有命令天然是 shell 权限。
     * 默认返回 false（跟随 server 权限），子类可覆盖。
     */
    public boolean isShellOnly(int uid) {
        return false;
    }
}
