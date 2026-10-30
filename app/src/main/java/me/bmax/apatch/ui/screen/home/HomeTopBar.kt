package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import me.bmax.apatch.ui.theme.MusicConfig
import me.bmax.apatch.util.MusicManager
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.RestartAlt
import me.bmax.apatch.ui.theme.BackgroundConfig
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import me.bmax.apatch.ui.component.folk.navOrigin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.generated.destinations.AboutScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenu
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenuItem
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.util.reboot
import me.bmax.apatch.ui.component.folk.folkDefaultAppBarColors

data class RebootOption(
    @param:StringRes val titleRes: Int,
    val reason: String,
    val icon: ImageVector
)

@Composable
fun getRebootOptions(): List<RebootOption> = listOf(
    RebootOption(R.string.reboot, "", Icons.Outlined.Refresh),
    RebootOption(R.string.reboot_recovery, "recovery", Icons.Outlined.SystemUpdate),
    RebootOption(R.string.reboot_bootloader, "bootloader", Icons.Outlined.Memory),
    RebootOption(R.string.reboot_download, "download", Icons.Outlined.Download),
    RebootOption(R.string.reboot_edl, "edl", Icons.Outlined.DeveloperMode),
    RebootOption(R.string.reboot_fastbootd, "fastboot", Icons.Outlined.RestartAlt),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
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

    TopAppBar(
        colors = folkDefaultAppBarColors(),
        title = {
        if (useAdvancedTitleStyle) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(BackgroundConfig.titleImageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = titleResId?.let { stringResource(it) } ?: customAppTitle,
                modifier = Modifier
                    .height(FolkDimens.Size40)
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
        IconButton(
            onClick = onInstallClick,
            modifier = Modifier.navOrigin(captureBitmap = true),
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoFixHigh,
                contentDescription = stringResource(id = R.string.mode_select_page_title)
            )
        }

        if (kpState != APApplication.State.UNKNOWN_STATE) {
            // Download/EDL drop the device into flashing modes that look dead to
            // a normal user, so they get a confirmation step first.
            val downloadTitle = stringResource(id = R.string.reboot_download)
            val downloadConfirmText = stringResource(id = R.string.reboot_download_confirm)
            val edlTitle = stringResource(id = R.string.reboot_edl)
            val edlConfirmText = stringResource(id = R.string.reboot_edl_confirm)
            var pendingRebootReason by remember { mutableStateOf<String?>(null) }
            val rebootConfirmDialog = rememberConfirmDialog(onConfirm = {
                pendingRebootReason?.let { reboot(it) }
            })

            Box {
                IconButton(onClick = { showDropdownReboot = true }) {
                    Icon(
                        imageVector = Icons.Outlined.PowerSettingsNew,
                        contentDescription = stringResource(id = R.string.reboot)
                    )
                }
                WallpaperAwareDropdownMenu(
                    expanded = showDropdownReboot,
                    onDismissRequest = { showDropdownReboot = false }
                ) {
                    getRebootOptions().forEach { option ->
                        WallpaperAwareDropdownMenuItem(
                            text = { Text(stringResource(option.titleRes)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showDropdownReboot = false
                                when (option.reason) {
                                    "download" -> {
                                        pendingRebootReason = "download"
                                        rebootConfirmDialog.showConfirm(
                                            title = downloadTitle, content = downloadConfirmText
                                        )
                                    }
                                    "edl" -> {
                                        pendingRebootReason = "edl"
                                        rebootConfirmDialog.showConfirm(
                                            title = edlTitle, content = edlConfirmText
                                        )
                                    }
                                    else -> reboot(option.reason)
                                }
                            }
                        )
                    }
                }
            }
        }

        Box {
            IconButton(onClick = { showDropdownMoreOptions = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(id = R.string.settings)
                )
                WallpaperAwareDropdownMenu(
                    expanded = showDropdownMoreOptions,
                    onDismissRequest = { showDropdownMoreOptions = false }
                ) {
                    if (MusicConfig.isMusicEnabled) {
                        val isPlaying by MusicManager.isPlaying.collectAsStateWithLifecycle()
                        WallpaperAwareDropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (isPlaying) R.string.home_more_menu_music_pause
                                        else R.string.home_more_menu_music_play
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showDropdownMoreOptions = false
                                MusicManager.toggle()
                            }
                        )
                    }
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

