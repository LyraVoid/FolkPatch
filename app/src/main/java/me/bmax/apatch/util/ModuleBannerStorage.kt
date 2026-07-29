package me.bmax.apatch.util

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * 模块 Banner 图片的本地存储管理。
 * APM 和 KPM 共用此工具，仅通过 [dirName] 区分存储目录。
 */
class ModuleBannerStorage(context: Context, private val dirName: String) {

    private val dir: File = File(context.filesDir, dirName).apply {
        if (!exists()) mkdirs()
    }

    private fun sanitizeKey(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun getBannerFile(key: String): File {
        return File(dir, sanitizeKey(key))
    }

    fun read(key: String): ByteArray? {
        return runCatching {
            val file = getBannerFile(key)
            if (file.exists()) file.readBytes().takeIf { it.isNotEmpty() } else null
        }.getOrNull()
    }

    fun write(context: Context, key: String, uri: Uri): ByteArray? {
        val data = SafeUriResolver.openInputStream(context, uri)?.use { it.readBytes() } ?: return null
        val file = getBannerFile(key)
        file.outputStream().use { it.write(data) }
        return data
    }

    fun clear(key: String): Boolean {
        val file = getBannerFile(key)
        return !file.exists() || file.delete()
    }
}
