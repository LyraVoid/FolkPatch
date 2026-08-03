package me.bmax.apatch.util

import android.util.Log
import android.net.Uri
import java.io.File
import java.io.IOException

/**
 * 私有目录文件写入兜底工具。
 *
 * 部分设备上壁纸/主题应用失败的常见根因：
 * 1. 目标路径某一级被同名普通文件占位，mkdirs 静默失败（ENOTDIR）；
 * 2. 目标文件路径被同名目录占用，FileOutputStream/copyTo 直接抛异常；
 * 3. 存储空间不足，写入失败但无明确诊断信息。
 * 每次写入前调用这里的兜底逻辑，而非仅在路径构建时处理一次。
 */
object FsUtils {
    private const val TAG = "FsUtils"

    /**
     * 确保目录存在且确实是目录。
     * 路径上残留同名普通文件时先清理再重建；仍失败则抛出带剩余空间提示的异常。
     */
    @Throws(IOException::class)
    fun ensureDirectory(dir: File) {
        if (dir.isDirectory) return
        // 向上找到第一个实际存在的路径节点；若它是普通文件，删掉后才能建子目录
        var existing: File? = dir
        while (existing != null && !existing.exists()) {
            existing = existing.parentFile
        }
        if (existing != null && !existing.isDirectory) {
            Log.w(TAG, "Path component is a regular file, deleting: ${existing.absolutePath}")
            if (!existing.delete()) {
                throw IOException("Cannot remove file blocking directory: ${existing.absolutePath}")
            }
        }
        if (!dir.mkdirs() && !dir.isDirectory) {
            val usable = runCatching { (existing ?: dir).usableSpace }.getOrDefault(-1L)
            val hint = if (usable in 0 until 10L * 1024 * 1024) {
                " (storage almost full: ${usable / 1024}KB left)"
            } else ""
            throw IOException("Cannot create directory: ${dir.absolutePath}$hint")
        }
    }

    /**
     * 写入文件前的兜底：确保父目录可用，并清理占用目标路径的同名目录。
     */
    @Throws(IOException::class)
    fun prepareTargetFile(file: File): File {
        file.parentFile?.let { ensureDirectory(it) }
        if (file.isDirectory) {
            Log.w(TAG, "Target file path is a directory, deleting: ${file.absolutePath}")
            if (!file.deleteRecursively()) {
                throw IOException("Cannot remove directory blocking file: ${file.absolutePath}")
            }
        }
        return file
    }

    /**
     * 带兜底的文件拷贝：目标父目录不存在或被占位时自动修复后再 copyTo。
     */
    @Throws(IOException::class)
    fun hardenedCopy(source: File, dest: File): File {
        prepareTargetFile(dest)
        return source.copyTo(dest, overwrite = true)
    }

    /**
     * 删除文件或目录（目录递归删除），失败仅记录日志。
     * 用于清理旧背景文件等场景：路径被同名目录占用时 File.delete() 必然失败。
     */
    fun deleteQuietly(file: File) {
        if (!file.exists()) return
        val deleted = if (file.isDirectory) file.deleteRecursively() else file.delete()
        if (!deleted) {
            Log.w(TAG, "Failed to delete: ${file.absolutePath}")
        }
    }

    /**
     * 从持久化的 file:// URI 中提取本地文件路径。
     * 非 file:// 的 URI（如 content://）返回 null，表示无法用本地路径校验。
     */
    fun localPathFromUri(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        return runCatching {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") uri.path else null
        }.getOrNull()
    }

    /**
     * 校验持久化的文件 URI 是否仍然指向存在的本地文件。
     * - URI 为空/非本地文件 URI：视为可用（不做破坏性清理）；
     * - file:// URI 指向的文件缺失或为空：视为失效，调用方应清除对应配置。
     */
    fun isFileUriAvailable(uriString: String?): Boolean {
        val path = localPathFromUri(uriString) ?: return true
        if (path.isBlank()) return true
        return runCatching {
            val file = File(path)
            file.isFile && file.length() > 0L
        }.getOrDefault(true)
    }
}
