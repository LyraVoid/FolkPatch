package me.bmax.apatch.util

import android.content.Context
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import me.bmax.apatch.apApp
import org.json.JSONObject
import java.io.File
import java.util.Properties

// ==================== Constants ====================

const val FOLK_BANNER_FILE_NAME = "FolkBanner"
private const val FOLK_BANNER_DIR_NAME = "folk_banners"
private const val KPM_BANNER_DIR_NAME = "kpm_banners"
private const val SCRIPT_BANNER_DIR_NAME = "script_banners"
private const val APM_CUSTOM_MODULE_INFO_DIR_NAME = "apm_custom_module_info"
private const val KPM_CUSTOM_MODULE_INFO_DIR_NAME = "kpm_custom_module_info"

// ==================== Banner Storage ====================

val apmBannerStorage by lazy { ModuleBannerStorage(apApp.applicationContext, FOLK_BANNER_DIR_NAME) }

val kpmBannerStorage by lazy { ModuleBannerStorage(apApp.applicationContext, KPM_BANNER_DIR_NAME) }

val scriptBannerStorage by lazy { ModuleBannerStorage(apApp.applicationContext, SCRIPT_BANNER_DIR_NAME) }

// ==================== Key Sanitization ====================

fun sanitizeModuleKey(raw: String): String {
    return raw.replace(Regex("[^a-zA-Z0-9._-]"), "_")
}

// ==================== Module Dir Resolution ====================

fun resolveModuleDir(rootShell: Shell, moduleId: String): String {
    val suFile = { path: String ->
        SuFile(path).apply { shell = rootShell }
    }
    val defaultDir = "/data/adb/modules/$moduleId"
    return runCatching {
        val direct = suFile(defaultDir)
        if (direct.exists()) {
            direct.path
        } else {
            val modulesRoot = suFile("/data/adb/modules")
            val dirs = modulesRoot.listFiles() ?: return@runCatching defaultDir
            for (dir in dirs) {
                if (!dir.isDirectory) continue
                val propFile = suFile("${dir.path}/module.prop")
                if (!propFile.exists()) continue
                val props = Properties()
                props.load(propFile.newInputStream())
                val id = props.getProperty("id")?.trim()
                if (id == moduleId) {
                    return@runCatching dir.path
                }
            }
            defaultDir
        }
    }.getOrDefault(defaultDir)
}

fun readModulePropBanner(rootShell: Shell, resolvedDir: String): String? {
    return runCatching {
        val propFile = SuFile("$resolvedDir/module.prop").apply { shell = rootShell }
        if (propFile.exists()) {
            val props = Properties()
            props.load(propFile.newInputStream())
            props.getProperty("banner")?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }.getOrNull()
}

fun clearLegacyFolkBanner(rootShell: Shell, resolvedDir: String): Boolean {
    val file = SuFile("$resolvedDir/$FOLK_BANNER_FILE_NAME").apply { shell = rootShell }
    return !file.exists() || file.delete()
}

// ==================== Custom Module Info ====================

data class CustomModuleInfo(
    val name: String? = null,
    val version: String? = null,
    val author: String? = null,
    val description: String? = null
) {
    fun hasAnyInfo(): Boolean = !name.isNullOrBlank() || !version.isNullOrBlank() || !author.isNullOrBlank() || !description.isNullOrBlank()

    fun toJson(): String {
        val json = JSONObject()
        name?.takeIf { it.isNotBlank() }?.let { json.put("name", it) }
        version?.takeIf { it.isNotBlank() }?.let { json.put("version", it) }
        author?.takeIf { it.isNotBlank() }?.let { json.put("author", it) }
        description?.takeIf { it.isNotBlank() }?.let { json.put("description", it) }
        return json.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): CustomModuleInfo? {
            return runCatching {
                val json = JSONObject(jsonStr)
                CustomModuleInfo(
                    name = if (json.has("name")) json.optString("name") else null,
                    version = if (json.has("version")) json.optString("version") else null,
                    author = if (json.has("author")) json.optString("author") else null,
                    description = if (json.has("description")) json.optString("description") else null,
                )
            }.getOrNull()
        }
    }
}

private fun getCustomModuleInfoFile(context: Context, dirName: String, moduleId: String): File {
    val dir = File(context.filesDir, dirName)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return File(dir, sanitizeModuleKey(moduleId) + ".json")
}

/**
 * 自定义模块信息存储，APM 与 KPM 各自使用独立目录，
 * 避免 prune 时互相清除对方的自定义信息。
 */
class CustomModuleInfoStorage(private val context: Context, private val dirName: String) {

    fun read(moduleId: String): CustomModuleInfo? {
        return runCatching {
            val file = getCustomModuleInfoFile(context, dirName, moduleId)
            if (file.exists()) {
                CustomModuleInfo.fromJson(file.readText())
            } else null
        }.getOrNull()
    }

    fun write(moduleId: String, info: CustomModuleInfo) {
        runCatching {
            val file = getCustomModuleInfoFile(context, dirName, moduleId)
            file.writeText(info.toJson())
        }
    }

    fun clear(moduleId: String) {
        runCatching {
            val file = getCustomModuleInfoFile(context, dirName, moduleId)
            if (file.exists()) file.delete()
        }
    }

    fun prune(validModuleIds: Set<String>) {
        runCatching {
            val dir = File(context.filesDir, dirName)
            if (!dir.exists()) return@runCatching
            val validFiles = validModuleIds.map { sanitizeModuleKey(it) + ".json" }.toSet()
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.name !in validFiles) {
                    file.delete()
                }
            }
        }
    }
}

val apmCustomModuleInfoStorage by lazy {
    CustomModuleInfoStorage(apApp.applicationContext, APM_CUSTOM_MODULE_INFO_DIR_NAME)
}

val kpmCustomModuleInfoStorage by lazy {
    CustomModuleInfoStorage(apApp.applicationContext, KPM_CUSTOM_MODULE_INFO_DIR_NAME)
}
