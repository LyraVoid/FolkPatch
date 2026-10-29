package me.bmax.apatch.ui.screen.settings

import androidx.compose.material.icons.automirrored.outlined.FeaturedPlayList
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.SegmentedNavigationItem
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSection
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.util.getKernelVersionCode
import me.bmax.apatch.util.isGkiKernel
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.setMagicMountEnabled

@Composable
fun GeneralMountSection(
    flat: Boolean,
    highlightKey: String?,
    magicMountTitle: String,
    magicMountSummary: String,
    isMagicMountEnabled: Boolean,
    onMagicMountChange: (Boolean) -> Unit,
    resetSuPathTitle: String,
    showResetSuPathDialog: MutableState<Boolean>,
) {
    val prefs = APApplication.sharedPreferences
    val scope = rememberCoroutineScope()

    SegmentedSection(title = stringResource(R.string.settings_section_general_mount)) {
     SegmentedColumn(flat = flat, highlightKey = highlightKey) {

        item(key = "general_magic_mount", visible = true) {
            SegmentedSwitchItem(
                icon = Icons.Outlined.FolderSpecial,
                title = magicMountTitle,
                summary = magicMountSummary,
                checked = isMagicMountEnabled,
                onCheckedChange = {
                    setMagicMountEnabled(it)
                    onMagicMountChange(it)
                },
            )
        }

        item(key = "general_sucompat", visible = true) {
            var sucompatEnabled by remember { mutableStateOf(prefs.getBoolean("sucompat_enabled", false)) }
            SegmentedSwitchItem(
                icon = Icons.AutoMirrored.Outlined.FeaturedPlayList,
                title = stringResource(id = R.string.settings_sucompat),
                summary = stringResource(id = R.string.settings_sucompat_summary),
                checked = sucompatEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        val result = if (enabled) {
                            // Enable: create marker file and register hooks via supercall
                            rootShellForResult("touch ${APApplication.SUCOMPAT_FILE}")
                            Natives.controlFeature("sucompat_extra", true)
                        } else {
                            // Disable: remove marker file and unregister hooks via supercall
                            rootShellForResult("rm -f ${APApplication.SUCOMPAT_FILE}")
                            Natives.controlFeature("sucompat_extra", false)
                        }
                        if (result == 0L) {
                            sucompatEnabled = enabled
                            prefs.edit().putBoolean("sucompat_enabled", enabled).apply()
                        }
                    }
                }
            )
        }
        item(key = "general_selinux_hide", visible = true) {
            val kernelVersion = remember { getKernelVersionCode() }
            val kernelSupported = (kernelVersion ?: 0) >= 419
            val isGki = remember { isGkiKernel() }
            var selinuxHideEnabled by rememberSaveable {
                mutableStateOf(prefs.getBoolean("selinux_hide_enabled", false))
            }
            val showSelinuxHideWarning = remember { mutableStateOf(false) }

            fun applySelinuxHide(enabled: Boolean) {
                scope.launch(Dispatchers.IO) {
                    val command = if (enabled) {
                        "touch ${APApplication.SELINUX_HIDE_FILE}"
                    } else {
                        "rm -f ${APApplication.SELINUX_HIDE_FILE}"
                    }
                    val result = rootShellForResult(command)
                    if (result.isSuccess) {
                        selinuxHideEnabled = enabled
                        prefs.edit().putBoolean("selinux_hide_enabled", enabled).apply()
                    }
                }
            }

            SegmentedSwitchItem(
                icon = Icons.Outlined.Security,
                title = stringResource(id = R.string.settings_selinux_hide),
                summary = stringResource(id = R.string.settings_selinux_hide_summary),
                checked = selinuxHideEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        // Only tested on 5.10+, and non-GKI carries a bigger risk, so warn first.
                        val below510 = (kernelVersion ?: 0) < 510
                        if (below510 || !isGki) {
                            showSelinuxHideWarning.value = true
                        } else {
                            applySelinuxHide(true)
                        }
                    } else {
                        applySelinuxHide(false)
                    }
                },
                enabled = kernelSupported,
            )

            if (showSelinuxHideWarning.value) {
                SelinuxHideWarningDialog(
                    showDialog = showSelinuxHideWarning,
                    kernelVersion = kernelVersion,
                    isGki = isGki,
                    onConfirm = { applySelinuxHide(true) },
                )
            }
        }
        item(key = "general_reset_su_path", visible = true) {
            SegmentedNavigationItem(
                icon = Icons.Outlined.LinkOff,
                title = resetSuPathTitle,
                onClick = { showResetSuPathDialog.value = true },
            )
        }

     }
    }
}
