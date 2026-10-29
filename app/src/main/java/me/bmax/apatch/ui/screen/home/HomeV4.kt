package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.ui.HomeBottomSpacer

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
    val isJailbreak = LocalHomeJailbreakState.current.isActive
    val kpStateResolved = if (kpState == APApplication.State.KERNELPATCH_NEED_UPDATE && (apApp.isKernelPatchUpdateBlocked() || isJailbreak)) {
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
    val showInstallDialog = remember { mutableStateOf(false) }

    // 对话框显示
    if (showUninstallDialog.value) {
        UninstallDialog(showDialog = showUninstallDialog, navigator)
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

    // 响应式布局
    val configuration = LocalConfiguration.current
    val isWide = configuration.screenWidthDp >= 600

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FolkDimens.Size16),
        verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
    ) {
        Spacer(Modifier.height(FolkDimens.Size0))

        // Hero状态卡
        HeroStatusCard(
            kpState = kpStateResolved,
            apState = apStateResolved,
            navigator = navigator,
            showUninstallDialog = showUninstallDialog,
            showInstallDialog = showInstallDialog,
            isWallpaperMode = isWallpaperMode
        )

        // Android补丁状态卡片（Half模式时显示）
        AnimatedVisibility(
            visible = kpStateResolved != APApplication.State.UNKNOWN_STATE && 
                apStateResolved != APApplication.State.UNKNOWN_STATE &&
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
                horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
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
                    verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
                ) {
                    HomeV4DeviceStatusCard(isWallpaperMode = isWallpaperMode)
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
            HomeV4DeviceStatusCard(isWallpaperMode = isWallpaperMode)
            StorageInfoCard()
            // 窄屏布局在下方显示
            if (!hideApatchCard) {
                LearnMoreCardV4()
            }
        }

        HomeBottomSpacer()
    }
}

/**
 * Hero状态卡 - 大型动态状态展示区
 */
