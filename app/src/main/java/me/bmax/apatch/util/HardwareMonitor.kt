package me.bmax.apatch.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hardware Monitor for CPU, GPU, Memory, and Swap/ZRAM.
 * Implements stateful calculations for differential metrics (like Adreno GPU load).
 */
object HardwareMonitor {
    private const val TAG = "HardwareMonitor"

    // CPU State
    private var prevTotalCpu: Long = 0
    private var prevIdleCpu: Long = 0

    data class MemoryInfo(
        val ramTotal: Long,
        val ramUsed: Long,
        val swapTotal: Long,
        val swapUsed: Long,
        val zramTotal: Long,
        val zramUsed: Long
    )

    data class CpuFreqInfo(
        val coreIndex: Int,
        val currentFreqKhz: Long,
        val maxFreqKhz: Long
    )

    /**
     * Get CPU Usage percentage (0-100) using differential /proc/stat.
     */
    suspend fun getCpuUsage(): Int {
        return withContext(Dispatchers.IO) {
            try {
                val result = rootShellForResult("cat /proc/stat")
                if (result.isSuccess && result.out.isNotEmpty()) {
                    val line = result.out.firstOrNull { it.startsWith("cpu ") } ?: return@withContext 0
                    val parts = line.split(Regex("\\s+"))
                    if (parts.size >= 8) {
                        // user, nice, system, idle, iowait, irq, softirq
                        val user = parts[1].toLong()
                        val nice = parts[2].toLong()
                        val system = parts[3].toLong()
                        val idle = parts[4].toLong()
                        val iowait = parts[5].toLong()
                        val irq = parts[6].toLong()
                        val softirq = parts[7].toLong()

                        val currentTotal = user + nice + system + idle + iowait + irq + softirq
                        val currentIdle = idle + iowait

                        if (prevTotalCpu == 0L || currentTotal < prevTotalCpu) {
                            prevTotalCpu = currentTotal
                            prevIdleCpu = currentIdle
                            return@withContext 0
                        }

                        val deltaTotal = currentTotal - prevTotalCpu
                        val deltaIdle = currentIdle - prevIdleCpu

                        prevTotalCpu = currentTotal
                        prevIdleCpu = currentIdle

                        if (deltaTotal > 0) {
                            val usage = (deltaTotal - deltaIdle) * 100 / deltaTotal
                            return@withContext usage.toInt().coerceIn(0, 100)
                        }
                    }
                }
                0
            } catch (e: Exception) {
                Log.e(TAG, "Error reading CPU usage", e)
                0
            }
        }
    }

