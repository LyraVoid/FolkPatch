package me.bmax.apatch.ui.screen

import android.os.Build
import android.system.Os
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Fingerprint
import me.bmax.apatch.ui.theme.MusicConfig
import me.bmax.apatch.util.MusicManager
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import me.bmax.apatch.ui.theme.BackgroundConfig
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Card
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import me.bmax.apatch.ui.theme.refreshTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.dropUnlessResumed
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenu
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenuItem
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.HomeBottomSpacer

private val managerVersion = getManagerVersion()

private enum class ApatchUninstallOption(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descRes: Int,
    val icon: ImageVector,
) {
    PATCH_ONLY(
        titleRes = R.string.home_dialog_uninstall_ap_only,
        descRes = R.string.home_dialog_uninstall_ap_only_desc,
        icon = Icons.Outlined.Delete
    ),
    FULL(
        titleRes = R.string.home_dialog_uninstall_all,
        descRes = R.string.home_dialog_uninstall_all_desc,
        icon = Icons.Outlined.DeleteForever
    ),
}

@Destination<RootGraph>(start = true)
@Composable
fun HomeScreen(navigator: DestinationsNavigator) {
    var showPatchFloatAction by remember { mutableStateOf(true) }

    val kpState by APApplication.kpStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val apState by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)

    SideEffect {
        if (kpState != APApplication.State.UNKNOWN_STATE) {
            showPatchFloatAction = false
        }
    }

    var homeLayout by remember { mutableStateOf(APApplication.sharedPreferences.getString("home_layout_style", "circle")) }
    val homeRefreshObserver by refreshTheme.observeAsState(false)
    if (homeRefreshObserver) {
        homeLayout = APApplication.sharedPreferences.getString("home_layout_style", "circle")
    }

    Scaffold(topBar = {
        TopBar(onInstallClick = dropUnlessResumed {
            navigator.navigate(InstallModeSelectScreenDestination)
        }, navigator, kpState)
    }) { innerPadding ->
        when (homeLayout) {
            "kernelsu" -> HomeScreenV2(innerPadding, navigator, kpState, apState)
            "focus" -> HomeScreenV3(innerPadding, navigator, kpState, apState)
            "sign" -> HomeScreenSign(innerPadding, navigator, kpState, apState)
            "circle" -> HomeScreenCircle(innerPadding, navigator, kpState, apState)
            "dashboard_ui" -> HomeScreenV4(innerPadding, navigator, kpState, apState)
            "stats" -> HomeScreenStats(innerPadding, navigator, kpState, apState)
            else -> HomeScreenV1(innerPadding, navigator, kpState, apState)
        }
    }
}

@Composable
fun HomeScreenV1(
    innerPadding: PaddingValues,
    navigator: DestinationsNavigator,
    kpState: APApplication.State,
    apState: APApplication.State
) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(0.dp))
        KStatusCard(kpState, apState, navigator)
        if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
            AStatusCard(apState)
        }
        ListInfoCard(kpState, apState)
        val hideApatchCard = APApplication.sharedPreferences.getBoolean("hide_apatch_card", false)
        if (!hideApatchCard) {
            LearnMoreCard()
        }
        HomeBottomSpacer()
    }
}

