package me.bmax.apatch.util

import android.os.Parcelable
import android.util.Log
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import java.io.File
import java.io.FileWriter
import kotlin.concurrent.thread

object PkgConfig {
    private const val TAG = "PkgConfig"

    private const val CSV_HEADER = "pkg,exclude,allow,uid,to_uid,sctx"

    @Immutable
    @Parcelize
    @Keep
    data class Config(
        var pkg: String = "", var exclude: Int = 0, var allow: Int = 0, var profile: Natives.Profile
    ) : Parcelable {
        companion object {
            /**
             * 解析单行配置。期望格式：pkg,exclude,allow,uid,to_uid,sctx
             * 对格式异常的行（字段不足、类型不匹配等）返回 null，由调用方跳过，
             * 避免配置文件中的一行坏数据导致整个应用崩溃（见 IndexOutOfBoundsException）。
             */
            fun fromLine(line: String): Config? {
                val sp = line.split(',', limit = 6)
                if (sp.size < 6) return null
                val pkg = sp[0].trim()
                val exclude = sp[1].trim().toIntOrNull()
                val allow = sp[2].trim().toIntOrNull()
                val uid = sp[3].trim().toIntOrNull()
                val toUid = sp[4].trim().toIntOrNull()
                val scontext = sp[5].trim()
                if (pkg.isEmpty() || exclude == null || allow == null ||
                    uid == null || toUid == null || scontext.isEmpty()
                ) return null
                return Config(pkg, exclude, allow, Natives.Profile(uid, toUid, scontext))
            }
        }

        fun isDefault(): Boolean {
            return allow == 0 && exclude == 0
        }

        fun toLine(): String {
            return "${pkg},${exclude},${allow},${profile.uid},${profile.toUid},${profile.scontext}"
        }
    }

    fun readConfigs(): HashMap<Int, Config> {
        val configs = HashMap<Int, Config>()
        val file = File(APApplication.PACKAGE_CONFIG_FILE)
        if (file.exists()) {
            file.readLines().filter { it.isNotBlank() }.forEach {
                Log.d(TAG, it)
                val p = Config.fromLine(it) ?: return@forEach // 跳过格式异常的行
                if (!p.isDefault()) {
                    configs[p.profile.uid] = p
                }
            }
        }
        return configs
    }

    private fun writeConfigs(configs: HashMap<Int, Config>) {
        val file = File(APApplication.PACKAGE_CONFIG_FILE)
        if (!file.parentFile?.exists()!!) file.parentFile?.mkdirs()
        val tmpFile = File.createTempFile("package_config", ".tmp", file.parentFile)
        try {
            val writer = FileWriter(tmpFile, false)
            writer.use { w ->
                w.write(CSV_HEADER + '\n')
                configs.values.forEach {
                    if (!it.isDefault()) {
                        w.write(it.toLine() + '\n')
                    }
                }
            }
            if (!tmpFile.renameTo(file)) {
                throw IllegalStateException("Failed to rename temp file to ${file.absolutePath}")
            }
        } catch (e: Exception) {
            tmpFile.delete()
            throw e
        }
    }

    fun changeConfig(config: Config) {
        synchronized(PkgConfig.javaClass) {
            Natives.su()
            val configs = readConfigs()
            val uid = config.profile.uid
            if (config.allow == 1) {
                config.exclude = 0
            }
            if (config.isDefault() && configs[uid] != null) {
                configs.remove(uid)
            } else {
                Log.d(TAG, "change config: $config")
                configs[uid] = config
            }
            writeConfigs(configs)
        }
    }

    fun batchChangeConfigs(newConfigs: List<Config>) {
        thread {
            updateConfigs(newConfigs)
        }
    }

    fun updateConfigs(newConfigs: List<Config>) {
        synchronized(PkgConfig.javaClass) {
            Natives.su()
            val configs = readConfigs()

            newConfigs.forEach { config ->
                val uid = config.profile.uid
                // Root App should not be excluded
                if (config.allow == 1) {
                    config.exclude = 0
                }

                if (config.isDefault() && configs[uid] != null) {
                    configs.remove(uid)
                } else {
                    configs[uid] = config
                }
            }
            writeConfigs(configs)
        }
    }

    fun overwriteConfigs(newConfigs: List<Config>) {
        synchronized(PkgConfig.javaClass) {
            val configMap = HashMap<Int, Config>()
            newConfigs.forEach { config ->
                // Root App should not be excluded
                if (config.allow == 1) {
                    config.exclude = 0
                }
                if (!config.isDefault()) {
                    configMap[config.profile.uid] = config
                }
            }
            writeConfigs(configMap)
        }
    }
}
