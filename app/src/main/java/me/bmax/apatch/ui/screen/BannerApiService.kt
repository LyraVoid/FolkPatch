package me.bmax.apatch.ui.screen

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.apApp
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/**
 * 横幅API模式服务
 * 用于从随机图API或本地目录加载模块横幅图片
 */
object BannerApiService {
    private const val TAG = "BannerApiService"
    private const val API_BANNER_DIR_NAME = "api_banners"
    private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L

    // 支持的图片扩展名
    private val IMAGE_EXTENSIONS = setOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")

    /**
     * 获取模块的横幅图片（API模式）
     * @param context Context
     * @param moduleId 模块ID
     * @param source API URL或本地目录路径
     * @return 图片字节数组，失败返回null
     */
    suspend fun getModuleBanner(
        context: Context,
        moduleId: String,
        source: String
    ): ByteArray? {
        if (source.isBlank()) {
            Log.w(TAG, "API source is empty")
            return null
        }

        val trimmedSource = source.trim()
        // Generate source hash for cache key - each API source has its own cache
        val sourceHash = getSourceHash(trimmedSource)

        return try {
            // 首先检查缓存（包含source hash）
            val cachedBanner = getCachedBanner(context, moduleId, sourceHash)
            if (cachedBanner != null) {
                Log.d(TAG, "Using cached banner for module: $moduleId (source: $sourceHash)")
                return cachedBanner
            }

            val bannerData = if (isLocalDirectory(trimmedSource)) {
                getFromLocalDirectory(context, moduleId, trimmedSource)
            } else {
                getFromApi(context, moduleId, trimmedSource)
            }

            if (bannerData != null) {
                // 缓存图片（包含source hash）
                cacheBanner(context, moduleId, sourceHash, bannerData)
            }

            bannerData
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get banner for module $moduleId: ${e.message}", e)
            null
        }
    }

    /**
     * Generate a hash for the API source URL/path
     * This ensures each API source has its own cache namespace
     */
    private fun getSourceHash(source: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(source.toByteArray())
        return digest.take(8).joinToString("") { String.format("%02x", it) }
    }

    /**
     * 判断source是本地目录还是远程API
     */
    private fun isLocalDirectory(source: String): Boolean {
        return source.startsWith("/") || source.startsWith("file://")
    }

