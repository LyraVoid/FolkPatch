package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.apApp
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.ui.HomeBottomSpacer

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
        val isWide = maxWidth >= FolkDimens.Size600 && maxWidth > maxHeight
        
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(FolkDimens.Size16),
            verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
        ) {
            if (isWide) {
                // Row 1: KernelPatch + APP
                Row(
                    horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size16),
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
                    horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size16),
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
