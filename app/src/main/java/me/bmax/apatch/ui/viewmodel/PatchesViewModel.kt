package me.bmax.apatch.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.system.Os
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.BuildConfig
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.copyAndClose
import me.bmax.apatch.util.copyAndCloseOut
import me.bmax.apatch.util.createRootShell
import me.bmax.apatch.util.inputStream
import me.bmax.apatch.util.shellForResult
import me.bmax.apatch.util.writeTo
import org.ini4j.Ini
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

import me.bmax.apatch.util.getFileNameFromUri
import me.bmax.apatch.util.ModuleBackupUtils

private const val TAG = "PatchViewModel"

class PatchesViewModel : ViewModel() {

    enum class PatchMode(val sId: Int) {
        PATCH_ONLY(R.string.patch_mode_bootimg_patch),
        PATCH_AND_INSTALL(R.string.patch_mode_patch_and_install),
        INSTALL_TO_NEXT_SLOT(R.string.patch_mode_install_to_next_slot),
        RESTORE(R.string.patch_mode_restore),
        UNPATCH(R.string.patch_mode_uninstall_patch),
    }

    private val _bootSlot = MutableStateFlow("")
    val bootSlot: StateFlow<String> = _bootSlot.asStateFlow()

    private val _bootDev = MutableStateFlow("")
    val bootDev: StateFlow<String> = _bootDev.asStateFlow()

    private val _kimgInfo = MutableStateFlow(KPModel.KImgInfo("", false))
    val kimgInfo: StateFlow<KPModel.KImgInfo> = _kimgInfo.asStateFlow()

    private val _kpimgInfo = MutableStateFlow(KPModel.KPImgInfo("", "", "", "", ""))
    val kpimgInfo: StateFlow<KPModel.KPImgInfo> = _kpimgInfo.asStateFlow()

    private val _superkey = MutableStateFlow(APApplication.superKey)
    val superkey: StateFlow<String> = _superkey.asStateFlow()

    val existedExtras = mutableStateListOf<KPModel.IExtraInfo>()
    val newExtras = mutableStateListOf<KPModel.IExtraInfo>()
    val newExtrasFileName = mutableListOf<String>()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _patching = MutableStateFlow(false)
    val patching: StateFlow<Boolean> = _patching.asStateFlow()

    private val _patchdone = MutableStateFlow(false)
    val patchdone: StateFlow<Boolean> = _patchdone.asStateFlow()

    private val _needReboot = MutableStateFlow(false)
    val needReboot: StateFlow<Boolean> = _needReboot.asStateFlow()

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error.asStateFlow()

    private val _patchLog = MutableStateFlow("")
    val patchLog: StateFlow<String> = _patchLog.asStateFlow()

    private val _errorEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private val patchDir: ExtendedFile = FileSystemManager.getLocal().getFile(apApp.filesDir.parent, "patch")
    private var srcBoot: ExtendedFile = patchDir.getChildFile("boot.img")
    private var shell: Shell = createRootShell()
    private var prepared: Boolean = false
    private var backupJob: Job? = null

    private fun prepare(): Unit {
        shell.newJob().add("rm -rf ${patchDir.path}").exec()

        patchDir.deleteRecursively()
        patchDir.mkdirs()
        val execs = listOf(
            "libkptools.so", "libbusybox.so", "libkpatch.so", "libbootctl.so",
        )
        _error.value = ""

        val info = apApp.applicationInfo
        val libs = File(info.nativeLibraryDir).listFiles { _, name ->
            execs.contains(name)
        } ?: emptyArray()

        for (lib in libs) {
            val name = lib.name.substring(3, lib.name.length - 3)
            try {
                Os.symlink(lib.path, "$patchDir/$name")
            } catch (e: Exception) {
                lib.inputStream().copyAndClose(File(patchDir, name).outputStream())
            }
        }

        for (script in listOf(
            "boot_patch.sh", "boot_unpatch.sh", "boot_extract.sh", "util_functions.sh", "kpimg", "boot_flash.sh",
        )) {
            val dest = File(patchDir, script)
            apApp.assets.open(script).writeTo(dest)
        }
    }

    private fun parseKpimg(): Unit {
        val result = shellForResult(
            shell, "cd $patchDir", "./kptools -l -k kpimg",
        )

        if (result.isSuccess) {
            val ini = Ini(StringReader(result.out.joinToString("\n")))
            val kpimg = ini["kpimg"]
            if (kpimg != null) {
                _kpimgInfo.value = KPModel.KPImgInfo(
                    kpimg["version"].toString(),
                    kpimg["compile_time"].toString(),
                    kpimg["config"].toString(),
                    _superkey.value,
                    kpimg["root_superkey"].toString(),
                )
            } else {
                _error.value += "parse kpimg error\n"
            }
        } else {
            _error.value = result.err.joinToString("\n")
        }
    }

