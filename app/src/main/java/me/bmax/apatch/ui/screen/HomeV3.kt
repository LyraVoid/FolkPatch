package me.bmax.apatch.ui.screen

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.apApp
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.copyableInfo
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.util.SystemInfoCollector
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.ui.HomeBottomSpacer
import me.bmax.apatch.util.ui.showToast

private val managerVersion = getManagerVersion()

@Composable
fun HomeScreenV3(
    paddingValues: PaddingValues,
    navigator: DestinationsNavigator,
    kpState: APApplication.State,
    apState: APApplication.State
) {
    val scrollState = rememberScrollState()
    
    // Check if update notification is blocked (including when jailbreak mode is active)
    val isJailbreak = LocalHomeJailbreakState.current.isActive
    val kpState = if (kpState == APApplication.State.KERNELPATCH_NEED_UPDATE && (apApp.isKernelPatchUpdateBlocked() || isJailbreak)) {
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
    val jailbreakState = LocalHomeJailbreakState.current
    val isJailbreak = jailbreakState.isActive
    val isPermissive = jailbreakState.isPermissive
    MagiskStyleCard(
        title = if (isJailbreak) stringResource(R.string.settings_jailbreak_mode) else "KernelPatch",
        icon = if (isJailbreak) Icons.Filled.LockOpen else Icons.Outlined.Extension,
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
private fun DeviceStatusCard(isWallpaperMode: Boolean, modifier: Modifier = Modifier) {
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
                .padding(vertical = 8.dp),
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
private fun StorageCard(isWallpaperMode: Boolean, modifier: Modifier = Modifier) {
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
        Spacer(modifier = Modifier.height(12.dp))
        StorageRow(
            label = stringResource(R.string.home_storage_ram),
            used = storageStatus.ramUsed,
            total = storageStatus.ramTotal,
            color = MaterialTheme.colorScheme.secondary
        )
        if (storageStatus.zramTotal > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            StorageRow(
                label = stringResource(R.string.home_storage_zram),
                used = storageStatus.zramUsed,
                total = storageStatus.zramTotal,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        if (storageStatus.swapTotal > 0) {
            Spacer(modifier = Modifier.height(12.dp))
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
private fun StorageRow(
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
    val primaryColor = if (isWallpaper) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryColor = if (isWallpaper && subColor != Color.Unspecified) subColor else MaterialTheme.colorScheme.onSurfaceVariant
    
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
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.small),
            color = if (isWallpaper) Color.White else color,
            trackColor = (if (isWallpaper) Color.White else color).copy(alpha = 0.2f),
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
    val isWallpaper = LocalFocusCardWallpaper.current
    val subColor = LocalFocusCardSubColor.current
    val effectiveColor = if (isWallpaper) Color.White else color
    val labelColor = if (isWallpaper && subColor != Color.Unspecified) subColor else MaterialTheme.colorScheme.onSurfaceVariant

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
                color = effectiveColor.copy(alpha = 0.2f),
                strokeWidth = 8.dp,
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = effectiveColor,
                strokeWidth = 8.dp,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWallpaper) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor
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
    cardId: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // FocusUI卡片壁纸状态
    val cardBgUri = if (cardId != null && BackgroundConfig.isFocusCardBackgroundEnabled) {
        BackgroundConfig.getFocusCardBgUri(cardId)
    } else null
    val hasCardWallpaper = !cardBgUri.isNullOrEmpty()
    // Match APatchTheme: manual light/dark mode must not be replaced by the
    // device configuration when the app is not following the system.
    val prefs = APApplication.sharedPreferences
    val isDarkTheme = if (prefs.getBoolean("night_mode_follow_sys", false)) {
        isSystemInDarkTheme()
    } else {
        prefs.getBoolean("night_mode_enabled", true)
    }
    val cardBgDim = BackgroundConfig.getEffectiveFocusCardBgDim(isDarkTheme)
    val cardBgOpacity = BackgroundConfig.getEffectiveFocusCardBgOpacity(isDarkTheme)

    // 长按对话框状态
    var showBgOptionsDialog by remember { mutableStateOf(false) }

    // 图片选择器
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (cardId != null) {
                scope.launch {
                    val success = BackgroundManager.saveAndApplyFocusCardBackground(context, cardId, it)
                    if (success) {
                        showToast(context, R.string.focus_card_background_saved)
                    } else {
                        showToast(context, R.string.focus_card_background_error)
                    }
                }
            }
        }
    }

    // 清除确认对话框
    val clearBgConfirmDialog = rememberConfirmDialog(
        onConfirm = {
            if (cardId != null) {
                scope.launch {
                    BackgroundManager.clearFocusCardBackground(context, cardId)
                    showToast(context, context.getString(R.string.focus_card_background_cleared))
                }
            }
        }
    )

    // 卡片壁纸模式下的内容颜色
    val contentColor = if (hasCardWallpaper) Color.White else MaterialTheme.colorScheme.onSurface
    val subContentColor = if (hasCardWallpaper) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (cardId != null && BackgroundConfig.isFocusCardBackgroundEnabled) {
                    Modifier.pointerInput(cardId) {
                        detectTapGestures(
                            onLongPress = { showBgOptionsDialog = true }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasCardWallpaper) Color.Transparent
                else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            contentColor = contentColor
        )
    ) {
        // 外层Box包裹内容，壁纸层通过 matchParentSize 精确填充内容尺寸
        // 注意：不能使用 fillMaxSize，因为卡片是 wrap-content 的，fillMaxSize 会导致尺寸堙缩为0
        Box {
            // 卡片壁纸层（作为背景，匹配内容尺寸）
            if (hasCardWallpaper) {
                // 配置支持GIF/动图的ImageLoader
                val imageLoader = ImageLoader.Builder(context)
                    .components {
                        if (Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                    }
                    .build()

                // 壁纸图片：matchParentSize 使其精确填充卡片内容区域
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(cardBgUri)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                            .alpha(cardBgOpacity)
                )
                // 暗度层，保证文字在壁纸上的可读性
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = cardBgDim))
                )
            }

            // 卡片内容层（决定卡片的实际尺寸）
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
                        tint = if (hasCardWallpaper) Color.White else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
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
                    color = if (hasCardWallpaper) Color.White.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Content Info
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompositionLocalProvider(
                        LocalFocusCardWallpaper provides hasCardWallpaper,
                        LocalFocusCardSubColor provides subContentColor
                    ) {
                        content()
                    }
                }
            }
        }
    }

    // 长按背景选项对话框
    if (cardId != null && BackgroundConfig.isFocusCardBackgroundEnabled) {
        BackgroundOptionsDialog(
            showDialog = showBgOptionsDialog,
            onDismiss = { showBgOptionsDialog = false },
            title = stringResource(R.string.focus_card_background_title),
            selectLabel = stringResource(R.string.settings_select_background_image),
            clearLabel = stringResource(R.string.focus_card_background_clear),
            hasExisting = hasCardWallpaper,
            onSelectImage = {
                if (PermissionUtils.hasExternalStoragePermission(context)) {
                    try {
                        pickImageLauncher.launch("image/*")
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, e.message ?: "")
                    }
                } else {
                    showToast(context, context.getString(R.string.focus_card_permission_required))
                }
            },
            onClearImage = {
                clearBgConfirmDialog.showConfirm(
                    title = context.getString(R.string.focus_card_background_clear),
                    content = context.getString(R.string.focus_card_background_clear_confirm),
                    markdown = false,
                )
            },
        )
    }
}

/**
 * CompositionLocal: 当前卡片是否启用了壁纸模式
 */
private val LocalFocusCardWallpaper = compositionLocalOf { false }

/**
 * CompositionLocal: 壁纸模式下的次要文字颜色
 */
private val LocalFocusCardSubColor = compositionLocalOf { Color.Unspecified }

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    val isWallpaper = LocalFocusCardWallpaper.current
    val subColor = LocalFocusCardSubColor.current
    val labelColor = if (isWallpaper && subColor != Color.Unspecified) subColor else MaterialTheme.colorScheme.onSurfaceVariant
    val valueColor = if (isWallpaper) Color.White else MaterialTheme.colorScheme.onSurface

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
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}
