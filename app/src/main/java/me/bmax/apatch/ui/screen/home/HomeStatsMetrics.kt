package me.bmax.apatch.ui.screen.home

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import java.util.Locale
import kotlin.math.roundToInt
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.chart.ModulePieChart
import me.bmax.apatch.ui.component.chart.PieSlice
import me.bmax.apatch.ui.component.chart.WaveChart
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.theme.tokens.FolkType
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.ui.viewmodel.StatsHistory
import me.bmax.apatch.ui.viewmodel.StatsMonitorState
import me.bmax.apatch.util.HardwareMonitor

@Composable
internal fun StatsMonitoringSection(
    monitor: StatsMonitorState,
    history: StatsHistory,
) {
    val colors = MaterialTheme.colorScheme
    val memory = monitor.memoryInfo
    val ramFraction = memory?.ramFraction
    val clusters = remember(monitor.cpuFrequencies) { cpuClusters(monitor.cpuFrequencies) }

    StatsCard(
        title = stringResource(R.string.home_device_status_title),
        icon = Icons.Outlined.Speed,
    ) {
        if (monitor.cpuTemp != null || history.cpuTemp.isNotEmpty()) {
            WaveChart(
                label = stringResource(R.string.home_device_status_cpu_temp),
                value = monitor.cpuTemp?.let { "${it.roundToInt()}°C" } ?: "—",
                samples = history.cpuTemp,
                color = colors.secondary,
            )
        }
        WaveChart(
            label = stringResource(R.string.home_storage_ram),
            value = ramFraction?.let { "${(it * 100f).roundToInt()}%" } ?: "—",
            samples = history.ram,
            color = colors.primary,
        )

        if (clusters.isNotEmpty()) {
            Text(
                text = stringResource(R.string.home_device_status_cpu_freq),
                style = FolkType.Caption,
                color = supportingColor(),
            )
            clusters.forEach { cluster ->
                StatsMetricBar(
                    label = cluster.label,
                    value = formatFrequency(cluster.averageKHz),
                    progress = cluster.usedFraction,
                    color = colors.primary,
                )
            }
        }

        StatsMetricBar(
            label = stringResource(R.string.home_device_status_battery_level),
            value = batteryValue(monitor),
            progress = (monitor.batteryLevel ?: 0) / 100f,
            color = when {
                monitor.batteryLevel == null -> colors.primary
                monitor.batteryLevel <= 20 -> colors.error
                monitor.batteryLevel <= 50 -> colors.tertiary
                else -> colors.primary
            },
        )

        if ((memory?.zramTotal ?: 0L) > 0L) {
            StorageBar(
                label = stringResource(R.string.home_storage_zram),
                usedBytes = memory?.zramUsed ?: 0L,
                totalBytes = memory?.zramTotal ?: 0L,
                color = colors.tertiary,
            )
        }
        if ((memory?.swapTotal ?: 0L) > 0L) {
            StorageBar(
                label = stringResource(R.string.home_storage_swap),
                usedBytes = memory?.swapUsed ?: 0L,
                totalBytes = memory?.swapTotal ?: 0L,
                color = colors.error,
            )
        }
    }
}

@Composable
internal fun StatsModuleSection(
    superuserCount: Int,
    apmModuleCount: Int,
    kernelModuleCount: Int,
) {
    val colors = MaterialTheme.colorScheme
    val kernelCount = kernelModuleCount.coerceAtLeast(0)
    val apmCount = apmModuleCount.coerceAtLeast(0)
    val superuserTotal = superuserCount.coerceAtLeast(0)
    val slices = remember(superuserTotal, apmCount, kernelCount, colors) {
        listOf(
            PieSlice(kernelCount, colors.primary),
            PieSlice(apmCount, colors.secondary),
            PieSlice(superuserTotal, colors.tertiary),
        )
    }
    val total = superuserTotal + apmCount + kernelCount

    StatsCard(
        title = stringResource(R.string.settings_home_layout_stats),
        icon = Icons.Outlined.PieChart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = FolkDimens.Size8),
            horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size20),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModulePieChart(
                slices = slices,
                centerLabel = total.toString(),
                modifier = Modifier.size(FolkDimens.Size140),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
            ) {
                ChartLegend(stringResource(R.string.home_info_kernel), kernelCount, colors.primary)
                ChartLegend(stringResource(R.string.module), apmCount, colors.secondary)
                ChartLegend(stringResource(R.string.superuser), superuserTotal, colors.tertiary)
            }
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    TonalCard(flat = true) {
        Column(
            modifier = Modifier.padding(horizontal = FolkDimens.Size20, vertical = FolkDimens.Size24),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(FolkDimens.Size32),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(FolkDimens.Size16))
                Text(text = title, style = FolkType.Title, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(FolkDimens.Size16))
            Column(verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16), content = content)
        }
    }
}

