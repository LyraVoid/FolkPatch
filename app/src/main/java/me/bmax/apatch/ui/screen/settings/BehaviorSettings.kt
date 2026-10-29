package me.bmax.apatch.ui.screen.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SegmentedCheckboxItem
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens
import me.bmax.apatch.ui.component.SegmentedPreferenceItem
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSection
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import androidx.compose.material.icons.outlined.*

@Composable
fun BehaviorSettingsContent(
    kPatchReady: Boolean,
    aPatchReady: Boolean,
    flat: Boolean = false,
    highlightKey: String? = null,
) {
    val prefs = APApplication.sharedPreferences

    var currentStyle by remember { mutableStateOf(prefs.getString("home_layout_style", APApplication.HOME_LAYOUT_STYLE_DEFAULT)) }
    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "home_layout_style") {
                currentStyle = sharedPreferences.getString("home_layout_style", APApplication.HOME_LAYOUT_STYLE_DEFAULT)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    if (kPatchReady || currentStyle != "focus") {
    SegmentedSection(title = stringResource(R.string.settings_section_behavior_display)) {
        SegmentedColumn(flat = flat, highlightKey = highlightKey) {
            item(key = "behavior_hide_apatch_card", visible = currentStyle != "focus") {
                var hideApatchCard by remember { mutableStateOf(prefs.getBoolean("hide_apatch_card", false)) }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.VisibilityOff,
                    title = stringResource(id = R.string.settings_hide_apatch_card),
                    summary = stringResource(id = R.string.settings_hide_apatch_card_summary),
                    checked = hideApatchCard,
                    onCheckedChange = {
                        hideApatchCard = it
                        prefs.edit().putBoolean("hide_apatch_card", it).apply()
                    },
                )
            }

            item(key = "behavior_hide_su_path", visible = kPatchReady) {
                var hideSuPath by remember { mutableStateOf(prefs.getBoolean("hide_su_path", false)) }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.VisibilityOff,
                    title = stringResource(id = R.string.home_hide_su_path),
                    summary = stringResource(id = R.string.home_hide_su_path_summary),
                    checked = hideSuPath,
                    onCheckedChange = {
                        hideSuPath = it
                        prefs.edit().putBoolean("hide_su_path", it).apply()
                    },
                )
            }

            item(key = "behavior_hide_kpatch_version", visible = kPatchReady) {
                var hideKpatchVersion by remember { mutableStateOf(prefs.getBoolean("hide_kpatch_version", false)) }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.VisibilityOff,
                    title = stringResource(id = R.string.home_hide_kpatch_version),
                    summary = stringResource(id = R.string.home_hide_kpatch_version_summary),
                    checked = hideKpatchVersion,
                    onCheckedChange = {
                        hideKpatchVersion = it
                        prefs.edit().putBoolean("hide_kpatch_version", it).apply()
                    },
                )
            }

            item(key = "behavior_hide_fingerprint", visible = kPatchReady) {
                var hideFingerprint by remember { mutableStateOf(prefs.getBoolean("hide_fingerprint", false)) }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Fingerprint,
                    title = stringResource(id = R.string.home_hide_fingerprint),
                    summary = stringResource(id = R.string.home_hide_fingerprint_summary),
                    checked = hideFingerprint,
                    onCheckedChange = {
                        hideFingerprint = it
                        prefs.edit().putBoolean("hide_fingerprint", it).apply()
                    },
                )
            }

            item(key = "behavior_hide_zygisk", visible = kPatchReady) {
                var hideZygisk by remember { mutableStateOf(prefs.getBoolean("hide_zygisk", false)) }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.VisibilityOff,
                    title = stringResource(id = R.string.home_hide_zygisk),
                    summary = stringResource(id = R.string.home_hide_zygisk_summary),
                    checked = hideZygisk,
                    onCheckedChange = {
                        hideZygisk = it
                        prefs.edit().putBoolean("hide_zygisk", it).apply()
                    },
                )
            }

            item(key = "behavior_hide_mount", visible = kPatchReady) {
                var hideMount by remember { mutableStateOf(prefs.getBoolean("hide_mount", false)) }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.VisibilityOff,
                    title = stringResource(id = R.string.home_hide_mount),
                    summary = stringResource(id = R.string.home_hide_mount_summary),
                    checked = hideMount,
                    onCheckedChange = {
                        hideMount = it
                        prefs.edit().putBoolean("hide_mount", it).apply()
                    },
                )
            }

            item(key = "behavior_badge_count", visible = kPatchReady) {
                var enableSuperUserBadge by remember { mutableStateOf(prefs.getBoolean("badge_superuser", true)) }
                var enableApmBadge by remember { mutableStateOf(prefs.getBoolean("badge_apm", true)) }
                var enableKernelBadge by remember { mutableStateOf(prefs.getBoolean("badge_kernel", true)) }
                var expanded by remember { mutableStateOf(false) }
                val rotationState by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    label = "ArrowRotation"
                )

                SegmentedPreferenceItem(
                    icon = Icons.Outlined.Badge,
                    title = stringResource(id = R.string.enable_badge_count),
                    summary = stringResource(id = R.string.enable_badge_count_summary),
                    onClick = { expanded = !expanded },
                    trailing = {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(FolkSettingsDimens.ChevronSize)
                                .rotate(rotationState),
                        )
                    },
                )

                if (expanded) {
                    SegmentedCheckboxItem(
                        title = stringResource(id = R.string.badge_superuser),
                        checked = enableSuperUserBadge,
                        onCheckedChange = {
                            enableSuperUserBadge = it
                            prefs.edit().putBoolean("badge_superuser", it).apply()
                        },
                    )
                    SegmentedCheckboxItem(
                        title = stringResource(id = R.string.badge_apm),
                        checked = enableApmBadge,
                        onCheckedChange = {
                            enableApmBadge = it
                            prefs.edit().putBoolean("badge_apm", it).apply()
                        },
                    )
                    SegmentedCheckboxItem(
                        title = stringResource(id = R.string.badge_kernel),
                        checked = enableKernelBadge,
                        onCheckedChange = {
                            enableKernelBadge = it
                            prefs.edit().putBoolean("badge_kernel", it).apply()
                        },
                    )
                }
            }
        }
    }
    }

    SegmentedSection(title = stringResource(R.string.settings_section_behavior_actions)) {
        SegmentedColumn(flat = flat, highlightKey = highlightKey) {
            item(key = "behavior_install_confirm", visible = aPatchReady) {
                var installConfirm by remember { mutableStateOf(prefs.getBoolean("apm_install_confirm_enabled", true)) }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Verified,
                    title = stringResource(id = R.string.settings_apm_install_confirm),
                    summary = stringResource(id = R.string.settings_apm_install_confirm_summary),
                    checked = installConfirm,
                    onCheckedChange = {
                        installConfirm = it
                        prefs.edit().putBoolean("apm_install_confirm_enabled", it).apply()
                    },
                )
            }

            item(key = "behavior_module_shortcut", visible = aPatchReady) {
                var enableModuleShortcutAdd by remember { mutableStateOf(prefs.getBoolean("enable_module_shortcut_add", true)) }
                SegmentedSwitchItem(
                    icon = Icons.AutoMirrored.Outlined.AddToHomeScreen,
                    title = stringResource(id = R.string.settings_enable_module_shortcut_add),
                    summary = stringResource(id = R.string.settings_enable_module_shortcut_add_summary),
                    checked = enableModuleShortcutAdd,
                    onCheckedChange = {
                        enableModuleShortcutAdd = it
                        prefs.edit().putBoolean("enable_module_shortcut_add", it).apply()
                    },
                )
            }

            item(key = "behavior_stay_on_page", visible = aPatchReady) {
                var stayOnPage by remember { mutableStateOf(prefs.getBoolean("apm_action_stay_on_page", true)) }
                SegmentedSwitchItem(
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    title = stringResource(id = R.string.settings_apm_stay_on_page),
                    summary = stringResource(id = R.string.settings_apm_stay_on_page_summary),
                    checked = stayOnPage,
                    onCheckedChange = {
                        stayOnPage = it
                        prefs.edit().putBoolean("apm_action_stay_on_page", it).apply()
                    },
                )
            }

            item(key = "behavior_web_debugging") {
                var enableWebDebugging by remember { mutableStateOf(prefs.getBoolean("enable_web_debugging", false)) }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.BugReport,
                    title = stringResource(id = R.string.enable_web_debugging),
                    summary = stringResource(id = R.string.enable_web_debugging_summary),
                    checked = enableWebDebugging,
                    onCheckedChange = {
                        enableWebDebugging = it
                        prefs.edit().putBoolean("enable_web_debugging", it).apply()
                    },
                )
            }

            item(key = "behavior_info_copy") {
                var infoCopyEnabled by remember { mutableStateOf(prefs.getBoolean("enable_info_copy", true)) }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.ContentCopy,
                    title = stringResource(id = R.string.settings_info_copy),
                    summary = stringResource(id = R.string.settings_info_copy_summary),
                    checked = infoCopyEnabled,
                    onCheckedChange = {
                        infoCopyEnabled = it
                        prefs.edit().putBoolean("enable_info_copy", it).apply()
                    },
                )
            }

            item(key = "behavior_legacy_su_page", visible = kPatchReady) {
                var useLegacySuPage by remember { mutableStateOf(prefs.getBoolean("use_legacy_su_page", false)) }
                SegmentedSwitchItem(
                    icon = Icons.Outlined.History,
                    title = stringResource(id = R.string.settings_use_legacy_su_page),
                    summary = stringResource(id = R.string.settings_use_legacy_su_page_summary),
                    checked = useLegacySuPage,
                    onCheckedChange = {
                        useLegacySuPage = it
                        prefs.edit().putBoolean("use_legacy_su_page", it).apply()
                    },
                )
            }
        }
    }
}
