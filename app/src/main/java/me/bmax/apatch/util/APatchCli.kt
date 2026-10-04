package me.bmax.apatch.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.system.Os
import android.util.Log
import android.widget.Toast
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.io.SuFile
import me.bmax.apatch.APApplication
import me.bmax.apatch.APApplication.Companion.SUPERCMD
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.screen.patches.MODULE_TYPE
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "APatchCli"
private const val SHELL_TIMEOUT_MS = 10_000L

data class ApdExecResult(
    val success: Boolean,
    val commandLabel: String,
    val exitCode: Int? = null,
    val output: String = "",
    val errorMessage: String? = null,
)

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
                try {
                    buildWithTimeout(builder, "sh")
                } catch (e2: Throwable) {
                    Log.e(TAG, "final sh fallback failed: ", e2)
                    throw IOException("Unable to create any shell", e2)
                }
            }
        }
    }
}

private fun closeQuietly(shell: Shell?) {
    try {
        shell?.close()
    } catch (_: Throwable) {
    }
}

private fun ensureRootShell(shell: Shell, reason: String): Shell {
    if (shell.isRoot) return shell
    closeQuietly(shell)
    throw IOException("Expected root shell for $reason, but received a non-root shell")
}

object APatchCli {
    @Volatile
    private var _shell: Shell? = null
    @Volatile
    private var _globalMntShell: Shell? = null

    val SHELL: Shell
        get() = _shell ?: try {
            createRootShellSafe(false).also { _shell = it }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create SHELL", e)
            throw e
        }

    val GLOBAL_MNT_SHELL: Shell
        get() = _globalMntShell ?: try {
            createRootShellSafe(true).also { _globalMntShell = it }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create GLOBAL_MNT_SHELL", e)
            throw e
        }

    fun refresh() {
        val old = _shell
        try {
            _shell = createRootShellSafe(false)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to refresh shell", e)
        }
        try { old?.close() } catch (_: Throwable) {}
    }
}

internal fun createRootShellSafe(globalMnt: Boolean = false): Shell {
    return try {
        createRootShell(globalMnt)
    } catch (e: Throwable) {
        Log.e(TAG, "Root shell creation failed, falling back to sh", e)
        try {
            Shell.Builder.create().setInitializers(RootShellInitializer::class.java).build("sh")
        } catch (e2: Throwable) {
            Log.e(TAG, "Even sh fallback failed, returning non-root shell", e2)
            try {
                Shell.Builder.create().build("sh")
            } catch (e3: Throwable) {
                Log.e(TAG, "All shell creation failed, app may not function correctly", e3)
                throw IOException("Unable to create any shell. Root functionality is unavailable.", e3)
            }
        }
    }
}

internal fun createRootShellStrict(
    globalMnt: Boolean = false,
    reason: String = "unknown"
): Shell {
    return try {
        ensureRootShell(createRootShell(globalMnt), reason)
    } catch (primaryError: Throwable) {
        Log.e(TAG, "Strict root shell creation failed for $reason", primaryError)
        val fallback = createRootShellSafe(globalMnt)
        ensureRootShell(fallback, reason)
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
    return try {
        if (newShell) {
            withNewRootShell {
                ShellUtils.fastCmdResult(this, "${APApplication.APD_PATH} $args")
            }
        } else {
            ShellUtils.fastCmdResult(getRootShell(), "${APApplication.APD_PATH} $args")
        }
    } catch (t: Throwable) {
        Log.e(TAG, "execApd failed: args='$args', newShell=$newShell", t)
        false
    }
}

private fun configureRootProcessEnv(builder: ProcessBuilder) {
    val basePath = System.getenv("PATH").orEmpty()
    builder.environment().apply {
        this["PATH"] = "$basePath:/system_ext/bin:/vendor/bin:${APApplication.APATCH_FOLDER}bin"
        this["BUSYBOX"] = "${APApplication.APATCH_FOLDER}bin/busybox"
    }
}

fun execApdBootFallback(vararg args: String, timeoutMs: Long = SHELL_TIMEOUT_MS): ApdExecResult {
    val effectiveSuperKey = APApplication.superKey.ifBlank { "su" }
    val command = mutableListOf(
        APApplication.SUPERCMD,
        "su",
        "-Z",
        APApplication.MAGISK_SCONTEXT,
        "exec",
        APApplication.APD_PATH,
        "-s",
        effectiveSuperKey,
    ).apply {
        addAll(args)
    }
    val commandLabel =
        "${File(APApplication.SUPERCMD).name} su -Z ${APApplication.MAGISK_SCONTEXT} exec ${APApplication.APD_PATH} -s <superkey> ${args.joinToString(" ")}"

    return try {
        val builder = ProcessBuilder(command).redirectErrorStream(true)
        configureRootProcessEnv(builder)

        val process = builder.start()
        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroy()
            if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
            }
            val output = runCatching {
                process.inputStream.bufferedReader().use { it.readText().trim() }
            }.getOrDefault("")
            ApdExecResult(
                success = false,
                commandLabel = commandLabel,
                output = output,
                errorMessage = "timed out after ${timeoutMs}ms",
            )
        } else {
            val exitCode = process.exitValue()
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }
            ApdExecResult(
                success = exitCode == 0,
                commandLabel = commandLabel,
                exitCode = exitCode,
                output = output,
                errorMessage = if (exitCode == 0) null else "exit code $exitCode",
            )
        }
    } catch (t: Throwable) {
        ApdExecResult(
            success = false,
            commandLabel = commandLabel,
            errorMessage = t.message ?: t.javaClass.simpleName,
        )
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
    return@withContext out.joinToString("\n").ifBlank { "[]" }
}

