package me.bmax.apatch.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.bmax.apatch.apApp
import me.bmax.apatch.util.FolkApiClient
import org.json.JSONArray
import java.util.Locale

class OnlinePluginViewModel : ViewModel() {
    companion object {
        private const val TAG = "OnlinePluginViewModel"
        const val PLUGINS_URL = "https://folk.mysqil.com/api/modules?type=plugin"
    }

    data class OnlinePlugin(
        val name: String,
        val version: String,
        val url: String,
        val description: String,
    )

    var plugins by mutableStateOf<List<OnlinePlugin>>(emptyList())
        private set

    private var allPlugins = listOf<OnlinePlugin>()

    var searchQuery by mutableStateOf("")
        private set

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            plugins = allPlugins
        } else {
            plugins = allPlugins.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
            }
        }
    }

    var isRefreshing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun fetchPlugins() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            errorMessage = null
            try {
                val locale = Locale.getDefault()
                val language = locale.language
                val lang = if (language == "zh" || language == "mgl") "zh" else "en"
                val token = me.bmax.apatch.Natives.getApiToken(apApp)
                val url = "$PLUGINS_URL&lang=$lang&token=$token"

                val result = FolkApiClient.fetchJson(url)
                val jsonString = result.getOrNull()
                if (jsonString != null) {
                    val jsonArray = JSONArray(jsonString)
                    val list = ArrayList<OnlinePlugin>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val pluginUrl = obj.optString("url")

                        // 安全校验：跳过URL无效的插件
                        if (pluginUrl.isBlank() ||
                            (!pluginUrl.startsWith("https://") && !pluginUrl.startsWith("http://"))) {
                            Log.w(TAG, "Skipping plugin with unsafe url: $pluginUrl")
                            continue
                        }

                        val descZh = obj.optString("description")
                        val descEn = obj.optString("description_en")
                        val finalDesc = if (lang == "zh") {
                            descZh
                        } else {
                            if (descEn.isNotEmpty()) descEn else descZh
                        }

                        list.add(
                            OnlinePlugin(
                                name = obj.optString("name"),
                                version = obj.optString("version"),
                                url = pluginUrl,
                                description = finalDesc,
                            )
                        )
                    }
                    allPlugins = list
                    onSearchQueryChange(searchQuery)
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Failed to fetch plugins: ${exception?.message}")
                    errorMessage = "Error: ${exception?.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching plugins", e)
                errorMessage = "Error: ${e.message}"
            } finally {
                isRefreshing = false
            }
        }
    }
}
