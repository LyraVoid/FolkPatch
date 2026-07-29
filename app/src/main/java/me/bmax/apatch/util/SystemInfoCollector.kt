package me.bmax.apatch.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unified system info collector for Home screens (V3/V4).
 * Encapsulates battery, CPU, and storage data collection logic.
 */
object SystemInfoCollector {

    data class DeviceStatus(
        val batteryTemp: Float = 0f,
        val batteryLevel: Int = 0,
        val cpuUsage: Int = 0
    )

    data class StorageStatus(
        val storageUsed: Long = 0L,
        val storageTotal: Long = 0L,
        val ramUsed: Long = 0L,
        val ramTotal: Long = 0L,
        val zramUsed: Long = 0L,
        val zramTotal: Long = 0L,
        val swapUsed: Long = 0L,
        val swapTotal: Long = 0L
    )

    /**
     * Collect battery temperature, battery level, and CPU usage.
     */
    suspend fun collectDeviceStatus(context: Context): DeviceStatus {
        return withContext(Dispatchers.IO) {
            val intent: Intent? = context.registerReceiver(
                null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val batteryTemp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryLevel = if (level != -1 && scale != -1) {
                (level * 100 / scale.toFloat()).toInt()
            } else 0

            val cpuUsage = HardwareMonitor.getCpuUsage()

            DeviceStatus(
                batteryTemp = batteryTemp,
                batteryLevel = batteryLevel,
                cpuUsage = cpuUsage
            )
        }
    }

    /**
     * Collect internal storage, RAM, ZRAM, and Swap usage.
     */
    suspend fun collectStorageStatus(): StorageStatus {
        return withContext(Dispatchers.IO) {
            // Internal Storage
            val dataDir = Environment.getDataDirectory()
            val stat = StatFs(dataDir.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            val storageTotal = totalBlocks * blockSize
            val storageUsed = storageTotal - (availableBlocks * blockSize)

            // Memory Info
            val memInfo = HardwareMonitor.getMemoryInfo()

            StorageStatus(
                storageUsed = storageUsed,
                storageTotal = storageTotal,
                ramUsed = memInfo.ramUsed,
                ramTotal = memInfo.ramTotal,
                zramUsed = memInfo.zramUsed,
                zramTotal = memInfo.zramTotal,
                swapUsed = memInfo.swapUsed,
                swapTotal = memInfo.swapTotal
            )
        }
    }
}