    private fun parseBootimg(bootimg: String): Unit {
        val result = shellForResult(
            shell,
            "cd $patchDir",
            "./kptools unpacknolog $bootimg",
            "./kptools -l -i kernel",
        )
        if (result.isSuccess) {
            val ini = Ini(StringReader(result.out.joinToString("\n")))
            Log.d(TAG, "kernel image info: $ini")

            val kernel = ini["kernel"]
            if (kernel == null) {
                _error.value += "empty kernel section"
                Log.d(TAG, _error.value)
                return
            }
            _kimgInfo.value = KPModel.KImgInfo(kernel["banner"].toString(), kernel["patched"].toBoolean())
            if (_kimgInfo.value.patched) {
                val superkey = ini["kpimg"]?.getOrDefault("superkey", "") ?: ""
                if (checkSuperKeyValidation(superkey)) {
                    _superkey.value = superkey
                    APApplication.superKey = superkey
                }
                _kpimgInfo.value = _kpimgInfo.value.copy(
                    superKey = superkey,
                    rootSuperkey = ini["kpimg"]?.getOrDefault("root_superkey", "") ?: "",
                )
                var kpmNum = kernel["extra_num"]?.toInt()
                if (kpmNum == null) {
                    val extras = ini["extras"]
                    kpmNum = extras?.get("num")?.toInt()
                }
                if (kpmNum != null && kpmNum > 0) {
                    for (i in 0..<kpmNum) {
                        val extra = ini["extra $i"]
                        if (extra == null) {
                            _error.value += "empty extra section"
                            break
                        }
                        val type = KPModel.ExtraType.valueOf(extra["type"]!!.uppercase())
                        val name = extra["name"].toString()
                        val args = extra["args"].toString()
                        var event = extra["event"].toString()
                        if (event.isEmpty()) {
                            event = KPModel.TriggerEvent.PRE_KERNEL_INIT.event
                        }
                        if (type == KPModel.ExtraType.KPM) {
                            val kpmInfo = KPModel.KPMInfo(
                                type, name, event, args,
                                extra["version"].toString(),
                                extra["license"].toString(),
                                extra["author"].toString(),
                                extra["description"].toString(),
                            )
                            existedExtras.add(kpmInfo)
                        }
                    }
                }
            }
        } else {
            _error.value += result.err.joinToString("\n")
        }
    }

    val checkSuperKeyValidation: (superKey: String) -> Boolean = { superKey ->
        superKey.length in 8..63 && superKey.any { it.isDigit() } && superKey.any { it.isLetter() }
    }

    fun setSuperKey(superKey: String): Unit {
        _superkey.value = superKey
        APApplication.superKey = superKey
    }

