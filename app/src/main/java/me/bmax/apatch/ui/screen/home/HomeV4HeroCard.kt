package me.bmax.apatch.ui.screen.home

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.ui.theme.LocalWallpaperContentColor
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.ui.showToast
import me.bmax.apatch.ui.theme.tokens.FolkShape

private val managerVersion = getManagerVersion()

@Composable
fun HeroStatusCard(
    kpState: APApplication.State,
    apState: APApplication.State,
    navigator: DestinationsNavigator,
    showUninstallDialog: MutableState<Boolean>,
    showInstallDialog: MutableState<Boolean>,
    isWallpaperMode: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isWorking = kpState == APApplication.State.KERNELPATCH_INSTALLED
    val isUpdate = kpState == APApplication.State.KERNELPATCH_NEED_UPDATE || 
        kpState == APApplication.State.KERNELPATCH_NEED_REBOOT
    val isUnknown = kpState == APApplication.State.UNKNOWN_STATE

    val jailbreakState = LocalHomeJailbreakState.current
    val isJailbreak = jailbreakState.isActive
    val isPermissive = jailbreakState.isPermissive

    val wallpaperEnabled = BackgroundConfig.isDashboardCardBackgroundEnabled
    val wallpaperUri = BackgroundConfig.dashboardCardBgUri
    val hasWallpaper = wallpaperEnabled && !wallpaperUri.isNullOrEmpty()
    val prefs = APApplication.sharedPreferences
    val isDarkTheme = if (prefs.getBoolean("night_mode_follow_sys", false)) {
        isSystemInDarkTheme()
    } else {
        prefs.getBoolean("night_mode_enabled", true)
    }
    val wallpaperDim = BackgroundConfig.getEffectiveDashboardCardBgDim(isDarkTheme)
    val wallpaperOpacity = BackgroundConfig.getEffectiveDashboardCardBgOpacity(isDarkTheme)
    var showBackgroundOptions by remember { mutableStateOf(false) }
    val pickBackground = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val success = BackgroundManager.saveAndApplyDashboardCardBackground(context, it)
                showToast(context, if (success) R.string.dashboard_card_background_saved else R.string.dashboard_card_background_error)
            }
        }
    }
    val clearBackgroundDialog = rememberConfirmDialog(
        onConfirm = {
            BackgroundManager.clearDashboardCardBackground(context)
            showToast(context, context.getString(R.string.dashboard_card_background_cleared))
        }
    )

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
            isJailbreak -> MaterialTheme.colorScheme.tertiaryContainer
            isWorking -> MaterialTheme.colorScheme.primary
            isUpdate -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.errorContainer
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "containerColor"
    )

    // 壁纸模式下卡片容器被 customBackgroundOpacity 压到近乎透明，壁纸直接透出，
    // 此时 onPrimary/onTertiaryContainer 不再匹配实际背景，改用随壁纸明暗取反的中性色。
    val wallpaperContentColor = LocalWallpaperContentColor.current
    val contentColor by animateColorAsState(
        targetValue = when {
            hasWallpaper -> Color.White
            wallpaperContentColor != null -> wallpaperContentColor
            isJailbreak -> MaterialTheme.colorScheme.onTertiaryContainer
            isWorking -> MaterialTheme.colorScheme.onPrimary
            isUpdate -> MaterialTheme.colorScheme.onSecondary
            else -> MaterialTheme.colorScheme.onErrorContainer
        },
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "contentColor"
    )

    // 渐变背景
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            containerColor.copy(
                alpha = (if (isWorking) breathAlpha else 1f) *
                    if (BackgroundConfig.isCustomBackgroundEnabled) BackgroundConfig.customBackgroundOpacity else 1f
            ),
            containerColor.copy(
                alpha = 0.8f *
                    if (BackgroundConfig.isCustomBackgroundEnabled) BackgroundConfig.customBackgroundOpacity else 1f
            )
        )
    )

    val classicEmojiEnabled = BackgroundConfig.isListWorkingCardModeHidden
    val isFull = apState == APApplication.State.ANDROIDPATCH_INSTALLED
    val modeText = BackgroundConfig.getCustomBadgeText() ?: if (isFull) "Kpm" else "Half"

    if (isWorking || isJailbreak) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .then(if (wallpaperEnabled) Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { showBackgroundOptions = true })
                } else Modifier),
            shape = FolkShape.Corner24,
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent,
                contentColor = contentColor
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!hasWallpaper) Modifier.background(gradientBrush) else Modifier)
            ) {
                if (hasWallpaper) {
                    Image(
                        painter = rememberAsyncImagePainter(wallpaperUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().alpha(wallpaperOpacity),
                    )
                    Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = wallpaperDim)))
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
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
                                imageVector = if (isJailbreak) Icons.Outlined.LockOpen else Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = contentColor
                            )

                            Spacer(Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (isJailbreak) {
                                        stringResource(R.string.settings_jailbreak_mode)
                                    } else if (classicEmojiEnabled) {
                                        stringResource(R.string.home_working) + "😋"
                                    } else {
                                        stringResource(R.string.home_working)
                                    },
                                    style = MaterialTheme.typography.headlineSmallEmphasized,
                                )

                                if (isJailbreak) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.settings_jailbreak_mode_summary),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = contentColor
                                    )
                                } else if (!classicEmojiEnabled) {
                                    Spacer(Modifier.height(4.dp))
                                    ModeLabelChip(label = modeText, contentColor = contentColor)
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        if (isJailbreak) {
                            IconButton(
                                onClick = jailbreakState::performPrimaryAction,
                                enabled = !jailbreakState.isTriggering,
                                colors = IconButtonDefaults.iconButtonColors(contentColor = contentColor),
                            ) {
                                if (jailbreakState.isTriggering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = contentColor,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.RestartAlt,
                                        contentDescription = stringResource(R.string.reboot_soft),
                                    )
                                }
                            }
                        } else {
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
                    }

                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(
                        color = contentColor.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                    if (isUnknown && isPermissive) {
                        jailbreakState.performPrimaryAction()
                    } else {
                        navigator.navigate(InstallModeSelectScreenDestination)
                    }
                },
            shape = FolkShape.Corner20,
            colors = CardDefaults.cardColors(
                containerColor = finalContainerColor,
                contentColor = wallpaperContentColor ?: MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isJailbreak -> Icon(
                        imageVector = Icons.Outlined.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
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

                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            isJailbreak -> stringResource(R.string.settings_jailbreak_mode)
                            isUpdate -> stringResource(R.string.home_kp_need_update)
                            isUnknown -> stringResource(R.string.home_install_unknown)
                            else -> stringResource(R.string.home_not_installed)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            if (isUnknown && isPermissive) R.string.jailbreak else R.string.home_click_to_install
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (isUnknown && isPermissive) {
                    Spacer(Modifier.width(12.dp))
                    if (jailbreakState.isTriggering) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.LockOpen,
                            contentDescription = stringResource(R.string.jailbreak),
                        )
                    }
                }
            }
        }
    }

    if (wallpaperEnabled) {
        BackgroundOptionsDialog(
            showDialog = showBackgroundOptions,
            onDismiss = { showBackgroundOptions = false },
            title = stringResource(R.string.dashboard_card_background_title),
            selectLabel = stringResource(R.string.settings_select_background_image),
            clearLabel = stringResource(R.string.dashboard_card_background_clear),
            hasExisting = hasWallpaper,
            onSelectImage = {
                if (PermissionUtils.hasExternalStoragePermission(context)) {
                    try {
                        pickBackground.launch("image/*")
                    } catch (e: ActivityNotFoundException) {
                        showToast(context, e.message ?: "")
                    }
                } else {
                    showToast(context, context.getString(R.string.focus_card_permission_required))
                }
            },
            onClearImage = {
                clearBackgroundDialog.showConfirm(
                    title = context.getString(R.string.dashboard_card_background_clear),
                    content = context.getString(R.string.dashboard_card_background_clear_confirm),
                    markdown = false,
                )
            },
            // 新增：恢复默认壁纸
            onRestoreDefault = {
                val restored = BackgroundManager.provisionDefaultDashboardCardBg(context)
                val message = if (restored) {
                    R.string.dashboard_card_background_restored
                } else {
                    R.string.dashboard_card_background_error
                }
                showToast(context, context.getString(message))
            },
            restoreLabel = stringResource(R.string.dashboard_card_background_restore)
        )
    }
}

