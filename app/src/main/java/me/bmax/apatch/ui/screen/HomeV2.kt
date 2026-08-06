package me.bmax.apatch.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.*
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.Version
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.ui.draw.alpha
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import android.os.Build
import me.bmax.apatch.apApp
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.ui.HomeBottomSpacer
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.ui.showToast

private val managerVersion = getManagerVersion()

@Composable
fun HomeScreenV2(
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
    
    val showUninstallDialog = remember { mutableStateOf(false) }

    if (showUninstallDialog.value) {
        UninstallDialog(showDialog = showUninstallDialog, navigator)
    }
    
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(0.dp))
        
        // Top Section: Split into Left (Status) and Right (Details)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Big Status Card
            StatusCardBig(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                kpState = kpState,
                apState = apState,
                onClick = {
                    when (kpState) {
                        APApplication.State.UNKNOWN_STATE -> navigator.navigate(InstallModeSelectScreenDestination)
                        APApplication.State.KERNELPATCH_NEED_UPDATE -> navigator.navigate(InstallModeSelectScreenDestination)
                        APApplication.State.KERNELPATCH_INSTALLED -> {} 
                        else -> navigator.navigate(InstallModeSelectScreenDestination)
                    }
                }
            )
            
            // Right: Two Small Cards
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Right: KP Version
                SmallInfoCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.kernel_patch),
                    value = if (kpState != APApplication.State.UNKNOWN_STATE) "${Version.installedKPVString()} (${managerVersion.second})" else "N/A",
                    icon = Icons.Outlined.Extension,
                    onClick = {
                        if (kpState == APApplication.State.KERNELPATCH_NEED_UPDATE) {
                             navigator.navigate(InstallModeSelectScreenDestination)
                        }
                    }
                )
                
                // Bottom Right: AP Version
                SmallInfoCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.android_patch),
                    value = when(apState) {
                        APApplication.State.ANDROIDPATCH_INSTALLED -> "Active"
                        APApplication.State.ANDROIDPATCH_NEED_UPDATE -> "Update"
                        APApplication.State.ANDROIDPATCH_INSTALLING -> "..."
                        else -> "Inactive"
                    },
                    icon = Icons.Outlined.Android,
                    onClick = {
                        if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) {
                            showUninstallDialog.value = true
                        } else if (apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED && kpState == APApplication.State.KERNELPATCH_INSTALLED) {
                            // Only allow install/uninstall if NOT in "Not Installed" state (per user request to disable click)
                            // Wait, if it is NOT installed, user wants NO trigger effect.
                            // So we only allow action if INSTALLED.
                            // But what about INSTALLING?
                            // User said: "When system patch (AP) is not installed... click... should be set to no trigger effect"
                            // So if apState == ANDROIDPATCH_NOT_INSTALLED -> No effect.
                            APApplication.installApatch()
                        }
                    }
                )
            }
        }

        
        // AndroidPatch Install Card (Only when not installed)
        if (kpState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_INSTALLED) {
            AStatusCard(apState)
        }
        
        // Info Card
        InfoCard(kpState, apState)
        
        // Learn More
        val hideApatchCard = APApplication.sharedPreferences.getBoolean("hide_apatch_card", false)
        if (!hideApatchCard) {
            LearnMoreCard()
        }
        
        HomeBottomSpacer()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusCardBig(
    modifier: Modifier = Modifier,
    kpState: APApplication.State,
    apState: APApplication.State,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isWorking = kpState == APApplication.State.KERNELPATCH_INSTALLED
    val isUpdate = kpState == APApplication.State.KERNELPATCH_NEED_UPDATE || kpState == APApplication.State.KERNELPATCH_NEED_REBOOT

    val jailbreakState = LocalHomeJailbreakState.current
    val isJailbreak = jailbreakState.isActive
    val isPermissive = jailbreakState.isPermissive
    
    val prefs = APApplication.sharedPreferences
    val darkThemeFollowSys = prefs.getBoolean("night_mode_follow_sys", false)
    val nightModeEnabled = prefs.getBoolean("night_mode_enabled", true)
    val isDark = if (darkThemeFollowSys) {
        isSystemInDarkTheme()
    } else {
        nightModeEnabled
    }
    
    // Colors
    val useCustomGridBg = BackgroundConfig.isGridWorkingCardBackgroundEnabled && !BackgroundConfig.gridWorkingCardBackgroundUri.isNullOrEmpty()
    
    val (baseContainerColor, baseContentColor) = if (isJailbreak) {
         val containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
             MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = BackgroundConfig.customBackgroundOpacity)
         } else {
             MaterialTheme.colorScheme.tertiaryContainer
         }
         containerColor to MaterialTheme.colorScheme.onTertiaryContainer
    } else if (BackgroundConfig.isCustomBackgroundEnabled) {
         val opacity = BackgroundConfig.customBackgroundOpacity
         val container = MaterialTheme.colorScheme.primary.copy(alpha = opacity)
         val content = if (opacity <= 0.1f) {
             if (isDark) Color.White else Color.Black
         } else {
             MaterialTheme.colorScheme.onPrimary
         }
         container to content
    } else {
        if (isWorking) {
             MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        } else if (isUpdate) {
             MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        } else {
             // Use secondaryContainer for Unknown/Not Installed (Fixed/Neutral color like original layout)
             MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        }
    }
    
    val containerColor = if (useCustomGridBg) Color.Transparent else baseContainerColor
    // If using custom grid bg, force content color to white (or based on some logic), or keep base logic?
    // Let's assume white text for image background with dimming
    val contentColor = if (useCustomGridBg) Color.White else baseContentColor

    // Long-press card options (only when working and card background feature enabled)
    val scope = rememberCoroutineScope()
    var showCardOptionsDialog by remember { mutableStateOf(false) }
    val isLongPressEnabled = isWorking && BackgroundConfig.isGridWorkingCardBackgroundEnabled

    val pickGridImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val success = BackgroundManager.saveAndApplyGridWorkingCardBackground(context, it)
                if (success) {
                    showToast(context, R.string.settings_grid_working_card_background_saved)
                } else {
                    showToast(context, R.string.settings_grid_working_card_background_error)
                }
            }
        }
    }

    val clearGridBackgroundDialog = rememberConfirmDialog(
        onConfirm = {
            scope.launch {
                BackgroundManager.clearGridWorkingCardBackground(context)
                showToast(context, context.getString(R.string.settings_grid_working_card_background_cleared))
            }
        }
    )

    Card(
        modifier = modifier
            .then(
                if (isLongPressEnabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (isJailbreak || kpState == APApplication.State.UNKNOWN_STATE && isPermissive) {
                                    jailbreakState.performPrimaryAction()
                                } else {
                                    onClick()
                                }
                            },
                            onLongPress = { showCardOptionsDialog = true }
                        )
                    }
                } else {
                    Modifier.clickable {
                        if (isJailbreak || kpState == APApplication.State.UNKNOWN_STATE && isPermissive) {
                            jailbreakState.performPrimaryAction()
                        } else {
                            onClick()
                        }
                    }
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (useCustomGridBg) {
                // Configure ImageLoader for GIF support explicitly
                val imageLoader = ImageLoader.Builder(context)
                    .components {
                        if (Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                    }
                    .build()

                val prefs = APApplication.sharedPreferences
                val darkThemeFollowSys = prefs.getBoolean("night_mode_follow_sys", false)
                val nightModeEnabled = prefs.getBoolean("night_mode_enabled", true)
                val isDarkTheme = if (darkThemeFollowSys) {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
                } else {
                    nightModeEnabled
                }

                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
                            .data(BackgroundConfig.gridWorkingCardBackgroundUri)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentScale = ContentScale.Crop
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(BackgroundConfig.getEffectiveGridBackgroundOpacity(isDarkTheme))
                )
                // Add a dim layer for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = BackgroundConfig.gridWorkingCardBackgroundDim))
                )
            }
            
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    if (!BackgroundConfig.isGridWorkingCardTextHidden) {
                        Text(
                            text = when {
                                isJailbreak -> stringResource(R.string.settings_jailbreak_mode)
                                kpState == APApplication.State.KERNELPATCH_INSTALLED -> stringResource(R.string.home_working)
                                kpState == APApplication.State.KERNELPATCH_NEED_UPDATE -> stringResource(R.string.home_kp_need_update)
                                kpState == APApplication.State.KERNELPATCH_NEED_REBOOT -> stringResource(R.string.home_ap_cando_reboot)
                                kpState == APApplication.State.UNKNOWN_STATE -> stringResource(R.string.home_install_unknown)
                                else -> stringResource(R.string.home_not_installed)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                    if (!isJailbreak && isWorking && !BackgroundConfig.isGridWorkingCardModeHidden) {
                        Spacer(Modifier.height(4.dp))
                        val customText = BackgroundConfig.getCustomBadgeText()
                        Text(
                            text = if (customText != null) "<$customText>" else if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) "<Full>" else "<Half>",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                    if (isJailbreak && !BackgroundConfig.isGridWorkingCardTextHidden) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.reboot_soft),
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor.copy(alpha = 0.78f),
                        )
                    } else if (kpState == APApplication.State.UNKNOWN_STATE && isPermissive) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.jailbreak),
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor.copy(alpha = 0.78f),
                        )
                    }
                } // End of Column
            }
            
            // Icon
            if (!BackgroundConfig.isGridWorkingCardCheckHidden) {
                Icon(
                    imageVector = when {
                        jailbreakState.isTriggering -> Icons.Outlined.RestartAlt
                        isJailbreak -> Icons.Filled.LockOpen
                        isWorking -> Icons.Filled.CheckCircle
                        else -> Icons.Filled.Warning
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp),
                    tint = contentColor
                )
            }
        }
    }

    // Card Options Dialog
    BackgroundOptionsDialog(
        showDialog = showCardOptionsDialog,
        onDismiss = { showCardOptionsDialog = false },
        title = stringResource(R.string.settings_grid_working_card_background),
        selectLabel = stringResource(R.string.settings_select_background_image),
        clearLabel = stringResource(R.string.settings_clear_grid_working_card_background),
        hasExisting = !BackgroundConfig.gridWorkingCardBackgroundUri.isNullOrEmpty(),
        onSelectImage = {
            if (PermissionUtils.hasExternalStoragePermission(context)) {
                try {
                    pickGridImageLauncher.launch("image/*")
                } catch (e: ActivityNotFoundException) {
                    showToast(context, e.message ?: "")
                }
            } else {
                showToast(context, context.getString(R.string.settings_background_permission_required))
            }
        },
        onClearImage = {
            clearGridBackgroundDialog.showConfirm(
                title = context.getString(R.string.settings_clear_grid_working_card_background),
                content = context.getString(R.string.settings_clear_grid_working_card_background_confirm),
                markdown = false,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallInfoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = BackgroundConfig.customBackgroundOpacity)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    }
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                 color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
