package me.bmax.apatch.util

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.io.SuFile
import me.bmax.apatch.APApplication
import me.bmax.apatch.APApplication.Companion.SUPERCMD
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.screen.MODULE_TYPE
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.zip.ZipFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "APatchCli"
private const val SHELL_TIMEOUT_MS = 10_000L

private fun getKPatchPath(): String {
    return apApp.applicationInfo.nativeLibraryDir + File.separator + "libkpatch.so"
}

class RootShellInitializer : Shell.Initializer() {
    override fun onInit(context: Context, shell: Shell): Boolean {
        shell.newJob().add(
            "export PATH=\$PATH:/system_ext/bin:/vendor/bin:${APApplication.APATCH_FOLDER}bin",
            "export BUSYBOX=${APApplication.APATCH_FOLDER}bin/busybox"
        ).exec()
        return true
    }
}

private fun buildWithTimeout(builder: Shell.Builder, vararg commands: String): Shell {
    var result: Shell? = null
    var error: Throwable? = null
    val t = Thread {
        try {
            result = builder.build(*commands)
        } catch (e: Throwable) {
            error = e
        }
    }
    t.name = "shell-build-${commands.firstOrNull() ?: "unknown"}"
    t.start()
    t.join(SHELL_TIMEOUT_MS)
    if (t.isAlive) {
        t.interrupt()
        throw IOException("Shell creation timed out after ${SHELL_TIMEOUT_MS}ms: ${commands.joinToString(" ")}")
    }
    return result ?: throw (error ?: IOException("Shell creation failed"))
}

fun createRootShell(globalMnt: Boolean = false): Shell {
    Shell.enableVerboseLogging = BuildConfig.DEBUG
    val builder = Shell.Builder.create().setInitializers(RootShellInitializer::class.java)

    if (android.os.Process.myUid() == 0 && !globalMnt) {
        try {
            return buildWithTimeout(builder, "sh")
        } catch (e: Throwable) {
            Log.e(TAG, "sh failed for root process", e)
        }
    }

    return try {
        buildWithTimeout(
            builder, SUPERCMD, APApplication.superKey, "-Z", APApplication.MAGISK_SCONTEXT
        )
    } catch (e: Throwable) {
        Log.e(TAG, "su failed: ", e)
        return try {
            Log.e(TAG, "retry compat kpatch su")
            if (globalMnt) {
                buildWithTimeout(
                    builder,
                    getKPatchPath(), APApplication.superKey, "su", "-Z", APApplication.MAGISK_SCONTEXT, "--mount-master"
                )
            }else{
                buildWithTimeout(
                    builder,
                    getKPatchPath(), APApplication.superKey, "su", "-Z", APApplication.MAGISK_SCONTEXT
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "retry kpatch su failed: ", e)
            return try {
                Log.e(TAG, "retry su: ", e)
                if (globalMnt) {
                    buildWithTimeout(builder, "su","-mm")
                }else{
                    buildWithTimeout(builder, "su")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "retry su failed: ", e)
                return buildWithTimeout(builder, "sh")
            }
        }
    }
}

object APatchCli {
    var SHELL: Shell = createRootShell()
    val GLOBAL_MNT_SHELL: Shell = createRootShell(true)
    fun refresh() {
        val tmp = SHELL
        SHELL = createRootShell()
        tmp.close()
    }
}

fun getRootShell(globalMnt: Boolean = false): Shell {

    return if (globalMnt) APatchCli.GLOBAL_MNT_SHELL else {
        APatchCli.SHELL
    }
}

inline fun <T> withNewRootShell(
    globalMnt: Boolean = false,
    block: Shell.() -> T
): T {
    return createRootShell(globalMnt).use(block)
}

fun rootAvailable(): Boolean {
    val shell = getRootShell()
    return shell.isRoot
}

fun tryGetRootShell(): Shell {
    Shell.enableVerboseLogging = BuildConfig.DEBUG
    val builder = Shell.Builder.create()
    return try {
        builder.build(
            SUPERCMD, APApplication.superKey, "-Z", APApplication.MAGISK_SCONTEXT
        )
    } catch (e: Throwable) {
        Log.e(TAG, "su failed: ", e)
        return try {
            Log.e(TAG, "retry compat kpatch su")
            builder.build(
                getKPatchPath(), APApplication.superKey, "su", "-Z", APApplication.MAGISK_SCONTEXT
            )
        } catch (e: Throwable) {
            Log.e(TAG, "retry kpatch su failed: ", e)
            return try {
                Log.e(TAG, "retry su: ", e)
                builder.build("su")
            } catch (e: Throwable) {
                Log.e(TAG, "retry su failed: ", e)
                builder.build("sh")
            }
        }
    }
}

fun shellForResult(shell: Shell, vararg cmds: String): Shell.Result {
    val out = ArrayList<String>()
    val err = ArrayList<String>()
    return shell.newJob().add(*cmds).to(out, err).exec()
}

fun rootShellForResult(vararg cmds: String): Shell.Result {
    val out = ArrayList<String>()
    val err = ArrayList<String>()
    return getRootShell().newJob().add(*cmds).to(out, err).exec()
}

fun execApd(args: String, newShell: Boolean = false): Boolean {
    return if (newShell) {
        withNewRootShell {
            ShellUtils.fastCmdResult(this, "${APApplication.APD_PATH} $args")
        }
    } else {
        ShellUtils.fastCmdResult(getRootShell(), "${APApplication.APD_PATH} $args")
    }
}

suspend fun listModules(): String = withContext(Dispatchers.IO) {
    val shell = getRootShell()
    val out = try {
        withTimeout(30000L) {
            shell.newJob().add("${APApplication.APD_PATH} module list").to(ArrayList(), null).exec().out
        }
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "listModules timed out after 30 seconds")
        ArrayList<String>()
    } catch (e: Exception) {
        Log.e(TAG, "listModules failed: ${e.message}")
        ArrayList<String>()
    }
    withNewRootShell {
        newJob().add("cp /data/user/*/me.bmax.apatch/patch/ori.img /data/adb/ap/ && rm /data/user/*/me.bmax.apatch/patch/ori.img")
            .to(ArrayList(), null).exec()
    }
    return@withContext out.joinToString("\n").ifBlank { "[]" }
}

