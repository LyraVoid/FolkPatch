package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.util.HardwareMonitor
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsMetricsTest {
    @Test
    fun cpuClustersGroupByCeilingAndSortByCoreIndex() {
        val clusters = cpuClusters(
            listOf(
                HardwareMonitor.CpuFreqInfo(coreIndex = 2, currentFreqKhz = 600_000, maxFreqKhz = 1_800_000),
                HardwareMonitor.CpuFreqInfo(coreIndex = 4, currentFreqKhz = 1_200_000, maxFreqKhz = 2_400_000),
                HardwareMonitor.CpuFreqInfo(coreIndex = 0, currentFreqKhz = 900_000, maxFreqKhz = 1_800_000),
                HardwareMonitor.CpuFreqInfo(coreIndex = 1, currentFreqKhz = 1_200_000, maxFreqKhz = 1_800_000),
            ),
        )

        assertEquals(listOf("CPU0-2", "CPU4"), clusters.map { it.label })
        assertEquals(900_000L, clusters[0].averageKHz)
        assertEquals(1_800_000L, clusters[0].maxKHz)
    }
}
