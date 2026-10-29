package me.bmax.apatch.ui.screen.module

import android.net.Uri
import android.util.Log
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.viewmodel.safeKpmModuleId
import me.bmax.apatch.util.inputStream
import me.bmax.apatch.util.writeTo
import me.bmax.apatch.util.rootShellForResult
import java.io.IOException
import me.bmax.apatch.util.ModuleBackupUtils
import me.bmax.apatch.util.getFileNameFromUri
import kotlinx.coroutines.CoroutineScope
import java.io.StringReader
import org.ini4j.Ini

private const val TAG = "KernelPatchModule"

suspend fun loadModule(loadingDialog: LoadingDialogHandle, uri: Uri, args: String): Int {
    val rc = loadingDialog.withLoading {
        withContext(Dispatchers.IO) {
            run {
                val kpmDir: ExtendedFile = FileSystemManager.getLocal().getFile(apApp.cacheDir.path, "kpm")
                kpmDir.deleteRecursively()
                kpmDir.mkdirs()
                val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
                val kpm = kpmDir.getChildFile("${rand}.kpm")
                Log.d(TAG, "save tmp kpm: ${kpm.path}")
                var rc = -1
                try {
                    uri.inputStream().buffered().writeTo(kpm)

                    // Auto Backup Logic for KPM Load
                    val fileName = getFileNameFromUri(apApp, uri)
                    // Launch backup asynchronously
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = ModuleBackupUtils.autoBackupModule(apApp, kpm, fileName, "KPM")
                            if (result != null && !result.startsWith("Duplicate")) {
                                Log.e(TAG, "KPM Auto backup failed: $result")
                            } else {
                                Log.d(TAG, "KPM Auto backup success")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "KPM Auto backup error: ${e.message}")
                        }
                    }

                    rc = Natives.loadKernelPatchModule(kpm.path, args).toInt()
                } catch (e: IOException) {
                    Log.e(TAG, "Copy kpm error: $e")
                }
                Log.d(TAG, "load ${kpm.path} rc: $rc")
                rc
            }
        }
    }
    return rc
}

/** Install a KPM from an app-local temporary file; it takes effect after reboot. */
suspend fun installKpm(uri: Uri): Int = withContext(Dispatchers.IO) {
    val tempDir: ExtendedFile =
        FileSystemManager.getLocal().getFile(apApp.cacheDir.path, "kpm-install")
    tempDir.deleteRecursively()
    tempDir.mkdirs()
    val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
    val temp = tempDir.getChildFile("$rand.kpm")
    try {
        Log.d(TAG, "save temporary KPM: ${temp.path}")
        uri.inputStream().buffered().writeTo(temp)
        val infoResult = rootShellForResult(
            "${APApplication.APATCH_FOLDER}bin/kptools -l -M '${temp.path}'"
        )
        if (!infoResult.isSuccess) return@withContext -2
        val section = Ini(StringReader(infoResult.out.joinToString("\n")))["kpm"] ?: return@withContext -3
        val name = section["name"]?.toString()?.trim().orEmpty()
        if (name.isEmpty()) return@withContext -4
        val id = safeKpmModuleId(name)
        val dir = "${APApplication.KPMS_DIR}$id"
        val destination = "$dir/$id.kpm"
        val result = rootShellForResult(
            "mkdir -p '$dir' && cp -f '${temp.path}' '$destination'"
        )
        if (!result.isSuccess) return@withContext -5

        // Installed KPMs are loaded by the boot-time loader. Do not load them in
        // the current session; installation takes effect after reboot.
        Log.i(TAG, "install KPM $name to $destination; reboot required")
        0
    } catch (e: Exception) {
        Log.e(TAG, "install KPM failed", e)
        -1
    } finally {
        tempDir.deleteRecursively()
    }
}