fun toggleModule(id: String, enable: Boolean): Boolean {
    val cmd = if (enable) {
        "module enable $id"
    } else {
        "module disable $id"
    }
    val result = execApd(cmd,true)
    Log.i(TAG, "$cmd result: $result")
    return result
}

fun uninstallModule(id: String): Boolean {
    val cmd = "module uninstall $id"
    val result = execApd(cmd,true)
    Log.i(TAG, "uninstall module $id result: $result")
    return result
}

fun undoUninstallModule(id: String): Boolean {
    val cmd = "module undo-uninstall $id"
    val result = execApd(cmd, true)
    Log.i(TAG, "undo uninstall module $id result: $result")
    return result
}

fun installModule(
    uri: Uri, type: MODULE_TYPE, onFinish: (Boolean) -> Unit, onStdout: (String) -> Unit, onStderr: (String) -> Unit
): Boolean {
    val permissionMessage = apApp.getString(R.string.file_picker_permission_desc)
    val inputStream = try {
        SafeUriResolver.openInputStream(apApp, uri)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open input stream", e)
        Handler(Looper.getMainLooper()).post {
            me.bmax.apatch.util.ui.showToast(apApp, permissionMessage)
        }
        onStderr("$permissionMessage\n")
        onFinish(false)
        return false
    }
    inputStream.use { input ->
        val file = File(apApp.cacheDir, "module_$type.zip")
        file.outputStream().use { output ->
            input.copyTo(output)
        }

        // Auto Backup Logic
        val fileName = getFileNameFromUri(apApp, uri)
        val backupSubDir = if (type == MODULE_TYPE.APM) "APM" else "KPM"
        
        // Create a temp copy for backup to prevent race condition (ENOENT) when file is deleted after install
        val backupTempFile = File(apApp.cacheDir, "backup_${System.currentTimeMillis()}_${file.name}")
        try {
            file.copyTo(backupTempFile, overwrite = true)
            
            // Launch backup asynchronously without blocking the main thread
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = ModuleBackupUtils.autoBackupModule(apApp, backupTempFile, fileName, backupSubDir)
                    withContext(Dispatchers.Main) {
                        if (result != null && !result.startsWith("Duplicate")) {
                            onStdout("Auto backup failed: $result\n")
                        } else if (result != null && result.startsWith("Duplicate")) {
                            // onStdout("Auto backup skipped: Duplicate found\n")
                        } else {
                            onStdout("Auto backup success\n")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onStdout("Auto backup error: ${e.message}\n")
                    }
                } finally {
                    // Clean up the temporary backup file
                    if (backupTempFile.exists()) {
                        backupTempFile.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create temp backup file", e)
            onStdout("Auto backup failed: Could not create temp file\n")
        }

        val stdoutCallback: CallbackList<String?> = object : CallbackList<String?>() {
            override fun onAddElement(s: String?) {
                onStdout(s ?: "")
            }
        }

        val stderrCallback: CallbackList<String?> = object : CallbackList<String?>() {
            override fun onAddElement(s: String?) {
                onStderr(s ?: "")
            }
        }

        val shell = getRootShell()

        var result = false
        if(type == MODULE_TYPE.APM) {
            val cmd = "${APApplication.APD_PATH} module install ${file.absolutePath}"
            // Add timeout to prevent hanging installations
            result = try {
                runBlocking {
                    withTimeout(300000L) { // 5 minute timeout
                        shell.newJob()
                            .add(cmd)
                            .to(stdoutCallback, stderrCallback)
                            .exec()
                            .isSuccess
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Module installation timed out after 5 minutes")
                onStderr("Installation timed out. The module may be incompatible or hanging.\n")
                false
            } catch (e: Exception) {
                Log.e(TAG, "Module installation failed", e)
                onStderr("Installation failed: ${e.message}\n")
                false
            }
        } else {
//            ZipUtils.
        }

        Log.i(TAG, "install $type module $uri result: $result")

        file.delete()

        onFinish(result)
        return result
    }
}

fun runAPModuleAction(
    moduleId: String, onStdout: (String) -> Unit, onStderr: (String) -> Unit
): Boolean {
    val stdoutCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStdout(s ?: "")
        }
    }

    val stderrCallback: CallbackList<String?> = object : CallbackList<String?>() {
        override fun onAddElement(s: String?) {
            onStderr(s ?: "")
        }
    }

    val result = withNewRootShell{ 
        newJob().add("${APApplication.APD_PATH} module action $moduleId")
        .to(stdoutCallback, stderrCallback).exec()
    }
    Log.i(TAG, "APModule runAction result: $result")

    return result.isSuccess
}

fun reboot(reason: String = "") {
    if (reason == "recovery") {
        // KEYCODE_POWER = 26, hide incorrect "Factory data reset" message
        getRootShell().newJob().add("/system/bin/input keyevent 26").exec()
    }
    getRootShell().newJob()
        .add("/system/bin/svc power reboot $reason || /system/bin/reboot $reason").exec()
}

fun hasMagisk(): Boolean {
    val shell = getRootShell()
    val result = shell.newJob().add("nsenter --mount=/proc/1/ns/mnt which magisk").exec()
    Log.i(TAG, "has magisk: ${result.isSuccess}")
    return result.isSuccess
}

fun isGlobalNamespaceEnabled(): Boolean {
    val shell = getRootShell()
    val result = ShellUtils.fastCmd(shell, "cat ${APApplication.GLOBAL_NAMESPACE_FILE}")
    Log.i(TAG, "is global namespace enabled: $result")
    return result == "1"
}

fun setGlobalNamespaceEnabled(value: String) {
    getRootShell().newJob().add("echo $value > ${APApplication.GLOBAL_NAMESPACE_FILE}")
        .submit { result ->
            Log.i(TAG, "setGlobalNamespaceEnabled result: ${result.isSuccess} [${result.out}]")
        }
}

fun isMagicMountEnabled(): Boolean {
    val magicMount = SuFile(APApplication.MAGIC_MOUNT_FILE)
    magicMount.shell = getRootShell()
    return magicMount.exists()
}

fun setMagicMountEnabled(enable: Boolean) {
    getRootShell().newJob().add("${if (enable) "touch" else "rm -rf"} ${APApplication.MAGIC_MOUNT_FILE}")
        .submit { result ->
            Log.i(TAG, "setMagicMountEnabled result: ${result.isSuccess} [${result.out}]")
        }
}

fun isHideServiceEnabled(): Boolean {
    val hideService = SuFile(APApplication.HIDE_SERVICE_FILE)
    hideService.shell = getRootShell()
    return hideService.exists()
}

fun setHideServiceEnabled(enable: Boolean) {
    val shell = getRootShell()
    shell.newJob().add("${if (enable) "touch" else "rm -rf"} ${APApplication.HIDE_SERVICE_FILE}")
        .submit { result ->
            Log.i(TAG, "setHideServiceEnabled result: ${result.isSuccess} [${result.out}]")
        }
    // 如果启用，异步执行一次 Hide 二进制（避免阻塞 UI 线程）
    if (enable) {
        CoroutineScope(Dispatchers.IO).launch {
            executeHideBinary()
        }
    }
}

fun isUtsSpoofEnabled(): Boolean {
    val flagFile = SuFile(APApplication.UTS_SPOOF_ENABLE_FILE)
    flagFile.shell = getRootShell()
    return flagFile.exists()
}

fun setUtsSpoofEnabled(enable: Boolean) {
    val shell = getRootShell()
    shell.newJob().add("${if (enable) "touch" else "rm -f"} ${APApplication.UTS_SPOOF_ENABLE_FILE}")
        .exec()
}

fun writeUtsSpoofConfig(release: String, version: String) {
    val shell = getRootShell()
    val escapedRelease = release.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "'\\''")
    val escapedVersion = version.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "'\\''")
    val json = "{\"release\":\"$escapedRelease\",\"version\":\"$escapedVersion\"}"
    shell.newJob().add("echo '$json' > ${APApplication.UTS_SPOOF_CONFIG_FILE}")
        .exec()
}

