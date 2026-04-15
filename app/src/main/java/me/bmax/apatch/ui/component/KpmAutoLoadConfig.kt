package me.bmax.apatch.ui.component

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import me.bmax.apatch.util.SafeUriResolver
import me.bmax.apatch.util.getRootShell
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import android.util.Base64

data class KpmAutoLoadConfig(
    val enabled: Boolean = false,
    val kpmPaths: List<String> = emptyList()
)

object KpmAutoLoadManager {
    private const val TAG = "KpmAutoLoadManager"
    private const val PREFS_NAME = "kpm_autoload_prefs"
    private const val KEY_FIRST_TIME_SHOWN = "first_time_shown"
    private const val KEY_FIRST_TIME_KPM_PAGE_SHOWN = "first_time_kpm_page_shown"
    private const val KPMS_BASE_DIR = "/data/adb/fp/kpms"
    private const val KPMS_AUTOLOAD_DIR = "/data/adb/fp/kpms/autoload"
    private const val CONFIG_PATH = "/data/adb/fp/kpms/kpm_autoload_config.json"

    var isEnabled = mutableStateOf(false)
        private set
    var kpmPaths = mutableStateOf<List<String>>(emptyList())
        private set

    fun isFirstTime(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_FIRST_TIME_SHOWN, false)
    }

    fun setFirstTimeShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FIRST_TIME_SHOWN, true).apply()
    }

    fun isFirstTimeKpmPage(context: Context): Boolean {
        return !context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FIRST_TIME_KPM_PAGE_SHOWN, false)
    }

    fun setFirstTimeKpmPageShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FIRST_TIME_KPM_PAGE_SHOWN, true).apply()
    }

    fun importKpm(context: Context, uri: android.net.Uri): String? {
        val rawFileName = getFileName(context, uri) ?: "unknown_${System.currentTimeMillis()}.kpm"
        val safeFileName = rawFileName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            .ifEmpty { "unknown_${System.currentTimeMillis()}.kpm" }
        val finalFileName = if (!safeFileName.endsWith(".kpm", ignoreCase = true))
            "${safeFileName}.kpm" else safeFileName

        var destPath = "$KPMS_AUTOLOAD_DIR/$finalFileName"
        val tempFile = File(context.cacheDir, finalFileName)

        try {
            SafeUriResolver.openInputStream(context, uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            val shell = getRootShell()

            val existsOutList = java.util.ArrayList<String>()
            val existsResult = shell.newJob().add("test -f '$destPath' && echo 1 || echo 0").to(existsOutList, null).exec()
            if (existsResult.isSuccess && existsOutList.any { it.trim() == "1" }) {
                val baseName = finalFileName.substringBeforeLast(".", finalFileName)
                destPath = "$KPMS_AUTOLOAD_DIR/${baseName}_${System.currentTimeMillis()}.kpm"
            }

            val result = shell.newJob().add(
                "mkdir -p '$KPMS_AUTOLOAD_DIR'",
                "cp -f '${tempFile.absolutePath}' '$destPath'",
                "chmod 644 '$destPath'"
            ).to(null, null).exec()

            return if (result.isSuccess) {
                Log.d(TAG, "KPM导入成功: $destPath")
                destPath
            } else {
                Log.e(TAG, "KPM导入失败")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "KPM导入失败: ${e.message}", e)
            return null
        } finally {
            tempFile.delete()
        }
    }

    private fun getFileName(context: Context, uri: android.net.Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) result = cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }

    fun cleanupUnusedKpms(currentPaths: List<String>) {
        try {
            val shell = getRootShell()
            val keepSet = currentPaths.toSet()
            val outList = java.util.ArrayList<String>()
            val listResult = shell.newJob().add("ls -1 '$KPMS_AUTOLOAD_DIR'/*.kpm 2>/dev/null").to(outList, null).exec()
            if (!listResult.isSuccess) return
            outList.filter { it.isNotEmpty() && !keepSet.contains(it) }.forEach { path ->
                shell.newJob().add("rm -f '${path.replace("'", "'\\''")}' 2>/dev/null").exec()
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理KPM文件失败: ${e.message}", e)
        }
    }

    fun loadConfig(context: Context): KpmAutoLoadConfig {
        return try {
            val shell = getRootShell()
            val outList = java.util.ArrayList<String>()
            val result = shell.newJob().add("cat '$CONFIG_PATH' 2>/dev/null").to(outList, null).exec()
            if (!result.isSuccess || outList.isEmpty()) {
                Log.d(TAG, "配置文件不存在或为空，使用默认配置")
                return KpmAutoLoadConfig()
            }

            val jsonContent = outList.joinToString("\n")
            val config = parseConfigFromJson(jsonContent) ?: KpmAutoLoadConfig()
            isEnabled.value = config.enabled
            kpmPaths.value = config.kpmPaths
            config
        } catch (e: Exception) {
            Log.e(TAG, "加载配置失败: ${e.message}", e)
            KpmAutoLoadConfig()
        }
    }

    fun saveConfig(context: Context, config: KpmAutoLoadConfig): Boolean {
        return try {
            val jsonContent = getConfigJson(config)
            val encoded = Base64.encodeToString(jsonContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val shell = getRootShell()
            val result = shell.newJob().add(
                "mkdir -p '$KPMS_BASE_DIR'",
                "echo '$encoded' | base64 -d > '$CONFIG_PATH'",
                "chmod 644 '$CONFIG_PATH'"
            ).to(null, null).exec()

            if (result.isSuccess) {
                isEnabled.value = config.enabled
                kpmPaths.value = config.kpmPaths
                cleanupUnusedKpms(config.kpmPaths)
                true
            } else {
                Log.e(TAG, "配置保存失败")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存配置失败: ${e.message}", e)
            false
        }
    }

    fun getConfigJson(): String {
        return getConfigJson(KpmAutoLoadConfig(isEnabled.value, kpmPaths.value))
    }

    fun getConfigJson(config: KpmAutoLoadConfig): String {
        val jsonObject = JSONObject()
        jsonObject.put("enabled", config.enabled)

        val pathsArray = JSONArray()
        config.kpmPaths.forEach { path -> pathsArray.put(path) }
        jsonObject.put("kpmPaths", pathsArray)

        return jsonObject.toString(2)
    }

    fun parseConfigFromJson(jsonString: String): KpmAutoLoadConfig? {
        return try {
            val jsonObject = JSONObject(jsonString)
            val enabled = jsonObject.optBoolean("enabled", false)

            val kpmPaths = mutableListOf<String>()
            val pathsArray = jsonObject.optJSONArray("kpmPaths")
            if (pathsArray != null) {
                for (i in 0 until pathsArray.length()) {
                    pathsArray.optString(i)?.let { path ->
                        if (path.isNotEmpty()) kpmPaths.add(path)
                    }
                }
            }

            KpmAutoLoadConfig(enabled, kpmPaths)
        } catch (e: Exception) {
            Log.e(TAG, "解析JSON失败: ${e.message}", e)
            null
        }
    }
}
