package me.bmax.apatch.util

import android.content.Context
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.security.MessageDigest

import android.os.Environment

import kotlinx.coroutines.async

object ModuleBackupUtils {

    private const val MODULE_DIR = "/data/adb/modules"

    suspend fun autoBackupModule(context: Context, file: File, originalFileName: String?, subDir: String): String? {
        return withContext(Dispatchers.IO) {
            val errors = StringBuilder()
            
            val webdavJob = async {
                if (me.bmax.apatch.ui.theme.BackupConfig.isBackupEnabled) {
                     try {
                         val basePath = me.bmax.apatch.ui.theme.BackupConfig.webdavPath
                         // Construct full subDir: basePath + subDir (e.g. "/Backup" + "APM")
                         val fullSubDir = if (basePath.endsWith("/")) "$basePath$subDir" else "$basePath/$subDir"
                         val cleanSubDir = if (fullSubDir.startsWith("/")) fullSubDir.substring(1) else fullSubDir
                         
                         val webDavResult = WebDavUtils.uploadFile(
                            me.bmax.apatch.ui.theme.BackupConfig.webdavUrl,
                            me.bmax.apatch.ui.theme.BackupConfig.webdavUsername,
                            me.bmax.apatch.ui.theme.BackupConfig.webdavPassword,
                            file,
                            cleanSubDir,
                            originalFileName
                         )
                         if (webDavResult.isFailure) {
                             "WebDAV: ${webDavResult.exceptionOrNull()?.message}"
                         } else {
                             null
                         }
                     } catch (e: Exception) {
                         "WebDAV Error: ${e.message}"
                     }
                } else {
                    null
                }
            }

            val localJob = async {
                if (APApplication.sharedPreferences.getBoolean("auto_backup_module", false)) {
                    try {
                        val baseBackupDir = File(getSafeDownloadsDir(me.bmax.apatch.apApp), "FolkPatch/ModuleBackups")
                        // Use subDir (APM/KPM) to separate backups
                        val backupDir = File(baseBackupDir, subDir)
                        
                        if (!backupDir.exists()) backupDir.mkdirs()

                        // Calculate hash of the incoming file
                        val digest = MessageDigest.getInstance("SHA-256")
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        file.inputStream().use { input ->
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                digest.update(buffer, 0, bytesRead)
                            }
                        }
                        val fileHash = digest.digest().joinToString("") { "%02x".format(it) }

                        val baseName = originalFileName ?: file.name
                        val nameWithoutExt = baseName.substringBeforeLast(".")
                        val ext = baseName.substringAfterLast(".", "")
                        val extWithDot = if (ext.isNotEmpty()) ".$ext" else ""

                        var counter = 0
                        while (true) {
                            val candidateName = if (counter == 0) baseName else "$nameWithoutExt ($counter)$extWithDot"
                            val candidateFile = File(backupDir, candidateName)

                            if (candidateFile.exists()) {
                                // Check hash
                                val existingDigest = MessageDigest.getInstance("SHA-256")
                                candidateFile.inputStream().use { input ->
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        existingDigest.update(buffer, 0, bytesRead)
                                    }
                                }
                                val existingHash = existingDigest.digest().joinToString("") { "%02x".format(it) }

                                if (fileHash == existingHash) {
                                    // Duplicate found
                                    break
                                }
                                // Hash mismatch, try next name
                                counter++
                            } else {
                                // File doesn't exist, save here
                                file.copyTo(candidateFile)
                                break
                            }
                        }
                        null
                    } catch (e: Exception) {
                        "Local Error: ${e.message}"
                    }
                } else {
                    null
                }
            }

            val webdavError = webdavJob.await()
            val localError = localJob.await()
            
            if (webdavError != null) errors.append("$webdavError; ")
            if (localError != null) errors.append("$localError; ")
            
            if (errors.isNotEmpty()) errors.toString() else null
        }
    }

    suspend fun backupModules(context: Context, snackBarHost: SnackbarHostState, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                // Use the busybox bundled with APatch
                val busyboxPath = "/data/adb/ap/bin/busybox"
                val tempFile = File(context.cacheDir, "backup_tmp.tar.gz")
                val tempPath = tempFile.absolutePath

                if (tempFile.exists()) tempFile.delete()

                // Construct command to tar the modules directory to temp file
                // And chmod it so the app can read it
                val command = "cd \"$MODULE_DIR\" && $busyboxPath tar -czf \"$tempPath\" ./* && chmod 666 \"$tempPath\""

                val result = getRootShell().newJob().add(command).exec()

                if (result.isSuccess) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        tempFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    tempFile.delete()
                    withContext(Dispatchers.Main) {
                        snackBarHost.showSnackbar(context.getString(R.string.apm_backup_success))
                    }
                } else {
                    val error = result.err.joinToString("\n")
                    withContext(Dispatchers.Main) {
                        snackBarHost.showSnackbar(context.getString(R.string.apm_backup_failed_msg, error))
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackBarHost.showSnackbar(context.getString(R.string.apm_backup_failed_msg, e.message))
                }
            }
        }
    }

    suspend fun restoreModules(context: Context, snackBarHost: SnackbarHostState, uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val busyboxPath = "/data/adb/ap/bin/busybox"
                val tempFile = File(context.cacheDir, "restore_tmp.tar.gz")
                val tempPath = tempFile.absolutePath

                if (tempFile.exists()) tempFile.delete()

                SafeUriResolver.openInputStream(context, uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Make sure root can read it
                tempFile.setReadable(true, false)

                val command = "cd \"$MODULE_DIR\" && $busyboxPath tar -xzf \"$tempPath\""
                val result = getRootShell().newJob().add(command).exec()

                tempFile.delete()

                if (result.isSuccess) {
                    // Refresh module list
                    // APatchCli.refresh() // Wait, this refreshes shell, not module list. 
                    // Module list is refreshed by viewModel.fetchModuleList() in UI
                    
                    withContext(Dispatchers.Main) {
                        snackBarHost.showSnackbar(context.getString(R.string.apm_restore_success))
                    }
                } else {
                    val error = result.err.joinToString("\n")
                    withContext(Dispatchers.Main) {
                        snackBarHost.showSnackbar(context.getString(R.string.apm_restore_failed_msg, error))
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackBarHost.showSnackbar(context.getString(R.string.apm_restore_failed_msg, e.message))
                }
            }
        }
    }
}