fun removeUtsSpoofConfig() {
    val shell = getRootShell()
    shell.newJob().add("rm -f ${APApplication.UTS_SPOOF_CONFIG_FILE}").exec()
}

fun isPathHideEnabled(): Boolean {
    val flagFile = SuFile(APApplication.PATHHIDE_ENABLE_FILE)
    flagFile.shell = getRootShell()
    return flagFile.exists()
}

fun setPathHideEnabled(enable: Boolean) {
    val shell = getRootShell()
    shell.newJob().add("mkdir -p ${APApplication.PATHHIDE_DIR}").exec()
    shell.newJob().add("${if (enable) "touch" else "rm -f"} ${APApplication.PATHHIDE_ENABLE_FILE}")
        .exec()
}

fun writePathHidePaths(paths: String) {
    val shell = getRootShell()
    shell.newJob().add("mkdir -p ${APApplication.PATHHIDE_DIR}").exec()
    val escapedPaths = normalizePathHidePaths(paths).replace("'", "'\\''")
    shell.newJob().add("echo -n '$escapedPaths' > ${APApplication.PATHHIDE_PATHS_FILE}")
        .exec()
}

fun readPathHidePaths(): String {
    val shell = getRootShell()
    val raw = ShellUtils.fastCmd(shell, "cat ${APApplication.PATHHIDE_PATHS_FILE} 2>/dev/null") ?: ""
    return normalizePathHidePaths(raw)
}