@Composable
fun HomeScreenSign(
    innerPadding: PaddingValues,
    navigator: DestinationsNavigator,
    kpState: APApplication.State,
    apState: APApplication.State
) {
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(0.dp))
        KStatusCard(kpState, apState, navigator)
        if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
            AStatusCard(apState)
        }
        SignInfoCard(kpState, apState)
        val hideApatchCard = APApplication.sharedPreferences.getBoolean("hide_apatch_card", false)
        if (!hideApatchCard) {
            LearnMoreCard()
        }
        HomeBottomSpacer()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UninstallDialog(showDialog: MutableState<Boolean>, navigator: DestinationsNavigator) {
    if (!showDialog.value) return

    val options = remember { listOf(ApatchUninstallOption.PATCH_ONLY, ApatchUninstallOption.FULL) }
    var selectedOption by remember { mutableStateOf<ApatchUninstallOption?>(null) }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = {
                Text(
                    text = stringResource(R.string.home_dialog_uninstall_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    options.forEach { option ->
                        val isSelected = selectedOption == option
                        val backgroundColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                        val subtitleColor = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(backgroundColor)
                                .clickable { selectedOption = option }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(option.titleRes),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = stringResource(option.descRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = subtitleColor,
                                )
                            }
                            Icon(
                                imageVector = if (isSelected) {
                                    Icons.Filled.RadioButtonChecked
                                } else {
                                    Icons.Filled.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
                SideEffect {
                    APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (selectedOption) {
                            ApatchUninstallOption.PATCH_ONLY -> {
                                showDialog.value = false
                                APApplication.uninstallApatch()
                            }

                            ApatchUninstallOption.FULL -> {
                                showDialog.value = false
                                APApplication.uninstallApatch()
                                navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.UNPATCH))
                            }

                            null -> Unit
                        }
                    },
                    enabled = selectedOption != null,
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 4.dp,
        )
    }
}

private data class RebootOption(
    @param:StringRes val titleRes: Int,
    val reason: String,
    val icon: ImageVector
)

@Composable
private fun getRebootOptions(): List<RebootOption> = listOf(
    RebootOption(R.string.reboot, "", Icons.Filled.Refresh),
    RebootOption(R.string.reboot_recovery, "recovery", Icons.Outlined.SystemUpdate),
    RebootOption(R.string.reboot_bootloader, "bootloader", Icons.Outlined.Memory),
    RebootOption(R.string.reboot_download, "download", Icons.Outlined.Download),
    RebootOption(R.string.reboot_edl, "edl", Icons.Outlined.DeveloperMode),
    RebootOption(R.string.reboot_fastbootd, "fastboot", Icons.Outlined.RestartAlt),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RebootDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onReboot: (String) -> Unit
) {
    if (!show) return

    val options = getRebootOptions()

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    options.forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            onClick = {
                                onDismiss()
                                onReboot(option.reason)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(option.titleRes),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onInstallClick: () -> Unit, navigator: DestinationsNavigator, kpState: APApplication.State
) {
    val uriHandler = LocalUriHandler.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDropdownMoreOptions by remember { mutableStateOf(false) }
    var showDropdownReboot by remember { mutableStateOf(false) }
    val prefs = APApplication.sharedPreferences
    val darkThemeFollowSys = prefs.getBoolean("night_mode_follow_sys", false)
    val nightModeEnabled = prefs.getBoolean("night_mode_enabled", true)
    val isDarkTheme = if (darkThemeFollowSys) {
        isSystemInDarkTheme()
    } else {
        nightModeEnabled
    }
    
    val currentTitle = prefs.getString("app_title", "folkpatch") ?: "folkpatch"
    val customAppTitle = prefs.getString("custom_app_title", "FolkPatch") ?: "FolkPatch"
    val isCustomTitle = currentTitle == "custom"
    val titleResId = when (currentTitle) {
        "custom" -> null
        "fpatch" -> R.string.app_title_fpatch
        "apatch_folk" -> R.string.app_title_apatch_folk
        "apatchx" -> R.string.app_title_apatchx
        "apatch" -> R.string.app_title_apatch
        "kernelpatch" -> R.string.app_title_kernelpatch
        "kernelsu" -> R.string.app_title_kernelsu
        "supersu" -> R.string.app_title_supersu
        "folksu" -> R.string.app_title_fpatch
        "superuser" -> R.string.app_title_superuser
        "superpatch" -> R.string.app_title_superpatch
        "magicpatch" -> R.string.app_title_magicpatch
        else -> R.string.app_title_folkpatch
    }

    val useAdvancedTitleStyle = BackgroundConfig.isAdvancedTitleStyleEnabled && 
                                !BackgroundConfig.titleImageUri.isNullOrEmpty()
    val titleOpacity = if (useAdvancedTitleStyle) {
        BackgroundConfig.getEffectiveTitleImageOpacity(isDarkTheme)
    } else 1f
    val titleDim = if (useAdvancedTitleStyle) {
        BackgroundConfig.getEffectiveTitleImageDim(isDarkTheme)
    } else 0f
    val titleOffsetX = if (useAdvancedTitleStyle) {
        BackgroundConfig.titleImageOffsetX * 100f
    } else 0f

    TopAppBar(title = {
        if (useAdvancedTitleStyle) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(BackgroundConfig.titleImageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = titleResId?.let { stringResource(it) } ?: customAppTitle,
                modifier = Modifier
                    .height(40.dp)
                    .offset(x = titleOffsetX.dp)
                    .alpha(titleOpacity)
                    .graphicsLayer {
                        if (titleDim > 0f) {
                            colorFilter = ColorFilter.colorMatrix(
                                ColorMatrix().apply {
                                    setToScale(
                                        1f - titleDim,
                                        1f - titleDim,
                                        1f - titleDim,
                                        1f
                                    )
                                }
                            )
                        }
                    },
                contentScale = ContentScale.Fit
            )
        } else {
            Text(if (isCustomTitle) customAppTitle else stringResource(titleResId!!))
        }
    }, actions = {
        if (MusicConfig.isMusicEnabled) {
            val isPlaying by MusicManager.isPlaying.collectAsState()
            IconButton(onClick = { MusicManager.toggle() }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Music Control"
                )
            }
        }

        IconButton(onClick = onInstallClick) {
            Icon(
                imageVector = Icons.Filled.AutoFixHigh,
                contentDescription = stringResource(id = R.string.mode_select_page_title)
            )
        }

        if (kpState != APApplication.State.UNKNOWN_STATE) {
            IconButton(onClick = { showDropdownReboot = true }) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = stringResource(id = R.string.reboot)
                )
            }

            RebootDialog(
                show = showDropdownReboot,
                onDismiss = { showDropdownReboot = false },
                onReboot = { reason -> reboot(reason) }
            )
        }

        Box {
            IconButton(onClick = { showDropdownMoreOptions = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(id = R.string.settings)
                )
                WallpaperAwareDropdownMenu(
                    expanded = showDropdownMoreOptions,
                    onDismissRequest = { showDropdownMoreOptions = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.home_more_menu_feedback_or_suggestion)) },
                        onClick = {
                            showDropdownMoreOptions = false
                            uriHandler.openUri("https://github.com/LyraVoid/FolkPatch/issues/new/choose")
                        }
                    )
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.home_more_menu_about)) },
                        onClick = {
                            navigator.navigate(AboutScreenDestination)
                            showDropdownMoreOptions = false
                        }
                    )
                }
            }
        }
    })
}

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        color = containerColor.copy(alpha = 1f),
        shape = RoundedCornerShape(4.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 1f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun KStatusCard(
    kpState: APApplication.State, apState: APApplication.State, navigator: DestinationsNavigator
) {

    val showUninstallDialog = remember { mutableStateOf(false) }
    if (showUninstallDialog.value) {
        UninstallDialog(showDialog = showUninstallDialog, navigator)
    }

    val prefs = APApplication.sharedPreferences
    
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
    
    val darkThemeFollowSys = prefs.getBoolean("night_mode_follow_sys", false)
    val nightModeEnabled = prefs.getBoolean("night_mode_enabled", true)
    val isDarkTheme = if (darkThemeFollowSys) {
        isSystemInDarkTheme()
    } else {
        nightModeEnabled
    }

    val (cardBackgroundColor, cardContentColor) = when (kpState) {
        APApplication.State.KERNELPATCH_INSTALLED -> {
            if (BackgroundConfig.isCustomBackgroundEnabled) {
                val opacity = BackgroundConfig.customBackgroundOpacity
                val contentColor = if (opacity <= 0.1f) {
                    if (isDarkTheme) Color.White else Color.Black
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
                MaterialTheme.colorScheme.primary.copy(alpha = opacity) to contentColor
            } else {
                MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
            }
        }

        APApplication.State.KERNELPATCH_NEED_UPDATE, APApplication.State.KERNELPATCH_NEED_REBOOT -> {
            if (BackgroundConfig.isCustomBackgroundEnabled) {
                MaterialTheme.colorScheme.secondary.copy(alpha = BackgroundConfig.customBackgroundOpacity) to MaterialTheme.colorScheme.onSecondary
            } else {
                MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
            }
        }

        else -> {
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) to MaterialTheme.colorScheme.onSurface
        }
    }

    Card(
        onClick = {
            if (kpState != APApplication.State.KERNELPATCH_INSTALLED) {
                navigator.navigate(InstallModeSelectScreenDestination)
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor,
            contentColor = cardContentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (kpState == APApplication.State.KERNELPATCH_NEED_UPDATE) {
                Row {
                    Text(
                        text = stringResource(R.string.kernel_patch),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (kpState) {
                    APApplication.State.KERNELPATCH_INSTALLED -> {
                        Icon(Icons.Filled.CheckCircle, stringResource(R.string.home_working))
                    }

                    APApplication.State.KERNELPATCH_NEED_UPDATE, APApplication.State.KERNELPATCH_NEED_REBOOT -> {
                        Icon(Icons.Outlined.SystemUpdate, stringResource(R.string.home_kp_need_update))
                    }

                    else -> {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, "Unknown")
                    }
                }
                Column(
                    Modifier
                        .weight(2f)
                        .padding(start = 16.dp)
                ) {
                    when (kpState) {
                        APApplication.State.KERNELPATCH_INSTALLED -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (BackgroundConfig.isListWorkingCardModeHidden) {
                                        stringResource(R.string.home_working) + "😋"
                                    } else {
                                        stringResource(R.string.home_working)
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (!BackgroundConfig.isListWorkingCardModeHidden) {
                                    Spacer(Modifier.width(8.dp))
                                    StatusBadge(
                                        text = BackgroundConfig.getCustomBadgeText() ?: if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) "Full" else "Half"
                                    )
                                }
                            }
                        }

                        APApplication.State.KERNELPATCH_NEED_UPDATE, APApplication.State.KERNELPATCH_NEED_REBOOT -> {
                            Text(
                                text = stringResource(R.string.home_kp_need_update),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.kpatch_version_update,
                                    Version.installedKPVString(),
                                    Version.buildKPVString()
                                ), style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        else -> {
                            Text(
                                text = stringResource(R.string.home_install_unknown),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.home_install_unknown_summary),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if (kpState != APApplication.State.UNKNOWN_STATE && kpState != APApplication.State.KERNELPATCH_NEED_UPDATE && kpState != APApplication.State.KERNELPATCH_NEED_REBOOT) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${Version.installedKPVString()} (${managerVersion.second})" + if (BackgroundConfig.isListWorkingCardModeHidden) " - " + (if (apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED) "Full" else "KernelPatch") else "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    val onAction = {
                        when (kpState) {
                            APApplication.State.UNKNOWN_STATE -> {
                                navigator.navigate(InstallModeSelectScreenDestination)
                            }

                            APApplication.State.KERNELPATCH_NEED_UPDATE -> {
                                // todo: remove legacy compact for kp < 0.9.0
                                if (Version.installedKPVUInt() < 0x900u) {
                                    navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_ONLY))
                                } else {
                                    navigator.navigate(InstallModeSelectScreenDestination)
                                }
                            }

                            APApplication.State.KERNELPATCH_NEED_REBOOT -> {
                                reboot()
                            }

                            APApplication.State.KERNELPATCH_UNINSTALLING -> {
                                // Do nothing
                            }

                            else -> {
                                if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) {
                                    showUninstallDialog.value = true
                                } else {
                                    navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.UNPATCH))
                                }
                            }
                        }
                    }

                    Button(onClick = { onAction() }, colors = if (BackgroundConfig.isCustomBackgroundEnabled && kpState == APApplication.State.KERNELPATCH_INSTALLED) {
                        val opacity = BackgroundConfig.customBackgroundOpacity
                        val contentColor = if (opacity <= 0.1f) {
                            if (isDarkTheme) Color.White else Color.Black
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        }
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = opacity),
                            contentColor = contentColor
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }, content = {
                        when (kpState) {
                            APApplication.State.UNKNOWN_STATE -> {
                                Text(text = stringResource(id = R.string.home_ap_cando_install))
                            }

                            APApplication.State.KERNELPATCH_NEED_UPDATE -> {
                                Text(text = stringResource(id = R.string.home_kp_cando_update))
                            }

                            APApplication.State.KERNELPATCH_NEED_REBOOT -> {
                                Text(text = stringResource(id = R.string.home_ap_cando_reboot))
                            }

                            APApplication.State.KERNELPATCH_UNINSTALLING -> {
                                Icon(Icons.Outlined.Cached, contentDescription = "busy")
                            }

                            else -> {
                                Text(text = stringResource(id = R.string.home_ap_cando_uninstall))
                            }
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun AStatusCard(apState: APApplication.State) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = run {
            if (BackgroundConfig.isCustomBackgroundEnabled) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = BackgroundConfig.customBackgroundOpacity)
            } else {
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            }
        })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(R.string.android_patch),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (apState) {
                    APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                        Icon(Icons.Outlined.Block, stringResource(R.string.home_not_installed))
                    }

                    APApplication.State.ANDROIDPATCH_INSTALLING -> {
                        Icon(Icons.Outlined.InstallMobile, stringResource(R.string.home_installing))
                    }

                    APApplication.State.ANDROIDPATCH_INSTALLED -> {
                        Icon(Icons.Outlined.CheckCircle, stringResource(R.string.home_working))
                    }

                    APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                        Icon(Icons.Outlined.SystemUpdate, stringResource(R.string.home_kp_need_update))
                    }

                    else -> {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            stringResource(R.string.home_install_unknown)
                        )
                    }
                }
                Column(
                    Modifier
                        .weight(2f)
                        .padding(start = 16.dp)
                ) {

                    when (apState) {
                        APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                            Text(
                                text = stringResource(R.string.home_not_installed),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        APApplication.State.ANDROIDPATCH_INSTALLING -> {
                            Text(
                                text = stringResource(R.string.home_installing),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        APApplication.State.ANDROIDPATCH_INSTALLED -> {
                            Text(
                                text = stringResource(R.string.home_working),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                            Text(
                                text = stringResource(R.string.home_kp_need_update),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        else -> {
                            Text(
                                text = stringResource(R.string.home_install_unknown),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                if (apState != APApplication.State.UNKNOWN_STATE) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Button(onClick = {
                            when (apState) {
                                APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                                    APApplication.installApatch()
                                }

                                APApplication.State.ANDROIDPATCH_UNINSTALLING -> {
                                    // Do nothing
                                }

                                APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                                    APApplication.installApatch()
                                }

                                else -> {
                                    APApplication.uninstallApatch()
                                }
                            }
                        }, content = {
                            when (apState) {
                                APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                                    Text(text = stringResource(id = R.string.home_ap_cando_install))
                                }

                                APApplication.State.ANDROIDPATCH_UNINSTALLING -> {
                                    Icon(Icons.Outlined.Cached, contentDescription = "busy")
                                }

                                APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                                    Text(text = stringResource(id = R.string.home_kp_cando_update))
                                }

                                else -> {
                                    Text(text = stringResource(id = R.string.home_ap_cando_uninstall))
                                }
                            }
                        })
                    }
                }
            }
        }
    }
}