    /**
     * 从本地目录获取随机图片
     * 为每个模块分配固定的随机索引，确保同一模块每次使用相同图片
     */
    private suspend fun getFromLocalDirectory(
        context: Context,
        moduleId: String,
        directoryPath: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val cleanPath = directoryPath.removePrefix("file://")
            val dir = File(cleanPath)

            if (!dir.exists() || !dir.isDirectory) {
                Log.w(TAG, "Directory does not exist or is not a directory: $cleanPath")
                return@withContext null
            }

            // 获取目录中所有图片文件
            val imageFiles = dir.listFiles()?.filter { file ->
                file.isFile && file.extension.lowercase(Locale.getDefault()) in IMAGE_EXTENSIONS.map { it.removePrefix(".") }
            }?.sortedBy { it.name } ?: emptyList()

            if (imageFiles.isEmpty()) {
                Log.w(TAG, "No image files found in directory: $cleanPath")
                return@withContext null
            }

            // 使用模块ID的哈希值确定索引，确保同一模块每次获得相同图片
            val index = getIndexFromModuleId(moduleId, imageFiles.size)
            val selectedFile = imageFiles[index]

            Log.d(TAG, "Loading image from local directory for module $moduleId: ${selectedFile.name} (index $index of ${imageFiles.size})")
            selectedFile.readBytes()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load image from local directory: ${e.message}", e)
            null
        }
    }

    /**
     * 从随机图API获取图片
     * 使用模块ID作为随机种子，确保同一模块每次获得相同图片
     */
    private suspend fun getFromApi(
        context: Context,
        moduleId: String,
        apiUrl: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // 构建带种子的URL
            val urlWithSeed = buildUrlWithSeed(apiUrl, moduleId)

            Log.d(TAG, "Fetching banner from API for module $moduleId: $urlWithSeed")

            val request = Request.Builder()
                .url(urlWithSeed)
                .build()

            val response = apApp.okhttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.w(TAG, "API request failed with code: ${response.code}")
                return@withContext null
            }

            response.body?.bytes()?.also {
                Log.d(TAG, "Successfully loaded banner from API for module $moduleId, size: ${it.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch banner from API: ${e.message}", e)
            null
        }
    }

    /**
     * 构建带种子的URL
     * 根据URL格式智能添加种子参数
     */
    private fun buildUrlWithSeed(baseUrl: String, moduleId: String): String {
        val seed = getSeedFromModuleId(moduleId)

        // 检查URL是否已包含查询参数
        return if (baseUrl.contains("?")) {
            // 已有查询参数，追加种子
            "$baseUrl&seed=$seed"
        } else {
            // 无查询参数，添加种子
            "$baseUrl?seed=$seed"
        }
    }

    /**
     * 使用模块ID生成确定的索引
     */
    private fun getIndexFromModuleId(moduleId: String, listSize: Int): Int {
        val hash = moduleId.hashCode().let { if (it < 0) -it else it }
        return hash % listSize
    }

    /**
     * 使用模块ID生成种子值
     */
    private fun getSeedFromModuleId(moduleId: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(moduleId.toByteArray())
        return digest.take(4).joinToString("") { String.format("%02x", it) }
    }

    /**
     * 获取API横幅缓存目录
     */
    private fun getApiBannerDir(context: Context): File {
        val dir = File(context.filesDir, API_BANNER_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * 获取模块的缓存横幅文件
     * 包含source hash以确保不同API源的缓存隔离
     */
    private fun getCachedBannerFile(context: Context, moduleId: String, sourceHash: String): File {
        val sanitizedId = sanitizeModuleId(moduleId)
        // Include source hash in filename: sourceHash_moduleId
        val fileName = "${sourceHash}_$sanitizedId"
        return File(getApiBannerDir(context), fileName)
    }

    /**
     * 获取缓存的横幅
     */
    private suspend fun getCachedBanner(context: Context, moduleId: String, sourceHash: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val file = getCachedBannerFile(context, moduleId, sourceHash)
            if (file.exists()) {
                if (System.currentTimeMillis() - file.lastModified() > CACHE_TTL_MS) {
                    file.delete()
                    Log.d(TAG, "Cache expired for module $moduleId (source: $sourceHash)")
                    return@withContext null
                }
                file.readBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read cached banner: ${e.message}", e)
            null
        }
    }

    /**
     * 缓存横幅图片
     */
    private suspend fun cacheBanner(context: Context, moduleId: String, sourceHash: String, data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            val file = getCachedBannerFile(context, moduleId, sourceHash)
            file.writeBytes(data)
            Log.d(TAG, "Cached banner for module $moduleId (source: $sourceHash)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache banner: ${e.message}", e)
        }
    }

    /**
     * 清除所有API模式横幅缓存
     */
    suspend fun clearAllCache(context: Context) = withContext(Dispatchers.IO) {
        try {
            val dir = getApiBannerDir(context)
            dir.listFiles()?.forEach { it.delete() }
            Log.d(TAG, "Cleared all API banner cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache: ${e.message}", e)
        }
    }

    /**
     * 清除指定模块的所有缓存（跨所有API源）
     */
    suspend fun clearModuleCache(context: Context, moduleId: String) = withContext(Dispatchers.IO) {
        try {
            val sanitizedId = sanitizeModuleId(moduleId)
            val dir = getApiBannerDir(context)
            // Find all cache files for this module (pattern: *_<moduleId>)
            dir.listFiles()?.filter { file ->
                file.name.endsWith("_$sanitizedId")
            }?.forEach { file ->
                file.delete()
                Log.d(TAG, "Cleared cache file: ${file.name}")
            }
            Log.d(TAG, "Cleared all cache for module $moduleId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear module cache: ${e.message}", e)
        }
    }

    /**
     * 清理模块ID，确保文件名安全
     */
    private fun sanitizeModuleId(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
