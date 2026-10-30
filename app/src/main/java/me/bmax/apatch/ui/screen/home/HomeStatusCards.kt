package me.bmax.apatch.ui.screen.home

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.LocalWallpaperContentColor
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.util.reboot
import me.bmax.apatch.core.ui.R as CoreR
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens

private val managerVersion = getManagerVersion()

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        color = containerColor.copy(alpha = 1f),
        shape = RoundedCornerShape(FolkDimens.Size4),
        tonalElevation = FolkDimens.Size0,
        shadowElevation = FolkDimens.Size0
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = FolkDimens.Size6, vertical = FolkDimens.Size2),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 1f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun KStatusCard(
    kpState: APApplication.State, apState: APApplication.State, navigator: DestinationsNavigator
) {

    val showUninstallDialog = remember { mutableStateOf(false) }
    if (showUninstallDialog.value) {
        UninstallDialog(showDialog = showUninstallDialog, navigator)
    }

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

    // Jailbreak button appears when the kernel is not installed and SELinux is permissive.
    val jailbreakState = LocalHomeJailbreakState.current
    val isPermissive = jailbreakState.isPermissive
    val isJailbreak = jailbreakState.isActive

    // 壁纸模式下半透明容器让壁纸透出，语义色（onPrimary 等）不再匹配实际背景，
    // 改用随壁纸明暗取反的中性色；非壁纸模式为 null，回退到语义色。
    val wallpaperContentColor = LocalWallpaperContentColor.current

    val (cardBackgroundColor, cardContentColor) = when {
        isJailbreak -> {
            val containerColor = if (BackgroundConfig.isCustomBackgroundEnabled) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = BackgroundConfig.customBackgroundOpacity)
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
            containerColor to (wallpaperContentColor ?: MaterialTheme.colorScheme.onTertiaryContainer)
        }

        kpState == APApplication.State.KERNELPATCH_INSTALLED -> {
            if (BackgroundConfig.isCustomBackgroundEnabled) {
                val opacity = BackgroundConfig.customBackgroundOpacity
                val contentColor = wallpaperContentColor ?: MaterialTheme.colorScheme.onPrimary
                MaterialTheme.colorScheme.primary.copy(alpha = opacity) to contentColor
            } else {
                MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
            }
        }

        kpState == APApplication.State.KERNELPATCH_NEED_UPDATE || kpState == APApplication.State.KERNELPATCH_NEED_REBOOT -> {
            if (BackgroundConfig.isCustomBackgroundEnabled) {
                MaterialTheme.colorScheme.secondary.copy(alpha = BackgroundConfig.customBackgroundOpacity) to
                    (wallpaperContentColor ?: MaterialTheme.colorScheme.onSecondary)
            } else {
                MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
            }
        }

        else -> {
            (LocalFolkWallpaperTokens.current?.group?.fill ?: FolkTheme.palette.groupedSurface) to
                (wallpaperContentColor ?: MaterialTheme.colorScheme.onSurface)
        }
    }

    Card(
        onClick = {
            if (!isJailbreak && kpState != APApplication.State.KERNELPATCH_INSTALLED) {
                navigator.navigate(InstallModeSelectScreenDestination)
            }
        },
        shape = FolkShape.Corner20,
        colors = CardDefaults.cardColors(
            containerColor = cardBackgroundColor,
            contentColor = cardContentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FolkDimens.Size12),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isJailbreak && kpState == APApplication.State.KERNELPATCH_NEED_UPDATE) {
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
                    .padding(FolkDimens.Size10),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isJailbreak -> {
                        Icon(Icons.Outlined.LockOpen, stringResource(R.string.settings_jailbreak_mode))
                    }

                    kpState == APApplication.State.KERNELPATCH_INSTALLED -> {
                        Icon(Icons.Outlined.CheckCircle, stringResource(R.string.home_working))
                    }

                    kpState == APApplication.State.KERNELPATCH_NEED_UPDATE || kpState == APApplication.State.KERNELPATCH_NEED_REBOOT -> {
                        Icon(Icons.Outlined.SystemUpdate, stringResource(R.string.home_kp_need_update))
                    }

                    else -> {
                        Icon(Icons.AutoMirrored.Outlined.HelpOutline, "Unknown")
                    }
                }
                Column(
                    Modifier
                        .weight(2f)
                        .padding(start = FolkDimens.Size16)
                ) {
                    when {
                        isJailbreak -> {
                            Text(
                                text = stringResource(R.string.settings_jailbreak_mode),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(FolkDimens.Size6))
                            Text(
                                text = stringResource(R.string.settings_jailbreak_mode_summary),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        kpState == APApplication.State.KERNELPATCH_INSTALLED -> {
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
                                    Spacer(Modifier.width(FolkDimens.Size8))
                                    StatusBadge(
                                        text = BackgroundConfig.getCustomBadgeText() ?: if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) "Kpm" else "Half"
                                    )
                                }
                            }
                        }

                        kpState == APApplication.State.KERNELPATCH_NEED_UPDATE || kpState == APApplication.State.KERNELPATCH_NEED_REBOOT -> {
                            Text(
                                text = stringResource(R.string.home_kp_need_update),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(FolkDimens.Size6))
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
                    if (!isJailbreak && kpState != APApplication.State.UNKNOWN_STATE && kpState != APApplication.State.KERNELPATCH_NEED_UPDATE && kpState != APApplication.State.KERNELPATCH_NEED_REBOOT) {
                        Spacer(Modifier.height(FolkDimens.Size4))
                        Text(
                            text = "${Version.installedKPVString()} (${managerVersion.second})" + if (BackgroundConfig.isListWorkingCardModeHidden) " - " + (if (apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED) "Kpm" else "KernelPatch") else "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    val onAction = {
                        when {
                            isJailbreak -> jailbreakState.performPrimaryAction()

                            kpState == APApplication.State.UNKNOWN_STATE -> {
                                navigator.navigate(InstallModeSelectScreenDestination)
                            }

                            kpState == APApplication.State.KERNELPATCH_NEED_UPDATE -> {
                                // todo: remove legacy compact for kp < 0.9.0
                                if (Version.installedKPVUInt() < 0x900u) {
                                    navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_ONLY))
                                } else {
                                    navigator.navigate(InstallModeSelectScreenDestination)
                                }
                            }

                            kpState == APApplication.State.KERNELPATCH_NEED_REBOOT -> {
                                reboot()
                            }

                            kpState == APApplication.State.KERNELPATCH_UNINSTALLING -> {
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

                    Button(
                        onClick = {
                            if (kpState == APApplication.State.UNKNOWN_STATE && isPermissive) {
                                jailbreakState.performPrimaryAction()
                            } else {
                                onAction()
                            }
                        },
                        enabled = !jailbreakState.isTriggering,
                        colors = if (BackgroundConfig.isCustomBackgroundEnabled && kpState == APApplication.State.KERNELPATCH_INSTALLED) {
                            val opacity = BackgroundConfig.customBackgroundOpacity
                            val contentColor = wallpaperContentColor ?: MaterialTheme.colorScheme.onPrimary
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = opacity),
                                contentColor = contentColor
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }, content = {
                            when {
                                jailbreakState.isTriggering -> Icon(Icons.Outlined.Cached, contentDescription = null)
                                isJailbreak -> Text(text = stringResource(R.string.reboot_soft))
                                kpState == APApplication.State.UNKNOWN_STATE && isPermissive -> Text(text = stringResource(R.string.jailbreak))
                                else -> when (kpState) {
                                APApplication.State.UNKNOWN_STATE -> Text(text = stringResource(id = R.string.home_ap_cando_install))
                                APApplication.State.KERNELPATCH_NEED_UPDATE -> Text(text = stringResource(id = R.string.home_kp_cando_update))
                                APApplication.State.KERNELPATCH_NEED_REBOOT -> Text(text = stringResource(id = R.string.home_ap_cando_reboot))
                                APApplication.State.KERNELPATCH_UNINSTALLING -> Icon(Icons.Outlined.Cached, contentDescription = stringResource(CoreR.string.core_state_busy))
                                else -> Text(text = stringResource(id = R.string.home_ap_cando_uninstall))
                                }
                            }
                        })
                }
            }
        }
    }
}
