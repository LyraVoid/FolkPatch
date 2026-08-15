package me.bmax.apatch.util

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Parcel
import android.util.Base64
import android.util.Log
import me.bmax.apatch.APApplication
import me.bmax.apatch.apApp
import rikka.parcelablelist.ParcelableListSlice
import rikka.shizuku.Shizuku
import rikka.shizuku.server.ServerConstants

/**
 * Shizuku 服务管理器。
 *
 * 管理内置 Shizuku Server 的生命周期：
 * - 通过 app_process 启动 rikka.shizuku.server.ShizukuService
 * - 授权界面由 [me.bmax.apatch.ui.ShizukuPermissionActivity] 提供
 * - 授权记录存储于 /data/user_de/0/com.android.shell/shizuku.json
 */
object ShizukuServiceManager {
    private const val TAG = "ShizukuMgr"

    /** 开关持久化 key */
    const val PREF_SHIZUKU_ENABLED = "shizuku_service_enabled"

    /** shizuku server 进程名（app_process --nice-name） */
    private const val SERVER_PROCESS_NAME = "shizuku_server"

    /** server 入口类 */
    private const val SERVER_CLASS = "rikka.shizuku.server.ShizukuService"
    private const val FLAG_ALLOWED = 1 shl 1
    private const val FLAG_DENIED = 1 shl 2
    private const val MASK_PERMISSION = FLAG_ALLOWED or FLAG_DENIED

    /** 防并发启动：快速反复拨动开关时避免重复拉起多个 server */
    private val startLock = Any()

    fun isEnabled(): Boolean =
        APApplication.sharedPreferences.getBoolean(PREF_SHIZUKU_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        APApplication.sharedPreferences.edit().putBoolean(PREF_SHIZUKU_ENABLED, enabled).apply()
    }

    /**
     * Shizuku Server 是否正在运行。
     * 优先使用 Shizuku API（无需 root，本应用作为 Manager 会被推送 Binder），
     * 未连接时回退到 root 进程检查。
     */
    fun isServerRunning(): Boolean {
        return try {
            if (Shizuku.pingBinder()) {
                true
            } else {
                checkServerProcess()
            }
        } catch (t: Throwable) {
            checkServerProcess()
        }
    }

    /** 通过 root 检查 shizuku_server 进程是否存活 */
    private fun checkServerProcess(): Boolean {
        return try {
            val out = ArrayList<String>()
            val err = ArrayList<String>()
            val result = getRootShell()
                .newJob()
                .add("pidof $SERVER_PROCESS_NAME || ps -A | grep $SERVER_PROCESS_NAME | grep -v grep")
                .to(out, err)
                .exec()
            result.isSuccess && out.isNotEmpty()
        } catch (t: Throwable) {
            Log.e(TAG, "checkServerProcess failed", t)
            false
        }
    }

    /**
     * 启动 Shizuku Server。
     *
     * 以 shell（uid 2000）身份通过 app_process 运行内置 server，与官方 ADB 模式一致：
     * - root 直接跑 app_process 会触发 ART 的 dalvik-cache chown 检查（uid 0 被当作 zygote）并 Abort；
     * - shell 身份则走普通 RuntimeInit，且 shell 持有 Shizuku 所需的 privileged 权限
     *   （INTERACT_ACROSS_USERS_FULL 等）。
     *
     * @return true 表示 server 已就绪（binder 可达）
     */
    fun start(context: Context): Boolean {
        synchronized(startLock) {
            return startInternal(context)
        }
    }

    private fun startInternal(context: Context): Boolean {
        return try {
            if (isServerRunning()) return true
            if (!waitForRoot(15_000L)) {
                Log.e(TAG, "start failed: root not available")
                return false
            }
            // 部署降权工具 fpdrop。失败不阻塞启动：server 端在 fpdrop 缺失时会
            // 降级为直接以当前身份执行命令，功能可用（仅分权不生效）。
            if (!ensureFpDrop()) {
                Log.w(TAG, "fpdrop deploy failed, Shizuku will run without per-app root control")
            }
            val apkPath = context.applicationInfo.sourceDir
            if (apkPath.isBlank()) {
                Log.e(TAG, "start failed: apk path is blank")
                return false
            }
            // app_process needs the extracted native library directory, not an APK-relative path.
            val libraryPath = context.applicationInfo.nativeLibraryDir
            val inner = "nohup env CLASSPATH=\"$apkPath\" app_process " +
                "-Djava.class.path=\"$apkPath\" " +
                "-Dshizuku.library.path=\"$libraryPath\" " +
                "/system/bin --nice-name=$SERVER_PROCESS_NAME $SERVER_CLASS " +
                ">/dev/null 2>&1 &"
            // 不同 su 实现/版本的降权语法存在差异，逐一尝试。
            // 首选以 root (uid 0) 启动 server：与官方 Shizuku root 模式一致，
            // 使通过 Shizuku 执行的命令具备 root 权限（可读取 /data 等受保护目录）。
            // root 启动的 app_process 直接处于全局 mount namespace，config 读写正常。
            // 降级到 shell (uid 2000) 时用 -M 进入全局 namespace 以保证 config 可写。
            val candidates = arrayOf(
                "su -c '$inner'",
                "su -M 2000 -c '$inner'",
                "su 2000 -M -c '$inner'",
                "su 2000 -c '$inner'",
                "su - 2000 -c '$inner'",
            )
            var launched = false
            for (cmd in candidates) {
                try {
                    val result = getRootShell().newJob().add(cmd).exec()
                    if (result.isSuccess) {
                        launched = true
                        break
                    }
                    Log.w(TAG, "start command failed: ${result.err.joinToString()}")
                } catch (t: Throwable) {
                    Log.w(TAG, "start command crashed: $cmd", t)
                }
            }
            if (!launched) return false

            // 等待 server binder 就绪（最长 10 秒，轮询更细以尽早返回）
            repeat(50) {
                Thread.sleep(200L)
                if (isServerRunning()) return true
            }
            Log.e(TAG, "start timed out waiting for server binder")
            false
        } catch (t: Throwable) {
            Log.e(TAG, "start failed", t)
            false
        }
    }