fun normalizePathHidePaths(paths: String): String {
    return paths.lines()
        .mapNotNull { normalizePathHidePath(it) }
        .distinct()
        .joinToString("\n")
}

private fun normalizePathHidePath(path: String): String? {
    val trimmed = path.trim()
    if (trimmed.isEmpty() || !trimmed.startsWith("/")) {
        return null
    }

    var normalized = trimmed.replace(Regex("/+"), "/")
    while (normalized.length > 1 && normalized.endsWith("/")) {
        normalized = normalized.dropLast(1)
    }

    return normalized.ifEmpty { null }
}

fun writePathHideUids(uids: String) {
    val shell = getRootShell()
    shell.newJob().add("mkdir -p ${APApplication.PATHHIDE_DIR}").exec()
    val escapedUids = uids.replace("'", "'\\''")
    shell.newJob().add("echo -n '$escapedUids' > ${APApplication.PATHHIDE_UIDS_FILE}")
        .exec()
}

fun readPathHideUids(): String {
    val shell = getRootShell()
    return ShellUtils.fastCmd(shell, "cat ${APApplication.PATHHIDE_UIDS_FILE} 2>/dev/null") ?: ""
}

fun isPathHideUidModeEnabled(): Boolean {
    val file = SuFile(APApplication.PATHHIDE_UID_MODE_FILE)
    file.shell = getRootShell()
    return file.exists()
}

fun isPathHideFilterSystemEnabled(): Boolean {
    val file = SuFile(APApplication.PATHHIDE_FILTER_SYSTEM_FILE)
    file.shell = getRootShell()
    return file.exists()
}

