package me.bmax.apatch.ui.screen

import android.os.BatteryManager
import android.os.Build
import android.system.Os
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.AppData
import me.bmax.apatch.util.HardwareMonitor
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.rootShellForResult

private val managerVersion = getManagerVersion()

/**
 * HomeV4 - Dashboard Pro 风格首页布局
 * 
 * 特色功能：
 * - Hero状态卡：大型动态状态展示区，显示APatch状态和工作模式
 * - 内核补丁安装/卸载UI：完整的安装流程和进度显示
 * - 计数卡片组：超级用户、APM模块、内核补丁模块数量
 * - 快捷操作面板：重启菜单、SELinux切换等
 * - 系统信息网格：设备信息、内核版本、存储空间
 * - 响应式设计：宽屏双栏，窄屏单栏
 * - 动画效果：呼吸动画、颜色过渡、组件显示/隐藏动画
 */
@Composable
fun HomeScreenV4(
    innerPadding: PaddingValues,
    navigator: DestinationsNavigator,
    kpState: APApplication.State,
    apState: APApplication.State
) {
    // 检查是否屏蔽更新通知
    val kpStateResolved = if (kpState == APApplication.State.KERNELPATCH_NEED_UPDATE && apApp.isKernelPatchUpdateBlocked()) {
        APApplication.State.KERNELPATCH_INSTALLED
    } else {
        kpState
    }

    val apStateResolved = if (apState == APApplication.State.ANDROIDPATCH_NEED_UPDATE && apApp.isAndroidPatchUpdateBlocked()) {
        APApplication.State.ANDROIDPATCH_INSTALLED
    } else {
        apState
    }

    // 对话框状态
    val showUninstallDialog = remember { mutableStateOf(false) }
    val showAuthFailedTipDialog = remember { mutableStateOf(false) }
    val showAuthKeyDialog = remember { mutableStateOf(false) }
    val showInstallDialog = remember { mutableStateOf(false) }

    // 对话框显示
    if (showUninstallDialog.value) {
        UninstallDialog(showDialog = showUninstallDialog, navigator)
    }
    if (showAuthFailedTipDialog.value) {
        AuthFailedTipDialog(showDialog = showAuthFailedTipDialog)
    }
    if (showAuthKeyDialog.value) {
        AuthSuperKey(showDialog = showAuthKeyDialog, showFailedDialog = showAuthFailedTipDialog)
    }
    if (showInstallDialog.value) {
        InstallProgressDialog(
            showDialog = showInstallDialog,
            kpState = kpStateResolved,
            apState = apStateResolved
        )
    }

    // 获取系统信息
    val context = LocalContext.current
    val prefs = APApplication.sharedPreferences
    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled && 
        (BackgroundConfig.customBackgroundUri != null || BackgroundConfig.isMultiBackgroundEnabled)
    
    // 隐藏APatch卡片设置
    val hideApatchCard = prefs.getBoolean("hide_apatch_card", false)

    // 系统信息状态
    var zygiskImplement by remember { mutableStateOf("None") }
    var mountImplement by remember { mutableStateOf("None") }
    var deviceSlot by remember { mutableStateOf(context.getString(R.string.home_info_auth_na)) }

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
            } catch (_: Exception) {}
        }
    }

    // 加载计数数据
    val showCoreCards = kpStateResolved != APApplication.State.UNKNOWN_STATE
    if (showCoreCards) {
        LaunchedEffect(Unit) {
            AppData.DataRefreshManager.ensureCountsLoaded()
        }
    }
    val superuserCount by AppData.DataRefreshManager.superuserCount.collectAsState()
    val apmModuleCount by AppData.DataRefreshManager.apmModuleCount.collectAsState()
    val kpmModuleCount by AppData.DataRefreshManager.kernelModuleCount.collectAsState()

    // 响应式布局
    val configuration = LocalConfiguration.current
    val isWide = configuration.screenWidthDp >= 600

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(0.dp))

        // Hero状态卡
        HeroStatusCard(
            kpState = kpStateResolved,
            apState = apStateResolved,
            navigator = navigator,
            showAuthKeyDialog = showAuthKeyDialog,
            showUninstallDialog = showUninstallDialog,
            showInstallDialog = showInstallDialog,
            isWallpaperMode = isWallpaperMode
        )

        // 计数卡片组
        AnimatedVisibility(
            visible = showCoreCards,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            CountCardsRow(
                superuserCount = superuserCount,
                apmModuleCount = apmModuleCount,
                kpmModuleCount = kpmModuleCount,
                navigator = navigator
            )
        }

        // Android补丁状态卡片（Half模式时显示）
        AnimatedVisibility(
            visible = kpStateResolved != APApplication.State.UNKNOWN_STATE && 
                apStateResolved != APApplication.State.ANDROIDPATCH_INSTALLED,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AndroidPatchCard(
                apState = apStateResolved,
                kpState = kpStateResolved,
                showInstallDialog = showInstallDialog,
                isWallpaperMode = isWallpaperMode
            )
        }

        // 系统信息网格
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SystemInfoCard(
                    kpState = kpStateResolved,
                    apState = apStateResolved,
                    zygiskImplement = zygiskImplement,
                    mountImplement = mountImplement,
                    modifier = Modifier.weight(1f)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DeviceStatusCard(isWallpaperMode = isWallpaperMode)
                    StorageInfoCard()
                    // 了解更多卡片 - 只在平板布局右侧栏显示
                    if (!hideApatchCard) {
                        LearnMoreCardV4()
                    }
                }
            }
        } else {
            SystemInfoCard(
                kpState = kpStateResolved,
                apState = apStateResolved,
                zygiskImplement = zygiskImplement,
                mountImplement = mountImplement
            )
            DeviceStatusCard(isWallpaperMode = isWallpaperMode)
            StorageInfoCard()
            // 窄屏布局在下方显示
            if (!hideApatchCard) {
                LearnMoreCardV4()
            }
        }

        Spacer(Modifier)
    }
}

