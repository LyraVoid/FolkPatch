package me.bmax.apatch.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.CheckboxItem
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.component.ToggleSettingCard

@Composable
fun BehaviorSettingsContent(
    kPatchReady: Boolean,
    aPatchReady: Boolean,
    flat: Boolean = false,
) {
    val prefs = APApplication.sharedPreferences

    var currentStyle by remember { mutableStateOf(prefs.getString("home_layout_style", "circle")) }
    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "home_layout_style") {
                currentStyle = sharedPreferences.getString("home_layout_style", "circle")
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    SplicedColumnGroup(flat = flat) {

    item {
    var enableWebDebugging by remember { mutableStateOf(prefs.getBoolean("enable_web_debugging", false)) }
    ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.BugReport,
        title = stringResource(id = R.string.enable_web_debugging),
        description = stringResource(id = R.string.enable_web_debugging_summary),
        checked = enableWebDebugging,
        onCheckedChange = {
            enableWebDebugging = it
            prefs.edit().putBoolean("enable_web_debugging", it).apply()
        }
    )
    }

    item(visible = aPatchReady) {
        var installConfirm by remember { mutableStateOf(prefs.getBoolean("apm_install_confirm_enabled", true)) }
        ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.Verified,
            title = stringResource(id = R.string.settings_apm_install_confirm),
            description = stringResource(id = R.string.settings_apm_install_confirm_summary),
            checked = installConfirm,
            onCheckedChange = {
                installConfirm = it
                prefs.edit().putBoolean("apm_install_confirm_enabled", it).apply()
            }
        )
    }

    item(visible = aPatchReady) {
        var enableModuleShortcutAdd by remember { mutableStateOf(prefs.getBoolean("enable_module_shortcut_add", true)) }
        ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.AddToHomeScreen,
            title = stringResource(id = R.string.settings_enable_module_shortcut_add),
            description = stringResource(id = R.string.settings_enable_module_shortcut_add_summary),
            checked = enableModuleShortcutAdd,
            onCheckedChange = {
                enableModuleShortcutAdd = it
                prefs.edit().putBoolean("enable_module_shortcut_add", it).apply()
            }
        )
    }

    item(visible = aPatchReady) {
        var stayOnPage by remember { mutableStateOf(prefs.getBoolean("apm_action_stay_on_page", true)) }
        ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.OpenInNew,
            title = stringResource(id = R.string.settings_apm_stay_on_page),
            description = stringResource(id = R.string.settings_apm_stay_on_page_summary),
            checked = stayOnPage,
            onCheckedChange = {
                stayOnPage = it
                prefs.edit().putBoolean("apm_action_stay_on_page", it).apply()
            }
        )
    }

    item(visible = currentStyle != "focus") {
        var hideApatchCard by remember { mutableStateOf(prefs.getBoolean("hide_apatch_card", false)) }
        ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.VisibilityOff,
            title = stringResource(id = R.string.settings_hide_apatch_card),
            description = stringResource(id = R.string.settings_hide_apatch_card_summary),
            checked = hideApatchCard,
            onCheckedChange = {
                hideApatchCard = it
                prefs.edit().putBoolean("hide_apatch_card", it).apply()
            }
        )
    }

    item(visible = kPatchReady) {
        var hideSuPath by remember { mutableStateOf(prefs.getBoolean("hide_su_path", false)) }
        ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.VisibilityOff,
            title = stringResource(id = R.string.home_hide_su_path),
            description = stringResource(id = R.string.home_hide_su_path_summary),
            checked = hideSuPath,
            onCheckedChange = {
                hideSuPath = it
                prefs.edit().putBoolean("hide_su_path", it).apply()
            }
        )
    }

    item(visible = kPatchReady) {
        var hideKpatchVersion by remember { mutableStateOf(prefs.getBoolean("hide_kpatch_version", false)) }
        ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.VisibilityOff,
            title = stringResource(id = R.string.home_hide_kpatch_version),
            description = stringResource(id = R.string.home_hide_kpatch_version_summary),
            checked = hideKpatchVersion,
            onCheckedChange = {
                hideKpatchVersion = it
                prefs.edit().putBoolean("hide_kpatch_version", it).apply()
            }
        )
    }

    item(visible = kPatchReady) {
        var hideFingerprint by remember { mutableStateOf(prefs.getBoolean("hide_fingerprint", false)) }
        ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.Fingerprint,
            title = stringResource(id = R.string.home_hide_fingerprint),
            description = stringResource(id = R.string.home_hide_fingerprint_summary),
            checked = hideFingerprint,
            onCheckedChange = {
                hideFingerprint = it
                prefs.edit().putBoolean("hide_fingerprint", it).apply()
            }
        )
    }

    item(visible = kPatchReady) {
        var hideZygisk by remember { mutableStateOf(prefs.getBoolean("hide_zygisk", false)) }
        ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.VisibilityOff,
            title = stringResource(id = R.string.home_hide_zygisk),
            description = stringResource(id = R.string.home_hide_zygisk_summary),
            checked = hideZygisk,
            onCheckedChange = {
                hideZygisk = it
                prefs.edit().putBoolean("hide_zygisk", it).apply()
            }
        )
    }

    item(visible = kPatchReady) {
        var hideMount by remember { mutableStateOf(prefs.getBoolean("hide_mount", false)) }
        ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.VisibilityOff,
            title = stringResource(id = R.string.home_hide_mount),
            description = stringResource(id = R.string.home_hide_mount_summary),
            checked = hideMount,
            onCheckedChange = {
                hideMount = it
                prefs.edit().putBoolean("hide_mount", it).apply()
            }
        )
    }

    item(visible = kPatchReady) {
        var useLegacySuPage by remember { mutableStateOf(prefs.getBoolean("use_legacy_su_page", false)) }
        ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.History,
            title = stringResource(id = R.string.settings_use_legacy_su_page),
            description = stringResource(id = R.string.settings_use_legacy_su_page_summary),
            checked = useLegacySuPage,
            onCheckedChange = {
                useLegacySuPage = it
                prefs.edit().putBoolean("use_legacy_su_page", it).apply()
            }
        )
    }

    item(visible = kPatchReady) {
        var enableSuperUserBadge by remember { mutableStateOf(prefs.getBoolean("badge_superuser", true)) }
        var enableApmBadge by remember { mutableStateOf(prefs.getBoolean("badge_apm", true)) }
        var enableKernelBadge by remember { mutableStateOf(prefs.getBoolean("badge_kernel", true)) }
        var expanded by remember { mutableStateOf(false) }
        val rotationState by animateFloatAsState(
            targetValue = if (expanded) 180f else 0f,
            label = "ArrowRotation"
        )
        val badgeCountTitle = stringResource(id = R.string.enable_badge_count)
        val badgeCountSummary = stringResource(id = R.string.enable_badge_count_summary)

        ExpressiveCard(
            flat = flat,
            onClick = { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Badge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = badgeCountTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = badgeCountSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotationState)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                CheckboxItem(
                    icon = null,
                    title = stringResource(id = R.string.badge_superuser),
                    summary = null,
                    checked = enableSuperUserBadge,
                    onCheckedChange = {
                        enableSuperUserBadge = it
                        prefs.edit().putBoolean("badge_superuser", it).apply()
                    }
                )

                CheckboxItem(
                    icon = null,
                    title = stringResource(id = R.string.badge_apm),
                    summary = null,
                    checked = enableApmBadge,
                    onCheckedChange = {
                        enableApmBadge = it
                        prefs.edit().putBoolean("badge_apm", it).apply()
                    }
                )

                CheckboxItem(
                    icon = null,
                    title = stringResource(id = R.string.badge_kernel),
                    summary = null,
                    checked = enableKernelBadge,
                    onCheckedChange = {
                        enableKernelBadge = it
                        prefs.edit().putBoolean("badge_kernel", it).apply()
                    }
                )
            }
        }
    }

    }
}