fun setPathHideUidMode(enable: Boolean) {
    val shell = getRootShell()
    shell.newJob().add("mkdir -p ${APApplication.PATHHIDE_DIR}").exec()
    shell.newJob().add("${if (enable) "touch" else "rm -f"} ${APApplication.PATHHIDE_UID_MODE_FILE}")
        .exec()
}

fun setPathHideFilterSystem(enable: Boolean) {
    val shell = getRootShell()
    shell.newJob().add("mkdir -p ${APApplication.PATHHIDE_DIR}").exec()
    shell.newJob().add("${if (enable) "touch" else "rm -f"} ${APApplication.PATHHIDE_FILTER_SYSTEM_FILE}")
        .exec()
}

fun setNetIsolateEnabled(enable: Boolean) {
    val shell = getRootShell()
    shell.newJob().add("mkdir -p ${APApplication.NETISOLATE_DIR}").exec()
    shell.newJob().add("${if (enable) "touch" else "rm -f"} ${APApplication.NETISOLATE_ENABLE_FILE}")
        .exec()
}

fun writeNetIsolateUids(uids: String) {
    val shell = getRootShell()
    shell.newJob().add("mkdir -p ${APApplication.NETISOLATE_DIR}").exec()
    val escapedUids = uids.replace("'", "'\\''")
    shell.newJob().add("echo -n '$escapedUids' > ${APApplication.NETISOLATE_UIDS_FILE}").exec()
}

fun isNetIsolateEnabled(): Boolean {
    val flagFile = SuFile(APApplication.NETISOLATE_ENABLE_FILE)
    flagFile.shell = getRootShell()
    return flagFile.exists()
}

fun readNetIsolateUids(): String {
    val shell = getRootShell()
    return ShellUtils.fastCmd(shell, "cat ${APApplication.NETISOLATE_UIDS_FILE} 2>/dev/null") ?: ""
}

fun executeHideBinary(): Boolean {
    val shell = getRootShell()
    val context = apApp.applicationContext

    // 确保 fp/bin 目录存在
    shell.newJob().add("mkdir -p /data/adb/fp/bin").exec()

    // 从 assets 复制 fpd 二进制文件到可执行目录
    try {
        val fpdAsset = context.assets.open("Service/fpd")
        val tempFile = File(context.cacheDir, "fpd_temp")
        tempFile.outputStream().use { output ->
            fpdAsset.copyTo(output)
        }
        fpdAsset.close()

        // 复制到目标目录并设置权限，然后执行
        val cmds = arrayOf(
            "cp ${tempFile.absolutePath} ${APApplication.HIDE_BINARY_PATH}",
            "chmod 755 ${APApplication.HIDE_BINARY_PATH}",
            "restorecon ${APApplication.HIDE_BINARY_PATH}",
            "${APApplication.HIDE_BINARY_PATH} -hide"
        )

        val result = shell.newJob().add(*cmds).exec()
        tempFile.delete()

        Log.i(TAG, "executeHideBinary result: ${result.isSuccess} [${result.out}]")
        return result.isSuccess
    } catch (e: Exception) {
        Log.e(TAG, "executeHideBinary failed: ${e.message}", e)
        return false
    }
}

fun isUmountServiceEnabled(): Boolean {
    val umountService = SuFile(APApplication.UMOUNT_SERVICE_FILE)
    umountService.shell = getRootShell()
    return umountService.exists()
}

fun setUmountServiceEnabled(enabled: Boolean): Boolean {
    val shell = getRootShell()
    val result = if (enabled) {
        shell.newJob().add("touch ${APApplication.UMOUNT_SERVICE_FILE}").exec().isSuccess
    } else {
        shell.newJob().add("rm -rf ${APApplication.UMOUNT_SERVICE_FILE}").exec().isSuccess
    }

    // 如果启用，立即执行一次 Umount 二进制复制
    if (enabled) {
        executeUmountBinary()
    }

    return result
}