@Composable
private fun StatsMetricBar(
    label: String,
    value: String,
    progress: Float,
    color: Color,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = FolkType.Summary)
            Text(text = value, style = FolkType.Summary, color = supportingColor())
        }
        Spacer(Modifier.height(FolkDimens.Size8))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(FolkDimens.Size8).clip(FolkShape.CornerFull),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

@Composable
private fun StorageBar(
    label: String,
    usedBytes: Long,
    totalBytes: Long,
    color: Color,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val value = remember(context, configuration, usedBytes, totalBytes) {
        "${Formatter.formatFileSize(context, usedBytes)} / ${Formatter.formatFileSize(context, totalBytes)}"
    }
    StatsMetricBar(
        label = label,
        value = value,
        progress = if (totalBytes > 0L) usedBytes.toFloat() / totalBytes else 0f,
        color = color,
    )
}

@Composable
private fun ChartLegend(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(FolkDimens.Size12).clip(CircleShape).background(color))
        Spacer(Modifier.width(FolkDimens.Size12))
        Text(
            text = label,
            style = FolkType.Summary,
            color = supportingColor(),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = count.toString(),
            style = FolkType.Summary.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun supportingColor(): Color =
    LocalFolkWallpaperTokens.current?.group?.supportingColor
        ?: MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun batteryValue(monitor: StatsMonitorState): String {
    val level = monitor.batteryLevel?.let { "$it%" } ?: "—"
    val charging = if (monitor.batteryCharging) {
        " · ${stringResource(R.string.home_device_status_battery_charging)}"
    } else {
        ""
    }
    val temperature = monitor.batteryTemp?.let { " · ${it.roundToInt()}°C" }.orEmpty()
    return level + charging + temperature
}

private val HardwareMonitor.MemoryInfo.ramFraction: Float
    get() = if (ramTotal > 0L) ramUsed.toFloat() / ramTotal else 0f

internal data class CpuCluster(
    val label: String,
    val averageKHz: Long,
    val maxKHz: Long,
) {
    val usedFraction: Float
        get() = if (maxKHz > 0L) averageKHz.toFloat() / maxKHz else 0f
}

internal fun cpuClusters(frequencies: List<HardwareMonitor.CpuFreqInfo>): List<CpuCluster> =
    frequencies
        .filter { it.currentFreqKhz > 100_000L && it.maxFreqKhz > 100_000L }
        .groupBy { it.maxFreqKhz }
        .values
        .map { cores ->
            val ordered = cores.sortedBy { it.coreIndex }
            val first = ordered.first()
            val last = ordered.last()
            CpuCluster(
                label = if (first == last) "CPU${first.coreIndex}" else "CPU${first.coreIndex}-${last.coreIndex}",
                averageKHz = ordered.sumOf { it.currentFreqKhz } / ordered.size,
                maxKHz = first.maxFreqKhz,
            )
        }
        .sortedBy { it.label }

private fun formatFrequency(khz: Long): String = when {
    khz >= 1_000_000L -> String.format(Locale.US, "%.2f GHz", khz / 1_000_000.0)
    khz >= 1_000L -> String.format(Locale.US, "%.0f MHz", khz / 1_000.0)
    else -> String.format(Locale.US, "%d kHz", khz)
}
