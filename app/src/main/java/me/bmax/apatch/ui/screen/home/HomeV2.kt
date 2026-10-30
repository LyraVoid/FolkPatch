package me.bmax.apatch.ui.screen.home

import android.animation.ValueAnimator
import me.bmax.apatch.ui.theme.tokens.FolkColors
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import me.bmax.apatch.ui.theme.LocalWallpaperContentColor
import me.bmax.apatch.util.Version

import androidx.compose.ui.draw.alpha
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import android.os.Build
import me.bmax.apatch.apApp
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.ui.HomeBottomSpacer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.theme.tokens.FolkShape

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
            .padding(horizontal = FolkDimens.Size16)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
    ) {
        Spacer(Modifier.height(FolkDimens.Size0))
        
        // Top Section: Split into Left (Status) and Right (Details)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size12)
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
                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size12)
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
    
    // Colors
    val useCustomGridBg = BackgroundConfig.isGridWorkingCardBackgroundEnabled && !BackgroundConfig.gridWorkingCardBackgroundUri.isNullOrEmpty()

    // 壁纸模式下半透明容器让壁纸透出，语义色（onPrimary 等）不再匹配实际背景，
    // 改用随壁纸明暗取反的中性色；非壁纸模式为 null，回退到语义色。
    val wallpaperContentColor = LocalWallpaperContentColor.current

    val (baseContainerColor, baseContentColor) = if (isJailbreak) {
         val containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
             MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = BackgroundConfig.customBackgroundOpacity)
         } else {
             MaterialTheme.colorScheme.tertiaryContainer
         }
         containerColor to (wallpaperContentColor ?: MaterialTheme.colorScheme.onTertiaryContainer)
    } else if (BackgroundConfig.isCustomBackgroundEnabled) {
         val opacity = BackgroundConfig.customBackgroundOpacity
         val container = MaterialTheme.colorScheme.primary.copy(alpha = opacity)
         val content = wallpaperContentColor ?: MaterialTheme.colorScheme.onPrimary
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
    
    val containerColor = if (useCustomGridBg) FolkColors.Transparent else baseContainerColor
    // If using custom grid bg, force content color to white (or based on some logic), or keep base logic?
    // Let's assume white text for image background with dimming
    val contentColor = if (useCustomGridBg) FolkColors.MediaContent else baseContentColor

    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val reduceMotion = !ValueAnimator.areAnimatorsEnabled()
    var longPressActive by remember { mutableStateOf(false) }
    var petCount by remember { mutableIntStateOf(0) }
    val petBounce = remember { Animatable(0f) }
    val pressed by interactionSource.collectIsPressedAsState()
    val heldDeformation by animateFloatAsState(
        targetValue = if (useCustomGridBg && !reduceMotion) {
            if (longPressActive) 0.16f else if (pressed) 0.045f else 0f
        } else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f),
        label = "petHeldDeformation",
    )

    LaunchedEffect(petCount, reduceMotion, useCustomGridBg) {
        if (petCount > 0 && !reduceMotion && useCustomGridBg) {
            petBounce.snapTo(0.14f)
            petBounce.animateTo(0f, spring(dampingRatio = 0.28f, stiffness = 180f))
        } else {
            petBounce.snapTo(0f)
        }
    }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release || interaction is PressInteraction.Cancel) {
                longPressActive = false
            }
        }
    }

    Card(
        modifier = modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onLongClick = if (useCustomGridBg) {
                    { longPressActive = true }
                } else {
                    null
                },
                onClick = {
                    if (useCustomGridBg) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isWorking && !isJailbreak) petCount++
                    }
                    if (isJailbreak || kpState == APApplication.State.UNKNOWN_STATE && isPermissive) {
                        jailbreakState.performPrimaryAction()
                    } else {
                        onClick()
                    }
                }
            ),
        shape = FolkShape.Corner20,
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
                        .graphicsLayer {
                            val deformation = heldDeformation + petBounce.value
                            scaleX = 1f + deformation * 0.7f
                            scaleY = 1f - deformation
                        }
                )
                // Add a dim layer for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(FolkColors.MediaScrim.copy(alpha = BackgroundConfig.gridWorkingCardBackgroundDim))
                )
            }
            
            Box(modifier = Modifier.fillMaxSize().padding(FolkDimens.Size16)) {
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
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = contentColor
                        )
                    }
                    if (!isJailbreak && isWorking && !BackgroundConfig.isGridWorkingCardModeHidden) {
                        Spacer(Modifier.height(FolkDimens.Size4))
                        val customText = BackgroundConfig.getCustomBadgeText()
                        Text(
                            text = if (customText != null) "<$customText>" else if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) "<Full>" else "<Half>",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.8f)
                        )
                    }
                    if (isJailbreak && !BackgroundConfig.isGridWorkingCardTextHidden) {
                        Spacer(Modifier.height(FolkDimens.Size4))
                        Text(
                            text = stringResource(R.string.reboot_soft),
                            style = MaterialTheme.typography.labelMedium,
                            color = contentColor.copy(alpha = 0.78f),
                        )
                    } else if (kpState == APApplication.State.UNKNOWN_STATE && isPermissive) {
                        Spacer(Modifier.height(FolkDimens.Size4))
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
                        isJailbreak -> Icons.Outlined.LockOpen
                        isWorking -> Icons.Outlined.CheckCircle
                        else -> Icons.Outlined.Warning
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(FolkDimens.Size48),
                    tint = contentColor
                )
            }
            if (useCustomGridBg && reduceMotion) {
                ReducedMotionPressBorder(interactionSource, longPressActive, contentColor)
            }
        }
    }
}

@Composable
private fun BoxScope.ReducedMotionPressBorder(
    interactionSource: MutableInteractionSource,
    longPressActive: Boolean,
    color: Color,
) {
    val pressed by interactionSource.collectIsPressedAsState()
    if (pressed) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = if (longPressActive) FolkDimens.Size3 else FolkDimens.Size2,
                    color = color.copy(alpha = if (longPressActive) 1f else 0.8f),
                    shape = FolkShape.Corner20,
                )
        )
    }
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
    TonalCard(
        onClick = onClick,
        modifier = modifier,
        flat = true,
    ) {
        Column(
            modifier = Modifier.padding(FolkDimens.Size16),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(FolkDimens.Size16),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(FolkDimens.Size8))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(FolkDimens.Size4))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                 color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
