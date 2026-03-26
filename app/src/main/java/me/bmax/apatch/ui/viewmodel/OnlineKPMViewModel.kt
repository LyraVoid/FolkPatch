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
import org.json.JSONArray
import java.util.Locale

class OnlineKPMViewModel : ViewModel() {
    companion object {
        private const val TAG = "OnlineKPMViewModel"
        // Placeholder URL. User should update this.
        const val MODULES_URL = "https://folk.mysqil.com/api/modules.php?type=kpm"
    }

    data class OnlineKPM(
        val name: String,
        val version: String,
        val url: String,
        val description: String,
        val needControl: Boolean
    )

    var modules by mutableStateOf<List<OnlineKPM>>(emptyList())
        private set

    private var allModules = listOf<OnlineKPM>()

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

    fun fetchModules() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            try {
                val locale = Locale.getDefault()
                val language = locale.language
                val lang = if (language == "zh" || language == "mgl") "zh" else "en"
                val token = me.bmax.apatch.Natives.getApiToken(apApp)
                val url = "$MODULES_URL&lang=$lang&token=$token"

                val response = apApp.okhttpClient.newCall(
                    okhttp3.Request.Builder().url(url).build()
                ).execute()
                
                if (response.isSuccessful) {
                    val jsonString = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(jsonString)
                    val list = ArrayList<OnlineKPM>()
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
                            OnlineKPM(
                                name = obj.optString("name"),
                                version = obj.optString("version"),
                                url = obj.optString("url"),
                                description = finalDesc,
                                needControl = obj.optInt("parameter", 0) == 1
                            )
                        )
                    }
                    allModules = list
                    onSearchQueryChange(searchQuery)
                } else {
                    Log.e(TAG, "Failed to fetch modules: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching modules", e)
            } finally {
                isRefreshing = false
            }
        }
    }
}