    /**
     * Get Memory and Swap info using `free` command and `/proc/swaps`.
     */
    suspend fun getMemoryInfo(): MemoryInfo {
        return withContext(Dispatchers.IO) {
            var ramTotal = 0L
            var ramUsed = 0L
            var swapTotal = 0L
            var swapUsed = 0L
            var zramTotal = 0L
            var zramUsed = 0L

            try {
                // Use /proc/meminfo for more reliable parsing
                val memInfoResult = rootShellForResult("cat /proc/meminfo")
                if (memInfoResult.isSuccess) {
                    var memTotal = 0L
                    var memFree = 0L
                    var memAvailable = 0L
                    var buffers = 0L
                    var cached = 0L
                    
                    memInfoResult.out.forEach { line ->
                        val parts = line.split(Regex(":\\s+"))
                        if (parts.size >= 2) {
                            val key = parts[0].trim()
                            val valueStr = parts[1].trim().split(Regex("\\s+"))[0]
                            val valueKb = valueStr.toLongOrNull() ?: 0L
                            val valueBytes = valueKb * 1024
                            
                            when (key) {
                                "MemTotal" -> memTotal = valueBytes
                                "MemFree" -> memFree = valueBytes
                                "MemAvailable" -> memAvailable = valueBytes
                                "Buffers" -> buffers = valueBytes
                                "Cached" -> cached = valueBytes
                            }
                        }
                    }
                    
                    ramTotal = memTotal
                    
                    // Calculate Available if MemAvailable is missing (older kernels)
                    if (memAvailable == 0L && memFree > 0) {
                         // Rough estimation: Free + Cached + Buffers
                         // Note: This is an over-estimation as not all cached is reclaimable,
                         // but better than nothing for old kernels.
                         memAvailable = memFree + cached + buffers
                    }
                    
                    // Calculate Used
                    // Used = Total - Available
                    if (ramTotal > 0) {
                        ramUsed = (ramTotal - memAvailable).coerceAtLeast(0)
                    }
                }
                
                // Parse /proc/swaps to distinguish ZRAM vs Swap File
                val swapsResult = rootShellForResult("cat /proc/swaps")
                if (swapsResult.isSuccess) {
                    swapsResult.out.forEach { line ->
                        if (line.trim().startsWith("Filename")) return@forEach
                        val parts = line.split(Regex("\\s+"))
                        if (parts.size >= 4) {
                            val filename = parts[0]
                            val sizeKb = parts[2].toLongOrNull() ?: 0L
                            val usedKb = parts[3].toLongOrNull() ?: 0L
                            
                            val sizeBytes = sizeKb * 1024
                            val usedBytes = usedKb * 1024
                            
                            if (filename.contains("zram")) {
                                zramTotal += sizeBytes
                                zramUsed += usedBytes
                            } else {
                                // Assume everything else is "Swap File" (e.g., /data/swapfile)
                                swapTotal += sizeBytes
                                swapUsed += usedBytes
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading Memory info", e)
            }

            MemoryInfo(ramTotal, ramUsed, swapTotal, swapUsed, zramTotal, zramUsed)
        }
    }

    suspend fun getCpuTemperature(): Float {
        return withContext(Dispatchers.IO) {
            try {
                val result = rootShellForResult(
                    "grep -rl 'cpu' /sys/class/thermal/thermal_zone*/type 2>/dev/null | " +
                    "sed 's|/type$|/temp|' | xargs cat 2>/dev/null | head -1"
                )
                if (result.isSuccess && result.out.isNotEmpty()) {
                    val temp = result.out[0].trim().toFloatOrNull()
                    if (temp != null && temp > 0) {
                        return@withContext if (temp > 1000) temp / 1000f else temp
                    }
                }

                val allZones = rootShellForResult("ls /sys/class/thermal/ 2>/dev/null")
                if (allZones.isSuccess) {
                    for (zone in allZones.out) {
                        val zoneName = zone.trim()
                        if (!zoneName.startsWith("thermal_zone")) continue
                        val typeResult = rootShellForResult("cat /sys/class/thermal/$zoneName/type 2>/dev/null")
                        if (!typeResult.isSuccess || typeResult.out.isEmpty()) continue
                        val type = typeResult.out[0].trim().lowercase()
                        if (!type.contains("cpu")) continue

                        val tempResult = rootShellForResult("cat /sys/class/thermal/$zoneName/temp 2>/dev/null")
                        if (tempResult.isSuccess && tempResult.out.isNotEmpty()) {
                            val temp = tempResult.out[0].trim().toFloatOrNull()
                            if (temp != null && temp > 0) {
                                return@withContext if (temp > 1000) temp / 1000f else temp
                            }
                        }
                    }
                }

                0f
            } catch (e: Exception) {
                Log.e(TAG, "Error reading CPU temperature", e)
                0f
            }
        }
    }

    suspend fun getCpuFrequencies(): List<CpuFreqInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val result = rootShellForResult("ls /sys/devices/system/cpu/ 2>/dev/null | grep -E '^cpu[0-9]+$'")
                if (!result.isSuccess) return@withContext emptyList()

                val infos = mutableListOf<CpuFreqInfo>()
                for (line in result.out) {
                    val coreName = line.trim()
                    val match = Regex("cpu(\\d+)").find(coreName) ?: continue
                    val coreIndex = match.groupValues[1].toIntOrNull() ?: continue

                    val onlineResult = rootShellForResult("cat /sys/devices/system/cpu/$coreName/online 2>/dev/null")
                    if (onlineResult.isSuccess && onlineResult.out.isNotEmpty()) {
                        if (onlineResult.out[0].trim() == "0") continue
                    }

                    val curFreqResult = rootShellForResult("cat /sys/devices/system/cpu/$coreName/cpufreq/scaling_cur_freq 2>/dev/null")
                    val maxFreqResult = rootShellForResult("cat /sys/devices/system/cpu/$coreName/cpufreq/cpuinfo_max_freq 2>/dev/null")

                    val curFreq = if (curFreqResult.isSuccess && curFreqResult.out.isNotEmpty())
                        curFreqResult.out[0].trim().toLongOrNull() ?: 0L else 0L
                    val maxFreq = if (maxFreqResult.isSuccess && maxFreqResult.out.isNotEmpty())
                        maxFreqResult.out[0].trim().toLongOrNull() ?: 0L else 0L

                    infos.add(CpuFreqInfo(coreIndex, curFreq, maxFreq))
                }

                infos
            } catch (e: Exception) {
                Log.e(TAG, "Error reading CPU frequencies", e)
                emptyList()
            }
        }
    }

}