@Composable
fun WarningCard() {
    var show by rememberSaveable { mutableStateOf(apApp.getBackupWarningState()) }
    if (show) {
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ), colors = CardDefaults.elevatedCardColors(containerColor = run {
                MaterialTheme.colorScheme.error
            })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = "warning")
                }
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.patch_warnning),
                        )

                        Spacer(Modifier.width(12.dp))

                        Icon(
                            Icons.Outlined.Clear,
                            contentDescription = "",
                            modifier = Modifier.clickable {
                                show = false
                                apApp.updateBackupWarningState(false)
                            },
                        )
                    }
                }
            }
        }
    }
}

fun getSystemVersion(): String {
    return "${Build.VERSION.RELEASE} ${if (Build.VERSION.PREVIEW_SDK_INT != 0) "Preview" else ""} (API ${Build.VERSION.SDK_INT})"
}

fun getDeviceInfo(): String {
    var manufacturer =
        Build.MANUFACTURER[0].uppercaseChar().toString() + Build.MANUFACTURER.substring(1)
    if (!Build.BRAND.equals(Build.MANUFACTURER, ignoreCase = true)) {
        manufacturer += " " + Build.BRAND[0].uppercaseChar() + Build.BRAND.substring(1)
    }
    manufacturer += " " + Build.MODEL + " "
    return manufacturer
}