    /**
     * 等待 root 可用（开机早期或 root 授权未就绪时 root shell 可能尚未就绪）。
     * @return timeoutMs 内 root 是否可用
     */
    /**
     * 部署降权工具 fpdrop 到 /data/local/tmp（root 可执行）。
     * 用 base64 经内存直写，避免 root shell 读取 app 私有 cache（app_data_file）
     * 被部分 su 实现（如 KernelSU/KernelPatch 的受限 context）拒绝。
     */
    private fun ensureFpDrop(): Boolean {
        return try {
            val bytes = contextForAssets().assets.open("fpdrop").readBytes()
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val result = getRootShell().newJob()
                .add("mkdir -p /data/local/tmp")
                .add("echo '$b64' | base64 -d > /data/local/tmp/fpdrop")
                .add("chmod 755 /data/local/tmp/fpdrop")
                .exec()
            if (!result.isSuccess) {
                Log.e(TAG, "ensureFpDrop failed: ${result.err.joinToString()}")
            } else {
                Log.i(TAG, "fpdrop deployed (${bytes.size} bytes)")
            }
            result.isSuccess
        } catch (t: Throwable) {
            Log.e(TAG, "ensureFpDrop crashed", t)
            false
        }
    }

    private fun contextForAssets(): Context = apApp

    fun waitForRoot(timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                if (getRootShell().isRoot) return true
            } catch (t: Throwable) {
                Log.w(TAG, "root not ready", t)
            }
            try {
                Thread.sleep(500L)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        return try {
            getRootShell().isRoot
        } catch (t: Throwable) {
            false
        }
    }

    /** 停止 Shizuku Server。优先走 Manager 通道优雅退出，再以 root 强杀兜底，并确认进程真正消失。 */
    fun stop(): Boolean {
        return try {
            // 1. 优雅退出：本应用是 Manager，binder 存活时由 server 内部 System.exit(0)
            if (Shizuku.pingBinder()) {
                try {
                    Shizuku.exit()
                } catch (t: Throwable) {
                    Log.w(TAG, "Shizuku.exit() failed, falling back to kill", t)
                }
                if (waitStopped()) return true
            }
            // 2. 兜底：pidof 精确匹配进程名后 kill -9
            getRootShell().newJob()
                .add("kill -9 \\$(pidof $SERVER_PROCESS_NAME) 2>/dev/null || true")
                .exec()
            waitStopped()
        } catch (t: Throwable) {
            Log.e(TAG, "stop failed", t)
            false
        }
    }

    /** 轮询等待 server 完全停止，最多 5 秒。 */
    private fun waitStopped(): Boolean {
        repeat(20) {
            Thread.sleep(250L)
            if (!isServerRunning()) return true
        }
        Log.w(TAG, "stop timed out waiting for server to exit")
        return false
    }

