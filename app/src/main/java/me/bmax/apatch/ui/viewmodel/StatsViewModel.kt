package me.bmax.apatch.ui.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.bmax.apatch.apApp
import me.bmax.apatch.util.HardwareMonitor

data class StatsMonitorState(
    val cpuTemp: Float? = null,
    val cpuFrequencies: List<HardwareMonitor.CpuFreqInfo> = emptyList(),
    val memoryInfo: HardwareMonitor.MemoryInfo? = null,
    val batteryLevel: Int? = null,
    val batteryTemp: Float? = null,
    val batteryCharging: Boolean = false,
)

data class StatsHistory(
    val cpuTemp: List<Float> = emptyList(),
    val ram: List<Float> = emptyList(),
)

data class StatsUiState(
    val monitor: StatsMonitorState = StatsMonitorState(),
    val history: StatsHistory = StatsHistory(),
)

class StatsViewModel : ViewModel() {
    companion object {
        private const val TAG = "StatsViewModel"
        private const val HISTORY_LIMIT = 30
        private const val CPU_POLL_INTERVAL_MS = 3_000L
        private const val MEMORY_POLL_INTERVAL_MS = 5_000L
    }

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var cpuJob: Job? = null
    private var memoryJob: Job? = null

    fun startPolling() {
        if (cpuJob != null) return

        cpuJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                runCatching {
                    HardwareMonitor.getCpuTemperature() to HardwareMonitor.getCpuFrequencies()
                }.onSuccess { (temperature, frequencies) ->
                    _uiState.update { current ->
                        current.copy(
                            monitor = current.monitor.copy(
                                cpuTemp = temperature.takeIf { it > 0f },
                                cpuFrequencies = frequencies,
                            ),
                            history = current.history.copy(
                                cpuTemp = current.history.cpuTemp.appendSample(temperature.takeIf { it > 0f }),
                            ),
                        )
                    }
                }.onFailure { error ->
                    Log.e(TAG, "CPU polling failed", error)
                }
                delay(CPU_POLL_INTERVAL_MS)
            }
        }

        memoryJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                runCatching {
                    val memory = HardwareMonitor.getMemoryInfo().orFallback()
                    val battery = readBattery()
                    val ramPercent = if (memory.ramTotal > 0) {
                        memory.ramUsed.toFloat() / memory.ramTotal.toFloat() * 100f
                    } else {
                        null
                    }
                    Triple(memory, battery, ramPercent)
                }.onSuccess { (memory, battery, ramPercent) ->
                    _uiState.update { current ->
                        current.copy(
                            monitor = current.monitor.copy(
                                memoryInfo = memory,
                                batteryLevel = battery.level,
                                batteryTemp = battery.temperature,
                                batteryCharging = battery.charging,
                            ),
                            history = current.history.copy(
                                ram = current.history.ram.appendSample(ramPercent),
                            ),
                        )
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Memory polling failed", error)
                }
                delay(MEMORY_POLL_INTERVAL_MS)
            }
        }
    }

    fun stopPolling() {
        cpuJob?.cancel()
        memoryJob?.cancel()
        cpuJob = null
        memoryJob = null
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }

    private fun List<Float>.appendSample(sample: Float?): List<Float> =
        if (sample == null) this else (this + sample).takeLast(HISTORY_LIMIT)

    private fun HardwareMonitor.MemoryInfo.orFallback(): HardwareMonitor.MemoryInfo {
        if (ramTotal > 0L) return this
        val manager = apApp.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
        return copy(
            ramTotal = info.totalMem,
            ramUsed = (info.totalMem - info.availMem).coerceAtLeast(0L),
        )
    }

    private fun readBattery(): BatterySnapshot {
        val intent = apApp.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        return BatterySnapshot(
            level = if (level >= 0 && scale > 0) level * 100 / scale else null,
            temperature = (temperature / 10f).takeIf { it > 0f },
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
        )
    }

    private data class BatterySnapshot(
        val level: Int?,
        val temperature: Float?,
        val charging: Boolean,
    )
}
