package me.bmax.apatch.util

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Parcel
import android.util.Log
import me.bmax.apatch.APApplication
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
            // 不同 su 实现/版本的降权语法存在差异，逐一尝试
            val candidates = arrayOf(
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
}
