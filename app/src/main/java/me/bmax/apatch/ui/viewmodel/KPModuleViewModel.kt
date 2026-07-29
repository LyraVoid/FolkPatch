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
import kotlinx.coroutines.sync.Semaphore
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import org.json.JSONArray
import java.text.Collator
import java.util.Locale

class KPModuleViewModel : ViewModel() {
    companion object {
        private const val TAG = "KPModuleViewModel"
        private const val CUSTOM_ORDER_ENABLED_KEY = "kpm_custom_order_enabled"
        private const val CUSTOM_ORDER_KEY = "kpm_custom_order"
        private var modules by mutableStateOf<List<KPModel.KPMInfo>>(emptyList())
        val bannerSemaphore = Semaphore(4)
    }

    var isRefreshing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private val prefs = APApplication.sharedPreferences
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

    fun fetchModuleList() {
        viewModelScope.launch(Dispatchers.IO) {
            errorMessage = null
            isRefreshing = true
            val oldModuleList = modules
            val start = SystemClock.elapsedRealtime()

            kotlin.runCatching {
                // Some older kernels return an uninitialized list buffer when no KPM is loaded.
                val names = if (Natives.kernelPatchModuleNum() > 0) {
                    Natives.kernelPatchModuleList()
                } else {
                    ""
                }
                val nameList = names.split('\n').toList()
                Log.d(TAG, "kpm list: $nameList")
                modules = nameList.filter { it.isNotEmpty() }.map {
                    val infoline = Natives.kernelPatchModuleInfo(it)
                    val spi = infoline.split('\n')
                    val name = spi.find { it.startsWith("name=") }?.removePrefix("name=")
                    val version = spi.find { it.startsWith("version=") }?.removePrefix("version=")
                    val license = spi.find { it.startsWith("license=") }?.removePrefix("license=")
                    val author = spi.find { it.startsWith("author=") }?.removePrefix("author=")
                    val description =
                        spi.find { it.startsWith("description=") }?.removePrefix("description=")
                    val rawArgs = spi.find { it.startsWith("args=") }?.removePrefix("args=")?.trim()
                    val args = if (rawArgs.isNullOrEmpty() || rawArgs == "(null)") "" else rawArgs
                    val info = KPModel.KPMInfo(
                        KPModel.ExtraType.KPM,
                        name ?: "",
                        "",
                        args ?: "",
                        version ?: "",
                        license ?: "",
                        author ?: "",
                        description ?: ""
                    )
                    info
                }
                isNeedRefresh = false
            }.onFailure { e ->
                Log.e(TAG, "fetchModuleList: ", e)
                errorMessage = e.message ?: "Failed to load modules"
                isRefreshing = false
            }

            reconcileCustomOrder()

            // when both old and new is kotlin.collections.EmptyList
            // moduleList update will don't trigger
            if (oldModuleList === modules) {
                isRefreshing = false
            }

            Log.i(TAG, "load cost: ${SystemClock.elapsedRealtime() - start}, modules: $modules")
        }
    }


}
