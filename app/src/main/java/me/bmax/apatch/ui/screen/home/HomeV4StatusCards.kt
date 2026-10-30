package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import me.bmax.apatch.ui.theme.tokens.FolkType

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
                .padding(vertical = FolkDimens.Size8),
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
        verticalArrangement = Arrangement.spacedBy(FolkDimens.Size8)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(FolkDimens.Size80)
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
                    wavelength = FolkDimens.Size24,
                )
            } else {
                // An empty gauge still shows a muted wavy ring, so a 0% value
                // does not collapse to a plain circle next to the others.
                CircularWavyProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = color.copy(alpha = 0.2f),
                    amplitude = { 1f },
                    wavelength = FolkDimens.Size24,
                )
            }
            Text(
                text = value,
                style = FolkType.Title,
            )
        }
        Text(
            text = label,
            style = FolkType.Caption,
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
    TonalCard(modifier = modifier, flat = true) {
        Column(
            modifier = Modifier.padding(horizontal = FolkDimens.Size20, vertical = FolkDimens.Size24),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(FolkDimens.Size32),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(FolkDimens.Size16))
                Text(
                    text = title,
                    style = FolkType.Title,
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
            
            Spacer(Modifier.height(FolkDimens.Size16))

            Column(
                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size8)
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
        shape = RoundedCornerShape(FolkDimens.Size8)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = FolkDimens.Size8, vertical = FolkDimens.Size4),
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
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        shape = FolkShape.Corner16,
        flat = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FolkDimens.Size16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            when (apState) {
                APApplication.State.ANDROIDPATCH_INSTALLED -> {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(FolkDimens.Size28),
                        tint = if (isWallpaperMode) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
                APApplication.State.ANDROIDPATCH_INSTALLING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(FolkDimens.Size28),
                        strokeWidth = FolkDimens.Size3,
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
                        modifier = Modifier.size(FolkDimens.Size28),
                        // The tertiary role turns pink under some seeds, which reads as
                        // an error here; stay on the app's action colour instead.
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Outlined.Android,
                        contentDescription = null,
                        modifier = Modifier.size(FolkDimens.Size28),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(FolkDimens.Size16))

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
