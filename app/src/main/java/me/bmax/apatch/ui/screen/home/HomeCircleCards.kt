package me.bmax.apatch.ui.screen.home

import android.os.Build
import android.system.Os
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.util.Version
import me.bmax.apatch.util.getSELinuxStatus
import me.bmax.apatch.util.Version.getManagerVersion
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.component.copyableInfo
import me.bmax.apatch.ui.component.TonalCard
import androidx.compose.material3.surfaceColorAtElevation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.core.ui.R as CoreR
import me.bmax.apatch.ui.theme.tokens.FolkShape

@Composable
fun StatusCardCircle(
    kpState: APApplication.State,
    apState: APApplication.State,
    navigator: DestinationsNavigator,
    showUninstallDialog: MutableState<Boolean>
) {
    val isWorking = kpState == APApplication.State.KERNELPATCH_INSTALLED
    val isUpdate = kpState == APApplication.State.KERNELPATCH_NEED_UPDATE || kpState == APApplication.State.KERNELPATCH_NEED_REBOOT
    val classicEmojiEnabled = BackgroundConfig.isListWorkingCardModeHidden

    val jailbreakState = LocalHomeJailbreakState.current
    val isJailbreak = jailbreakState.isActive
    val isPermissive = jailbreakState.isPermissive

    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled
    // 壁纸模式下状态卡统一用中性 surface/onSurface；强调容器降 alpha 后其 on 色
    // 会失去配对的对比度，不再在壁纸上使用。
    val finalContainerColor = if (isWallpaperMode) {
        MaterialTheme.colorScheme.surface
    } else if (isJailbreak) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else if (isWorking) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    TonalCard(
        containerColor = finalContainerColor,
        contentColor = if (isWallpaperMode) MaterialTheme.colorScheme.onSurface else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    when {
                        isJailbreak -> jailbreakState.performPrimaryAction()
                        isWorking -> {
                            if (apState == APApplication.State.ANDROIDPATCH_INSTALLED) {
                                showUninstallDialog.value = true
                            }
                        }
                        else -> navigator.navigate(com.ramcosta.composedestinations.generated.destinations.InstallModeSelectScreenDestination)
                    }
                }
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isJailbreak) {
                Icon(Icons.Outlined.LockOpen, stringResource(R.string.settings_jailbreak_mode))
                Column(Modifier.padding(start = 20.dp).weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_jailbreak_mode),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_jailbreak_mode_summary),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else if (isWorking) {
                 Icon(Icons.Outlined.CheckCircle, stringResource(R.string.home_working))
                 Column(Modifier.padding(start = 20.dp).weight(1f)) {
                     val isFull = apState == APApplication.State.ANDROIDPATCH_INSTALLED
                    val modeTextTitle = if (isFull) "Kpm" else "Half"
                    val modeTextCaps = if (isFull) "KPM" else "HALF"
                    val modeText = BackgroundConfig.getCustomBadgeText() ?: modeTextCaps

                     Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (classicEmojiEnabled) {
                                stringResource(R.string.home_working) + "😋"
                            } else {
                                stringResource(R.string.home_working)
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.width(8.dp))

                       // Full/Half Label
                      if (!classicEmojiEnabled) {
                          ModeLabelText(label = modeText)
                      }
                   }
                   Spacer(Modifier.height(4.dp))
                   Text(
                       text = stringResource(R.string.home_version, getManagerVersion().second.toString()) +
                               if (classicEmojiEnabled) " - $modeTextTitle" else "",
                       style = MaterialTheme.typography.bodyMedium
                   )
                }
            } else {
                 // Not installed or error
                 val icon = if (isUpdate) Icons.Outlined.SystemUpdate else Icons.Outlined.Warning
                 val title = if (isUpdate) stringResource(R.string.home_kp_need_update) else stringResource(R.string.home_not_installed)

                 Icon(icon, title)
                 Column(Modifier.padding(start = 20.dp).weight(1f)) {
                     Text(
                         text = title,
                         style = MaterialTheme.typography.titleMedium
                     )
                     Spacer(Modifier.height(4.dp))
                     Text(
                         text = stringResource(R.string.home_click_to_install),
                         style = MaterialTheme.typography.bodyMedium
                     )
                 }
            }
            if (isJailbreak || kpState == APApplication.State.UNKNOWN_STATE && isPermissive) {
                IconButton(
                    onClick = jailbreakState::performPrimaryAction,
                    enabled = !jailbreakState.isTriggering,
                ) {
                    if (jailbreakState.isTriggering) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = if (isJailbreak) Icons.Outlined.RestartAlt else Icons.Outlined.LockOpen,
                            contentDescription = stringResource(if (isJailbreak) R.string.reboot_soft else R.string.jailbreak),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCardCircle(kpState: APApplication.State, apState: APApplication.State) {
    val context = LocalContext.current

    TonalCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            val uname = Os.uname()
            val prefs = APApplication.sharedPreferences

            var hideSuPath by remember { mutableStateOf(prefs.getBoolean("hide_su_path", false)) }
            var hideKpatchVersion by remember { mutableStateOf(prefs.getBoolean("hide_kpatch_version", false)) }
            var hideFingerprint by remember { mutableStateOf(prefs.getBoolean("hide_fingerprint", false)) }
            var hideZygisk by remember { mutableStateOf(prefs.getBoolean("hide_zygisk", false)) }
            var hideMount by remember { mutableStateOf(prefs.getBoolean("hide_mount", false)) }
            

            DisposableEffect(Unit) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                    when (key) {
                        "hide_su_path" -> hideSuPath = sharedPreferences.getBoolean("hide_su_path", false)
                        "hide_kpatch_version" -> hideKpatchVersion = sharedPreferences.getBoolean("hide_kpatch_version", false)
                        "hide_fingerprint" -> hideFingerprint = sharedPreferences.getBoolean("hide_fingerprint", false)
                        "hide_zygisk" -> hideZygisk = sharedPreferences.getBoolean("hide_zygisk", false)
                        "hide_mount" -> hideMount = sharedPreferences.getBoolean("hide_mount", false)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            var zygiskImplement by remember { mutableStateOf("None") }
            var mountImplement by remember { mutableStateOf("None") }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try {
                        zygiskImplement = me.bmax.apatch.util.getZygiskImplement()
                        mountImplement = me.bmax.apatch.util.getMountImplement()
                    } catch (_: Exception) {
                    }
                }
            }

            @Composable
            fun InfoCardItem(
                label: String,
                content: String,
                icon: @Composable () -> Unit
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .copyableInfo(label, content),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    icon()
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            @Composable
            fun InfoCardItem(icon: ImageVector, label: String, content: String) = InfoCardItem(
                label = label,
                content = content,
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            val managerVersion = getManagerVersion()
            InfoCardItem(
                icon = Icons.Outlined.Apps,
                label = stringResource(R.string.home_manager_version),
                content = managerVersion.first
            )

            Spacer(Modifier.height(16.dp))
            if (kpState != APApplication.State.UNKNOWN_STATE && !hideKpatchVersion) {
                InfoCardItem(
                    icon = Icons.Outlined.Extension,
                    label = stringResource(R.string.home_kpatch_version),
                    content = Version.installedKPVString()
                )
                Spacer(Modifier.height(16.dp))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && !hideSuPath) {
                InfoCardItem(
                    icon = Icons.Outlined.Code,
                    label = stringResource(R.string.home_su_path),
                    content = Natives.suPath()
                )
                Spacer(Modifier.height(16.dp))
            }

            if (apState != APApplication.State.UNKNOWN_STATE && apState != APApplication.State.ANDROIDPATCH_NOT_INSTALLED) {
                InfoCardItem(
                    icon = Icons.Outlined.Android,
                    label = stringResource(R.string.home_apatch_version),
                    content = managerVersion.second.toString()
                )
                Spacer(Modifier.height(16.dp))
            }

            InfoCardItem(
                icon = Icons.Outlined.PhoneAndroid,
                label = stringResource(R.string.home_device_info),
                content = getDeviceInfo()
            )

            Spacer(Modifier.height(16.dp))
            InfoCardItem(
                icon = Icons.Outlined.DeveloperBoard,
                label = stringResource(R.string.home_kernel),
                content = uname.release
            )

            Spacer(Modifier.height(16.dp))
            InfoCardItem(
                icon = Icons.Outlined.Info,
                label = stringResource(R.string.home_system_version),
                content = getSystemVersion()
            )

            Spacer(Modifier.height(16.dp))
            if (!hideFingerprint) {
                InfoCardItem(
                    icon = Icons.Outlined.Fingerprint,
                    label = stringResource(R.string.home_fingerprint),
                    content = Build.FINGERPRINT
                )
                Spacer(Modifier.height(16.dp))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && zygiskImplement != "None" && !hideZygisk) {
                InfoCardItem(
                    icon = Icons.Outlined.Layers,
                    label = stringResource(R.string.home_zygisk_implement),
                    content = zygiskImplement
                )
                Spacer(Modifier.height(16.dp))
            }

            if (kpState != APApplication.State.UNKNOWN_STATE && mountImplement != "None" && !hideMount) {
                InfoCardItem(
                    icon = Icons.Outlined.SdStorage,
                    label = stringResource(R.string.home_mount_implement),
                    content = mountImplement
                )
                Spacer(Modifier.height(16.dp))
            }

            InfoCardItem(
                icon = Icons.Outlined.Shield,
                label = stringResource(R.string.home_selinux_status),
                content = getSELinuxStatus()
            )
        }
    }
}

@Composable
fun ModeLabelText(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimary,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .padding(end = 4.dp)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        )
    }
}

