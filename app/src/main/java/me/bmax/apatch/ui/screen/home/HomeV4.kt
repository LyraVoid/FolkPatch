package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Extension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.ui.HomeBottomSpacer

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

    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled &&
        (BackgroundConfig.customBackgroundUri != null || BackgroundConfig.isMultiBackgroundEnabled)
    val hideApatchCard = APApplication.sharedPreferences.getBoolean("hide_apatch_card", false)
    val showCounts = kpStateResolved != APApplication.State.UNKNOWN_STATE || isJailbreak
    if (showCounts) {
        LaunchedEffect(Unit) { me.bmax.apatch.util.AppData.DataRefreshManager.ensureCountsLoaded() }
    }
    val superuserCount by me.bmax.apatch.util.AppData.DataRefreshManager.superuserCount.collectAsStateWithLifecycle()
    val moduleCount by me.bmax.apatch.util.AppData.DataRefreshManager.apmModuleCount.collectAsStateWithLifecycle()

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

        if (showCounts) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size16)) {
                DashboardCountCard(
                    icon = androidx.compose.material.icons.Icons.Outlined.Person,
                    label = androidx.compose.ui.res.stringResource(R.string.superuser),
                    count = superuserCount,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        navigator.navigate(me.bmax.apatch.ui.navigation.BottomBarDestination.SuperUser.direction) {
                            popUpTo(com.ramcosta.composedestinations.generated.NavGraphs.root) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
                DashboardCountCard(
                    icon = androidx.compose.material.icons.Icons.Outlined.Extension,
                    label = androidx.compose.ui.res.stringResource(R.string.module),
                    count = moduleCount,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        navigator.navigate(me.bmax.apatch.ui.navigation.BottomBarDestination.AModule.direction) {
                            popUpTo(com.ramcosta.composedestinations.generated.NavGraphs.root) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
        InfoCardCircle(kpStateResolved, apStateResolved)
        HomeV4DeviceStatusCard(isWallpaperMode = isWallpaperMode)
        StorageInfoCard()
        if (!hideApatchCard) LearnMoreCardV4()

        HomeBottomSpacer()
    }
}

@Composable
private fun DashboardCountCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val supporting = me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens.current?.group?.supportingColor
        ?: androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
    me.bmax.apatch.ui.component.TonalCard(modifier = modifier, onClick = onClick, flat = true) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = FolkDimens.Size20, vertical = FolkDimens.Size16),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(icon, contentDescription = null, modifier = Modifier.size(FolkDimens.Size20), tint = supporting)
            Spacer(Modifier.width(FolkDimens.Size16))
            Column {
                androidx.compose.material3.Text(label, style = me.bmax.apatch.ui.theme.tokens.FolkType.Caption, color = supporting, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Spacer(Modifier.height(FolkDimens.Size4))
                androidx.compose.material3.Text(
                    count.toString(),
                    style = me.bmax.apatch.ui.theme.tokens.FolkType.Title,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