@Composable
fun InfoCard(kpState: APApplication.State, apState: APApplication.State) {
    // 隐藏设定状态
    val hideSuPath = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_su_path", false)) }
    val hideKpatchVersion = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_kpatch_version", false)) }
    val hideFingerprint = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_fingerprint", false)) }
    val hideZygisk = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_zygisk", false)) }
    val hideMount = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_mount", false)) }

    var zygiskImplement by remember { mutableStateOf("None") }
    var mountImplement by remember { mutableStateOf("None") }
    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                zygiskImplement = me.bmax.apatch.util.getZygiskImplement()
                mountImplement = me.bmax.apatch.util.getMountImplement()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            val contents = StringBuilder()
            val uname = Os.uname()

            @Composable
            fun InfoCardItem(label: String, content: String) {
                contents.appendLine(label).appendLine(content).appendLine()
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                Text(text = content, style = MaterialTheme.typography.bodyMedium)
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && !hideKpatchVersion.value) {
                InfoCardItem(
                    stringResource(R.string.home_kpatch_version), Version.installedKPVString()
                )

                Spacer(Modifier.height(16.dp))
            }
            
            if (kpState != APApplication.State.UNKNOWN_STATE && !hideSuPath.value) {
                InfoCardItem(stringResource(R.string.home_su_path), Natives.suPath())

                Spacer(Modifier.height(16.dp))
            }

            if (apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED) {
                InfoCardItem(
                    stringResource(R.string.home_apatch_version), managerVersion.second.toString()
                )
                Spacer(Modifier.height(16.dp))
            }

            InfoCardItem(stringResource(R.string.home_device_info), getDeviceInfo())

            Spacer(Modifier.height(16.dp))
            InfoCardItem(stringResource(R.string.home_kernel), uname.release)

            Spacer(Modifier.height(16.dp))
            InfoCardItem(stringResource(R.string.home_system_version), getSystemVersion())

            Spacer(Modifier.height(16.dp))
            if (!hideFingerprint.value) {
                InfoCardItem(stringResource(R.string.home_fingerprint), Build.FINGERPRINT)

                Spacer(Modifier.height(16.dp))
            }
            
            if (kpState != APApplication.State.UNKNOWN_STATE && zygiskImplement != "None" && !hideZygisk.value) {
                InfoCardItem(stringResource(R.string.home_zygisk_implement), zygiskImplement)

                Spacer(Modifier.height(16.dp))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && mountImplement != "None" && !hideMount.value) {
                InfoCardItem(stringResource(R.string.home_mount_implement), mountImplement)

                Spacer(Modifier.height(16.dp))
            }

            InfoCardItem(stringResource(R.string.home_selinux_status), getSELinuxStatus())

        }
    }
}

