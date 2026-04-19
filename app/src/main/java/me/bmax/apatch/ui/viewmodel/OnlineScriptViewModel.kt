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

class OnlineScriptViewModel : ViewModel() {
    companion object {
        private const val TAG = "OnlineScriptViewModel"
        const val MODULES_URL = "https://folk.mysqil.com/api/modules?type=script"
    }

    data class OnlineScript(
        val name: String,
        val version: String,
        val url: String,
        val description: String
    )

    var modules by mutableStateOf<List<OnlineScript>>(emptyList())
        private set

    private var allModules = listOf<OnlineScript>()

    var searchQuery by mutableStateOf("")
        private set

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            modules = allModules
        } else {
            modules = allModules.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }
    }

    var isRefreshing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun fetchModules() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            errorMessage = null
            try {
                val locale = Locale.getDefault()
                val language = locale.language
                val lang = if (language == "zh" || language == "mgl") "zh" else "en"
                val token = me.bmax.apatch.Natives.getApiToken(apApp)
                val url = "$MODULES_URL&lang=$lang&token=$token"

                val result = FolkApiClient.fetchJson(url)
                val jsonString = result.getOrNull()
                if (jsonString != null) {
                    val jsonArray = JSONArray(jsonString)
                    val list = ArrayList<OnlineScript>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val descZh = obj.optString("description")
                        val descEn = obj.optString("description_en")
                        val finalDesc = if (lang == "zh") {
                            descZh
                        } else {
                            if (descEn.isNotEmpty()) descEn else descZh
                        }
                        
                        list.add(
                            OnlineScript(
                                name = obj.optString("name"),
                                version = obj.optString("version"),
                                url = obj.optString("url"),
                                description = finalDesc
                            )
                        )
                    }
                    allModules = list
                    onSearchQueryChange(searchQuery)
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Failed to fetch scripts: ${exception?.message}")
                    errorMessage = "Error: ${exception?.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching modules", e)
                errorMessage = "Error: ${e.message}"
            } finally {
                isRefreshing = false
            }
        }
    }
}
