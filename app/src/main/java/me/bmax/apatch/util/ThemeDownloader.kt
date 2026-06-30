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

            // 1. 下载主题文件（致命：失败则整体失败）
            val themeResult = downloadFileWithRetry(
                url = theme.downloadUrl,
                file = themeFile,
                taskId = taskId,
                isThemeFile = true
            ) { fileProgress ->
                // 主题文件进度占 70%
                val overall = fileProgress * 0.7f
                updateProgress(taskId, fileProgress, 0f, overall, DownloadStatus.DOWNLOADING, null)
            }

            if (!themeResult.success) {
                updateProgress(taskId, 0f, 0f, 0f, DownloadStatus.FAILED, themeResult.error)
                return@flow
            }

            // 2. 下载预览图（非致命：失败仅记录并跳过，不阻断主题安装）
            val previewFile = getPreviewImagePath(theme.author, theme.name)
            emit(DownloadProgress(
                themeId = taskId,
                fileProgress = 1f,
                imageProgress = 0f,
                overallProgress = 0.7f,
                status = DownloadStatus.DOWNLOADING,
                errorMessage = null
            ))

            val imageResult = downloadFileWithRetry(
                url = theme.previewUrl,
                file = previewFile,
                taskId = taskId,
                isThemeFile = false
            ) { imageProgress ->
                // 预览图进度占 30%
                val overall = 0.7f + (imageProgress * 0.3f)
                updateProgress(taskId, 1f, imageProgress, overall, DownloadStatus.DOWNLOADING, null)
            }

            if (!imageResult.success) {
                // 预览图下载失败：删除可能残留的不完整文件，记录警告，但主题仍算成功
                Log.w(TAG, "Preview image download skipped for theme ${theme.id}: ${imageResult.error}")
                if (previewFile.exists()) previewFile.delete()
            }

            // 3. 下载完成（预览图缺失不影响主题可用）
            updateProgress(taskId, 1f, 1f, 1f, DownloadStatus.COMPLETED, null)
            
            // 4. 保存主题元数据 JSON
            saveThemeMetadata(theme)
            
            // 5. 备份 FPT 文件到外部存储
            backupThemeFileToExternal(theme)
            
            // 从任务列表中移除
            downloadTasks.remove(taskId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for theme ${theme.id}", e)
            updateProgress(taskId, 0f, 0f, 0f, DownloadStatus.FAILED, describeException(e))
            downloadTasks.remove(taskId)
        } finally {
            downloadSemaphore.release()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 把异常转换为有意义的、非 null 的错误描述。
     * 优先用异常自带的 message，否则回退到异常类型名。
     */
    private fun describeException(e: Throwable): String {
        val msg = e.message
        return if (!msg.isNullOrBlank()) msg else (e::class.simpleName ?: "Unknown error")
    }

    /**
     * 单个文件下载结果。失败时 [error] 始终为非空、有意义的描述。
     */
    private data class DownloadFileResult(
        val success: Boolean,
        val error: String? = null
    )

    /**
     * 下载文件（支持断点续传）
     *
     * 续传正确性保证：仅当服务器真正返回 206 Partial Content 时才以 append 模式续写；
     * 若服务器忽略 Range 头返回 200（gh-proxy / gitee raw 等代理常见行为），
     * 则先截断已有半截文件、从头覆盖写入，避免出现「半截 + 完整」拼接的损坏文件。
     */
    private suspend fun downloadFileWithRetry(
        url: String,
        file: File,
        taskId: String,
        isThemeFile: Boolean,
        onProgress: (Float) -> Unit
    ): DownloadFileResult = withContext(Dispatchers.IO) {
        // 空/无效 URL 早期校验，避免 OkHttp 抛出无 message 的 IllegalArgumentException
        if (url.isBlank()) {
            return@withContext DownloadFileResult(false, "Invalid download URL")
        }

        var retryCount = 0
        var lastError: String = "Unknown error"

        while (retryCount < MAX_RETRIES) {
            try {
                val expectedSize = downloadOnce(url, file, taskId, onProgress)
                Log.d(TAG, "Download completed: ${file.absolutePath}")

                // 下载后完整性校验：避免残缺/被 CDN 替换的文件被标记为成功
                validateDownloadedFile(file, isThemeFile, expectedSize)

                return@withContext DownloadFileResult(true)
            } catch (e: Exception) {
                lastError = describeException(e)
                Log.w(TAG, "Download attempt ${retryCount + 1} failed: $lastError")
                retryCount++

                if (retryCount < MAX_RETRIES) {
                    // 等待后重试
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                }
            }
        }

        Log.e(TAG, "Download failed after $MAX_RETRIES retries: $lastError")
        // 彻底失败：清理可能不完整的残文件，避免下次续传从坏起点开始
        runCatching { if (file.exists()) file.delete() }
        DownloadFileResult(false, lastError)
    }

    /**
     * 执行一次下载尝试。正确处理 206 续传 / 200 全量覆盖两种情形。
     * @return 整个文件的期望总大小（字节），未知时为 -1
     */
    private fun downloadOnce(
        url: String,
        file: File,
        taskId: String,
        onProgress: (Float) -> Unit
    ): Long {
        // 已下载字节数（断点续传起点）
        val downloadedBytes = if (file.exists()) file.length() else 0L

        val request = Request.Builder()
            .url(url)
            .apply {
                if (downloadedBytes > 0) {
                    addHeader("Range", "bytes=$downloadedBytes-")
                }
            }
            .build()

        // 捕获期望总大小以便返回给调用方做完整性校验
        var expectedSize = -1L

        client.newCall(request).execute().use { response ->
            // 处理响应
            if (response.code !in 200..299) {
                throw IOException("HTTP error: ${response.code}")
            }

            // 关键：只有 206 才说明服务器真正按 Range 续传。
            // 200 表示服务器忽略了 Range，返回完整内容 —— 必须从头覆盖写。
            val resumeFromPartial = response.code == 206 && downloadedBytes > 0

            // 服务器本次响应声明的剩余部分大小（206 时是剩余，200 时是整体）
            val responseContentLength = response.body?.contentLength() ?: -1L

            // 整个文件的总大小（用于进度计算）
            val totalBytes = if (responseContentLength > 0) {
                if (resumeFromPartial) downloadedBytes + responseContentLength else responseContentLength
            } else {
                -1L // 未知总大小
            }
            expectedSize = totalBytes

            // 写文件：续传用 append，全量用 truncate（覆盖）
            FileOutputStream(file, resumeFromPartial).use { output ->
                response.body?.byteStream()?.use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Long = 0

                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read

                        // 计算进度
                        val progress = if (totalBytes > 0) {
                            ((if (resumeFromPartial) downloadedBytes else 0L) + bytesRead).toFloat() / totalBytes
                        } else {
                            // 未知总大小：用已下载字节数做渐进估算，避免恒卡在某个固定值
                            estimateUnknownProgress(bytesRead)
                        }

                        onProgress(progress.coerceIn(0f, 1f))

                        // 检查是否被取消
                        if (downloadTasks[taskId]?.isCancelled == true) {
                            throw IOException("Download cancelled")
                        }
                    }
                }
            }
        }

        return expectedSize
    }

    /**
     * 下载完成后校验文件完整性。
     * 防止以下情况被误标记为"下载完成"：
     * - CDN/代理返回 200 但内容是 HTML 错误页
     * - 传输中途连接断开但 InputStream 未抛异常
     * - Content-Length 与实际大小不匹配
     *
     * @throws IOException 若校验不通过，触发重试
     */
    @Throws(IOException::class)
    private fun validateDownloadedFile(file: File, isThemeFile: Boolean, expectedSize: Long) {
        if (!file.exists() || file.length() <= 0L) {
            throw IOException("Downloaded file is empty or missing")
        }

        // 如果知道期望大小，比对实际大小
        if (expectedSize > 0L && file.length() != expectedSize) {
            throw IOException(
                "File size mismatch: expected ${expectedSize}, got ${file.length()}"
            )
        }

        // 对 .fpt 主题文件做额外检查
        if (isThemeFile) {
            val fileSize = file.length()

            // .fpt 至少需要 16 字节 IV + 最小加密块（16 字节 AES 块）
            if (fileSize < 32L) {
                throw IOException("Theme file too small (${fileSize} bytes), minimum is 32")
            }

            // 读文件首部，检测是否被 CDN/代理替换为 HTML/JSON 错误页
            val header = ByteArray(16)
            runCatching {
                file.inputStream().use { it.read(header) }
            }.onFailure {
                throw IOException("Cannot read theme file header", it)
            }

            // 检查 HTML/JSON/纯零 签名
            // HTML 通常以 '<' 开头，JSON 以 '{' 开头，全零表示传输失败
            val firstByte = header[0].toInt() and 0xFF
            if (firstByte == '<'.code || firstByte == '{'.code) {
                val preview = String(header, 0, minOf(header.size, 60), Charsets.UTF_8)
                    .replace(0x00.toChar(), '.')
                throw IOException("Theme file appears to be a web error page, not encrypted data: $preview")
            }
            if (header.all { it == 0.toByte() }) {
                throw IOException("Theme file header is all zeros — transfer likely incomplete")
            }
        }
    }

    /**
     * 未知 Content-Length 时的进度估算：基于已下载字节数做平滑递增。
     * 主题文件通常 < 5MB，预览图 < 500KB；用渐进曲线逼近 1.0，避免恒停在一个固定值。
     * 公式：p = 1 - 1/(1 + bytes/256KB)，对任意大小都连续单调递增、永不超过 1。
     */
    private fun estimateUnknownProgress(downloaded: Long): Float {
        if (downloaded <= 0) return 0f
        val ref = 256.0 * 1024.0 // 256KB 作为「半程」参考量
        val p = 1.0 - 1.0 / (1.0 + downloaded.toDouble() / ref)
        return p.toFloat().coerceIn(0f, 0.99f)
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
