package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkColors
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.copyableInfo
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.SystemInfoCollector
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.getSELinuxStatus

private val managerVersion = getManagerVersion()

@Composable
fun KernelPatchCard(
    kpState: APApplication.State,
    navigator: DestinationsNavigator,
    isWallpaperMode: Boolean,
    zygiskImplement: String,
    mountImplement: String,
    modifier: Modifier = Modifier
) {
    val jailbreakState = LocalHomeJailbreakState.current
    val isJailbreak = jailbreakState.isActive
    val isPermissive = jailbreakState.isPermissive
    MagiskStyleCard(
        title = if (isJailbreak) stringResource(R.string.settings_jailbreak_mode) else "KernelPatch",
        icon = if (isJailbreak) Icons.Outlined.LockOpen else Icons.Outlined.Extension,
        actionText = when {
            isJailbreak -> stringResource(R.string.reboot_soft)
            kpState == APApplication.State.KERNELPATCH_NEED_UPDATE -> stringResource(R.string.home_kp_cando_update)
            kpState == APApplication.State.UNKNOWN_STATE && isPermissive -> stringResource(R.string.jailbreak)
            else -> stringResource(R.string.kpm_install)
        },
        showAction = isJailbreak || kpState != APApplication.State.KERNELPATCH_INSTALLED,
        actionEnabled = !jailbreakState.isTriggering,
        isWallpaperMode = isWallpaperMode,
        onActionClick = {
            if (isJailbreak || kpState == APApplication.State.UNKNOWN_STATE && isPermissive) {
                jailbreakState.performPrimaryAction()
            } else {
                navigator.navigate(InstallModeSelectScreenDestination)
            }
        },
        modifier = modifier,
        cardId = BackgroundConfig.FOCUS_CARD_KERNEL
    ) {
        if (isJailbreak) {
            InfoRow(
                label = stringResource(R.string.settings_jailbreak_mode),
                value = stringResource(R.string.settings_jailbreak_mode_summary)
            )
        }
        InfoRow(
            label = stringResource(R.string.home_kpatch_version),
            value = if (kpState != APApplication.State.UNKNOWN_STATE) Version.installedKPVString() else stringResource(R.string.home_not_installed)
        )
        if (kpState != APApplication.State.UNKNOWN_STATE && zygiskImplement != "None") {
            InfoRow(
                label = stringResource(R.string.home_zygisk_implement),
                value = zygiskImplement
            )
        }
        if (kpState != APApplication.State.UNKNOWN_STATE && mountImplement != "None") {
            InfoRow(
                label = stringResource(R.string.home_mount_implement),
                value = mountImplement
            )
        }
        InfoRow(
            label = stringResource(R.string.home_info_kernel),
            value = System.getProperty("os.version") ?: stringResource(R.string.home_selinux_status_unknown)
        )
        if (kpState != APApplication.State.UNKNOWN_STATE) {
            InfoRow(
                label = stringResource(R.string.home_info_superkey),
                value = if (APApplication.superKey.isNotEmpty()) stringResource(R.string.home_info_auth_auth) else stringResource(R.string.home_info_auth_na)
            )
        }
    }
}

@Composable
fun AppCard(
    apState: APApplication.State,
    kpState: APApplication.State,
    deviceSlot: String,
    showUninstallDialog: MutableState<Boolean>,
    isWallpaperMode: Boolean,
    modifier: Modifier = Modifier
) {
    MagiskStyleCard(
        title = stringResource(R.string.app_name),
        icon = Icons.Outlined.Android,
        actionText = if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) stringResource(R.string.home_ap_cando_uninstall) else stringResource(R.string.kpm_install),
        showAction = true,
        actionEnabled = kpState == APApplication.State.KERNELPATCH_INSTALLED || kpState == APApplication.State.KERNELPATCH_NEED_UPDATE || apState == APApplication.State.ANDROIDPATCH_INSTALLED,
        isWallpaperMode = isWallpaperMode,
        onActionClick = {
            if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) {
                showUninstallDialog.value = true
            } else if (kpState == APApplication.State.KERNELPATCH_INSTALLED || kpState == APApplication.State.KERNELPATCH_NEED_UPDATE) {
                APApplication.installApatch()
            }
        },
        modifier = modifier,
        cardId = BackgroundConfig.FOCUS_CARD_APP
    ) {
        InfoRow(
            label = stringResource(R.string.home_apatch_version),
            value = "${managerVersion.second} (${managerVersion.first})"
        )
        InfoRow(
            label = stringResource(R.string.home_info_device_slot),
            value = deviceSlot
        )
        InfoRow(
            label = stringResource(R.string.home_info_device_model),
            value = Build.MODEL
        )
        InfoRow(
            label = stringResource(R.string.home_info_running_mode),
            value = if (apState == APApplication.State.ANDROIDPATCH_INSTALLED)
                (BackgroundConfig.getCustomBadgeText() ?: stringResource(R.string.home_info_mode_full))
            else if (kpState == APApplication.State.KERNELPATCH_INSTALLED || kpState == APApplication.State.KERNELPATCH_NEED_UPDATE)
                (BackgroundConfig.getCustomBadgeText() ?: stringResource(R.string.home_info_mode_half))
            else
                stringResource(R.string.home_info_auth_na)
        )
        InfoRow(
            label = stringResource(R.string.home_selinux_status),
            value = getSELinuxStatus()
        )
        InfoRow(
            label = stringResource(R.string.home_su_path),
            value = if (kpState != APApplication.State.UNKNOWN_STATE) Natives.suPath() else stringResource(R.string.home_info_auth_na)
        )
    }
}