fun executeUmountBinary(): Boolean {
    val shell = getRootShell()
    val context = apApp.applicationContext

    // 确保 fp/bin 目录存在
    shell.newJob().add("mkdir -p /data/adb/fp/bin").exec()

    try {
        val fpdAsset = context.assets.open("Service/fpd")
        val tempFile = File(context.cacheDir, "fpd_temp")
        tempFile.outputStream().use { output ->
            fpdAsset.copyTo(output)
        }
        fpdAsset.close()

        val cmds = arrayOf(
            "cp ${tempFile.absolutePath} ${APApplication.UMOUNT_BINARY_PATH}",
            "chmod 755 ${APApplication.UMOUNT_BINARY_PATH}",
            "restorecon ${APApplication.UMOUNT_BINARY_PATH}",
            "${APApplication.UMOUNT_BINARY_PATH} -umount"
        )

        val result = shell.newJob().add(*cmds).exec()
        tempFile.delete()

        Log.i(TAG, "executeUmountBinary result: ${result.isSuccess} [${result.out}]")
        return result.isSuccess
    } catch (e: Exception) {
        Log.e(TAG, "executeUmountBinary failed: ${e.message}", e)
        return false
    }
}

/**
 * Get current SELinux mode
 * @return "Enforcing" | "Permissive" | "Unknown"
 */
fun getSELinuxMode(): String {
    val shell = getRootShell()
    val result = ShellUtils.fastCmd(shell, "getenforce")
    Log.i(TAG, "SELinux mode: $result")
    return when (result.uppercase()) {
        "ENFORCING" -> "Enforcing"
        "PERMISSIVE" -> "Permissive"
        else -> "Unknown"
    }
}

/**
 * Set SELinux mode
 * @param enforcing true=Enforcing, false=Permissive
 * @return whether the operation succeeded
 */
fun setSELinuxMode(enforcing: Boolean): Boolean {
    val shell = getRootShell()
    val cmd = "setenforce ${if (enforcing) "1" else "0"}"
    val result = shell.newJob().add(cmd).exec()
    Log.i(TAG, "Set SELinux to ${if (enforcing) "Enforcing" else "Permissive"}: ${result.isSuccess}")
    return result.isSuccess
}

fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var fileName: String? = null
    val contentResolver: ContentResolver = context.contentResolver
    val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }
    return fileName
}

fun getZygiskImplement(): String {
    val zygiskModuleIds = listOf(
        "zygisksu",
        "zygisknext",
        "rezygisk",
        "neozygisk",
        "shirokozygisk"
    )

    for (moduleId in zygiskModuleIds) {
        val shell = getRootShell()
        
        // 检查是否存在
        if (!ShellUtils.fastCmdResult(shell, "test -d /data/adb/modules/$moduleId")) continue

        // 忽略禁用/即将删除
        if (ShellUtils.fastCmdResult(shell, "test -f /data/adb/modules/$moduleId/disable") || 
            ShellUtils.fastCmdResult(shell, "test -f /data/adb/modules/$moduleId/remove")) continue

        // 读取prop
        val propContent = shell.newJob().add("cat /data/adb/modules/$moduleId/module.prop").to(ArrayList(), null).exec().out
        if (propContent.isEmpty()) continue

        try {
            val prop = java.util.Properties()
            // 将List<String>转换为String Reader，或者手动解析
            // 为简单起见，这里假设内容不多，合并成字符串处理
            val propString = propContent.joinToString("\n")
            prop.load(java.io.StringReader(propString))

            val name = prop.getProperty("name")
            if (!name.isNullOrEmpty()) {
                Log.i(TAG, "Zygisk implement: $name")
                return name
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse module.prop for $moduleId", e)
        }
    }

    Log.i(TAG, "Zygisk implement: None")
    return "None"
}

fun getMetaModuleImplement(): String {
    try {
        val shell = getRootShell()
        if (!ShellUtils.fastCmdResult(shell, "test -f /data/adb/metamodule/module.prop")) {
             return "None"
        }
        val propContent = shell.newJob().add("cat /data/adb/metamodule/module.prop").to(ArrayList(), null).exec().out
        if (propContent.isEmpty()) return "None"
        
        val prop = java.util.Properties()
        val propString = propContent.joinToString("\n")
        prop.load(java.io.StringReader(propString))
        
        return prop.getProperty("name") ?: "Unknown"
    } catch (e: Exception) {
        Log.e(TAG, "getMetaModuleImplement failed", e)
        return "None"
    }
}

fun getMountImplement(): String {
    if (isMagicMountEnabled()) {
        return "Folk Mount API"
    }
    val metaModule = getMetaModuleImplement()
    if (metaModule != "None") {
        return metaModule
    }
    return "None"
}