// Devices patched via PATCH_ONLY and flashed manually (e.g. fastboot) never go
// through the patch-completion handoff, so their stock boot backup is still in
// the app-private patch dir. Move it next to apd once root is available;
// idempotent and a no-op when nothing is pending.
fun migrateStockBootBackup() {
    withNewRootShell {
        newJob().add(
            "mkdir -p /data/adb/ap && cp /data/user/*/me.yuki.folk/patch/ori.img /data/adb/ap/ 2>/dev/null && rm -f /data/user/*/me.yuki.folk/patch/ori.img; true"
        ).exec()
    }
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

fun listPlugins(): String = runCatching {
    val shell = getRootShell()
    shell.newJob().add("${APApplication.APD_PATH} plugin list").to(ArrayList(), null).exec().out
        .joinToString("\n")
}.getOrElse { e ->
    Log.e(TAG, "listPlugins failed: ${e.message}")
    ""
}

fun setPluginState(id: String, enable: Boolean): Boolean {
    val cmd = if (enable) "plugin enable $id" else "plugin disable $id"
    val result = execApd(cmd, true)
    Log.i(TAG, "$cmd result: $result")
    return result
}

fun runPluginCallback(id: String, function: String): Boolean {
    val cmd = "plugin run $id $function"
    val result = execApd(cmd, true)
    Log.i(TAG, "run plugin $id $function result: $result")
    return result
}

/**
 * Run a plugin callback and capture its stdout output.
 * Returns a pair of (success, output).
 */
fun runPluginCallbackWithOutput(id: String, function: String): Pair<Boolean, String> {
    val cmd = "${APApplication.APD_PATH} plugin run $id $function"
    return runCatching {
        val result = rootShellForResult(cmd)
        val output = result.out.joinToString("\n").trim()
        val success = result.isSuccess
        Log.i(TAG, "run plugin $id $function success=$success output_len=${output.length}")
        success to output
    }.getOrElse { e ->
        Log.e(TAG, "runPluginCallbackWithOutput failed: ${e.message}")
        false to ""
    }
}

fun installPlugin(zipPath: String): Boolean {
    val cmd = "plugin install $zipPath"
    val result = execApd(cmd, true)
    Log.i(TAG, "install plugin $zipPath result: $result")
    return result
}

fun uninstallPlugin(id: String): Boolean {
    val cmd = "plugin uninstall $id"
    val result = execApd(cmd, true)
    Log.i(TAG, "uninstall plugin $id result: $result")
    return result
}

fun getPluginConfig(id: String, key: String): String {
    val cmd = "${APApplication.APD_PATH} plugin config --id $id get $key"
    return runCatching {
        rootShellForResult(cmd).out.joinToString("\n").trim()
    }.getOrElse { e ->
        Log.e(TAG, "getPluginConfig failed: ${e.message}")
        ""
    }
}

fun setPluginConfig(id: String, key: String, value: String): Boolean {
    // Escape single quotes for safe shell passing.
    val escaped = value.replace("'", "'\\''")
    val cmd = "plugin config --id $id set $key '$escaped'"
    val result = execApd(cmd, true)
    Log.i(TAG, "setPluginConfig $id $key result: $result")
    return result
}

fun runPluginAction(id: String): Boolean = runPluginCallback(id, "action")

fun getPluginLog(id: String): String {
    val cmd = "${APApplication.APD_PATH} plugin log $id"
    return runCatching {
        rootShellForResult(cmd).out.joinToString("\n").trim()
    }.getOrElse { e ->
        Log.e(TAG, "getPluginLog failed: ${e.message}")
        ""
    }
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
                kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                    kotlinx.coroutines.withTimeout(300000L) { // 5 minute timeout
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
        newJob().add("${APApplication.APD_PATH} module action ${shellQuote(moduleId)}")
            .to(stdoutCallback, stderrCallback).exec()
    }
    Log.i(TAG, "APModule runAction result: $result")

    return result.isSuccess
}

fun reboot(reason: String = "") {
    if (reason == "soft_reboot") {
        restartFramework()
        return
    }
    if (reason == "recovery") {
        // KEYCODE_POWER = 26, hide incorrect "Factory data reset" message
        getRootShell().newJob().add("/system/bin/input keyevent 26").exec()
    }
    getRootShell().newJob()
        .add("/system/bin/svc power reboot $reason || /system/bin/reboot $reason").exec()
}

/** Restart Android userspace while keeping runtime-loaded kernel modules active. */
fun restartFramework() {
    getRootShell().newJob().add("${APApplication.APD_PATH} soft-reboot").exec()
}

/**
 * Detect the Kernel Module Interface (KMI) of the running kernel, e.g.
 * `android14-5.15`, from `uname -r` (same parsing as KernelSU).
 */
fun getKmi(): String? {
    val release = runCatching { Os.uname().release }.getOrNull() ?: return null
    val m = Regex("(.* )?(\\d+\\.\\d+)(\\S+)?(android\\d+)(.*)").find(release) ?: return null
    return "${m.groupValues[4]}-${m.groupValues[2]}"
}

/**
 * Running kernel version as a comparable integer, e.g. `4.19` -> 419,
 * `5.10` -> 510, `6.1` -> 601. Returns null if it can't be parsed.
 */
fun getKernelVersionCode(): Int? {
    val release = runCatching { Os.uname().release }.getOrNull() ?: return null
    val m = Regex("^(\\d+)\\.(\\d+)").find(release) ?: return null
    val major = m.groupValues[1].toIntOrNull() ?: return null
    val minor = m.groupValues[2].toIntOrNull() ?: return null
    return major * 100 + minor
}

/** Whether the running kernel is a GKI kernel (i.e. exposes `android<N>` in uname). */
fun isGkiKernel(): Boolean = getKmi() != null

/** Asset name of the KernelPatch ko matching this device's kernel (KMI). */
fun jailbreakAssetName(): String? {
    val kmi = getKmi() ?: return null
    return "${kmi}_kernelpatch.ko"
}

/** Extract the bundled kernelpatch.ko for this device's kernel to the app files dir. */
fun extractJailbreakKo(): File? {
    val name = jailbreakAssetName() ?: return null
    val file = File(apApp.filesDir, "kernelpatch.ko")
    return runCatching {
        apApp.assets.open(name).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file
    }.getOrNull()
}

/**
 * Install jailbreak mode: extract the bundled kernelpatch.ko for this kernel to
 * the app files dir (no root needed), then trigger the magica chain via the
 * isolated app-zygote service. The apd then escalates to full root through adb
 * and runs `late-load` (loads the module, applies Magisk policy, marks jailbreak).
 */
fun installJailbreak(): Boolean {
    // Do not load the jailbreak module if the KernelPatch supercall interface is
    // already available: either a real KernelPatch is installed, or the jailbreak
    // module is already loaded. Loading it again would conflict / stack a duplicate.
    if (Natives.nativeReady(APApplication.superKey)) {
        Log.i(TAG, "installJailbreak skipped: KernelPatch interface already ready")
        return false
    }
    val ko = extractJailbreakKo() ?: return false
    if (!ko.exists() || ko.length() == 0L) {
        Log.e(TAG, "extracted jailbreak ko is missing or empty")
        return false
    }
    return try {
        val intent = Intent(apApp, me.bmax.apatch.magica.MagicaService::class.java)
        apApp.startService(intent)
        Log.i(TAG, "MagicaService started for jailbreak")
        true
    } catch (e: Throwable) {
        Log.e(TAG, "start MagicaService failed: $e")
        false
    }
}

/** Whether the SELinux mode is permissive (getenforce), the prerequisite for jailbreak. */
fun isSELinuxPermissive(): Boolean {
    return try {
        val shell = Shell.Builder.create().build("sh")
        val out = ArrayList<String>()
        val result = shell.newJob().add("getenforce").to(out, ArrayList()).exec()
        shell.close()
        result.isSuccess &&
            out.firstOrNull()?.trim()?.equals("Permissive", ignoreCase = true) == true
    } catch (e: Exception) {
        Log.e(TAG, "Failed to check SELinux status", e)
        false
    }
}

/** Whether jailbreak mode is active in the current boot. */
fun isJailbreakMode(): Boolean {
    val hasMarker = runCatching { SuFile(APApplication.JAILBREAK_FILE).exists() }.getOrDefault(false)
    if (!hasMarker) return false

    // The marker persists across full reboots, while a late-loaded module does not.
    // A boot-patched KernelPatch can therefore coexist with a stale marker after the
    // user flashes boot.img from fastboot. Only treat the marker as active when the
    // runtime module is still loaded in this boot.
    val runtimeModuleLoaded = File("/sys/module/kernelpatch").exists()
    if (!runtimeModuleLoaded && Natives.nativeReady(APApplication.superKey)) {
        clearJailbreakMarker()
        Log.i(TAG, "removed stale jailbreak marker after boot-patched KernelPatch was detected")
    }
    return runtimeModuleLoaded
}

/**
 * Whether patching/installing is blocked by jailbreak mode.
 * The persistent marker is validated against the runtime module so a stale marker left
 * after flashing a patched boot image cannot block patching.
 */
fun isJailbreakPatchBlocked(): Boolean {
    return isJailbreakMode()
}

/**
 * True when a real KernelPatch is installed to boot (the supercall interface is ready
 * and there is no jailbreak marker). Used to avoid offering jailbreak on a patched device.
 */
fun isRealKernelPatchInstalled(): Boolean {
    return !isJailbreakMode() && Natives.nativeReady(APApplication.superKey)
}

/** Remove the jailbreak marker file, e.g. after a real KernelPatch installation succeeds. */
fun clearJailbreakMarker(): Boolean {
    return rootShellForResult("rm -f ${APApplication.JAILBREAK_FILE}").isSuccess
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

private fun migrateFeatureFile(current: String, legacy: String): Boolean {
    val parent = File(current).parent
    val shell = getRootShell()
    val commands = mutableListOf<String>()
    if (parent != null) {
        commands += "mkdir -p '$parent'"
    }
    commands += "if [ ! -e '$current' ] && [ -e '$legacy' ]; then mv '$legacy' '$current' 2>/dev/null || cp -af '$legacy' '$current'; fi"
    commands += "if [ -e '$current' ]; then rm -f '$legacy'; fi"
    shell.newJob().add(*commands.toTypedArray()).exec()
    val file = SuFile(current)
    file.shell = shell
    return file.exists()
}

private fun featureFileExists(current: String, legacy: String): Boolean {
    return migrateFeatureFile(current, legacy)
}

private fun readFeatureText(current: String, legacy: String): String {
    migrateFeatureFile(current, legacy)
    return ShellUtils.fastCmd(
        getRootShell(),
        "cat '$current' 2>/dev/null",
    ) ?: ""
}

private fun removeFeatureFile(current: String, legacy: String) {
    getRootShell().newJob().add("rm -f '$current' '$legacy'").exec()
}

private fun writeFeatureText(current: String, legacy: String, content: String): Boolean {
    val shell = getRootShell()
    val parent = File(current).parent
    val escapedContent = content.replace("'", "'\\''")
    val commands = mutableListOf<String>()
    if (parent != null) {
        commands += "mkdir -p '$parent'"
    }
    commands += "printf '%s' '$escapedContent' > '$current'"
    commands += "test -e '$current'"
    commands += "rm -f '$legacy'"
    val result = shell.newJob().add(*commands.toTypedArray()).exec()
    if (!result.isSuccess) {
        Log.e(TAG, "Failed to write feature config '$current': ${result.out}")
    }
    return result.isSuccess
}

fun isHideServiceEnabled(): Boolean {
    return featureFileExists(
        APApplication.HIDE_SERVICE_FILE,
        APApplication.LEGACY_HIDE_SERVICE_FILE,
    )
}

fun setHideServiceEnabled(enable: Boolean) {
    val shell = getRootShell()
    if (enable) {
        migrateFeatureFile(
            APApplication.HIDE_SERVICE_FILE,
            APApplication.LEGACY_HIDE_SERVICE_FILE,
        )
    } else {
        removeFeatureFile(
            APApplication.HIDE_SERVICE_FILE,
            APApplication.LEGACY_HIDE_SERVICE_FILE,
        )
    }
    if (enable) {
        shell.newJob().add("touch '${APApplication.HIDE_SERVICE_FILE}'")
        .submit { result ->
            Log.i(TAG, "setHideServiceEnabled result: ${result.isSuccess} [${result.out}]")
        }
    }
    // 如果启用，异步执行一次 Hide 二进制（避免阻塞 UI 线程）
    if (enable) {
        CoroutineScope(Dispatchers.IO).launch {
            executeHideBinary()
        }
    }
}

fun isUtsSpoofEnabled(): Boolean {
    return featureFileExists(
        APApplication.UTS_SPOOF_ENABLE_FILE,
        APApplication.LEGACY_UTS_SPOOF_ENABLE_FILE,
    )
}

fun setUtsSpoofEnabled(enable: Boolean) {
    if (enable) {
        migrateFeatureFile(
            APApplication.UTS_SPOOF_ENABLE_FILE,
            APApplication.LEGACY_UTS_SPOOF_ENABLE_FILE,
        )
        getRootShell().newJob().add("touch '${APApplication.UTS_SPOOF_ENABLE_FILE}'").exec()
    } else {
        removeFeatureFile(
            APApplication.UTS_SPOOF_ENABLE_FILE,
            APApplication.LEGACY_UTS_SPOOF_ENABLE_FILE,
        )
    }
}

fun writeUtsSpoofConfig(release: String, version: String) {
    val escapedRelease = release.replace("\\", "\\\\").replace("\"", "\\\"")
    val escapedVersion = version.replace("\\", "\\\\").replace("\"", "\\\"")
    val json = "{\"release\":\"$escapedRelease\",\"version\":\"$escapedVersion\"}"
    writeFeatureText(
        APApplication.UTS_SPOOF_CONFIG_FILE,
        APApplication.LEGACY_UTS_SPOOF_CONFIG_FILE,
        json,
    )
}

fun removeUtsSpoofConfig() {
    removeFeatureFile(
        APApplication.UTS_SPOOF_CONFIG_FILE,
        APApplication.LEGACY_UTS_SPOOF_CONFIG_FILE,
    )
}

fun isPathHideEnabled(): Boolean {
    return featureFileExists(
        APApplication.PATHHIDE_ENABLE_FILE,
        APApplication.LEGACY_PATHHIDE_ENABLE_FILE,
    )
}

fun setPathHideEnabled(enable: Boolean) {
    if (enable) {
        migrateFeatureFile(
            APApplication.PATHHIDE_ENABLE_FILE,
            APApplication.LEGACY_PATHHIDE_ENABLE_FILE,
        )
        getRootShell().newJob().add("touch '${APApplication.PATHHIDE_ENABLE_FILE}'").exec()
    } else {
        removeFeatureFile(
            APApplication.PATHHIDE_ENABLE_FILE,
            APApplication.LEGACY_PATHHIDE_ENABLE_FILE,
        )
    }
}

fun writePathHidePaths(paths: String) {
    writeFeatureText(
        APApplication.PATHHIDE_PATHS_FILE,
        APApplication.LEGACY_PATHHIDE_PATHS_FILE,
        normalizePathHidePaths(paths),
    )
}

fun readPathHidePaths(): String {
    val raw = readFeatureText(
        APApplication.PATHHIDE_PATHS_FILE,
        APApplication.LEGACY_PATHHIDE_PATHS_FILE,
    )
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
    writeFeatureText(
        APApplication.PATHHIDE_UIDS_FILE,
        APApplication.LEGACY_PATHHIDE_UIDS_FILE,
        uids,
    )
}

fun readPathHideUids(): String {
    return readFeatureText(
        APApplication.PATHHIDE_UIDS_FILE,
        APApplication.LEGACY_PATHHIDE_UIDS_FILE,
    )
}

fun isPathHideUidModeEnabled(): Boolean {
    return featureFileExists(
        APApplication.PATHHIDE_UID_MODE_FILE,
        APApplication.LEGACY_PATHHIDE_UID_MODE_FILE,
    )
}

fun isPathHideFilterSystemEnabled(): Boolean {
    return featureFileExists(
        APApplication.PATHHIDE_FILTER_SYSTEM_FILE,
        APApplication.LEGACY_PATHHIDE_FILTER_SYSTEM_FILE,
    )
}

fun setPathHideUidMode(enable: Boolean) {
    if (enable) {
        migrateFeatureFile(
            APApplication.PATHHIDE_UID_MODE_FILE,
            APApplication.LEGACY_PATHHIDE_UID_MODE_FILE,
        )
        getRootShell().newJob().add("touch '${APApplication.PATHHIDE_UID_MODE_FILE}'").exec()
    } else {
        removeFeatureFile(
            APApplication.PATHHIDE_UID_MODE_FILE,
            APApplication.LEGACY_PATHHIDE_UID_MODE_FILE,
        )
    }
}

fun setPathHideFilterSystem(enable: Boolean) {
    if (enable) {
        migrateFeatureFile(
            APApplication.PATHHIDE_FILTER_SYSTEM_FILE,
            APApplication.LEGACY_PATHHIDE_FILTER_SYSTEM_FILE,
        )
        getRootShell().newJob().add("touch '${APApplication.PATHHIDE_FILTER_SYSTEM_FILE}'").exec()
    } else {
        removeFeatureFile(
            APApplication.PATHHIDE_FILTER_SYSTEM_FILE,
            APApplication.LEGACY_PATHHIDE_FILTER_SYSTEM_FILE,
        )
    }
}

fun setNetIsolateEnabled(enable: Boolean) {
    if (enable) {
        migrateFeatureFile(
            APApplication.NETISOLATE_ENABLE_FILE,
            APApplication.LEGACY_NETISOLATE_ENABLE_FILE,
        )
        getRootShell().newJob().add("touch '${APApplication.NETISOLATE_ENABLE_FILE}'").exec()
    } else {
        removeFeatureFile(
            APApplication.NETISOLATE_ENABLE_FILE,
            APApplication.LEGACY_NETISOLATE_ENABLE_FILE,
        )
    }
}

fun writeNetIsolateUids(uids: String) {
    writeFeatureText(
        APApplication.NETISOLATE_UIDS_FILE,
        APApplication.LEGACY_NETISOLATE_UIDS_FILE,
        uids,
    )
}

fun isNetIsolateEnabled(): Boolean {
    return featureFileExists(
        APApplication.NETISOLATE_ENABLE_FILE,
        APApplication.LEGACY_NETISOLATE_ENABLE_FILE,
    )
}

fun readNetIsolateUids(): String {
    return readFeatureText(
        APApplication.NETISOLATE_UIDS_FILE,
        APApplication.LEGACY_NETISOLATE_UIDS_FILE,
    )
}

fun executeHideBinary(): Boolean {
    val shell = getRootShell()
    val context = apApp.applicationContext

    shell.newJob().add("mkdir -p '${File(APApplication.HIDE_BINARY_PATH).parent}'").exec()

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
            "rm -f ${APApplication.LEGACY_FPD_PATH}",
            "rmdir /data/adb/fp/bin /data/adb/fp 2>/dev/null || true",
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

    shell.newJob().add("mkdir -p '${File(APApplication.UMOUNT_BINARY_PATH).parent}'").exec()

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
            "rm -f ${APApplication.LEGACY_FPD_PATH}",
            "rmdir /data/adb/fp/bin /data/adb/fp 2>/dev/null || true",
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
        "shirokozygisk",
        "onyxzygisk"
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