@Composable
fun ListInfoCard(kpState: APApplication.State, apState: APApplication.State) {
    val hideSuPath = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_su_path", false)) }
    val hideKpatchVersion = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_kpatch_version", false)) }
    val hideFingerprint = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_fingerprint", false)) }
    val hideZygisk = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_zygisk", false)) }
    val hideMount = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_mount", false)) }

    var zygiskImplement by remember { mutableStateOf("None") }
    var mountImplement by remember { mutableStateOf("None") }
    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                zygiskImplement = me.bmax.apatch.util.getZygiskImplement()
                mountImplement = me.bmax.apatch.util.getMountImplement()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            val uname = Os.uname()

            @Composable
            fun InfoCardItem(label: String, content: String) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                Text(text = content, style = MaterialTheme.typography.bodyMedium)
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && !hideKpatchVersion.value) {
                InfoCardItem(stringResource(R.string.home_kpatch_version), Version.installedKPVString())
                Spacer(Modifier.height(16.dp))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && !hideSuPath.value) {
                InfoCardItem(stringResource(R.string.home_su_path), Natives.suPath())
                Spacer(Modifier.height(16.dp))
            }

            if (apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED) {
                InfoCardItem(stringResource(R.string.home_apatch_version), managerVersion.second.toString())
                Spacer(Modifier.height(16.dp))
            }

            InfoCardItem(stringResource(R.string.home_device_info), getDeviceInfo())
            Spacer(Modifier.height(16.dp))

            InfoCardItem(stringResource(R.string.home_kernel), uname.release)
            Spacer(Modifier.height(16.dp))

            InfoCardItem(stringResource(R.string.home_system_version), getSystemVersion())
            Spacer(Modifier.height(16.dp))

            if (!hideFingerprint.value) {
                InfoCardItem(stringResource(R.string.home_fingerprint), Build.FINGERPRINT)
                Spacer(Modifier.height(16.dp))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && zygiskImplement != "None" && !hideZygisk.value) {
                InfoCardItem(stringResource(R.string.home_zygisk_implement), zygiskImplement)
                Spacer(Modifier.height(16.dp))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && mountImplement != "None" && !hideMount.value) {
                InfoCardItem(stringResource(R.string.home_mount_implement), mountImplement)
                Spacer(Modifier.height(16.dp))
            }

            InfoCardItem(stringResource(R.string.home_selinux_status), getSELinuxStatus())
        }
    }
}

