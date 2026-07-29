package me.bmax.apatch.util

import android.system.Os
import android.util.Log
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import me.bmax.apatch.apApp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.ini4j.Ini
import java.io.File
import java.io.StringReader

/**
 * 通过解析当前 Boot 镜像，获取其中「嵌入」的 KPM 模块名单。
 *
 * 原理：嵌入的 KPM 会被打包进 boot 镜像（kpimg 的 extras 中），
 * 而运行时加载的 KPM 不在其中。因此将当前 boot 镜像中的 KPM 名单
 * 与运行时已加载的 KPM 列表对比，即可区分「已嵌入」与「已加载」。
 *
 * 返回值约定：
 * - `null`  -> 解析失败（无法读取 boot 分区等），调用方不应展示区分徽标
 * - `Set`   -> 成功，集合内为嵌入的 KPM 名称（可能为空，表示没有嵌入任何 KPM）
 */
object EmbeddedKpmUtils {
    private const val TAG = "EmbeddedKpmUtils"
    private const val WORK_DIR_NAME = "kpm_embed_check"
    private val parseMutex = Mutex()

    /**
     * 解析当前 boot 镜像，返回其中嵌入的 KPM 名称集合；失败时返回 null。
     * 该操作需要 root 权限并会读取 boot 分区，耗时较长，应在 IO 线程调用并缓存结果。
     */
    suspend fun getEmbeddedKpmNames(): Set<String>? = parseMutex.withLock {
        val workDir = FileSystemManager.getLocal().getFile(apApp.filesDir.parent, WORK_DIR_NAME)
        try {
            setupWorkDir(workDir)
            val shell = createRootShellSafe(false)
            Log.i(TAG, "shell created, isRoot=${shell.isRoot}")
            try {
                val bootDev = extractBootDevice(shell, workDir) ?: return@withLock null
                Log.i(TAG, "boot device: $bootDev")
                parseEmbeddedKpms(shell, workDir, bootDev)
            } finally {
                runCatching { shell.close() }
                cleanupUnpacked(workDir)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "getEmbeddedKpmNames failed", e)
            null
        }
    }

    /**
     * 准备工作目录：释放 kptools / busybox 以及定位 boot 分区所需的脚本。
     * 每次都清空重建，避免应用更新后 nativeLibraryDir 路径变化导致符号链接失效。
     */
    private fun setupWorkDir(workDir: ExtendedFile) {
        workDir.deleteRecursively()
        workDir.mkdirs()

        val execs = listOf("libkptools.so", "libbusybox.so")
        val info = apApp.applicationInfo
        val libs = File(info.nativeLibraryDir).listFiles { _, name -> execs.contains(name) }
            ?: emptyArray()
        Log.i(TAG, "nativeLibraryDir=${info.nativeLibraryDir}, libs=${libs.map { it.name }}")

        for (lib in libs) {
            val name = lib.name.substring(3, lib.name.length - 3)
            val target = File(workDir.path, name)
            try {
                Os.symlink(lib.path, target.path)
            } catch (e: Exception) {
                Log.w(TAG, "symlink $name failed, fallback to copy: ${e.message}")
                lib.inputStream().copyAndClose(target.outputStream())
            }
        }

        for (script in listOf("boot_extract.sh", "util_functions.sh")) {
            val dest = File(workDir.path, script)
            apApp.assets.open(script).writeTo(dest)
        }
    }

    /**
     * 运行 boot_extract.sh 定位当前 boot 分区路径。
     */
    private fun extractBootDevice(shell: com.topjohnwu.superuser.Shell, workDir: ExtendedFile): String? {
        val result = shellForResult(
            shell,
            "export ASH_STANDALONE=1",
            "cd ${workDir.path}",
            "./busybox sh ./boot_extract.sh",
        )
        if (!result.isSuccess) {
            Log.e(TAG, "boot_extract failed: out=${result.out.joinToString(" | ")} err=${result.err.joinToString(" | ")}")
            return null
        }
        return result.out.find { it.startsWith("BOOTIMAGE=") }?.removePrefix("BOOTIMAGE=")
    }

    /**
     * 解包 boot 镜像并解析其中嵌入的 KPM 名称。
     */
    private fun parseEmbeddedKpms(
        shell: com.topjohnwu.superuser.Shell,
        workDir: ExtendedFile,
        bootDev: String
    ): Set<String>? {
        val result = shellForResult(
            shell,
            "cd ${workDir.path}",
            "./kptools unpacknolog $bootDev",
            "./kptools -l -i kernel",
        )
        if (!result.isSuccess) {
            Log.e(TAG, "kptools parse failed: out=${result.out.joinToString(" | ")} err=${result.err.joinToString(" | ")}")
            return null
        }
        Log.i(TAG, "kptools raw output: ${result.out.joinToString("\n")}")

        return try {
            val ini = Ini(StringReader(result.out.joinToString("\n")))
            val kernel = ini["kernel"] ?: return null
            val patched = kernel["patched"]?.toString()?.toBooleanStrictOrNull() ?: run {
                Log.e(TAG, "missing or invalid patched state")
                return null
            }
            if (!patched) {
                return emptySet()
            }

            var kpmNum = kernel["extra_num"]?.toString()?.toIntOrNull()
            if (kpmNum == null) {
                kpmNum = ini["extras"]?.get("num")?.toString()?.toIntOrNull()
            }
            if (kpmNum == null || kpmNum < 0) {
                Log.e(TAG, "missing or invalid extras count")
                return null
            }

            val names = mutableSetOf<String>()
            if (kpmNum > 0) {
                for (i in 0..<kpmNum) {
                    val extra = ini["extra $i"] ?: run {
                        Log.e(TAG, "missing extra section at index $i")
                        return null
                    }
                    val type = extra["type"]?.toString()?.trim()?.uppercase() ?: run {
                        Log.e(TAG, "missing extra type at index $i")
                        return null
                    }
                    if (type == "KPM") {
                        val name = extra["name"]?.toString()?.trim().orEmpty()
                        if (name.isEmpty()) {
                            Log.e(TAG, "missing KPM name at index $i")
                            return null
                        }
                        names.add(name)
                    }
                }
            }
            Log.i(TAG, "parsed embedded kpm names: $names")
            names
        } catch (e: Exception) {
            Log.e(TAG, "parse INI failed", e)
            null
        }
    }

    /**
     * 清理解包产生的临时文件，释放存储空间。
     */
    private fun cleanupUnpacked(workDir: ExtendedFile) {
        runCatching {
            File(workDir.path, "kernel").delete()
            File(workDir.path, "boot.img").delete()
        }
    }
}
