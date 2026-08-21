package me.bmax.apatch.ui.viewmodel

import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.util.EmbeddedKpmUtils
import me.bmax.apatch.util.HanziToPinyin
import me.bmax.apatch.util.rootShellForResult
import org.ini4j.Ini
import java.io.StringReader
import org.json.JSONArray
import java.text.Collator
import java.util.Locale

private const val TAG = "KPModuleViewModel"

/** Keep filesystem paths safe: no separators, shell metacharacters or traversal. */
fun safeKpmModuleId(name: String): String = name.trim()
    .replace(Regex("[^A-Za-z0-9._-]"), "_")
    .trim('.', '_', '-')
    .take(64)
    .ifEmpty { "kpm" }

private fun parseKpmInfo(raw: String, fallbackId: String = ""): KPModel.KPMInfo? = runCatching {
    val section = Ini(StringReader(raw))["kpm"] ?: return null
    val name = section["name"]?.toString()?.trim().orEmpty().ifEmpty { fallbackId }
    KPModel.KPMInfo(
        KPModel.ExtraType.KPM, name, section["load_event"]?.toString().orEmpty(),
        section["args"]?.toString().orEmpty(), section["version"]?.toString().orEmpty(),
        section["license"]?.toString().orEmpty(), section["author"]?.toString().orEmpty(),
        section["description"]?.toString().orEmpty(), safeKpmModuleId(name),
        section["load_source"]?.toString().orEmpty()
    )
}.getOrNull()

private fun parseKernelKpmInfo(raw: String, fallbackName: String): KPModel.KPMInfo? {
    val lines = raw.split('\n')
    if (lines.none { it.startsWith("name=") }) return null
    fun value(key: String) = lines.firstOrNull { it.startsWith("$key=") }
        ?.removePrefix("$key=") ?: ""
    val name = value("name").ifBlank { fallbackName }
    return KPModel.KPMInfo(
        KPModel.ExtraType.KPM,
        name,
        value("load_event"),
        value("args"),
        value("version"),
        value("license"),
        value("author"),
        value("description"),
        safeKpmModuleId(fallbackName),
        value("load_source")
    )
}

class KPModuleViewModel : ViewModel() {
    companion object {
        private const val TAG = "KPModuleViewModel"
        private const val CUSTOM_ORDER_ENABLED_KEY = "kpm_custom_order_enabled"
        private const val CUSTOM_ORDER_KEY = "kpm_custom_order"
        private var modules by mutableStateOf<List<KPModel.KPMInfo>>(emptyList())
        val bannerSemaphore = Semaphore(4)
    }

    /** null means the current boot image could not be inspected reliably. */
    var embeddedKpmNames by mutableStateOf<Set<String>?>(null)
        private set

