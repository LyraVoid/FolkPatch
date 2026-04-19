package me.bmax.apatch.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.theme.ThemeManager
import me.bmax.apatch.util.DownloadStatus
import me.bmax.apatch.util.DownloadProgress
import me.bmax.apatch.util.FolkApiClient
import me.bmax.apatch.util.ThemeDownloader
import org.json.JSONArray
import java.io.File

class ThemeStoreViewModel(private val context: Context) : ViewModel() {
    companion object {
        private const val TAG = "ThemeStoreViewModel"
        private const val THEMES_URL = "https://folk.mysqil.com/api/themes"
        
        // SharedPreferences 文件名
        private const val PREFS_NAME = "theme_store_prefs"
        private const val KEY_DOWNLOADED_THEMES = "downloaded_themes"
    }

    data class RemoteTheme(
        val id: String,
        val name: String,
        val author: String,
        val description: String,
        val version: String,
        val previewUrl: String,
        val downloadUrl: String,
        val type: String,
        val source: String
    )

    data class LocalTheme(
        val id: String,
        val name: String,
        val author: String,
        val description: String,
        val version: String,
        val previewUrl: String,
        val downloadUrl: String,
        val type: String,
        val source: String,
        val localPath: String,
        val previewImagePath: String,
        val downloadProgress: Float = 1f,
        val isDownloading: Boolean = false,
        val downloadStatus: DownloadStatus = DownloadStatus.COMPLETED
    )

    // Original full list
    private var allThemes = listOf<RemoteTheme>()

    var themes by mutableStateOf<List<RemoteTheme>>(emptyList())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var filterAuthor by mutableStateOf("")
        private set
    var filterSource by mutableStateOf("all")
        private set
    var filterTypePhone by mutableStateOf(true)
        private set
    var filterTypeTablet by mutableStateOf(true)
        private set

    // 下载器
    private val themeDownloader = ThemeDownloader(context)
    
    // 本地主题列表
    var localThemes by mutableStateOf<List<LocalTheme>>(emptyList())
        private set
    
    // 本地主题搜索查询
    var localSearchQuery by mutableStateOf("")
        private set
    
    // 过滤后的本地主题列表
    private var filteredLocalThemes = mutableListOf<LocalTheme>()
    
    // 下载任务
    private val downloadJobs = mutableMapOf<String, Job>()

    fun updateFilters(author: String, source: String, phone: Boolean, tablet: Boolean) {
        filterAuthor = author
        filterSource = source
        filterTypePhone = phone
        filterTypeTablet = tablet
        applyFilters()
    }

    private fun applyFilters() {
        themes = allThemes.filter { theme ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                theme.name.contains(searchQuery, ignoreCase = true) ||
                theme.author.contains(searchQuery, ignoreCase = true) ||
                theme.description.contains(searchQuery, ignoreCase = true)
            }

            val matchesAuthor = if (filterAuthor.isBlank()) true else {
                theme.author.contains(filterAuthor, ignoreCase = true)
            }

            val matchesSource = when (filterSource) {
                "official" -> theme.source == "official"
                "third_party" -> theme.source != "official"
                else -> true
            }

            val matchesType = (filterTypePhone && theme.type == "phone") ||
                              (filterTypeTablet && theme.type == "tablet")

            matchesSearch && matchesAuthor && matchesSource && matchesType
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        applyFilters()
    }

