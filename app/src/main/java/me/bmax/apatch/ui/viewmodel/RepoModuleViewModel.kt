package me.bmax.apatch.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.bmax.apatch.util.FolkApiClient
import org.json.JSONArray
import org.json.JSONObject

data class RepoModule(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Long,
    val author: String,
    val description: String,
    val license: String = "",
    val homepage: String = "",
    val source: String = "",
    val support: String = "",
    val versions: List<RepoVersion> = emptyList()
)

data class RepoVersion(
    val version: String,
    val versionCode: Long,
    val zipUrl: String,
    val changelog: String = "",
    val timestamp: Double = 0.0
)

data class ExploreRepository(
    val name: String,
    val url: String,
    val description: String = "",
    val modulesCount: Int = 0,
    val cover: String = ""
)

class RepoModuleViewModel : ViewModel() {
    var modules by mutableStateOf<List<RepoModule>>(emptyList())
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var searchQuery by mutableStateOf("")
        private set

    var repositories by mutableStateOf<List<ExploreRepository>>(emptyList())
        private set
    var isReposLoading by mutableStateOf(false)
        private set

    private var allModules: List<RepoModule> = emptyList()

    private val defaultRepositories = listOf(
        ExploreRepository(
            name = "Default Repository",
            url = "https://gr.dergoogler.com/gmr/",
            description = "Official default repository"
        )
    )

    var selectedModule by mutableStateOf<RepoModule?>(null)
        private set

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        modules = if (query.isBlank()) {
            allModules
        } else {
            allModules.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
        }
    }

    fun resetModules() {
        modules = emptyList()
        allModules = emptyList()
        errorMessage = null
    }

    fun selectModule(moduleId: String) {
        selectedModule = allModules.find { it.id == moduleId }
    }

    fun fetchModules(repoUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            errorMessage = null
            try {
                val actualUrl = if (repoUrl.endsWith(".json")) {
                    repoUrl
                } else {
                    val base = if (repoUrl.endsWith("/")) repoUrl else "$repoUrl/"
                    "${base}json/modules.json"
                }

                val result = FolkApiClient.fetchJson(actualUrl)
                result.fold(
                    onSuccess = { json -> parseModules(json, repoUrl) },
                    onFailure = { e -> errorMessage = e.message ?: "Network error" }
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
            } finally {
                isRefreshing = false
            }
        }
    }

    private fun parseModules(jsonStr: String, baseUrl: String) {
        val json = JSONObject(jsonStr)
        val modulesArray = json.optJSONArray("modules") ?: JSONArray()
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val parsed = mutableListOf<RepoModule>()
        for (i in 0 until modulesArray.length()) {
            val obj = modulesArray.getJSONObject(i)
            val track = obj.optJSONObject("track") ?: JSONObject()

            val versionsArray = obj.optJSONArray("versions") ?: JSONArray()
            val versions = mutableListOf<RepoVersion>()
            for (j in 0 until versionsArray.length()) {
                val vObj = versionsArray.getJSONObject(j)
                var zipUrl = vObj.optString("zipUrl", "")
                if (zipUrl.isNotEmpty() && !zipUrl.startsWith("http")) {
                    zipUrl = base + zipUrl
                }
                versions.add(
                    RepoVersion(
                        version = vObj.optString("version", ""),
                        versionCode = vObj.optLong("versionCode", 0),
                        zipUrl = zipUrl,
                        changelog = vObj.optString("changelog", ""),
                        timestamp = vObj.optDouble("timestamp", 0.0)
                    )
                )
            }

            parsed.add(
                RepoModule(
                    id = obj.optString("id", ""),
                    name = obj.optString("name", ""),
                    version = obj.optString("version", ""),
                    versionCode = obj.optLong("versionCode", 0),
                    author = obj.optString("author", ""),
                    description = obj.optString("description", ""),
                    license = track.optString("license", ""),
                    homepage = track.optString("homepage", ""),
                    source = track.optString("source", ""),
                    support = track.optString("support", ""),
                    versions = versions
                )
            )
        }

        allModules = parsed
        modules = parsed
    }

    fun fetchRepositories() {
        viewModelScope.launch(Dispatchers.IO) {
            isReposLoading = true
            try {
                val result = FolkApiClient.fetchJson("https://mmrl.dev/api/repositories.json")
                result.fold(
                    onSuccess = { json -> parseRepositories(json) },
                    onFailure = {
                        repositories = defaultRepositories
                    }
                )
            } catch (e: Exception) {
                repositories = defaultRepositories
            } finally {
                isReposLoading = false
            }
        }
    }

    private fun parseRepositories(jsonStr: String) {
        val array = JSONArray(jsonStr)
        val parsed = mutableListOf<ExploreRepository>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            parsed.add(
                ExploreRepository(
                    name = obj.optString("name", ""),
                    url = obj.optString("url", ""),
                    description = obj.optString("description", ""),
                    modulesCount = obj.optInt("modules_count", 0),
                    cover = obj.optString("cover", "")
                )
            )
        }
        val urls = parsed.map { it.url }.toSet()
        val merged = defaultRepositories.filter { it.url !in urls } + parsed
        repositories = merged
    }
}