@Composable
fun SignInfoCard(kpState: APApplication.State, apState: APApplication.State) {
    val hideSuPath = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_su_path", false)) }
    val hideKpatchVersion = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_kpatch_version", false)) }
    val hideFingerprint = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_fingerprint", false)) }
    val hideZygisk = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_zygisk", false)) }
    val hideMount = remember { mutableStateOf(APApplication.sharedPreferences.getBoolean("hide_mount", false)) }

    var zygiskImplement by remember { mutableStateOf("None") }
    var mountImplement by remember { mutableStateOf("None") }
    LaunchedEffect(Unit) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                zygiskImplement = me.bmax.apatch.util.getZygiskImplement()
                mountImplement = me.bmax.apatch.util.getMountImplement()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            val contents = StringBuilder()
            val uname = Os.uname()

            @Composable
            fun InfoCardItem(icon: ImageVector, label: String, content: String) {
                contents.appendLine(label).appendLine(content).appendLine()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        Text(text = content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && !hideKpatchVersion.value) {
                InfoCardItem(
                    Icons.Outlined.Extension,
                    stringResource(R.string.home_kpatch_version),
                    Version.installedKPVString()
                )
                Spacer(Modifier.height(16.dp))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && !hideSuPath.value) {
                InfoCardItem(
                    Icons.Outlined.Code,
                    stringResource(R.string.home_su_path),
                    Natives.suPath()
                )
                Spacer(Modifier.height(16.dp))
            }

            if (apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED) {
                InfoCardItem(
                    Icons.Outlined.Android,
                    stringResource(R.string.home_apatch_version),
                    managerVersion.second.toString()
                )
                Spacer(Modifier.height(16.dp))
            }

            InfoCardItem(Icons.Outlined.PhoneAndroid, stringResource(R.string.home_device_info), getDeviceInfo())
            Spacer(Modifier.height(16.dp))

            InfoCardItem(Icons.Outlined.DeveloperBoard, stringResource(R.string.home_kernel), uname.release)
            Spacer(Modifier.height(16.dp))

            InfoCardItem(Icons.Outlined.Info, stringResource(R.string.home_system_version), getSystemVersion())
            Spacer(Modifier.height(16.dp))

            if (!hideFingerprint.value) {
                InfoCardItem(Icons.Filled.Fingerprint, stringResource(R.string.home_fingerprint), Build.FINGERPRINT)
                Spacer(Modifier.height(16.dp))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && zygiskImplement != "None" && !hideZygisk.value) {
                InfoCardItem(Icons.Outlined.Layers, stringResource(R.string.home_zygisk_implement), zygiskImplement)
                Spacer(Modifier.height(16.dp))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && mountImplement != "None" && !hideMount.value) {
                InfoCardItem(Icons.Outlined.SdStorage, stringResource(R.string.home_mount_implement), mountImplement)
                Spacer(Modifier.height(16.dp))
            }

            InfoCardItem(Icons.Outlined.Shield, stringResource(R.string.home_selinux_status), getSELinuxStatus())
        }
    }
}

@Composable
fun WarningCard(
    message: String, color: Color = MaterialTheme.colorScheme.error, onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = color
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (BackgroundConfig.isCustomBackgroundEnabled) 0.dp else 6.dp
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(onClick?.let { Modifier.clickable { it() } } ?: Modifier)
                .padding(24.dp)) {
            Text(
                text = message, style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun LearnMoreCard() {
    val uriHandler = LocalUriHandler.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    uriHandler.openUri("https://fp.mysqil.com/")
                }
                .padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = stringResource(R.string.home_learn_apatch),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_click_to_learn_apatch),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