/**
 * Hero状态卡 - 大型动态状态展示区
 */
@Composable
private fun HeroStatusCard(
    kpState: APApplication.State,
    apState: APApplication.State,
    navigator: DestinationsNavigator,
    showAuthKeyDialog: MutableState<Boolean>,
    showUninstallDialog: MutableState<Boolean>,
    showInstallDialog: MutableState<Boolean>,
    isWallpaperMode: Boolean
) {
    val isWorking = kpState == APApplication.State.KERNELPATCH_INSTALLED
    val isUpdate = kpState == APApplication.State.KERNELPATCH_NEED_UPDATE || 
        kpState == APApplication.State.KERNELPATCH_NEED_REBOOT
    val isUnknown = kpState == APApplication.State.UNKNOWN_STATE

    // 呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathAlpha"
    )

    // 颜色状态动画
    val containerColor by animateColorAsState(
        targetValue = when {
            isWorking -> MaterialTheme.colorScheme.primary
            isUpdate -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.errorContainer
        },
        animationSpec = tween(500),
        label = "containerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isWorking -> MaterialTheme.colorScheme.onPrimary
            isUpdate -> MaterialTheme.colorScheme.onSecondary
            else -> MaterialTheme.colorScheme.onErrorContainer
        },
        animationSpec = tween(500),
        label = "contentColor"
    )

    // 渐变背景
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            if (isWorking) containerColor.copy(alpha = breathAlpha) else containerColor,
            containerColor.copy(alpha = 0.8f)
        )
    )

    val classicEmojiEnabled = BackgroundConfig.isListWorkingCardModeHidden
    val isFull = apState == APApplication.State.ANDROIDPATCH_INSTALLED
    val modeText = BackgroundConfig.getCustomBadgeText() ?: if (isFull) "Full" else "Half"

    if (isWorking) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
                contentColor = contentColor
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradientBrush)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = contentColor
                            )

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (classicEmojiEnabled) 
                                        stringResource(R.string.home_working) + "😋" 
                                    else 
                                        stringResource(R.string.home_working),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                if (!classicEmojiEnabled) {
                                    Spacer(Modifier.height(4.dp))
                                    ModeLabelChip(label = modeText, contentColor = contentColor)
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { showUninstallDialog.value = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = contentColor
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                contentColor.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.home_ap_cando_uninstall),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(
                        color = contentColor.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        VersionInfoColumn(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.home_kpatch_version),
                            value = Version.installedKPVString()
                        )
                        VersionInfoColumn(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.home_apatch_version),
                            value = managerVersion.second.toString()
                        )
                        VersionInfoColumn(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.home_selinux_status),
                            value = getSELinuxStatus()
                        )
                    }
                }
            }
        }
    } else {
        val finalContainerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = BackgroundConfig.customBackgroundOpacity)
        } else {
            MaterialTheme.colorScheme.errorContainer
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (isUnknown) {
                        showAuthKeyDialog.value = true
                    } else {
                        navigator.navigate(InstallModeSelectScreenDestination)
                    }
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = finalContainerColor,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isUpdate -> Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    isUnknown -> Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.width(20.dp))

                Column {
                    Text(
                        text = when {
                            isUpdate -> stringResource(R.string.home_kp_need_update)
                            isUnknown -> stringResource(R.string.home_install_unknown)
                            else -> stringResource(R.string.home_not_installed)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isUnknown) stringResource(R.string.super_key) else stringResource(R.string.home_click_to_install),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
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
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            withContext(Dispatchers.IO) {
                val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                batteryTemp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level != -1 && scale != -1) {
                    batteryLevel = (level * 100 / scale.toFloat()).toInt()
                }

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
private fun StatusCircle(
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
private fun ModeLabelChip(label: String, contentColor: Color) {
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
private fun VersionInfoColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 计数卡片组 - 超级用户、APM模块、内核补丁模块
 */
@Composable
private fun CountCardsRow(
    superuserCount: Int,
    apmModuleCount: Int,
    kpmModuleCount: Int,
    navigator: DestinationsNavigator
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CountCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Security,
            label = stringResource(R.string.superuser),
            count = superuserCount,
            onClick = {
                navigator.navigate(BottomBarDestination.SuperUser.direction) {
                    popUpTo(NavGraphs.root.startRoute) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        CountCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Widgets,
            label = stringResource(R.string.module),
            count = apmModuleCount,
            onClick = {
                navigator.navigate(BottomBarDestination.AModule.direction) {
                    popUpTo(NavGraphs.root.startRoute) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        CountCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Extension,
            label = stringResource(R.string.kpm),
            count = kpmModuleCount,
            onClick = {
                navigator.navigate(BottomBarDestination.KModule.direction) {
                    popUpTo(NavGraphs.root.startRoute) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

/**
 * 单个计数卡片
 */
@Composable
private fun CountCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: () -> Unit
) {
    val containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = BackgroundConfig.customBackgroundOpacity)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Android补丁状态卡片
 */
@Composable
private fun AndroidPatchCard(
    apState: APApplication.State,
    kpState: APApplication.State,
    showInstallDialog: MutableState<Boolean>,
    isWallpaperMode: Boolean
) {
    val containerColor = when {
        BackgroundConfig.isCustomBackgroundEnabled -> {
            MaterialTheme.colorScheme.surface.copy(alpha = BackgroundConfig.customBackgroundOpacity)
        }
        else -> {
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                APApplication.State.ANDROIDPATCH_INSTALLING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                    Icon(
                        imageVector = Icons.Outlined.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
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
                    apState != APApplication.State.UNKNOWN_STATE
            ) {
                when (apState) {
                    APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> 
                        Text(stringResource(R.string.home_ap_cando_install))
                    APApplication.State.ANDROIDPATCH_NEED_UPDATE -> 
                        Text(stringResource(R.string.home_kp_cando_update))
                    APApplication.State.ANDROIDPATCH_INSTALLING,
                    APApplication.State.ANDROIDPATCH_UNINSTALLING -> 
                        Icon(Icons.Outlined.Cached, contentDescription = "busy")
                    else -> 
                        Text(stringResource(R.string.home_ap_cando_uninstall))
                }
            }
        }
    }
}

/**
 * 安装进度对话框
 */
@Composable
private fun InstallProgressDialog(
    showDialog: MutableState<Boolean>,
    kpState: APApplication.State,
    apState: APApplication.State
) {
    if (!showDialog.value) return

    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("准备安装...") }

    LaunchedEffect(Unit) {
        // 模拟安装进度
        for (i in 1..100) {
            delay(50)
            progress = i / 100f
            statusText = when {
                i < 20 -> "正在准备..."
                i < 40 -> "正在备份..."
                i < 60 -> "正在写入..."
                i < 80 -> "正在验证..."
                else -> "正在完成..."
            }
        }
        showDialog.value = false
    }

    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.kpm_install)) },
        text = {
            Column {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { }
    )
}

/**
 * 系统信息卡片
 */
@Composable
internal fun SystemInfoCard(
    kpState: APApplication.State,
    apState: APApplication.State,
    zygiskImplement: String,
    mountImplement: String,
    modifier: Modifier = Modifier
) {
    val uname = Os.uname()
    val prefs = APApplication.sharedPreferences
    
    var hideSuPath by remember { mutableStateOf(prefs.getBoolean("hide_su_path", false)) }
    var hideKpatchVersion by remember { mutableStateOf(prefs.getBoolean("hide_kpatch_version", false)) }
    var hideFingerprint by remember { mutableStateOf(prefs.getBoolean("hide_fingerprint", false)) }
    var hideZygisk by remember { mutableStateOf(prefs.getBoolean("hide_zygisk", false)) }
    var hideMount by remember { mutableStateOf(prefs.getBoolean("hide_mount", false)) }

    TonalCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_kpatch_info_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 信息列表
            InfoItem(Icons.Outlined.PhoneAndroid, stringResource(R.string.home_device_info), getDeviceInfo())
            if (kpState != APApplication.State.UNKNOWN_STATE && !hideKpatchVersion) {
                InfoItem(Icons.Outlined.Extension, stringResource(R.string.home_kpatch_version), Version.installedKPVString())
            }
            if (kpState != APApplication.State.UNKNOWN_STATE && !hideSuPath) {
                InfoItem(Icons.Outlined.Code, stringResource(R.string.home_su_path), Natives.suPath())
            }
            if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) {
                InfoItem(Icons.Outlined.Android, stringResource(R.string.home_apatch_version), managerVersion.second.toString())
            }
            InfoItem(Icons.Outlined.DeveloperBoard, stringResource(R.string.home_kernel), uname.release)
            InfoItem(Icons.Outlined.Info, stringResource(R.string.home_system_version), getSystemVersion())
            if (!hideFingerprint) {
                InfoItem(Icons.Outlined.Fingerprint, stringResource(R.string.home_fingerprint), Build.FINGERPRINT)
            }
            if (kpState != APApplication.State.UNKNOWN_STATE && zygiskImplement != "None" && !hideZygisk) {
                InfoItem(Icons.Outlined.Layers, stringResource(R.string.home_zygisk_implement), zygiskImplement)
            }
            if (kpState != APApplication.State.UNKNOWN_STATE && mountImplement != "None" && !hideMount) {
                InfoItem(Icons.Outlined.SdStorage, stringResource(R.string.home_mount_implement), mountImplement)
            }
            InfoItem(Icons.Outlined.Shield, stringResource(R.string.home_selinux_status), getSELinuxStatus())
        }
    }
}

/**
 * 信息项
 */
@Composable
private fun InfoItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 存储信息卡片
 */
@Composable
private fun StorageInfoCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var ramUsed by remember { mutableStateOf(0L) }
    var ramTotal by remember { mutableStateOf(0L) }
    var storageUsed by remember { mutableStateOf(0L) }
    var storageTotal by remember { mutableStateOf(0L) }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            withContext(Dispatchers.IO) {
                // 内部存储
                val dataDir = android.os.Environment.getDataDirectory()
                val stat = android.os.StatFs(dataDir.path)
                val blockSize = stat.blockSizeLong
                val totalBlocks = stat.blockCountLong
                val availableBlocks = stat.availableBlocksLong
                storageTotal = totalBlocks * blockSize
                storageUsed = storageTotal - (availableBlocks * blockSize)

                // 内存信息
                val memInfo = HardwareMonitor.getMemoryInfo()
                ramTotal = memInfo.ramTotal
                ramUsed = memInfo.ramUsed

                delay(5000)
            }
        }
    }

    TonalCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SdStorage,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_storage_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 存储进度
            StorageProgressBar(
                label = stringResource(R.string.home_storage_internal),
                used = storageUsed,
                total = storageTotal,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            StorageProgressBar(
                label = stringResource(R.string.home_storage_ram),
                used = ramUsed,
                total = ramTotal,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * 存储进度条
 */
@Composable
private fun StorageProgressBar(
    label: String,
    used: Long,
    total: Long,
    color: Color
) {
    val context = LocalContext.current
    val progress = if (total > 0) used.toFloat() / total.toFloat() else 0f
    val usedStr = android.text.format.Formatter.formatFileSize(context, used)
    val totalStr = android.text.format.Formatter.formatFileSize(context, total)

    Column {
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
        )
    }
}

/**
 * 了解更多卡片 V4
 */
@Composable
internal fun LearnMoreCardV4() {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    TonalCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri("https://fp.mysqil.com/") }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.home_learn_apatch),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.home_click_to_learn_apatch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
