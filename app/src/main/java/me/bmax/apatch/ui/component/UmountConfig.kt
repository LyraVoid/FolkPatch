package me.bmax.apatch.ui.component

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.topjohnwu.superuser.Shell
import me.bmax.apatch.util.getRootShell
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

/**
 * Umount 配置数据类
 */
data class UmountConfig(
    val enabled: Boolean = false,
    val paths: String = ""  // 挂载点路径，每行一个
)


object UmountConfigManager {
    private const val TAG = "UmountConfigManager"
    private const val CONFIG_FILE_NAME = "umount_config.json"
    private const val UMOUNT_PATH_FILE = "/data/adb/fp/UmountPATH"

    // 当前配置状态
    var isEnabled = mutableStateOf(false)
        private set
    var paths = mutableStateOf("")
        private set

    /**
     * 加载配置文件
     */
    fun loadConfig(context: Context): UmountConfig {
        return try {
            val shell = me.bmax.apatch.util.getRootShell()

            val enabledFlag = try {
                val result = shell.newJob().add("test -f ${me.bmax.apatch.APApplication.UMOUNT_SERVICE_FILE}").exec()
                result.isSuccess
            } catch (_: Exception) { false }

            val pathsContent = try {
                val job = shell.newJob().add("cat $UMOUNT_PATH_FILE 2>/dev/null")
                val list = mutableListOf<String>()
                job.to(list, null).exec()
                list.joinToString("\n")
            } catch (_: Exception) { "" }

            if (enabledFlag || pathsContent.isNotBlank()) {
                isEnabled.value = enabledFlag
                paths.value = pathsContent
                Log.d(TAG, "配置从 /data/adb/ 加载: enabled=$enabledFlag, paths length=${pathsContent.length}")
                UmountConfig(enabledFlag, pathsContent)
            } else {
                val configFile = File(context.filesDir, CONFIG_FILE_NAME)
                if (configFile.exists()) {
                    val reader = FileReader(configFile)
                    val jsonContent = reader.readText()
                    reader.close()
                    val config = parseConfigFromJson(jsonContent) ?: UmountConfig()
                    isEnabled.value = config.enabled
                    paths.value = config.paths
                    Log.d(TAG, "配置从应用私有目录加载: enabled=${config.enabled}")
                    config
                } else {
                    UmountConfig()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载配置失败: ${e.message}", e)
            UmountConfig()
        }
    }

    /**
     * 保存配置文件
     *
     * 流程：
     * 1. 保存配置到应用私有目录（JSON）
     * 2. 如果 paths 不为空，创建/更新 UmountPATH 文件
     * 3. 如果 paths 为空，删除 UmountPATH 文件
     * 4. 根据 enabled 状态设置服务开关（控制开机是否执行）
     */
    fun saveConfig(context: Context, config: UmountConfig): Boolean {
        Log.d(TAG, "=== saveConfig 开始 ===")
        Log.d(TAG, "enabled: ${config.enabled}, paths: '${config.paths}'")
        return try {
            // 1. 保存 JSON 配置到应用私有目录
            val configFile = File(context.filesDir, CONFIG_FILE_NAME)
            val jsonContent = getConfigJson(config)
            val writer = FileWriter(configFile)
            writer.write(jsonContent)
            writer.close()

            // 更新内存状态
            isEnabled.value = config.enabled
            paths.value = config.paths
            Log.d(TAG, "配置保存成功: enabled=${config.enabled}")

            // 2. 处理 UmountPATH 文件（只要 paths 不为空就创建）
            if (config.paths.isNotBlank()) {
                Log.d(TAG, "paths 不为空，调用 createUmountPathFile")
                val result = createUmountPathFile(context, config.paths)
                Log.d(TAG, "createUmountPathFile 返回: $result")
            } else {
                Log.d(TAG, "paths 为空，调用 deleteUmountPathFile")
                deleteUmountPathFile(context)
            }

            // 3. 设置服务开关（控制开机是否执行）
            Log.d(TAG, "调用 setUmountServiceEnabled(${config.enabled})")
            me.bmax.apatch.util.setUmountServiceEnabled(config.enabled)

            Log.d(TAG, "=== saveConfig 完成 ===")
            true
        } catch (e: Exception) {
            Log.e(TAG, "保存配置失败: ${e.message}", e)
            false
        }
    }

    /**
     * 获取配置的JSON字符串
     */
    fun getConfigJson(): String {
        return getConfigJson(UmountConfig(isEnabled.value, paths.value))
    }

    /**
     * 获取指定配置的JSON字符串
     */
    fun getConfigJson(config: UmountConfig): String {
        val jsonObject = JSONObject()
        jsonObject.put("enabled", config.enabled)
        jsonObject.put("paths", config.paths)
        return jsonObject.toString(2)
    }

    /**
     * 从JSON字符串解析配置
     */
    fun parseConfigFromJson(jsonString: String): UmountConfig? {
        return try {
            val jsonObject = JSONObject(jsonString)
            val enabled = jsonObject.optBoolean("enabled", false)
            val paths = jsonObject.optString("paths", "")
            UmountConfig(enabled, paths)
        } catch (e: Exception) {
            Log.e(TAG, "解析JSON失败: ${e.message}", e)
            null
        }
    }

    /**
     * 创建或更新 UmountPATH 文件
     * 需要 Root 权限
     */
    private fun createUmountPathFile(context: Context, paths: String): Boolean {
        return try {
            val shell = getRootShell()

            // 确保目录存在
            shell.newJob().add("mkdir -p /data/adb/fp/bin").exec()

            // 写入临时文件
            val tempFile = File(context.cacheDir, "UmountPATH_temp")
            tempFile.writeText(paths)

            // 使用 Root 权限复制文件并设置权限
            val result = shell.newJob().add(
                "cp ${tempFile.absolutePath} $UMOUNT_PATH_FILE",
                "chmod 644 $UMOUNT_PATH_FILE",
                "restorecon $UMOUNT_PATH_FILE"
            ).exec()

            tempFile.delete()

            if (result.isSuccess) {
                Log.d(TAG, "创建/更新 UmountPATH 文件成功")
                true
            } else {
                Log.e(TAG, "创建 UmountPATH 文件失败: ${result.err.joinToString()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建 UmountPATH 文件失败: ${e.message}", e)
            false
        }
    }

    /**
     * 删除 UmountPATH 文件
     */
    private fun deleteUmountPathFile(context: Context): Boolean {
        return try {
            val shell = getRootShell()
            val result = shell.newJob().add("rm -f $UMOUNT_PATH_FILE").exec()

            if (result.isSuccess) {
                Log.d(TAG, "删除 UmountPATH 文件成功")
                true
            } else {
                Log.e(TAG, "删除 UmountPATH 文件失败: ${result.err.joinToString()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除 UmountPATH 文件失败: ${e.message}", e)
            false
        }
    }

    /**
     * 调试方法：检查当前配置状态
     */
    fun debugConfigState() {
        Log.d(TAG, "=== Umount 配置状态 ===")
        Log.d(TAG, "enabled=${isEnabled.value}")
        Log.d(TAG, "paths length=${paths.value.length}")
        Log.d(TAG, "========================")
    }
}