    var isRefreshing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun fetchThemes() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            errorMessage = null
            try {
                val token = me.bmax.apatch.Natives.getApiToken(apApp)
                val url = if (THEMES_URL.contains("?")) "$THEMES_URL&token=$token" else "$THEMES_URL?token=$token"

                val result = FolkApiClient.fetchJson(url)
                val jsonString = result.getOrNull()
                if (jsonString != null) {
                    val jsonArray = JSONArray(jsonString)
                    val list = ArrayList<RemoteTheme>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            RemoteTheme(
                                id = obj.optString("id"),
                                name = obj.optString("name"),
                                author = obj.optString("author"),
                                description = obj.optString("description"),
                                version = obj.optString("version"),
                                previewUrl = obj.optString("preview_url"),
                                downloadUrl = obj.optString("download_url"),
                                type = obj.optString("type", "phone"),
                                source = obj.optString("source", "third_party")
                            )
                        )
                    }
                    allThemes = list
                    onSearchQueryChange(searchQuery)
                    loadLocalThemes()
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Failed to fetch themes: ${exception?.message}")
                    errorMessage = "Error: ${exception?.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching themes", e)
                errorMessage = "Error: ${e.message}"
            } finally {
                isRefreshing = false
            }
        }
    }

    // ==================== 下载相关方法 ====================

    /**
     * 开始下载主题
     */
    fun startDownload(remoteTheme: RemoteTheme) {
        val themeId = remoteTheme.id
        
        // 检查是否已在下载
        if (downloadJobs.containsKey(themeId)) {
            Log.d(TAG, "Theme $themeId is already downloading")
            return
        }

        // 检查是否已下载
        if (isThemeDownloaded(remoteTheme.id)) {
            Log.d(TAG, "Theme $themeId is already downloaded")
            return
        }

        downloadJobs[themeId] = viewModelScope.launch {
            themeDownloader.downloadTheme(remoteTheme).collect { progress ->
                when (progress.status) {
                    DownloadStatus.COMPLETED -> {
                        // 下载完成，添加到本地主题列表
                        addLocalTheme(remoteTheme)
                        downloadJobs.remove(themeId)
                    }
                    DownloadStatus.FAILED -> {
                        Log.e(TAG, "Download failed: ${progress.errorMessage}")
                        downloadJobs.remove(themeId)
                    }
                    else -> {
                        // 更新下载进度
                        Log.d(TAG, "Download progress: ${progress.overallProgress * 100}%")
                    }
                }
            }
        }
    }

    /**
     * 取消下载
     */
    fun cancelDownload(themeId: String) {
        viewModelScope.launch {
            themeDownloader.cancelDownload(themeId)
            downloadJobs.remove(themeId)
        }
    }

    /**
     * 获取下载进度
     */
    fun getDownloadProgress(themeId: String): DownloadProgress? {
        return themeDownloader.getProgress(themeId)
    }

    /**
     * 获取下载进度 StateFlow
     */
    fun getDownloadProgressFlow(): StateFlow<Map<String, DownloadProgress>> {
        return themeDownloader.downloadProgress
    }

    // ==================== 本地主题管理 ====================

    /**
     * 加载本地主题列表
     */
    fun loadLocalThemes() {
        viewModelScope.launch(Dispatchers.IO) {
            val themesDir = themeDownloader.getThemesDir()
            val themes = mutableListOf<LocalTheme>()
            
            // 遍历作者目录
            themesDir.listFiles()?.forEach { authorDir ->
                if (authorDir.isDirectory) {
                    // 遍历主题目录
                    authorDir.listFiles()?.forEach { themeDir ->
                        if (themeDir.isDirectory) {
                            // 查找主题文件
                            val themeFile = themeDir.listFiles { file ->
                                file.extension == "fpt"
                            }?.firstOrNull()
                            
                            if (themeFile != null) {
                                val previewFile = File(themeDir, "Theme.webp")
                                val metaFile = File(themeDir, "theme_meta.json")
                                
                                // 从文件名提取主题名
                                val themeName = themeFile.nameWithoutExtension
                                val author = authorDir.name
                                
                                // 优先从 JSON 文件加载元数据
                                val localTheme = if (metaFile.exists()) {
                                    try {
                                        val json = org.json.JSONObject(metaFile.readText())
                                        LocalTheme(
                                            id = json.optString("id", "${author}_$themeName"),
                                            name = json.optString("name", themeName),
                                            author = json.optString("author", author),
                                            description = json.optString("description", ""),
                                            version = json.optString("version", "unknown"),
                                            previewUrl = json.optString("previewUrl", ""),
                                            downloadUrl = json.optString("downloadUrl", ""),
                                            type = json.optString("type", "phone"),
                                            source = json.optString("source", "third_party"),
                                            localPath = themeFile.absolutePath,
                                            previewImagePath = previewFile.absolutePath
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to read metadata for $themeName", e)
                                        // JSON 读取失败，回退到基本模式
                                        createBasicLocalTheme(themeFile, previewFile, author, themeName)
                                    }
                                } else {
                                    // 没有 JSON 文件，尝试从 API 获取信息（旧主题迁移）
                                    val remoteTheme = allThemes.find { 
                                        sanitizeFilename(it.author) == sanitizeFilename(author) && 
                                        sanitizeFilename(it.name) == themeName
                                    }
                                    
                                    if (remoteTheme != null) {
                                        // 从 API 获取到信息，创建 JSON 文件（迁移）
                                        themeDownloader.saveThemeMetadata(remoteTheme)
                                        LocalTheme(
                                            id = remoteTheme.id,
                                            name = remoteTheme.name,
                                            author = remoteTheme.author,
                                            description = remoteTheme.description,
                                            version = remoteTheme.version,
                                            previewUrl = remoteTheme.previewUrl,
                                            downloadUrl = remoteTheme.downloadUrl,
                                            type = remoteTheme.type,
                                            source = remoteTheme.source,
                                            localPath = themeFile.absolutePath,
                                            previewImagePath = previewFile.absolutePath
                                        )
                                    } else {
                                        // 没有 API 信息，使用基本信息
                                        createBasicLocalTheme(themeFile, previewFile, author, themeName)
                                    }
                                }
                                
                                themes.add(localTheme)
                            }
                        }
                    }
                }
            }
            
            localThemes = themes
            filteredLocalThemes = localThemes.toMutableList()
            applyLocalFilter()
            saveDownloadedThemes(themes)
        }
    }
    
    /**
     * 创建基本的 LocalTheme（没有元数据时）
     */
    private fun createBasicLocalTheme(
        themeFile: File,
        previewFile: File,
        author: String,
        themeName: String
    ): LocalTheme {
        return LocalTheme(
            id = "${author}_$themeName",
            name = themeName,
            author = author,
            description = "",
            version = "unknown",
            previewUrl = "",
            downloadUrl = "",
            type = "phone",
            source = "third_party",
            localPath = themeFile.absolutePath,
            previewImagePath = previewFile.absolutePath
        )
    }

    /**
     * 添加本地主题
     */
    private fun addLocalTheme(remoteTheme: RemoteTheme) {
        val localTheme = LocalTheme(
            id = remoteTheme.id,
            name = remoteTheme.name,
            author = remoteTheme.author,
            description = remoteTheme.description,
            version = remoteTheme.version,
            previewUrl = remoteTheme.previewUrl,
            downloadUrl = remoteTheme.downloadUrl,
            type = remoteTheme.type,
            source = remoteTheme.source,
            localPath = themeDownloader.getThemeFilePath(remoteTheme.author, remoteTheme.name).absolutePath,
            previewImagePath = themeDownloader.getPreviewImagePath(remoteTheme.author, remoteTheme.name).absolutePath
        )
        
        localThemes = localThemes + localTheme
        saveDownloadedThemes(localThemes)
    }

    /**
     * 检查主题是否已下载
     */
    fun isThemeDownloaded(themeId: String): Boolean {
        return localThemes.any { it.id == themeId }
    }

    /**
     * 检查主题是否正在下载
     */
    fun isThemeDownloading(themeId: String): Boolean {
        return downloadJobs.containsKey(themeId)
    }
    
    /**
     * 本地主题搜索查询变更
     */
    fun onLocalSearchQueryChange(query: String) {
        localSearchQuery = query
        applyLocalFilter()
    }
    
    /**
     * 应用本地主题过滤器
     */
    private fun applyLocalFilter() {
        val filtered = filteredLocalThemes.filter { theme ->
            if (localSearchQuery.isBlank()) {
                true
            } else {
                theme.name.contains(localSearchQuery, ignoreCase = true) ||
                theme.author.contains(localSearchQuery, ignoreCase = true) ||
                theme.description.contains(localSearchQuery, ignoreCase = true)
            }
        }
        localThemes = filtered
    }

    /**
     * 应用主题
     */
    suspend fun applyTheme(localTheme: LocalTheme): Boolean = withContext(Dispatchers.IO) {
        try {
            val themeFile = File(localTheme.localPath)
            if (!themeFile.exists()) {
                Log.e(TAG, "Theme file not found: ${localTheme.localPath}")
                return@withContext false
            }
            
            val uri = Uri.fromFile(themeFile)
            val success = ThemeManager.importTheme(context, uri)
            
            if (success) {
                Log.i(TAG, "Theme applied: ${localTheme.name}")
            } else {
                Log.e(TAG, "Failed to apply theme: ${localTheme.name}")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error applying theme", e)
            false
        }
    }

    /**
     * 删除主题
     */
    suspend fun deleteTheme(localTheme: LocalTheme): Boolean = withContext(Dispatchers.IO) {
        try {
            // 删除主题文件
            val themeFile = File(localTheme.localPath)
            if (themeFile.exists()) {
                themeFile.delete()
            }
            
            // 删除预览图
            val previewFile = File(localTheme.previewImagePath)
            if (previewFile.exists()) {
                previewFile.delete()
            }
            
            // 删除主题目录（如果为空）
            val themeDir = File(localTheme.localPath).parentFile
            if (themeDir != null && themeDir.isDirectory && (themeDir.listFiles()?.isEmpty() != false)) {
                themeDir.delete()
            }
            
            // 从列表中移除
            localThemes = localThemes.filter { it.id != localTheme.id }
            saveDownloadedThemes(localThemes)
            
            Log.i(TAG, "Theme deleted: ${localTheme.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting theme", e)
            false
        }
    }

    /**
     * 获取主题存储目录
     */
    fun getThemesDir(): File {
        return themeDownloader.getThemesDir()
    }

    // ==================== SharedPreferences 持久化 ====================

    /**
     * 保存已下载主题列表
     */
    private fun saveDownloadedThemes(themes: List<LocalTheme>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeIds = themes.map { it.id }.toSet()
        prefs.edit().putStringSet(KEY_DOWNLOADED_THEMES, themeIds).apply()
    }

    /**
     * 清理文件名
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
     * ViewModel 工厂类
     */
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ThemeStoreViewModel::class.java)) {
                return ThemeStoreViewModel(context.applicationContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
