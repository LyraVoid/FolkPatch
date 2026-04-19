package me.bmax.apatch.ui.viewmodel

import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import me.bmax.apatch.APApplication
import me.bmax.apatch.apApp
import me.bmax.apatch.util.getRootShell
import me.bmax.apatch.util.listModules
import me.bmax.apatch.util.toggleModule
import org.json.JSONArray
import org.json.JSONObject
import java.text.Collator
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

class APModuleViewModel : ViewModel() {
    companion object {
        private const val TAG = "ModuleViewModel"
        private var modules by mutableStateOf<List<ModuleInfo>>(emptyList())
        private val zygiskModuleIds = listOf(
            "zygisksu",
            "zygisknext",
            "rezygisk",
            "neozygisk",
            "shirokozygisk"
        )
    }

    class ModuleInfo(
        val id: String,
        val name: String,
        val author: String,
        val version: String,
        val versionCode: Int,
        val description: String,
        val enabled: Boolean,
        val update: Boolean,
        val remove: Boolean,
        val updateJson: String,
        val hasWebUi: Boolean,
        val hasActionScript: Boolean,
        val webuiIcon: String?,
        val actionIcon: String?,
        val isMetamodule: Boolean,
        val isZygisk: Boolean,
        val isLSPosed: Boolean,
    )

    data class ModuleUpdateInfo(
        val version: String,
        val versionCode: Int,
        val zipUrl: String,
        val changelog: String,
    )

    data class BannerInfo(
        val bytes: ByteArray?,
        val url: String?
    )

    var isRefreshing by mutableStateOf(false)
        private set

