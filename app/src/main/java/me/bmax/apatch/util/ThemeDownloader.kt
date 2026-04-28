package me.bmax.apatch.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.viewmodel.ThemeStoreViewModel
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 主题下载器 - 支持断点续传、并发下载、后台下载
 */
class ThemeDownloader(private val context: Context) {
    companion object {
        private const val TAG = "ThemeDownloader"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 2000L
        private const val DOWNLOAD_TIMEOUT_SEC = 300L
        private const val BUFFER_SIZE = 8192
        
        // 下载目录
        private const val THEMES_DIR_NAME = "themes"
        
        // 并发控制 - 最多 3 个同时下载
        private val downloadSemaphore = kotlinx.coroutines.sync.Semaphore(3)
    }

    // 下载任务状态
    private val downloadTasks = ConcurrentHashMap<String, DownloadTask>()
    
    // 下载进度状态
    private val _downloadProgress = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, DownloadProgress>> = _downloadProgress.asStateFlow()
    
    // 进度更新锁
    private val progressMutex = Mutex()
    
    // OkHttp 客户端
    private val client = OkHttpClient.Builder()
        .connectTimeout(DOWNLOAD_TIMEOUT_SEC, TimeUnit.SECONDS)
        .readTimeout(DOWNLOAD_TIMEOUT_SEC, TimeUnit.SECONDS)
        .build()

    /**
     * 获取主题存储目录
     */
    fun getThemesDir(): File {
        val themesDir = File(context.filesDir, THEMES_DIR_NAME)
        if (!themesDir.exists()) {
            themesDir.mkdirs()
        }
        return themesDir
    }

    /**
     * 获取主题的本地路径
     */
    fun getThemePath(author: String, themeName: String): File {
        val safeAuthor = sanitizeFilename(author)
        val safeThemeName = sanitizeFilename(themeName)
        val themeDir = File(getThemesDir(), "$safeAuthor/$safeThemeName")
        if (!themeDir.exists()) {
            themeDir.mkdirs()
        }
        return themeDir
    }

    /**
     * 获取主题文件路径
     */
    fun getThemeFilePath(author: String, themeName: String): File {
        return File(getThemePath(author, themeName), "${sanitizeFilename(themeName)}.fpt")
    }

    /**
     * 获取预览图路径
     */
    fun getPreviewImagePath(author: String, themeName: String): File {
        return File(getThemePath(author, themeName), "Theme.webp")
    }

    /**
     * 获取外部存储主题目录 (/storage/emulated/0/Download/FolkPatch/Themes/)
     */
    private fun getExternalThemesDir(): File {
        val externalDir = File(
            getSafeDownloadsDir(context),
            "FolkPatch/Themes"
        )
        if (!externalDir.exists()) {
            externalDir.mkdirs()
        }
        return externalDir
    }

    /**
     * 获取外部存储主题文件路径
     */
    private fun getExternalThemeFilePath(author: String, themeName: String): File {
        val safeAuthor = sanitizeFilename(author)
        val safeThemeName = sanitizeFilename(themeName)
        val themeDir = File(getExternalThemesDir(), "$safeAuthor/$safeThemeName")
        if (!themeDir.exists()) {
            themeDir.mkdirs()
        }
        return File(themeDir, "${sanitizeFilename(themeName)}.fpt")
    }