    var isRefreshing by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val prefs = APApplication.sharedPreferences
    private val refreshMutex = Mutex()
    private var customOrderEnabled by mutableStateOf(prefs.getBoolean(CUSTOM_ORDER_ENABLED_KEY, false))
    private var customOrder by mutableStateOf(readCustomOrder())
    private val collator = Collator.getInstance(Locale.getDefault())

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> }

    val moduleList by derivedStateOf {
        if (customOrderEnabled) {
            val positions = customOrder.withIndex().associate { it.value to it.index }
            modules.sortedBy { positions[it.name] ?: Int.MAX_VALUE }
        } else {
            val comparator = compareBy(collator, KPModel.KPMInfo::name)
            modules.sortedWith(comparator)
        }.also {
            isRefreshing = false
        }
    }

    val isCustomOrderEnabled: Boolean get() = customOrderEnabled

    var isNeedRefresh by mutableStateOf(false)
        private set

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    private suspend fun refreshEmbeddedKpmNames(force: Boolean) {
        if (!force && embeddedKpmNames != null) return
        if (force) embeddedKpmNames = null

        val names = EmbeddedKpmUtils.getEmbeddedKpmNames()
        embeddedKpmNames = names
        if (names != null) {
            Log.i(TAG, "embedded kpm names: $names")
        } else {
            Log.w(TAG, "failed to resolve embedded kpm names")
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun setCustomModuleOrder(names: List<String>) {
        val validNames = modules.mapTo(hashSetOf()) { it.name }
        customOrder = names.distinct().filter { it in validNames } +
                modules.map { it.name }.filter { it !in names }
        customOrderEnabled = true
        persistCustomOrder()
    }

    fun toggleCustomOrder() {
        if (customOrderEnabled) {
            resetCustomModuleOrder()
        } else {
            customOrder = modules.map { it.name }
            customOrderEnabled = true
            persistCustomOrder()
        }
    }

    fun resetCustomModuleOrder() {
        customOrderEnabled = false
        customOrder = emptyList()
        prefs.edit()
            .putBoolean(CUSTOM_ORDER_ENABLED_KEY, false)
            .remove(CUSTOM_ORDER_KEY)
            .apply()
    }

    private fun readCustomOrder(): List<String> = runCatching {
        val array = JSONArray(prefs.getString(CUSTOM_ORDER_KEY, "[]"))
        (0 until array.length()).map { array.getString(it) }.distinct()
    }.getOrDefault(emptyList())

    private fun persistCustomOrder() {
        prefs.edit()
            .putBoolean(CUSTOM_ORDER_ENABLED_KEY, customOrderEnabled)
            .putString(CUSTOM_ORDER_KEY, JSONArray(customOrder).toString())
            .apply()
    }

    private fun reconcileCustomOrder() {
        if (!customOrderEnabled) return
        val moduleNames = modules.map { it.name }
        val validNames = moduleNames.toHashSet()
        val reconciled = customOrder.filter { it in validNames } + moduleNames.filter { it !in customOrder }
        if (reconciled != customOrder) {
            customOrder = reconciled
            persistCustomOrder()
        }
    }

    fun updateModuleDisabled(moduleId: String, disabled: Boolean) {
        modules = modules.map { module ->
            if (module.moduleId == moduleId) module.copy(disabled = disabled) else module
        }
    }
    fun fetchModuleList(forceEmbeddedRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            refreshMutex.withLock {
                errorMessage = null
                isRefreshing = true
                val oldModuleList = modules
                val start = SystemClock.elapsedRealtime()

                refreshEmbeddedKpmNames(forceEmbeddedRefresh)

                kotlin.runCatching {
                    val result = linkedMapOf<String, KPModel.KPMInfo>()
                val names = if (Natives.kernelPatchModuleNum() > 0) {
                    Natives.kernelPatchModuleList()
                } else {
                    ""
                }
                val loadedIds = names.split('\n')
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .toSet()
                names.split('\n').filter(String::isNotBlank).forEach { kernelName ->
                    val lines = Natives.kernelPatchModuleInfo(kernelName).split('\n')
                    val info = KPModel.KPMInfo(
                        KPModel.ExtraType.KPM,
                        lines.firstOrNull { it.startsWith("name=") }?.removePrefix("name=") ?: kernelName,
                        lines.firstOrNull { it.startsWith("load_event=") }?.removePrefix("load_event=") ?: "",
                        lines.firstOrNull { it.startsWith("args=") }?.removePrefix("args=") ?: "",
                        lines.firstOrNull { it.startsWith("version=") }?.removePrefix("version=") ?: "",
                        lines.firstOrNull { it.startsWith("license=") }?.removePrefix("license=") ?: "",
                        lines.firstOrNull { it.startsWith("author=") }?.removePrefix("author=") ?: "",
                        lines.firstOrNull { it.startsWith("description=") }?.removePrefix("description=") ?: "",
                        safeKpmModuleId(kernelName),
                        lines.firstOrNull { it.startsWith("load_source=") }?.removePrefix("load_source=") ?: ""
                    )
                    // load_source=file only describes where this instance was loaded from.
                    // It does not mean the KPM belongs to APatch's persistent install store.
                    // The installed flag is set only when the directory scan below finds
                    // /data/adb/ap/kpm/<id>/<id>.kpm.
                    result[info.moduleId] = info.copy(installed = false, disabled = false)
                }
                val dirs = rootShellForResult("find ${APApplication.KPMS_DIR} -mindepth 1 -maxdepth 1 -type d -print").out
                dirs.map { it.trim().substringAfterLast('/') }.filter(String::isNotBlank).forEach { id ->
                    val file = "${APApplication.KPMS_DIR}$id/$id.kpm"
                    val parsed = rootShellForResult("${APApplication.APATCH_FOLDER}bin/kptools -l -M '$file'")
                        .out.joinToString("\n").let { parseKpmInfo(it, id) } ?: return@forEach
                    val key = safeKpmModuleId(id)
                    val old = result[key]
                    // Refresh the same live metadata exposed by `truncate su module info <name>`.
                    val live = if (id in loadedIds) {
                        parseKernelKpmInfo(Natives.kernelPatchModuleInfo(id), id)
                    } else {
                        null
                    }
                    val current = live ?: old
                    val disabled = rootShellForResult("[ -e '${APApplication.KPMS_DIR}$id/disable' ]").isSuccess
                    result[key] = current?.copy(
                        moduleId = id, installed = true, disabled = disabled,
                        version = current.version.ifBlank { parsed.version },
                        license = current.license.ifBlank { parsed.license },
                        author = current.author.ifBlank { parsed.author },
                        description = current.description.ifBlank { parsed.description }
                    ) ?: parsed.copy(moduleId = id, installed = true, disabled = disabled, loadSource = "")
                }
                modules = result.values.toList()
                    isNeedRefresh = false
                }.onFailure { e ->
                    Log.e(TAG, "fetchModuleList: ", e)
                    errorMessage = e.message ?: "Failed to load modules"
                }

                reconcileCustomOrder()

                // when both old and new is kotlin.collections.EmptyList
                // moduleList update will don't trigger
                isRefreshing = false

                Log.i(TAG, "load cost: ${SystemClock.elapsedRealtime() - start}, modules: $modules, unchanged=${oldModuleList === modules}")
            }
        }
    }
}
