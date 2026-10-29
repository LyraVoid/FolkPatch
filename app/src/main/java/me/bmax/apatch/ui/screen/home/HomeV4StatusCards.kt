package me.bmax.apatch.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.SystemInfoCollector
import me.bmax.apatch.core.ui.R as CoreR
import me.bmax.apatch.ui.theme.tokens.FolkShape

@Composable
fun HomeV4DeviceStatusCard(isWallpaperMode: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var deviceStatus by remember { mutableStateOf(SystemInfoCollector.DeviceStatus()) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            deviceStatus = SystemInfoCollector.collectDeviceStatus(context)
            delay(10000)
        }
    }

    HomeV4MagiskStyleCard(
        title = stringResource(R.string.home_device_status_title),
        icon = Icons.Outlined.Settings,
        actionText = "",
        showAction = false,
        isWallpaperMode = isWallpaperMode,
        onActionClick = {},
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HomeV4StatusCircle(
                value = "${deviceStatus.batteryTemp}°C",
                label = stringResource(R.string.home_device_status_battery_temp),
                progress = (deviceStatus.batteryTemp / 50f).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.primary
            )
            HomeV4StatusCircle(
                value = "${deviceStatus.cpuUsage}%",
                label = stringResource(R.string.home_device_status_cpu_load),
                progress = (deviceStatus.cpuUsage / 100f).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.secondary
            )
            HomeV4StatusCircle(
                value = "${deviceStatus.batteryLevel}%",
                label = stringResource(R.string.home_device_status_battery_level),
                progress = (deviceStatus.batteryLevel / 100f).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeV4StatusCircle(
    value: String,
    label: String,
    progress: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            if (progress > 0f) {
                CircularWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f),
                    // Keep the standard wave height; a longer wavelength gives
                    // fewer, broader ripples, which reads calmer.
                    amplitude = { 1f },
                    wavelength = 24.dp,
                )
            } else {
                // An empty gauge still shows a muted wavy ring, so a 0% value
                // does not collapse to a plain circle next to the others.
                CircularWavyProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = color.copy(alpha = 0.2f),
                    amplitude = { 1f },
                    wavelength = 24.dp,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeV4MagiskStyleCard(
    title: String,
    icon: ImageVector,
    actionText: String,
    showAction: Boolean,
    actionEnabled: Boolean = true,
    isWallpaperMode: Boolean,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    TonalCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    modifier = Modifier.weight(1f)
                )
                
                if (showAction) {
                    Button(
                        onClick = onActionClick,
                        enabled = actionEnabled,
                        contentPadding = ButtonDefaults.MediumContentPadding
                    ) {
                        Text(text = actionText)
                    }
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * 模式标签芯片
 */
@Composable
fun ModeLabelChip(label: String, contentColor: Color) {
    Surface(
        color = contentColor.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

/**
 * 版本信息列
 */
@Composable
fun VersionInfoColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = LocalContentColor.current,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLargeEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Android补丁状态卡片
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AndroidPatchCard(
    apState: APApplication.State,
    kpState: APApplication.State,
    showInstallDialog: MutableState<Boolean>,
    isWallpaperMode: Boolean
) {
    val containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
        // 中性 surface，避免强调容器降 alpha 后 on*Container 角色错配。
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = FolkShape.Corner16,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = if (isWallpaperMode) MaterialTheme.colorScheme.onSurface else LocalContentColor.current,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            when (apState) {
                APApplication.State.ANDROIDPATCH_INSTALLED -> {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (isWallpaperMode) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
                APApplication.State.ANDROIDPATCH_INSTALLING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = if (isWallpaperMode) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
                APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        // The tertiary role turns pink under some seeds, which reads as
                        // an error here; stay on the app's action colour instead.
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Outlined.Android,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // 状态文字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.android_patch),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
            }

            // 操作按钮
            FilledTonalButton(
                onClick = {
                    when (apState) {
                        APApplication.State.ANDROIDPATCH_NOT_INSTALLED,
                        APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                            APApplication.installApatch()
                        }
                        APApplication.State.ANDROIDPATCH_INSTALLED -> {
                            APApplication.uninstallApatch()
                        }
                        else -> {}
                    }
                },
                enabled = apState != APApplication.State.ANDROIDPATCH_INSTALLING &&
                    apState != APApplication.State.ANDROIDPATCH_UNINSTALLING &&
                    apState != APApplication.State.UNKNOWN_STATE,
                contentPadding = ButtonDefaults.MediumContentPadding
            ) {
                when (apState) {
                    APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> 
                        Text(stringResource(R.string.home_ap_cando_install))
                    APApplication.State.ANDROIDPATCH_NEED_UPDATE -> 
                        Text(stringResource(R.string.home_kp_cando_update))
                    APApplication.State.ANDROIDPATCH_INSTALLING,
                    APApplication.State.ANDROIDPATCH_UNINSTALLING -> 
                        Icon(Icons.Outlined.Cached, contentDescription = stringResource(CoreR.string.core_state_busy))
                    else -> 
                        Text(stringResource(R.string.home_ap_cando_uninstall))
                }
            }
        }
    }
}

