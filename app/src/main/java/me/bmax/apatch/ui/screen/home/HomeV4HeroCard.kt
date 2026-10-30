package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkColors
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
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
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.ui.showToast
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.component.folk.navOrigin

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HeroStatusCard(
    kpState: APApplication.State,
    apState: APApplication.State,
    navigator: DestinationsNavigator,
    showUninstallDialog: MutableState<Boolean>,
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

    val wallpaper = me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens.current
    var wallpaperReady by remember(wallpaperUri) { mutableStateOf(false) }
    val painter = rememberAsyncImagePainter(
        model = coil.request.ImageRequest.Builder(context)
            .data(wallpaperUri)
            .listener(
                onSuccess = { _, _ -> wallpaperReady = true },
                onError = { _, _ -> wallpaperReady = false },
            )
            .build(),
    )
    val overImage = hasWallpaper && wallpaperReady
    val contentColor = if (overImage) FolkColors.MediaContent else wallpaper?.group?.contentColor
        ?: MaterialTheme.colorScheme.onSurface
    val supportingColor = if (overImage) contentColor.copy(alpha = 0.8f) else wallpaper?.group?.supportingColor
        ?: MaterialTheme.colorScheme.onSurfaceVariant
    val classicEmojiEnabled = BackgroundConfig.isListWorkingCardModeHidden
    val isFull = apState == APApplication.State.ANDROIDPATCH_INSTALLED
    val modeText = BackgroundConfig.getCustomBadgeText() ?: if (isFull) "Kpm" else "Half"
    val stateText = if (isWorking && !classicEmojiEnabled) {
        "${stringResource(R.string.home_info_running_mode)}: $modeText"
    } else {
        null
    }
    val statusTitle = when {
        isJailbreak -> stringResource(R.string.settings_jailbreak_mode)
        isWorking -> stringResource(R.string.home_working) + if (classicEmojiEnabled) "😋" else ""
        isUpdate -> stringResource(R.string.home_kp_need_update)
        isUnknown -> stringResource(R.string.home_install_unknown)
        else -> stringResource(R.string.home_not_installed)
    }
    val summary = when {
        isJailbreak -> stringResource(R.string.settings_jailbreak_mode_summary)
        isWorking -> Version.installedKPVString()
        isUnknown && isPermissive -> stringResource(R.string.jailbreak)
        isUpdate -> stringResource(R.string.home_kp_cando_update)
        else -> stringResource(R.string.home_click_to_install)
    }
    val cardFill = wallpaper?.group?.fill
        ?: me.bmax.apatch.ui.theme.tokens.FolkTheme.palette.groupedSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FolkShape.Corner20)
            .navOrigin(
                cornerRadius = FolkDimens.Size20,
                color = cardFill,
            )
            .then(
                if (wallpaperEnabled || (!isWorking && !isJailbreak)) {
                    Modifier.combinedClickable(
                        onClick = {
                            if (!isWorking && !isJailbreak) {
                                if (isUnknown && isPermissive) jailbreakState.performPrimaryAction()
                                else navigator.navigate(InstallModeSelectScreenDestination)
                            }
                        },
                        onLongClick = if (wallpaperEnabled) ({ showBackgroundOptions = true }) else null,
                    )
                } else Modifier
            ),
        shape = FolkShape.Corner20,
        colors = CardDefaults.cardColors(
            containerColor = cardFill,
            contentColor = contentColor,
        ),
    ) {
        Box {
            if (hasWallpaper) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().alpha(wallpaperOpacity),
                )
                Box(Modifier.matchParentSize().background(FolkColors.MediaScrim.copy(alpha = wallpaperDim)))
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(FolkDimens.Size24),
                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size20),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when {
                            isJailbreak -> Icons.Outlined.LockOpen
                            isWorking -> Icons.Outlined.CheckCircle
                            isUpdate -> Icons.Outlined.SystemUpdate
                            isUnknown -> Icons.Outlined.Warning
                            else -> Icons.Outlined.Block
                        },
                        contentDescription = statusTitle,
                        modifier = Modifier.size(FolkDimens.Size36),
                        tint = if (overImage) contentColor else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(FolkDimens.Size16))
                    Column(Modifier.weight(1f)) {
                        Text(statusTitle, style = me.bmax.apatch.ui.theme.tokens.FolkType.Title)
                        Spacer(Modifier.height(FolkDimens.Size4))
                        Text(summary, style = me.bmax.apatch.ui.theme.tokens.FolkType.Summary, color = supportingColor, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    if (isJailbreak) {
                        IconButton(onClick = jailbreakState::performPrimaryAction, enabled = !jailbreakState.isTriggering) {
                            if (jailbreakState.isTriggering) CircularProgressIndicator(Modifier.size(FolkDimens.Size22), strokeWidth = FolkDimens.Size2)
                            else Icon(Icons.Outlined.RestartAlt, stringResource(R.string.reboot_soft), tint = contentColor)
                        }
                    } else if (isWorking) {
                        IconButton(onClick = { showUninstallDialog.value = true }) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.home_ap_cando_uninstall), tint = contentColor)
                        }
                    }
                }
                if (stateText != null) {
                    Text(stateText, style = me.bmax.apatch.ui.theme.tokens.FolkType.Caption, color = supportingColor)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size16)) {
                    DashboardHeroFact(stringResource(R.string.home_kpatch_version), if (isWorking) Version.installedKPVString() else "—", supportingColor, Modifier.weight(1f))
                    DashboardHeroFact(stringResource(R.string.home_kernel), android.system.Os.uname().release, supportingColor, Modifier.weight(1f))
                    DashboardHeroFact(stringResource(R.string.home_selinux_status), getSELinuxStatus(), supportingColor, Modifier.weight(1f))
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
        )
    }
}


@Composable
private fun DashboardHeroFact(label: String, value: String, supportingColor: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = me.bmax.apatch.ui.theme.tokens.FolkType.Caption, color = supportingColor, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Spacer(Modifier.height(FolkDimens.Size4))
        Text(value, style = me.bmax.apatch.ui.theme.tokens.FolkType.Summary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}
