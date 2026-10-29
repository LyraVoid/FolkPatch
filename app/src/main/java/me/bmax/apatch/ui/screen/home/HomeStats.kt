package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.viewmodel.DashboardViewModel
import me.bmax.apatch.util.AppData
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.ui.HomeBottomSpacer

@Composable
fun HomeScreenStats(
    innerPadding: PaddingValues,
    navigator: DestinationsNavigator,
    kpState: APApplication.State,
    apState: APApplication.State
) {
    val viewModel: DashboardViewModel = viewModel()
    val uiState by viewModel.dashboardUiState.collectAsStateWithLifecycle()
    val timeSeries by viewModel.timeSeriesData.collectAsStateWithLifecycle()

    val isJailbreak = LocalHomeJailbreakState.current.isActive

    // Check if update notification is blocked (including when jailbreak mode is active)
    val kpState = if (kpState == APApplication.State.KERNELPATCH_NEED_UPDATE && (apApp.isKernelPatchUpdateBlocked() || isJailbreak)) {
        APApplication.State.KERNELPATCH_INSTALLED
    } else {
        kpState
    }

    val showCoreCards = kpState != APApplication.State.UNKNOWN_STATE || isJailbreak
    if (showCoreCards) {
        LaunchedEffect(Unit) {
            AppData.DataRefreshManager.ensureCountsLoaded()
        }
    }
    val superuserCount by AppData.DataRefreshManager.superuserCount.collectAsStateWithLifecycle()
    val apmModuleCount by AppData.DataRefreshManager.apmModuleCount.collectAsStateWithLifecycle()
    val kernelModuleCount by AppData.DataRefreshManager.kernelModuleCount.collectAsStateWithLifecycle()

    LifecycleStartEffect(Unit) {
        viewModel.startPeriodicPolling()
        onStopOrDispose {
            viewModel.stopPeriodicPolling()
        }
    }

    val showUninstallDialog = remember { mutableStateOf(false) }
    if (showUninstallDialog.value) {
        UninstallDialog(showDialog = showUninstallDialog, navigator)
    }

    val hideApatchCard = APApplication.sharedPreferences.getBoolean("hide_apatch_card", false)
    val isInstalled = kpState != APApplication.State.UNKNOWN_STATE || isJailbreak
    val statsTopLayout = APApplication.sharedPreferences.getString("stats_top_layout", "list") ?: "list"
    val useGridTop = statsTopLayout == "grid"
    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled &&
        (BackgroundConfig.customBackgroundUri != null || BackgroundConfig.isMultiBackgroundEnabled)

    var zygiskImplement by remember { mutableStateOf("None") }
    var mountImplement by remember { mutableStateOf("None") }
    if (isInstalled) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                try {
                    zygiskImplement = me.bmax.apatch.util.getZygiskImplement()
                    mountImplement = me.bmax.apatch.util.getMountImplement()
                } catch (_: Exception) {}
            }
        }
    }

    LifecycleStartEffect(isInstalled) {
        if (isInstalled) {
            viewModel.startPeriodicPolling()
        }
        onStopOrDispose {
            viewModel.stopPeriodicPolling()
        }
    }

    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    if (isWideScreen) {
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = FolkDimens.Size16)
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
        ) {
            if (isWallpaperMode) { Spacer(Modifier.height(FolkDimens.Size8)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16)) {
                if (useGridTop) {
                    StatsGridTopSection(kpState, apState, navigator, showUninstallDialog)
                } else {
                    StatusCardCircle(kpState, apState, navigator, showUninstallDialog)
                    if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
                        AStatusCardCircle(apState)
                    }
                }
                if (isInstalled) {
                    SystemMonitoringSection(uiState.systemMonitor, timeSeries)
                }
                HomeBottomSpacer()
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16)) {
                if (isInstalled) {
                    ModuleStatisticsSection(superuserCount, apmModuleCount, kernelModuleCount)
                }
                SystemInfoCard(kpState, apState, zygiskImplement, mountImplement)
                if (!hideApatchCard) {
                    LearnMoreCardV4()
                }
                HomeBottomSpacer()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FolkDimens.Size16),
            verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
        ) {
            if (isWallpaperMode) { Spacer(Modifier.height(FolkDimens.Size8)) }
            if (useGridTop) {
                StatsGridTopSection(kpState, apState, navigator, showUninstallDialog)
            } else {
                StatusCardCircle(kpState, apState, navigator, showUninstallDialog)
                if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
                    AStatusCardCircle(apState)
                }
            }
            if (isInstalled) {
                SystemMonitoringSection(uiState.systemMonitor, timeSeries)
                ModuleStatisticsSection(superuserCount, apmModuleCount, kernelModuleCount)
            }
            SystemInfoCard(kpState, apState, zygiskImplement, mountImplement)
            if (!hideApatchCard) {
                LearnMoreCardV4()
            }
            HomeBottomSpacer()
        }
    }
}

