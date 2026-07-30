package me.bmax.apatch.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.internal.UiThreadHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.apApp
import me.bmax.apatch.data.ScriptInfo
import java.io.File

 object ScriptLibraryManager {

    private const val LEGACY_BASE_DIR = "/storage/emulated/0/Download/FolkPatch"
    private const val LEGACY_SCRIPTS_DIR = "$LEGACY_BASE_DIR/script"
    private const val LEGACY_CONFIG_FILE = "$LEGACY_BASE_DIR/scripts_library.json"

    private val gson = Gson()

    private fun appBaseDir(): File {
        val external = apApp.getExternalFilesDir(null)
        val base = external ?: apApp.filesDir
        return File(base, "FolkPatch")
    }

    private fun appScriptsDir(): File = File(appBaseDir(), "script")

    private fun appConfigFile(): File = File(appBaseDir(), "scripts_library.json")

    private fun legacyBaseDir(): File = File(LEGACY_BASE_DIR)

    private fun legacyScriptsDir(): File = File(LEGACY_SCRIPTS_DIR)

    private fun legacyConfigFile(): File = File(LEGACY_CONFIG_FILE)

    private fun ensureDir(dir: File) {
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    private fun isLegacyWritable(): Boolean {
        val legacyDir = legacyBaseDir()
        ensureDir(legacyDir)
        return legacyDir.exists() && legacyDir.canWrite()
    }

    private fun writableScriptsDir(): File {
        val dir = if (isLegacyWritable()) legacyScriptsDir() else appScriptsDir()
        ensureDir(dir)
        return dir
    }

    private fun readConfig(file: File): List<ScriptInfo> {
        return try {
            if (!file.exists()) {
                emptyList()
            } else {
                val json = file.readText()
                val type = object : TypeToken<List<ScriptInfo>>() {}.type
                gson.fromJson<List<ScriptInfo>>(json, type) ?: emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildScriptsFromDirectories(directories: List<File>): List<ScriptInfo> {
        val files = directories
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir -> dir.listFiles()?.filter { it.isFile } ?: emptyList() }
            .sortedBy { it.name.lowercase() }
        return files.map { file ->
            val name = if (file.name.endsWith(".sh", ignoreCase = true)) {
                file.name.substring(0, file.name.length - 3)
            } else {
                file.name
            }
            ScriptInfo(path = file.absolutePath, alias = name)
        }
    }

    data class ScriptExecutionResult(
        val success: Boolean,
        val exitCode: Int,
        val output: String,
        val error: String
    )

    suspend fun loadScripts(): List<ScriptInfo> = withContext(Dispatchers.IO) {
        try {
            val parsedLegacy = if (isLegacyWritable()) readConfig(legacyConfigFile()) else emptyList()
            val parsedApp = readConfig(appConfigFile())
            val merged = linkedMapOf<String, ScriptInfo>()
            parsedLegacy.forEach { merged[it.path] = it }
            parsedApp.forEach { merged[it.path] = it }
            val combined = merged.values.filter { File(it.path).exists() }
            if (combined.isNotEmpty()) {
                saveScripts(combined)
                return@withContext combined
            }

            val rebuilt = buildScriptsFromDirectories(listOf(legacyScriptsDir(), appScriptsDir()))
            if (rebuilt.isNotEmpty()) {
                saveScripts(rebuilt)
            }
            rebuilt
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun saveScripts(scripts: List<ScriptInfo>): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(scripts)
            val appBase = appBaseDir()
            ensureDir(appBase)
            val appScripts = appScriptsDir()
            ensureDir(appScripts)
            appConfigFile().writeText(json)
            if (isLegacyWritable()) {
                ensureDir(legacyScriptsDir())
                legacyConfigFile().writeText(json)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun addScript(sourceFile: File, alias: String): ScriptInfo? = withContext(Dispatchers.IO) {
        try {
            val scriptsDir = writableScriptsDir()

            val scriptId = java.util.UUID.randomUUID().toString()
            
            val originalName = sourceFile.name
            var scriptFileName = originalName
            
            if (!originalName.endsWith(".sh", ignoreCase = true)) {
                scriptFileName = "$originalName.sh"
            }
            
            var scriptFile = File(scriptsDir, scriptFileName)
            var counter = 1
            
            while (scriptFile.exists()) {
                val nameWithoutExt = if (scriptFileName.endsWith(".sh", ignoreCase = true)) {
                    scriptFileName.substring(0, scriptFileName.length - 3)
                } else {
                    scriptFileName
                }
                scriptFileName = "${nameWithoutExt}_$counter.sh"
                scriptFile = File(scriptsDir, scriptFileName)
                counter++
            }
            
            val scriptPath = scriptFile.absolutePath

            sourceFile.inputStream().use { input ->
                scriptFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            scriptFile.setExecutable(true)

            val finalAlias = alias.ifEmpty { 
                if (scriptFileName.endsWith(".sh", ignoreCase = true)) {
                    scriptFileName.substring(0, scriptFileName.length - 3)
                } else {
                    scriptFileName
                }
            }
            
            val scriptInfo = ScriptInfo(
                id = scriptId,
                path = scriptPath,
                alias = finalAlias
            )

            scriptInfo
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun removeScript(scriptInfo: ScriptInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            val scriptFile = File(scriptInfo.path)
            if (scriptFile.exists()) {
                scriptFile.delete()
            }
            scriptBannerStorage.clear(scriptInfo.id)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun executeScript(scriptInfo: ScriptInfo): ScriptExecutionResult = withContext(Dispatchers.IO) {
        try {
            val outList = ArrayList<String>()
            val errList = ArrayList<String>()

            val result = getRootShell().newJob()
                .add("sh \"${scriptInfo.path}\"")
                .to(outList, errList)
                .exec()

            ScriptExecutionResult(
                success = result.isSuccess,
                exitCode = result.code,
                output = outList.joinToString("\n"),
                error = errList.joinToString("\n")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ScriptExecutionResult(
                success = false,
                exitCode = -1,
                output = "",
                error = e.message ?: "执行失败"
            )
        }
    }

    suspend fun executeScriptWithCallbacks(
        scriptInfo: ScriptInfo,
        onStdout: (String) -> Unit,
        onStderr: (String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val shell = getRootShell()
            var emittedOut = 0
            var emittedErr = 0
            val stdoutCallback = object : CallbackList<String>(UiThreadHandler::runAndWait) {
                override fun onAddElement(s: String) {
                    emittedOut++
                    onStdout(s)
                }
            }

            val stderrCallback = object : CallbackList<String>(UiThreadHandler::runAndWait) {
                override fun onAddElement(s: String) {
                    emittedErr++
                    onStderr(s)
                }
            }
            val result = shell.newJob()
                .add("sh \"${scriptInfo.path}\"")
                .to(stdoutCallback, stderrCallback)
                .exec()
            if (emittedOut < stdoutCallback.size) {
                for (i in emittedOut until stdoutCallback.size) {
                    onStdout(stdoutCallback[i])
                }
            }
            if (emittedErr < stderrCallback.size) {
                for (i in emittedErr until stderrCallback.size) {
                    onStderr(stderrCallback[i])
                }
            }
            result.isSuccess
        } catch (e: Exception) {
            e.printStackTrace()
            onStderr("Error: ${e.message}\n")
            false
        }
    }
}
