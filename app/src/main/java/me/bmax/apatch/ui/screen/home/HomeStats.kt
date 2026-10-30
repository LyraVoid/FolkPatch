package me.bmax.apatch.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.navigation.BottomBarDestination
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.viewmodel.StatsViewModel
import me.bmax.apatch.util.AppData
import me.bmax.apatch.util.ui.HomeBottomSpacer

@Composable
fun HomeScreenStats(
    innerPadding: PaddingValues,
    navigator: DestinationsNavigator,
    kpState: APApplication.State,
    apState: APApplication.State,
) {
    val viewModel: StatsViewModel = viewModel()
    val stats by viewModel.uiState.collectAsStateWithLifecycle()
    val isJailbreak = LocalHomeJailbreakState.current.isActive
    val resolvedKpState = if (
        kpState == APApplication.State.KERNELPATCH_NEED_UPDATE &&
        (apApp.isKernelPatchUpdateBlocked() || isJailbreak)
    ) {
        APApplication.State.KERNELPATCH_INSTALLED
    } else {
        kpState
    }
    val resolvedApState = if (
        apState == APApplication.State.ANDROIDPATCH_NEED_UPDATE &&
        apApp.isAndroidPatchUpdateBlocked()
    ) {
        APApplication.State.ANDROIDPATCH_INSTALLED
    } else {
        apState
    }
    val countsAvailable = resolvedKpState != APApplication.State.UNKNOWN_STATE || isJailbreak
    val installed = countsAvailable

    if (countsAvailable) {
        LaunchedEffect(Unit) { AppData.DataRefreshManager.ensureCountsLoaded() }
    }
    LifecycleStartEffect(Unit) {
        viewModel.startPolling()
        onStopOrDispose { viewModel.stopPolling() }
    }

    val superuserCount by AppData.DataRefreshManager.superuserCount.collectAsStateWithLifecycle()
    val moduleCount by AppData.DataRefreshManager.apmModuleCount.collectAsStateWithLifecycle()
    val kernelModuleCount by AppData.DataRefreshManager.kernelModuleCount.collectAsStateWithLifecycle()
    val zygiskImplement = remember { mutableStateOf("None") }
    val mountImplement = remember { mutableStateOf("None") }
    if (installed) {
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                runCatching {
                    zygiskImplement.value = me.bmax.apatch.util.getZygiskImplement()
                    mountImplement.value = me.bmax.apatch.util.getMountImplement()
                }
            }
        }
    }

    val showUninstallDialog = remember { mutableStateOf(false) }
    if (showUninstallDialog.value) {
        UninstallDialog(showUninstallDialog, navigator)
    }

    val hideApatchCard = APApplication.sharedPreferences.getBoolean("hide_apatch_card", false)
    val useGridTop = APApplication.sharedPreferences.getString("stats_top_layout", "list") == "grid"
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val openSuperUser = {
        navigator.navigate(BottomBarDestination.SuperUser.direction) {
            popUpTo(com.ramcosta.composedestinations.generated.NavGraphs.root) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openModules = {
        navigator.navigate(BottomBarDestination.AModule.direction) {
            popUpTo(com.ramcosta.composedestinations.generated.NavGraphs.root) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (wide) {
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = FolkDimens.Size16)
                .verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size16),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16),
            ) {
                StatsStatusSection(
                    kpState = resolvedKpState,
                    apState = resolvedApState,
                    navigator = navigator,
                    showUninstallDialog = showUninstallDialog,
                    countsAvailable = countsAvailable,
                    superuserCount = superuserCount,
                    moduleCount = moduleCount,
                    onOpenSuperUser = openSuperUser,
                    onOpenModule = openModules,
                    grid = useGridTop,
                )
                StatsMonitoringSection(stats.monitor, stats.history)
                if (!hideApatchCard) LearnMoreCardV4()
                HomeBottomSpacer()
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16),
            ) {
                StatsModuleSection(superuserCount, moduleCount, kernelModuleCount)
                SystemInfoCard(
                    kpState = resolvedKpState,
                    apState = resolvedApState,
                    zygiskImplement = zygiskImplement.value,
                    mountImplement = mountImplement.value,
                )
                HomeBottomSpacer()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FolkDimens.Size16),
            verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16),
        ) {
            StatsStatusSection(
                kpState = resolvedKpState,
                apState = resolvedApState,
                navigator = navigator,
                showUninstallDialog = showUninstallDialog,
                countsAvailable = countsAvailable,
                superuserCount = superuserCount,
                moduleCount = moduleCount,
                onOpenSuperUser = openSuperUser,
                onOpenModule = openModules,
                grid = useGridTop,
            )
            StatsMonitoringSection(stats.monitor, stats.history)
            StatsModuleSection(superuserCount, moduleCount, kernelModuleCount)
            SystemInfoCard(
                kpState = resolvedKpState,
                apState = resolvedApState,
                zygiskImplement = zygiskImplement.value,
                mountImplement = mountImplement.value,
            )
            if (!hideApatchCard) LearnMoreCardV4()
            HomeBottomSpacer()
        }
    }
}
