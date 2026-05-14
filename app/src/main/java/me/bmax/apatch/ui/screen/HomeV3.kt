package me.bmax.apatch.ui.screen

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.apApp
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.HardwareMonitor
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.ui.HomeBottomSpacer

private val managerVersion = getManagerVersion()

@Composable
fun HomeScreenV3(
    paddingValues: PaddingValues,
    navigator: DestinationsNavigator,
    kpState: APApplication.State,
    apState: APApplication.State
) {
    val scrollState = rememberScrollState()
    
    // Check if update notification is blocked
    val kpState = if (kpState == APApplication.State.KERNELPATCH_NEED_UPDATE && apApp.isKernelPatchUpdateBlocked()) {
        APApplication.State.KERNELPATCH_INSTALLED
    } else {
        kpState
    }
    
    val apState = if (apState == APApplication.State.ANDROIDPATCH_NEED_UPDATE && apApp.isAndroidPatchUpdateBlocked()) {
        APApplication.State.ANDROIDPATCH_INSTALLED
    } else {
        apState
    }
    
    val context = LocalContext.current
    // Only enable wallpaper mode (no card shadow) if custom background is actually enabled
    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled && (BackgroundConfig.customBackgroundUri != null || BackgroundConfig.isMultiBackgroundEnabled)
    
    val showUninstallDialog = remember { mutableStateOf(false) }

    val defaultSlot = stringResource(R.string.home_info_auth_na)
    var deviceSlot by remember { mutableStateOf(defaultSlot) }
    var zygiskImplement by remember { mutableStateOf("None") }
    var mountImplement by remember { mutableStateOf("None") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                zygiskImplement = me.bmax.apatch.util.getZygiskImplement()
                mountImplement = me.bmax.apatch.util.getMountImplement()

                val result = rootShellForResult("getprop ro.boot.slot_suffix")
                if (result.isSuccess) {
                    val slot = result.out.firstOrNull()?.trim()?.removePrefix("_")
                    if (!slot.isNullOrEmpty()) {
                        deviceSlot = slot.uppercase()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (showUninstallDialog.value) {
        UninstallDialog(showDialog = showUninstallDialog, navigator)
    }
    
    BoxWithConstraints {
        val isWide = maxWidth >= 600.dp && maxWidth > maxHeight
        
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isWide) {
                // Row 1: KernelPatch + APP
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(IntrinsicSize.Max)
                ) {
                    KernelPatchCard(
                        kpState = kpState,
                        navigator = navigator,
                        isWallpaperMode = isWallpaperMode,
                        zygiskImplement = zygiskImplement,
                        mountImplement = mountImplement,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    AppCard(
                        apState = apState,
                        kpState = kpState,
                        deviceSlot = deviceSlot,
                        showUninstallDialog = showUninstallDialog,
                        isWallpaperMode = isWallpaperMode,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                
                // Row 2: Device + Storage
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(IntrinsicSize.Max)
                ) {
                    DeviceStatusCard(
                        isWallpaperMode = isWallpaperMode,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                    StorageCard(
                        isWallpaperMode = isWallpaperMode,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            } else {
                // Vertical Stack
                KernelPatchCard(
                    kpState = kpState,
                    navigator = navigator,
                    isWallpaperMode = isWallpaperMode,
                    zygiskImplement = zygiskImplement,
                    mountImplement = mountImplement
                )
                AppCard(
                    apState = apState,
                    kpState = kpState,
                    deviceSlot = deviceSlot,
                    showUninstallDialog = showUninstallDialog,
                    isWallpaperMode = isWallpaperMode
                )
                DeviceStatusCard(isWallpaperMode = isWallpaperMode)
                StorageCard(isWallpaperMode = isWallpaperMode)
            }

            HomeBottomSpacer()
        }
    }
}

@Composable
private fun KernelPatchCard(
    kpState: APApplication.State,
    navigator: DestinationsNavigator,
    isWallpaperMode: Boolean,
    zygiskImplement: String,
    mountImplement: String,
    modifier: Modifier = Modifier
) {
    MagiskStyleCard(
        title = "KernelPatch",
        icon = Icons.Outlined.Extension,
        actionText = when (kpState) {
            APApplication.State.KERNELPATCH_NEED_UPDATE -> stringResource(R.string.home_kp_cando_update)
            APApplication.State.UNKNOWN_STATE -> stringResource(R.string.kpm_install)
            else -> stringResource(R.string.kpm_install)
        },
        showAction = kpState != APApplication.State.KERNELPATCH_INSTALLED,
        isWallpaperMode = isWallpaperMode,
        onActionClick = {
            if (kpState != APApplication.State.UNKNOWN_STATE) {
                navigator.navigate(InstallModeSelectScreenDestination)
            }
        },
        modifier = modifier
    ) {
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
private fun AppCard(
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
        modifier = modifier
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
private fun DeviceStatusCard(isWallpaperMode: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var batteryTemp by remember { mutableStateOf(0f) }
    var batteryLevel by remember { mutableIntStateOf(0) }
    var cpuUsage by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            withContext(Dispatchers.IO) {
                // Battery Info
                val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                batteryTemp = (intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
                val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level != -1 && scale != -1) {
                    batteryLevel = (level * 100 / scale.toFloat()).toInt()
                }

                // CPU Usage (HardwareMonitor)
                cpuUsage = HardwareMonitor.getCpuUsage()

                kotlinx.coroutines.delay(10000)
            }
        }
    }

    MagiskStyleCard(
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
            StatusCircle(
                value = "${batteryTemp}°C",
                label = stringResource(R.string.home_device_status_battery_temp),
                progress = (batteryTemp / 50f).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.primary
            )
            StatusCircle(
                value = "$cpuUsage%",
                label = stringResource(R.string.home_device_status_cpu_load),
                progress = (cpuUsage / 100f).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.secondary
            )
            StatusCircle(
                value = "$batteryLevel%",
                label = stringResource(R.string.home_device_status_battery_level),
                progress = (batteryLevel / 100f).coerceIn(0f, 1f),
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun StorageCard(isWallpaperMode: Boolean, modifier: Modifier = Modifier) {
    var ramUsed by remember { mutableLongStateOf(0L) }
    var ramTotal by remember { mutableLongStateOf(0L) }
    var storageUsed by remember { mutableLongStateOf(0L) }
    var storageTotal by remember { mutableLongStateOf(0L) }

    // ZRAM & Swap
    var zramUsed by remember { mutableLongStateOf(0L) }
    var zramTotal by remember { mutableLongStateOf(0L) }
    var swapUsed by remember { mutableLongStateOf(0L) }
    var swapTotal by remember { mutableLongStateOf(0L) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // [OPTIMIZE] 只在 RESUMED 状态时定期更新,并在 PAUSED 时自动停止
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            withContext(Dispatchers.IO) {
                // Internal Storage (Standard Java API)
                val dataDir = android.os.Environment.getDataDirectory()
                val stat = android.os.StatFs(dataDir.path)
                val blockSize = stat.blockSizeLong
                val totalBlocks = stat.blockCountLong
                val availableBlocks = stat.availableBlocksLong
                storageTotal = totalBlocks * blockSize
                storageUsed = storageTotal - (availableBlocks * blockSize)

                // Memory Info (HardwareMonitor)
                val memInfo = HardwareMonitor.getMemoryInfo()
                ramTotal = memInfo.ramTotal
                ramUsed = memInfo.ramUsed
                zramTotal = memInfo.zramTotal
                zramUsed = memInfo.zramUsed
                swapTotal = memInfo.swapTotal
                swapUsed = memInfo.swapUsed

                // [OPTIMIZE] 保持5秒更新频率,存储信息变化较慢
                kotlinx.coroutines.delay(5000)
            }
        }
    }
    
    MagiskStyleCard(
        title = stringResource(R.string.home_storage_title),
        icon = Icons.Outlined.SdStorage,
        actionText = "",
        showAction = false,
        isWallpaperMode = isWallpaperMode,
        onActionClick = {},
        modifier = modifier
    ) {
        StorageRow(
            label = stringResource(R.string.home_storage_internal),
            used = storageUsed,
            total = storageTotal,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        StorageRow(
            label = stringResource(R.string.home_storage_ram),
            used = ramUsed,
            total = ramTotal,
            color = MaterialTheme.colorScheme.secondary
        )
        if (zramTotal > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            StorageRow(
                label = stringResource(R.string.home_storage_zram),
                used = zramUsed,
                total = zramTotal,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        if (swapTotal > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            StorageRow(
                label = stringResource(R.string.home_storage_swap),
                used = swapUsed,
                total = swapTotal,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun StorageRow(
    label: String,
    used: Long,
    total: Long,
    color: androidx.compose.ui.graphics.Color
) {
    val progress = if (total > 0) used.toFloat() / total.toFloat() else 0f
    val usedStr = android.text.format.Formatter.formatFileSize(LocalContext.current, used)
    val totalStr = android.text.format.Formatter.formatFileSize(LocalContext.current, total)
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$usedStr / $totalStr",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.small),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

@Composable
private fun StatusCircle(
    value: String,
    label: String,
    progress: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = color.copy(alpha = 0.2f),
                strokeWidth = 8.dp,
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 8.dp,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MagiskStyleCard(
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                if (showAction) {
                    Button(
                        onClick = onActionClick,
                        enabled = actionEnabled,
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Text(text = actionText)
                    }
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            // Content Info
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