    /** Returns Shizuku-compatible applications exposed by the embedded server. */
    fun getApplications(): List<PackageInfo> {
        if (!Shizuku.pingBinder()) return emptyList()
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken("moe.shizuku.server.IShizukuService")
            data.writeInt(-1)
            Shizuku.getBinder()!!.transact(ServerConstants.BINDER_TRANSACTION_getApplications, data, reply, 0)
            reply.readException()
            @Suppress("UNCHECKED_CAST")
            (ParcelableListSlice.CREATOR.createFromParcel(reply) as ParcelableListSlice<PackageInfo>).list.orEmpty()
        } catch (t: Throwable) {
            Log.e(TAG, "getApplications failed", t)
            emptyList()
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun isAllowed(uid: Int): Boolean =
        (Shizuku.getFlagsForUid(uid, MASK_PERMISSION) and FLAG_ALLOWED) != 0

    fun setAllowed(uid: Int, allowed: Boolean) {
        Shizuku.updateFlagsForUid(uid, MASK_PERMISSION, if (allowed) FLAG_ALLOWED else 0)
    }

    /**
     * 分权控制：该 uid 的应用是否降级为 shell (uid 2000) 执行。
     * 仅当 server 以 root 运行时才有区分意义；shell 模式下所有命令天然是 shell 权限。
     */
    fun getShellOnly(uid: Int): Boolean {
        if (!Shizuku.pingBinder()) return false
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken("moe.shizuku.server.IShizukuService")
            data.writeInt(uid)
            Shizuku.getBinder()!!.transact(ServerConstants.BINDER_TRANSACTION_getShellOnly, data, reply, 0)
            reply.readException()
            reply.readInt() != 0
        } catch (t: Throwable) {
            Log.e(TAG, "getShellOnly failed", t)
            false
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    fun setShellOnly(uid: Int, shellOnly: Boolean) {
        if (!Shizuku.pingBinder()) return
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("moe.shizuku.server.IShizukuService")
            data.writeInt(uid)
            data.writeInt(if (shellOnly) 1 else 0)
            Shizuku.getBinder()!!.transact(ServerConstants.BINDER_TRANSACTION_setShellOnly, data, reply, 0)
            reply.readException()
        } catch (t: Throwable) {
            Log.e(TAG, "setShellOnly failed", t)
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    /** server 是否以 root (uid 0) 运行（决定分权开关是否有意义）。 */
    fun isRootServer(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.getUid() == 0
        } catch (t: Throwable) {
            false
        }
    }

    // ==================== Log reading ====================

    /** Path of the server's persisted log file (same directory as shizuku.json; survives restarts). */
    private const val SERVER_LOG_FILE = "/data/user_de/0/com.android.shell/shizuku_folk.log"
    private const val SERVER_LOG_FILE_BACKUP = "/data/user_de/0/com.android.shell/shizuku_folk.log.1"

    /** Logcat tags used by the server's Logger, for grabbing extra context. */
    private val LOGCAT_TAGS = arrayOf(
        "Service", "ConfigManager", "ClientManager", "UserServiceManager",
        "ShizukuService", "Starter", "AppProcess",
    )

    /**
     * Reads the server's persisted log.
     *
     * Prefers the binder (fastest while the server is alive, includes the latest lines from the
     * memory buffer); when the binder is unreachable it falls back to reading the log file directly
     * via root — so even if the server crashed or failed to start, the log left by the previous
     * session is still visible (key to troubleshooting "config disappeared" / "server won't start").
     */
    fun getServerLog(): String {
        if (Shizuku.pingBinder()) {
            val viaBinder = getLogViaBinder()
            if (viaBinder != null) return viaBinder
        }
        return readLogFileViaRoot()
    }

    private fun getLogViaBinder(): String? {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken("moe.shizuku.server.IShizukuService")
            Shizuku.getBinder()!!.transact(ServerConstants.BINDER_TRANSACTION_getLog, data, reply, 0)
            reply.readException()
            reply.readString().orEmpty()
        } catch (t: Throwable) {
            Log.e(TAG, "getLog via binder failed", t)
            null
        } finally {
            reply.recycle()
            data.recycle()
        }
    }

    /** Fallback when the server is unreachable: read the log files (including the rotated backup) directly via root. */
    private fun readLogFileViaRoot(): String {
        return try {
            val out = ArrayList<String>()
            val err = ArrayList<String>()
            getRootShell().newJob()
                .add("cat $SERVER_LOG_FILE_BACKUP 2>/dev/null; cat $SERVER_LOG_FILE 2>/dev/null")
                .to(out, err)
                .exec()
            out.joinToString("\n")
        } catch (t: Throwable) {
            Log.e(TAG, "readLogFileViaRoot failed", t)
            ""
        }
    }

    /**
     * Grabs live logcat entries for the Shizuku server's tags, adding system-side context
     * beyond the binder/file logs (e.g. ART/zygote aborts, SELinux denials in early startup).
     */
    fun getLogcat(): String {
        return try {
            val out = ArrayList<String>()
            val err = ArrayList<String>()
            val filter = LOGCAT_TAGS.joinToString(" ") { "$it:V" }
            getRootShell().newJob()
                .add("logcat -d -v time -t 2000 $filter *:S 2>/dev/null")
                .to(out, err)
                .exec()
            out.joinToString("\n")
        } catch (t: Throwable) {
            Log.e(TAG, "getLogcat failed", t)
            ""
        }
    }

    /** Clears the server's persisted log (binder first, falls back to deleting the files via root). */
    fun clearServerLog(): Boolean {
        if (Shizuku.pingBinder()) {
            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken("moe.shizuku.server.IShizukuService")
                Shizuku.getBinder()!!.transact(ServerConstants.BINDER_TRANSACTION_clearLog, data, reply, 0)
                reply.readException()
                return true
            } catch (t: Throwable) {
                Log.e(TAG, "clearLog via binder failed", t)
            } finally {
                reply.recycle()
                data.recycle()
            }
        }
        return try {
            getRootShell().newJob()
                .add("rm -f $SERVER_LOG_FILE $SERVER_LOG_FILE_BACKUP")
                .exec()
                .isSuccess
        } catch (t: Throwable) {
            Log.e(TAG, "clearServerLog via root failed", t)
            false
        }
    }
}
