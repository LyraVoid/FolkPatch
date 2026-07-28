package me.bmax.apatch.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import java.io.File
import java.security.MessageDigest

import kotlinx.coroutines.async

object ModuleBackupUtils {

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
}