    /**
     * 备份主题 FPT 文件到外部存储
     */
    private fun backupThemeFileToExternal(theme: ThemeStoreViewModel.RemoteTheme) {
        try {
            val internalThemeFile = getThemeFilePath(theme.author, theme.name)
            val externalThemeFile = getExternalThemeFilePath(theme.author, theme.name)
            
            // 复制 FPT 文件到外部存储
            if (internalThemeFile.exists()) {
                internalThemeFile.copyTo(externalThemeFile, overwrite = true)
                Log.d(TAG, "Backed up theme FPT to: ${externalThemeFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup theme FPT to external storage", e)
        }
    }

    /**
     * 开始下载主题
     */
    fun downloadTheme(theme: ThemeStoreViewModel.RemoteTheme): Flow<DownloadProgress> = flow {
        val taskId = theme.id
        
        // 检查是否已在下载
        if (downloadTasks.containsKey(taskId)) {
            emit(DownloadProgress(
                themeId = taskId,
                fileProgress = 0f,
                imageProgress = 0f,
                overallProgress = 0f,
                status = DownloadStatus.DOWNLOADING,
                errorMessage = "Already downloading"
            ))
            return@flow
        }

        // 获取信号量（限制并发数）
        downloadSemaphore.acquire()
        
        try {
            val task = DownloadTask(theme)
            downloadTasks[taskId] = task
            
            // 初始状态
            emit(DownloadProgress(
                themeId = taskId,
                fileProgress = 0f,
                imageProgress = 0f,
                overallProgress = 0f,
                status = DownloadStatus.DOWNLOADING,
                errorMessage = null
            ))
            
            // 1. 下载主题文件
            val themeFile = getThemeFilePath(theme.author, theme.name)
            emit(DownloadProgress(
                themeId = taskId,
                fileProgress = 0f,
                imageProgress = 0f,
                overallProgress = 0f,
                status = DownloadStatus.DOWNLOADING,
                errorMessage = null
            ))
            
            val themeSuccess = downloadFileWithRetry(
                url = theme.downloadUrl,
                file = themeFile,
                taskId = taskId,
                isThemeFile = true
            ) { fileProgress ->
                // 主题文件进度占 70%
                val overall = fileProgress * 0.7f
                updateProgress(taskId, fileProgress, 0f, overall, DownloadStatus.DOWNLOADING, null)
            }
            
            if (!themeSuccess) {
                updateProgress(taskId, 0f, 0f, 0f, DownloadStatus.FAILED, "Failed to download theme file")
                return@flow
            }
            
            // 2. 下载预览图
            val previewFile = getPreviewImagePath(theme.author, theme.name)
            emit(DownloadProgress(
                themeId = taskId,
                fileProgress = 1f,
                imageProgress = 0f,
                overallProgress = 0.7f,
                status = DownloadStatus.DOWNLOADING,
                errorMessage = null
            ))
            
            val imageSuccess = downloadFileWithRetry(
                url = theme.previewUrl,
                file = previewFile,
                taskId = taskId,
                isThemeFile = false
            ) { imageProgress ->
                // 预览图进度占 30%
                val overall = 0.7f + (imageProgress * 0.3f)
                updateProgress(taskId, 1f, imageProgress, overall, DownloadStatus.DOWNLOADING, null)
            }
            
            if (!imageSuccess) {
                updateProgress(taskId, 1f, 0f, 0.7f, DownloadStatus.FAILED, "Failed to download preview image")
                return@flow
            }
            
            // 3. 下载完成
            updateProgress(taskId, 1f, 1f, 1f, DownloadStatus.COMPLETED, null)
            
            // 4. 保存主题元数据 JSON
            saveThemeMetadata(theme)
            
            // 5. 备份 FPT 文件到外部存储
            backupThemeFileToExternal(theme)
            
            // 从任务列表中移除
            downloadTasks.remove(taskId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for theme ${theme.id}", e)
            updateProgress(taskId, 0f, 0f, 0f, DownloadStatus.FAILED, e.message ?: "Unknown error")
            downloadTasks.remove(taskId)
        } finally {
            downloadSemaphore.release()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 下载文件（支持断点续传）
     */
    private suspend fun downloadFileWithRetry(
        url: String,
        file: File,
        taskId: String,
        isThemeFile: Boolean,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var retryCount = 0
        var lastError: String? = null
        
        while (retryCount < MAX_RETRIES) {
            try {
                // 获取已下载的字节数（断点续传）
                val downloadedBytes = if (file.exists()) file.length() else 0L
                
                // 构建请求
                val request = Request.Builder()
                    .url(url)
                    .apply {
                        if (downloadedBytes > 0) {
                            addHeader("Range", "bytes=$downloadedBytes-")
                        }
                    }
                    .build()
                
                val response = client.newCall(request).execute()
                
                // 处理响应
                if (response.code !in 200..299) {
                    throw IOException("HTTP error: ${response.code}")
                }
                
                val totalBytes = if (response.body?.contentLength() ?: -1L > 0) {
                    downloadedBytes + (response.body?.contentLength() ?: 0L)
                } else {
                    -1L
                }
                
                // 写入文件
                FileOutputStream(file, downloadedBytes > 0).use { output ->
                    response.body?.byteStream()?.use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Long = 0
                        
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read
                            
                            // 计算进度
                            val progress = if (totalBytes > 0) {
                                (downloadedBytes + bytesRead).toFloat() / totalBytes
                            } else {
                                // 未知总大小时，使用已下载字节数估算
                                0.9f // 暂时显示 90%
                            }
                            
                            onProgress(progress.coerceIn(0f, 1f))
                            
                            // 检查是否被取消
                            if (downloadTasks[taskId]?.isCancelled == true) {
                                throw IOException("Download cancelled")
                            }
                        }
                    }
                }
                
                Log.d(TAG, "Download completed: ${file.absolutePath}")
                return@withContext true
                
            } catch (e: Exception) {
                lastError = e.message ?: "Unknown error"
                Log.w(TAG, "Download attempt ${retryCount + 1} failed: $lastError")
                retryCount++
                
                if (retryCount < MAX_RETRIES) {
                    // 等待后重试
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                }
            }
        }
        
        Log.e(TAG, "Download failed after $MAX_RETRIES retries: $lastError")
        false
    }

    /**
     * 更新进度状态
     */
    private fun updateProgress(
        themeId: String,
        fileProgress: Float,
        imageProgress: Float,
        overallProgress: Float,
        status: DownloadStatus,
        errorMessage: String?
    ) {
        val currentMap = _downloadProgress.value.toMutableMap()
        currentMap[themeId] = DownloadProgress(
            themeId = themeId,
            fileProgress = fileProgress,
            imageProgress = imageProgress,
            overallProgress = overallProgress,
            status = status,
            errorMessage = errorMessage
        )
        _downloadProgress.value = currentMap
    }

    /**
     * 获取主题元数据 JSON 文件路径
     */
    fun getMetaJsonFile(author: String, themeName: String): File {
        return File(getThemePath(author, themeName), "theme_meta.json")
    }

    /**
     * 保存主题元数据到 JSON 文件
     */
    fun saveThemeMetadata(theme: ThemeStoreViewModel.RemoteTheme) {
        try {
            val metaFile = getMetaJsonFile(theme.author, theme.name)
            val json = JSONObject().apply {
                put("id", theme.id)
                put("name", theme.name)
                put("author", theme.author)
                put("description", theme.description)
                put("version", theme.version)
                put("type", theme.type)
                put("source", theme.source)
                put("previewUrl", theme.previewUrl)
                put("downloadUrl", theme.downloadUrl)
            }
            metaFile.writeText(json.toString(2)) // 格式化输出
            Log.d(TAG, "Theme metadata saved: ${metaFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save theme metadata", e)
        }
    }

    /**
     * 取消下载
     */
    suspend fun cancelDownload(themeId: String) {
        val task = downloadTasks[themeId]
        if (task != null) {
            task.isCancelled = true
            
            // 删除已下载的文件和目录
            withContext(Dispatchers.IO) {
                val themeDir = getThemePath(task.theme.author, task.theme.name)
                if (themeDir.exists()) {
                    themeDir.deleteRecursively()
                    Log.d(TAG, "Cancelled download, deleted theme directory: ${themeDir.absolutePath}")
                }
            }
        }
        
        progressMutex.withLock {
            val currentMap = _downloadProgress.value.toMutableMap()
            currentMap.remove(themeId)
            _downloadProgress.value = currentMap
        }
    }

    /**
     * 获取下载进度
     */
    fun getProgress(themeId: String): DownloadProgress? {
        return _downloadProgress.value[themeId]
    }

    /**
     * 清理文件名中的非法字符
     */
    private fun sanitizeFilename(filename: String): String {
        val illegalChars = "<>:\"/\\|?*"
        var result = filename
        for (char in illegalChars) {
            result = result.replace(char, '_')
        }
        return result.trim()
    }

    /**
     * 下载任务
     */
    private class DownloadTask(val theme: ThemeStoreViewModel.RemoteTheme) {
        @Volatile
        var isCancelled = false
    }
}

/**
 * 下载进度数据类
 */
data class DownloadProgress(
    val themeId: String,
    val fileProgress: Float,      // 主题文件进度 0.0-1.0
    val imageProgress: Float,     // 预览图进度 0.0-1.0
    val overallProgress: Float,   // 总体进度 0.0-1.0
    val status: DownloadStatus,
    val errorMessage: String? = null
)

/**
 * 下载状态枚举
 */
enum class DownloadStatus {
    PENDING,      // 等待下载
    DOWNLOADING,  // 下载中
    PAUSED,       // 已暂停
    COMPLETED,    // 已完成
    FAILED,       // 下载失败
    RETRYING      // 重试中
}