    fun copyAndParseBootimg(uri: Uri): Unit {
        viewModelScope.launch(Dispatchers.IO) {
            if (_running.value) return@launch
            _running.value = true
            try {
                uri.inputStream().buffered().use { src ->
                    srcBoot.also {
                        src.copyAndCloseOut(it.newOutputStream())
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "copy boot image error: $e")
            }
            parseBootimg(srcBoot.path)
            _running.value = false
        }
    }

    private fun extractAndParseBootimg(mode: PatchMode): Unit {
        var cmdBuilder = "./boot_extract.sh"

        if (mode == PatchMode.INSTALL_TO_NEXT_SLOT) {
            cmdBuilder += " true"
        }

        val result = shellForResult(
            shell,
            "export ASH_STANDALONE=1",
            "cd $patchDir",
            "./busybox sh $cmdBuilder",
        )

        if (result.isSuccess) {
            _bootSlot.value = if (!result.out.toString().contains("SLOT=")) {
                ""
            } else {
                result.out.filter { it.startsWith("SLOT=") }[0].removePrefix("SLOT=")
            }
            _bootDev.value =
                result.out.filter { it.startsWith("BOOTIMAGE=") }[0].removePrefix("BOOTIMAGE=")
            Log.i(TAG, "current slot: ${_bootSlot.value}")
            Log.i(TAG, "current bootimg: ${_bootDev.value}")
            srcBoot = FileSystemManager.getLocal().getFile(_bootDev.value)
            parseBootimg(_bootDev.value)
        } else {
            _error.value = result.err.joinToString("\n")
        }
        _running.value = false
    }

    fun prepare(mode: PatchMode): Unit {
        viewModelScope.launch(Dispatchers.IO) {
            if (prepared) return@launch
            prepared = true

            _running.value = true
            try {
                prepare()
                if (mode != PatchMode.UNPATCH) {
                    parseKpimg()
                }
                if (mode == PatchMode.PATCH_AND_INSTALL || mode == PatchMode.UNPATCH ||
                    mode == PatchMode.INSTALL_TO_NEXT_SLOT || mode == PatchMode.RESTORE
                ) {
                    extractAndParseBootimg(mode)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Prepare failed", e)
                _error.value = "Initialization failed: ${e.message}"
            } finally {
                _running.value = false
            }
        }
    }

    fun embedKPM(uri: Uri): Unit {
        viewModelScope.launch(Dispatchers.IO) {
            if (_running.value) return@launch
            _running.value = true
            _error.value = ""

            val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
            val kpmFileName = "${rand}.kpm"
            val kpmFile: ExtendedFile = patchDir.getChildFile(kpmFileName)

            Log.i(TAG, "copy kpm to: ${kpmFile.path}")
            try {
                uri.inputStream().buffered().use { src ->
                    kpmFile.also {
                        src.copyAndCloseOut(it.newOutputStream())
                    }
                }

                val originalFileName = getFileNameFromUri(apApp, uri)
                launch(Dispatchers.IO) {
                     val result = ModuleBackupUtils.autoBackupModule(
                        apApp,
                        kpmFile,
                        originalFileName,
                        "KPM"
                    )
                    if (result != null && !result.startsWith("Duplicate")) {
                        Log.e(TAG, "KPM Auto backup failed: $result")
                    } else {
                        Log.d(TAG, "KPM Auto backup success")
                    }
                }

            } catch (e: IOException) {
                Log.e(TAG, "Copy kpm error: $e")
            }

            val result = shellForResult(
                shell, "cd $patchDir", "./kptools -l -M ${kpmFile.path}"
            )

            if (result.isSuccess) {
                val ini = Ini(StringReader(result.out.joinToString("\n")))
                val kpm = ini["kpm"]
                if (kpm != null) {
                    val kpmInfo = KPModel.KPMInfo(
                        KPModel.ExtraType.KPM,
                        kpm["name"].toString(),
                        KPModel.TriggerEvent.PRE_KERNEL_INIT.event,
                        "",
                        kpm["version"].toString(),
                        kpm["license"].toString(),
                        kpm["author"].toString(),
                        kpm["description"].toString(),
                    )
                    newExtras.add(kpmInfo)
                    newExtrasFileName.add(kpmFileName)
                }
            } else {
                _error.value = "Invalid KPM\n"
            }
            _running.value = false
        }
    }

    fun doUnpatch(): Unit {
        viewModelScope.launch(Dispatchers.IO) {
            _patching.value = true
            _patchLog.value = ""
            Log.i(TAG, "starting unpatching...")

            val logs = object : CallbackList<String>() {
                override fun onAddElement(e: String?) {
                    _patchLog.value += e
                    Log.i(TAG, "" + e)
                    _patchLog.value += "\n"
                }
            }

            val result = shell.newJob().add(
                "export ASH_STANDALONE=1",
                "cd $patchDir",
                "cp /data/adb/ap/ori.img new-boot.img",
                "./busybox sh ./boot_unpatch.sh $bootDev",
                "rm -f ${APApplication.APD_PATH}",
                "rm -rf ${APApplication.APATCH_FOLDER}",
            ).to(logs, logs).exec()

            if (result.isSuccess) {
                logs.add(" Unpatch successful")
                _needReboot.value = true
                APApplication.markNeedReboot()
            } else {
                logs.add(" Unpatched failed")
                _error.value = result.err.joinToString("\n")
            }
            logs.add("****************************")

            _patchdone.value = true
            _patching.value = false
        }
    }
    fun doPatch(mode: PatchMode): Unit {
        viewModelScope.launch(Dispatchers.IO) {
            _patching.value = true
            Log.d(TAG, "starting patching...")

            val apVer = Version.getManagerVersion().second
            val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
            val outFilename = "folk_patched_${apVer}_${BuildConfig.buildKPV}_${rand}.img"

            val logs = object : CallbackList<String>() {
                override fun onAddElement(e: String?) {
                    _patchLog.value += e
                    Log.d(TAG, "" + e)
                    _patchLog.value += "\n"
                }
            }
            val prefs = APApplication.sharedPreferences
            var backupDirPath = ""
            if (prefs.getBoolean("auto_backup_boot", true) && mode == PatchMode.PATCH_ONLY) {
                try {
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    val backupDir = File("/data/FolkPatch_patched_${apVer}_${timestamp}")
                    backupDirPath = backupDir.absolutePath
                    backupDir.mkdirs()
                    val originalBoot = File(backupDir, "boot.img")
                    srcBoot.inputStream().use { input ->
                        originalBoot.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Backup failed", e)
                }
            }

            if (mode == PatchMode.RESTORE) {
                logs.add(" Restoring boot image...")
                val restoreCommand = mutableListOf("./busybox", "sh", "boot_flash.sh", _bootDev.value, srcBoot.path)

                val result = shell.newJob().add(
                    "export ASH_STANDALONE=1",
                    "cd $patchDir",
                    restoreCommand.joinToString(" "),
                ).to(logs, logs).exec()

                if (result.isSuccess) {
                    logs.add(" Restore successful")
                    _needReboot.value = true
                    APApplication.markNeedReboot()
                } else {
                    logs.add(" Restore failed")
                    _error.value = result.err.joinToString("\n")
                }
                logs.add("****************************")
                _patchdone.value = true
                _patching.value = false
                return@launch
            }

            var patchCommand = mutableListOf("./busybox sh boot_patch.sh \"$0\" \"$@\"")

            var isKpOld = false
            val currentSuperKey = _superkey.value

            if (mode == PatchMode.PATCH_AND_INSTALL || mode == PatchMode.INSTALL_TO_NEXT_SLOT) {

                val KPCheck = shell.newJob().add("truncate $currentSuperKey -Z u:r:magisk:s0 -c whoami").exec()

                if (KPCheck.isSuccess && !isSuExecutable()) {
                    patchCommand.addAll(0, listOf("truncate", currentSuperKey, "-Z", APApplication.MAGISK_SCONTEXT, "-c"))
                    patchCommand.addAll(listOf(currentSuperKey, srcBoot.path, "true"))
                } else {
                    patchCommand = mutableListOf("./busybox", "sh", "boot_patch.sh")
                    patchCommand.addAll(listOf(currentSuperKey, srcBoot.path, "true"))
                    isKpOld = true
                }

            } else {
                patchCommand.addAll(0, listOf("sh", "-c"))
                patchCommand.addAll(listOf(_superkey.value, srcBoot.path))
            }

            for (i in 0..<newExtrasFileName.size) {
                patchCommand.addAll(listOf("-M", newExtrasFileName[i]))
                val extra = newExtras[i]
                if (extra.args.isNotEmpty()) {
                    patchCommand.addAll(listOf("-A", extra.args))
                }
                if (extra.event.isNotEmpty()) {
                    patchCommand.addAll(listOf("-V", extra.event))
                }
                patchCommand.addAll(listOf("-T", extra.type.desc))
            }

            for (i in 0..<existedExtras.size) {
                val extra = existedExtras[i]
                patchCommand.addAll(listOf("-E", extra.name))
                if (extra.args.isNotEmpty()) {
                    patchCommand.addAll(listOf("-A", extra.args))
                }
                if (extra.event.isNotEmpty()) {
                    patchCommand.addAll(listOf("-V", extra.event))
                }
                patchCommand.addAll(listOf("-T", extra.type.desc))
            }

            val builder = ProcessBuilder(patchCommand)

            Log.i(TAG, "patchCommand: $patchCommand")

            var succ = false

            if (isKpOld) {
                val resultString = "\"" + patchCommand.joinToString(separator = "\" \"") + "\""
                val result = shell.newJob().add(
                    "export ASH_STANDALONE=1",
                    "cd $patchDir",
                    resultString,
                ).to(logs, logs).exec()
                succ = result.isSuccess
            } else {
                builder.environment().put("ASH_STANDALONE", "1")
                builder.directory(patchDir)
                builder.redirectErrorStream(true)

                val process = builder.start()

                Thread {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            _patchLog.value += line
                            Log.i(TAG, "" + line)
                            _patchLog.value += "\n"
                        }
                    }
                }.start()
                succ = process.waitFor() == 0
            }

            if (!succ) {
                val msg = " Patch failed."
                _error.value = msg
                logs.add(_error.value)
                logs.add("****************************")
                _patching.value = false
                return@launch
            }

            if (backupDirPath.isNotEmpty()) {
                try {
                    val newBootFile = patchDir.getChildFile("new-boot.img")
                    val patchedBoot = File(backupDirPath, "boot_patched_${apVer}.img")
                    val copyPatchedResult = shell.newJob().add("cp $newBootFile $patchedBoot").exec()
                    if (!copyPatchedResult.isSuccess) {
                        throw Exception("Failed to copy patched boot image: ${copyPatchedResult.err.joinToString("\n")}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Save patched boot failed", e)
                }
            }

            if (mode == PatchMode.PATCH_AND_INSTALL) {
                logs.add("- Reboot to finish the installation...")
                _needReboot.value = true
                APApplication.markNeedReboot()
            } else if (mode == PatchMode.INSTALL_TO_NEXT_SLOT) {
                logs.add("- Connecting boot hal...")
                val bootctlStatus = shell.newJob().add(
                    "cd $patchDir", "chmod 0777 $patchDir/bootctl", "./bootctl hal-info",
                ).to(logs, logs).exec()
                if (!bootctlStatus.isSuccess) {
                    logs.add("[X] Failed to connect to boot hal, you may need switch slot manually")
                } else {
                    val currSlot = shellForResult(
                        shell, "cd $patchDir", "./bootctl get-current-slot",
                    ).out.toString()
                    val targetSlot = if (currSlot.contains("0")) 1 else 0
                    logs.add("- Switching to next slot: $targetSlot...")
                    val setNextActiveSlot = shell.newJob().add(
                        "cd $patchDir", "./bootctl set-active-boot-slot $targetSlot",
                    ).exec()
                    if (setNextActiveSlot.isSuccess) {
                        logs.add("- Switch done")
                        logs.add("- Writing boot marker script...")
                        val markBootableScript = shell.newJob().add(
                            "mkdir -p /data/adb/post-fs-data.d && rm -rf /data/adb/post-fs-data.d/post_ota.sh && touch /data/adb/post-fs-data.d/post_ota.sh",
                            "echo \"chmod 0777 $patchDir/bootctl\" > /data/adb/post-fs-data.d/post_ota.sh",
                            "echo \"chown root:root 0777 $patchDir/bootctl\" > /data/adb/post-fs-data.d/post_ota.sh",
                            "echo \"$patchDir/bootctl mark-boot-successful\" > /data/adb/post-fs-data.d/post_ota.sh",
                            "echo >> /data/adb/post-fs-data.d/post_ota.sh",
                            "echo \"rm -rf $patchDir\" >> /data/adb/post-fs-data.d/post_ota.sh",
                            "echo >> /data/adb/post-fs-data.d/post_ota.sh",
                            "echo \"rm -f /data/adb/post-fs-data.d/post_ota.sh\" >> /data/adb/post-fs-data.d/post_ota.sh",
                            "chmod 0777 /data/adb/post-fs-data.d/post_ota.sh",
                            "chown root:root /data/adb/post-fs-data.d/post_ota.sh",
                        ).to(logs, logs).exec()
                        if (markBootableScript.isSuccess) {
                            logs.add("- Boot marker script write done")
                        } else {
                            logs.add("[X] Boot marker scripts write failed")
                        }
                    }
                }
                logs.add("- Reboot to finish the installation...")
                _needReboot.value = true
                APApplication.markNeedReboot()
            } else if (mode == PatchMode.PATCH_ONLY) {
                val newBootFile = patchDir.getChildFile("new-boot.img")
                val outDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!outDir.exists()) outDir.mkdirs()
                val outPath = File(outDir, outFilename)
                val inputUri = newBootFile.getUri(apApp)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val outUri = createDownloadUri(apApp, outFilename)
                    succ = insertDownload(apApp, outUri, inputUri)
                } else {
                    newBootFile.inputStream().copyAndClose(outPath.outputStream())
                }
                if (succ) {
                    logs.add(apApp.getString(R.string.patch_output_written_to))
                    logs.add(" ${outPath.path}")
                } else {
                    logs.add(apApp.getString(R.string.patch_write_failed))
                }
            }
            logs.add("****************************")
            _patchdone.value = true
            _patching.value = false
        }
    }

    override fun onCleared(): Unit {
        super.onCleared()
        backupJob?.cancel()
    }

    private fun isSuExecutable(): Boolean {
        val suFile = File("/system/bin/su")
        return suFile.exists() && suFile.canExecute()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createDownloadUri(context: Context, outFilename: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, outFilename)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        return context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertDownload(context: Context, outUri: Uri?, inputUri: Uri): Boolean {
        if (outUri == null) return false
        try {
            val resolver = context.contentResolver
            resolver.openInputStream(inputUri)?.use { inputStream ->
                resolver.openOutputStream(outUri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(outUri, contentValues, null, null)
            return true
        } catch (_: FileNotFoundException) {
            return false
        }
    }

    private fun File.getUri(context: Context): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)
    }
}
