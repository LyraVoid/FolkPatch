package me.bmax.apatch.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.util.getPluginConfig
import me.bmax.apatch.util.getPluginLog
import me.bmax.apatch.util.installPlugin
import me.bmax.apatch.util.listPlugins
import me.bmax.apatch.util.runPluginCallback
import me.bmax.apatch.util.runPluginCallbackWithOutput
import me.bmax.apatch.util.setPluginConfig
import me.bmax.apatch.util.setPluginState
import me.bmax.apatch.util.uninstallPlugin
import org.json.JSONArray
import org.json.JSONObject

class PluginViewModel : ViewModel() {
    companion object {
        private const val TAG = "PluginViewModel"
    }

    data class PluginConfigField(
        val key: String,
        val label: String,
        val labels: Map<String, String>,
        val type: String,
        val default: String,
        val options: List<String>,
    )

    data class PluginQuickAction(
        val function: String,
        val label: String,
        val labels: Map<String, String>,
    )

    data class PluginInfo(
        val id: String,
        val name: String,
        val author: String,
        val version: String,
        val description: String,
        val descriptions: Map<String, String>,
        val license: String,
        val enabled: Boolean,
        val hasManifest: Boolean,
        val hasAction: Boolean,
        val quickAction: PluginQuickAction?,
        val config: List<PluginConfigField>,
    )

    var plugins by mutableStateOf<List<PluginInfo>>(emptyList())
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun fetchPlugins() {
        viewModelScope.launch(Dispatchers.IO) {
            errorMessage = null
            isRefreshing = true
            try {
                val raw = listPlugins().trim()
                plugins = if (raw.startsWith("[")) {
                    parseJson(raw)
                } else {
                    parsePlain(raw)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load plugins", e)
                errorMessage = e.message ?: "Failed to load plugins"
            } finally {
                isRefreshing = false
            }
        }
    }

    private fun parseJson(raw: String): List<PluginInfo> {
        val array = JSONArray(raw)
        return (0 until array.length()).mapNotNull { i ->
            val obj: JSONObject = array.getJSONObject(i)
            val id = obj.optString("id").trim()
            if (id.isEmpty()) return@mapNotNull null
            val configArray = obj.optJSONArray("config")
            val config = if (configArray == null) emptyList() else {
                (0 until configArray.length()).mapNotNull { j ->
                    val c = configArray.optJSONObject(j) ?: return@mapNotNull null
                    val optionsArray = c.optJSONArray("options")
                    val options = if (optionsArray == null) emptyList() else {
                        (0 until optionsArray.length()).mapNotNull { optionsArray.optString(it) }
                    }
                    PluginConfigField(
                        key = c.optString("key"),
                        label = c.optString("label").ifBlank { c.optString("key") },
                        labels = parseLabelsMap(c.optJSONObject("labels")),
                        type = c.optString("type", "text"),
                        default = c.opt("default")?.toString() ?: "",
                        options = options,
                    )
                }
            }
            val quickActionObj = obj.optJSONObject("quick_action")
            val quickAction = quickActionObj?.let { q ->
                PluginQuickAction(
                    function = q.optString("function").ifBlank { "action" },
                    label = q.optString("label").ifBlank { q.optString("function") },
                    labels = parseLabelsMap(q.optJSONObject("labels")),
                )
            }
            PluginInfo(
                id = id,
                name = obj.optString("name").ifBlank { id },
                author = obj.optString("author"),
                version = obj.optString("version"),
                description = obj.optString("description"),
                descriptions = parseLabelsMap(obj.optJSONObject("descriptions")),
                license = obj.optString("license"),
                enabled = obj.optBoolean("enabled", true),
                hasManifest = obj.optBoolean("has_manifest", false),
                hasAction = obj.optBoolean("has_action", false),
                quickAction = quickAction,
                config = config,
            )
        }
    }

    private fun parseLabelsMap(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        val map = mutableMapOf<String, String>()
        obj.keys().forEach { key -> map[key] = obj.optString(key) }
        return map
    }

    private fun parsePlain(raw: String): List<PluginInfo> {
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split('\t', ' ', limit = 2)
                if (parts.isEmpty()) return@mapNotNull null
                val id = parts[0].trim()
                if (id.isEmpty()) return@mapNotNull null
                val enabled = parts.getOrNull(1)?.trim().orEmpty() != "disabled"
                PluginInfo(
                    id = id,
                    name = id,
                    author = "",
                    version = "",
                    description = "",
                    descriptions = emptyMap(),
                    license = "",
                    enabled = enabled,
                    hasManifest = false,
                    hasAction = false,
                    quickAction = null,
                    config = emptyList(),
                )
            }
            .toList()
    }

    suspend fun setPluginEnabled(id: String, enabled: Boolean): Boolean {
        val success = withContext(Dispatchers.IO) { setPluginState(id, enabled) }
        if (success) {
            plugins = plugins.map { if (it.id == id) it.copy(enabled = enabled) else it }
        }
        return success
    }

    fun installPluginZip(zipPath: String): Boolean =
        installPlugin(zipPath)

    fun removePlugin(id: String): Boolean =
        uninstallPlugin(id)

    fun getConfigValue(id: String, key: String): String =
        getPluginConfig(id, key)

    fun saveConfigValue(id: String, key: String, value: String): Boolean =
        setPluginConfig(id, key, value)

    suspend fun runCallback(id: String, function: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) { runPluginCallbackWithOutput(id, function) }

    suspend fun fetchLog(id: String): String =
        withContext(Dispatchers.IO) { getPluginLog(id) }
}