    private val prefs = APApplication.sharedPreferences
    var sortOptimizationEnabled by mutableStateOf(prefs.getBoolean("module_sort_optimization", true))
    private val bannerCache = mutableStateMapOf<String, BannerInfo>()
    private val moduleSizeCache = mutableStateMapOf<String, Long>()
    private val updateSemaphore = Semaphore(3)
    val bannerSemaphore = Semaphore(4)

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "module_sort_optimization") {
            sortOptimizationEnabled = prefs.getBoolean("module_sort_optimization", true)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private val collator = Collator.getInstance(Locale.getDefault())

    val moduleList by derivedStateOf {
        if (sortOptimizationEnabled) {
            modules.sortedWith(
                compareByDescending<ModuleInfo> { it.isMetamodule }
                    .thenByDescending { it.isZygisk }
                    .thenByDescending { it.isLSPosed }
                    .thenByDescending { it.hasWebUi }
                    .thenByDescending { it.hasActionScript }
                    .thenByDescending { it.name.contains("vector", ignoreCase = true) }
                    .thenBy(collator) { it.id }
            )
        } else {
            modules.sortedWith(compareBy(collator, ModuleInfo::id))
        }
    }

    var isNeedRefresh by mutableStateOf(false)
        private set

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    fun disableAllModules() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            modules.forEach { 
                if (it.enabled) {
                    toggleModule(it.id, false)
                }
            }
            fetchModuleList()
        }
    }

    fun getBannerInfo(id: String): BannerInfo? = bannerCache[id]

    fun putBannerInfo(id: String, info: BannerInfo) {
        bannerCache[id] = info
    }

    fun removeBannerInfo(id: String) {
        bannerCache.remove(id)
    }

    fun clearBannerCache() {
        bannerCache.clear()
    }

    private fun pruneBannerCache(validIds: Set<String>) {
        val keysToRemove = bannerCache.keys.filter { it !in validIds }
        keysToRemove.forEach { bannerCache.remove(it) }
    }

    private fun pruneModuleSizeCache(validIds: Set<String>) {
        val keysToRemove = moduleSizeCache.keys.filter { it !in validIds }
        keysToRemove.forEach { moduleSizeCache.remove(it) }
    }

    private val updateCache = mutableStateMapOf<String, Triple<String, String, String>>()

    fun fetchModuleList() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true

            try {
                val start = SystemClock.elapsedRealtime()

                val result = listModules()

                Log.i(TAG, "result: $result")

                val array = JSONArray(result)
                modules = (0 until array.length())
                    .asSequence()
                    .map { array.getJSONObject(it) }
                    .map { obj ->
                        ModuleInfo(
                            obj.getString("id"),

                            obj.optString("name"),
                            obj.optString("author", "Unknown"),
                            obj.optString("version", "Unknown"),
                            obj.optInt("versionCode", 0),
                            obj.optString("description"),
                            obj.getBoolean("enabled"),
                            obj.getBoolean("update"),
                            obj.getBoolean("remove"),
                            obj.optString("updateJson"),
                            obj.optBoolean("web"),
                            obj.optBoolean("action"),
                            obj.optString("webuiIcon").trim().ifEmpty { null },
                            obj.optString("actionIcon").trim().ifEmpty { null },
                            obj.optString("metamodule").let { it == "1" || it.equals("true", ignoreCase = true) },
                            zygiskModuleIds.contains(obj.getString("id")),
                            obj.optString("name").contains("LSPosed", ignoreCase = true)
                        )
                    }.toList()
                isNeedRefresh = false

                // Non-critical: run in background without blocking module display
                val ids = modules.map { it.id }
                pruneBannerCache(ids.toSet())
                pruneModuleSizeCache(ids.toSet())
                viewModelScope.launch(Dispatchers.IO) { prefetchModuleSizes(ids) }
                viewModelScope.launch(Dispatchers.IO) { batchCheckUpdates() }

                Log.i(TAG, "load cost: ${SystemClock.elapsedRealtime() - start}, modules: $modules")
            } catch (e: Exception) {
                Log.e(TAG, "fetchModuleList: ", e)
            } finally {
                isRefreshing = false
            }
        }
    }

    private fun sanitizeVersionString(version: String): String {
        return version.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
    }

    suspend fun checkUpdate(m: ModuleInfo): Triple<String, String, String> {
        updateCache[m.id]?.let { return it }
        updateSemaphore.withPermit {
            val result = checkUpdateInternal(m)
            if (result.first.isNotEmpty()) {
                updateCache[m.id] = result
            }
            return result
        }
    }

    fun getCachedUpdate(moduleId: String): Triple<String, String, String> {
        return updateCache[moduleId] ?: Triple("", "", "")
    }

    fun batchCheckUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            val eligible = modules.filter {
                it.updateJson.isNotEmpty() && !it.remove && !it.update && it.enabled
            }
            eligible.forEach { m ->
                if (updateCache.containsKey(m.id)) return@forEach
                try {
                    val result = updateSemaphore.withPermit { checkUpdateInternal(m) }
                    if (result.first.isNotEmpty()) {
                        updateCache[m.id] = result
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun checkUpdateInternal(m: ModuleInfo): Triple<String, String, String> {
        val empty = Triple("", "", "")
        if (prefs.getBoolean("disable_module_update_check", false)) {
            return empty
        }
        if (m.updateJson.isEmpty() || m.remove || m.update || !m.enabled) {
            return empty
        }
        // download updateJson
        val result = kotlin.runCatching {
            val url = m.updateJson
            Log.i(TAG, "checkUpdate url: $url")
            val response = apApp.okhttpClient
                .newCall(
                    okhttp3.Request.Builder()
                        .url(url)
                        .build()
                ).execute()
            Log.d(TAG, "checkUpdate code: ${response.code}")
            if (response.isSuccessful) {
                response.body?.string() ?: ""
            } else {
                ""
            }
        }.getOrDefault("")
        Log.i(TAG, "checkUpdate result: $result")

        if (result.isEmpty()) {
            return empty
        }

        val updateJson = kotlin.runCatching {
            JSONObject(result)
        }.getOrNull() ?: return empty

        val version = sanitizeVersionString(updateJson.optString("version", ""))
        val versionCode = updateJson.optInt("versionCode", 0)
        val zipUrl = updateJson.optString("zipUrl", "")
        val changelog = updateJson.optString("changelog", "")
        if (versionCode <= m.versionCode || zipUrl.isEmpty()) {
            return empty
        }

        return Triple(zipUrl, version, changelog)
    }

    private fun prefetchModuleSizes(moduleIds: List<String>) {
        if (moduleIds.isEmpty()) return
        val idsToFetch = moduleIds.filter { it !in moduleSizeCache }
        if (idsToFetch.isEmpty()) return

        runCatching {
            val sb = StringBuilder("for d in")
            for (id in idsToFetch) {
                sb.append(" /data/adb/modules/")
                sb.append(id.replace(" ", "\\ "))
            }
            sb.append("; do if [ -d \"\$d\" ]; then /data/adb/ap/bin/busybox du -sb \"\$d\" 2>/dev/null; fi; done")
            val result = getRootShell().newJob().add(sb.toString()).to(ArrayList(), null).exec()
            if (result.isSuccess) {
                for (line in result.out) {
                    val parts = line.split("\t", limit = 2)
                    if (parts.size == 2) {
                        val size = parts[0].trim().toLongOrNull() ?: continue
                        val path = parts[1].trim()
                        val id = path.removePrefix("/data/adb/modules/")
                        if (id in idsToFetch) {
                            moduleSizeCache[id] = size
                        }
                    }
                }
            }
        }.onFailure { e ->
            Log.e(TAG, "Error prefetching module sizes", e)
        }
    }

    fun getModuleSize(moduleId: String): String {
        moduleSizeCache[moduleId]?.let { cachedBytes ->
            return formatFileSize(cachedBytes)
        }
        return "0 KB"
    }
}

/**
 * 格式化文件大小显示
 *
 * This function is derived from SukiSU-Ultra project
 * Project: https://github.com/SukiSU-Ultra/SukiSU-Ultra
 * Original source: manager/app/src/main/java/com/sukisu/ultra/ui/viewmodel/ModuleViewModel.kt
 * Commit: 787c88ab2d070f3c6ec7ddff2f4ace1f3ebdd0c3
 * View at: https://github.com/SukiSU-Ultra/SukiSU-Ultra/blob/787c88ab2d070f3c6ec7ddff2f4ace1f3ebdd0c3/manager/app/src/main/java/com/sukisu/ultra/ui/viewmodel/ModuleViewModel.kt
 *
 * SukiSU-Ultra is a Kernel-based Android Root Solution & KPM
 */
private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)

    return DecimalFormat("#,##0.#").format(
        bytes / 1024.0.pow(digitGroups.toDouble())
    ) + " " + units[digitGroups]
}

// End of file