@Composable
fun AStatusCardCircle(apState: APApplication.State) {
    TonalCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.android_patch),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                    FilledTonalButton(onClick = {
                        when (apState) {
                            APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                                APApplication.installApatch()
                            }

                            APApplication.State.ANDROIDPATCH_UNINSTALLING -> {
                            }

                            APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                                APApplication.installApatch()
                            }

                            else -> {
                                APApplication.uninstallApatch()
                            }
                        }
                    }) {
                        when (apState) {
                            APApplication.State.ANDROIDPATCH_NOT_INSTALLED -> {
                                Text(text = stringResource(id = R.string.home_ap_cando_install))
                            }

                            APApplication.State.ANDROIDPATCH_UNINSTALLING -> {
                                Icon(Icons.Outlined.Cached, contentDescription = stringResource(CoreR.string.core_state_busy))
                            }

                            APApplication.State.ANDROIDPATCH_NEED_UPDATE -> {
                                Text(text = stringResource(id = R.string.home_kp_cando_update))
                            }

                            else -> {
                                Text(text = stringResource(id = R.string.home_ap_cando_uninstall))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LearnMoreCardCircle() {
    val uriHandler = LocalUriHandler.current

    TonalCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    uriHandler.openUri("https://fp.mysqil.com/")
                }
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_learn_apatch),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_click_to_learn_apatch),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