@Composable
fun DeviceStatusCard(isWallpaperMode: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var deviceStatus by remember { mutableStateOf(SystemInfoCollector.DeviceStatus()) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            deviceStatus = SystemInfoCollector.collectDeviceStatus(context)
            kotlinx.coroutines.delay(10000)
        }
    }

    MagiskStyleCard(
        title = stringResource(R.string.home_device_status_title),
        icon = Icons.Outlined.Settings,
        actionText = "",
        showAction = false,
        isWallpaperMode = isWallpaperMode,
        onActionClick = {},
        modifier = modifier,
        cardId = BackgroundConfig.FOCUS_CARD_DEVICE
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = FolkDimens.Size8),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusCircle(
                value = "${deviceStatus.batteryTemp}°C",
                label = stringResource(R.string.home_device_status_battery_temp),
                progress = (deviceStatus.batteryTemp / 50f).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.primary
            )
            StatusCircle(
                value = "${deviceStatus.cpuUsage}%",
                label = stringResource(R.string.home_device_status_cpu_load),
                progress = (deviceStatus.cpuUsage / 100f).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.secondary
            )
            StatusCircle(
                value = "${deviceStatus.batteryLevel}%",
                label = stringResource(R.string.home_device_status_battery_level),
                progress = (deviceStatus.batteryLevel / 100f).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun StorageCard(isWallpaperMode: Boolean, modifier: Modifier = Modifier) {
    var storageStatus by remember { mutableStateOf(SystemInfoCollector.StorageStatus()) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            storageStatus = SystemInfoCollector.collectStorageStatus()
            kotlinx.coroutines.delay(5000)
        }
    }
    
    MagiskStyleCard(
        title = stringResource(R.string.home_storage_title),
        icon = Icons.Outlined.SdStorage,
        actionText = "",
        showAction = false,
        isWallpaperMode = isWallpaperMode,
        onActionClick = {},
        modifier = modifier,
        cardId = BackgroundConfig.FOCUS_CARD_STORAGE
    ) {
        StorageRow(
            label = stringResource(R.string.home_storage_internal),
            used = storageStatus.storageUsed,
            total = storageStatus.storageTotal,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(FolkDimens.Size12))
        StorageRow(
            label = stringResource(R.string.home_storage_ram),
            used = storageStatus.ramUsed,
            total = storageStatus.ramTotal,
            color = MaterialTheme.colorScheme.secondary
        )
        if (storageStatus.zramTotal > 0) {
            Spacer(modifier = Modifier.height(FolkDimens.Size12))
            StorageRow(
                label = stringResource(R.string.home_storage_zram),
                used = storageStatus.zramUsed,
                total = storageStatus.zramTotal,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        if (storageStatus.swapTotal > 0) {
            Spacer(modifier = Modifier.height(FolkDimens.Size12))
            StorageRow(
                label = stringResource(R.string.home_storage_swap),
                used = storageStatus.swapUsed,
                total = storageStatus.swapTotal,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun StorageRow(
    label: String,
    used: Long,
    total: Long,
    color: androidx.compose.ui.graphics.Color
) {
    val progress = if (total > 0) used.toFloat() / total.toFloat() else 0f
    val usedStr = android.text.format.Formatter.formatFileSize(LocalContext.current, used)
    val totalStr = android.text.format.Formatter.formatFileSize(LocalContext.current, total)
    val isWallpaper = LocalFocusCardWallpaper.current
    val subColor = LocalFocusCardSubColor.current
    val primaryColor = if (isWallpaper) FolkColors.MediaContent else MaterialTheme.colorScheme.onSurface
    val secondaryColor = if (isWallpaper && subColor != FolkColors.Unspecified) subColor else MaterialTheme.colorScheme.onSurfaceVariant
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = primaryColor
            )
            Text(
                text = "$usedStr / $totalStr",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryColor
            )
        }
        Spacer(modifier = Modifier.height(FolkDimens.Size8))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(FolkDimens.Size8)
                .clip(MaterialTheme.shapes.small),
            color = if (isWallpaper) FolkColors.MediaContent else color,
            trackColor = (if (isWallpaper) FolkColors.MediaContent else color).copy(alpha = 0.2f),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusCircle(
    value: String,
    label: String,
    progress: Float,
    color: androidx.compose.ui.graphics.Color
) {
    val isWallpaper = LocalFocusCardWallpaper.current
    val subColor = LocalFocusCardSubColor.current
    val effectiveColor = if (isWallpaper) FolkColors.MediaContent else color
    val labelColor = if (isWallpaper && subColor != FolkColors.Unspecified) subColor else MaterialTheme.colorScheme.onSurfaceVariant

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
                    color = effectiveColor,
                    trackColor = effectiveColor.copy(alpha = 0.2f),
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
                    color = effectiveColor.copy(alpha = 0.2f),
                    amplitude = { 1f },
                    wavelength = FolkDimens.Size24,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = if (isWallpaper) FolkColors.MediaContent else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = labelColor
        )
    }
}


/**
 * CompositionLocal: 当前卡片是否启用了壁纸模式
 */
val LocalFocusCardWallpaper = compositionLocalOf { false }

/**
 * CompositionLocal: 壁纸模式下的次要文字颜色
 */
val LocalFocusCardSubColor = compositionLocalOf { FolkColors.Unspecified }

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    val isWallpaper = LocalFocusCardWallpaper.current
    val subColor = LocalFocusCardSubColor.current
    val labelColor = if (isWallpaper && subColor != FolkColors.Unspecified) subColor else MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = if (isWallpaper) FolkColors.MediaContent else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.copyableInfo(label, value),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = valueColor
        )
    }
}
