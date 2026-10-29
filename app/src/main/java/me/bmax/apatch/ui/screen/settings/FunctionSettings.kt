package me.bmax.apatch.ui.screen.settings

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSection
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.util.setHideServiceEnabled
import androidx.compose.material.icons.outlined.*

@Composable
fun FunctionSettingsContent(
    kPatchReady: Boolean,
    aPatchReady: Boolean,
    jailbreakEnabled: Boolean,
    onJailbreakChange: (Boolean) -> Unit,
    jailbreakAvailable: Boolean = true,
    isHideServiceEnabled: Boolean,
    onHideServiceChange: (Boolean) -> Unit,
    isKernelSpoofEnabled: Boolean,
    onKernelSpoofChange: (Boolean) -> Unit,
    kernelSpoofVersion: String,
    onKernelSpoofVersionChange: (String) -> Unit,
    kernelSpoofBuildTime: String,
    onKernelSpoofBuildTimeChange: (String) -> Unit,
    onKernelSpoofSave: () -> Unit,
    onKernelSpoofRestore: () -> Unit,
    snackBarHost: SnackbarHostState,
    isPathHideEnabled: Boolean,
    onPathHideChange: (Boolean) -> Unit,
    pathHidePaths: String,
    onPathHidePathsChange: (String) -> Unit,
    onPathHideSave: () -> Unit,
    isPathHideUidMode: Boolean,
    onPathHideUidModeChange: (Boolean) -> Unit,
    isPathHideFilterSystem: Boolean,
    onPathHideFilterSystemChange: (Boolean) -> Unit,
    selectedUids: Set<Int>,
    onUidToggle: (Int) -> Unit,
    onUidRemoveStale: () -> Unit,
    isUmountEnabled: Boolean,
    onUmountEnabledChange: (Boolean) -> Unit,
    umountPaths: String,
    onUmountPathsChange: (String) -> Unit,
    onUmountSave: () -> Unit,
    isNetIsolateEnabled: Boolean,
    onNetIsolateChange: (Boolean) -> Unit,
    niSelectedUids: Set<Int>,
    onNiUidToggle: (Int) -> Unit,
    isShizukuEnabled: Boolean,
    isShizukuRunning: Boolean,
    onShizukuToggle: (Boolean) -> Unit,
    onShizukuManage: () -> Unit,
    flat: Boolean = false,
    highlightKey: String? = null,
) {
    val hideServiceTitle = stringResource(id = R.string.settings_hide_service)
    val hideServiceSummary = stringResource(id = R.string.settings_hide_service_summary)
    val umountServiceTitle = stringResource(id = R.string.settings_umount_service)
    val umountServiceSummary = stringResource(id = R.string.settings_umount_service_summary)

    if (kPatchReady && aPatchReady) {
    SegmentedSection(title = stringResource(R.string.settings_section_function_root)) {
     SegmentedColumn(flat = flat, highlightKey = highlightKey) {
        item(key = "function_jailbreak", visible = kPatchReady && aPatchReady && jailbreakAvailable) {
            SegmentedSwitchItem(
                icon = Icons.Outlined.LockOpen,
                title = stringResource(R.string.settings_jailbreak_mode),
                summary = stringResource(R.string.settings_jailbreak_mode_summary),
                checked = jailbreakEnabled,
                onCheckedChange = onJailbreakChange,
            )
        }

        item(key = "function_hide_service", visible = kPatchReady && aPatchReady) {
            SegmentedSwitchItem(
                icon = Icons.Outlined.VisibilityOff,
                title = hideServiceTitle,
                summary = hideServiceSummary,
                checked = isHideServiceEnabled,
                onCheckedChange = {
                    setHideServiceEnabled(it)
                    onHideServiceChange(it)
                }
            )
        }

     }
    }
    }

    if (kPatchReady && aPatchReady) {
    SegmentedSection(title = stringResource(R.string.settings_section_function_shizuku)) {
     SegmentedColumn(flat = flat, highlightKey = highlightKey) {
        item(key = "function_shizuku", visible = kPatchReady && aPatchReady) {
            val shizukuSummary = if (isShizukuRunning) {
                stringResource(id = R.string.settings_shizuku_service_running)
            } else {
                stringResource(id = R.string.settings_shizuku_service_summary)
            }
            Column {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.WaterDrop,
                    title = stringResource(id = R.string.settings_shizuku_service),
                    summary = shizukuSummary,
                    checked = isShizukuEnabled,
                    onCheckedChange = onShizukuToggle,
                )
                AnimatedVisibility(visible = isShizukuEnabled && isShizukuRunning) {
                    TextButton(
                        onClick = onShizukuManage,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.shizuku_manage_apps))
                    }
                }
            }
        }

     }
    }
    }

    if (kPatchReady && aPatchReady) {
    SegmentedSection(title = stringResource(R.string.settings_section_function_mount)) {
     SegmentedColumn(flat = flat, highlightKey = highlightKey) {
        item(key = "function_umount", visible = kPatchReady && aPatchReady) {
            val umountPathsLabel = stringResource(id = R.string.umount_config_paths_label)
            val umountPathsPlaceholder = stringResource(id = R.string.umount_config_paths_placeholder)
            val umountPathsHelper = stringResource(id = R.string.umount_config_paths_helper)

            TonalCard(flat = flat) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FolkDimens.Size16),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(FolkDimens.Size24),
                            )
                            Spacer(Modifier.width(FolkDimens.Size16))
                            Column {
                                Text(
                                    text = umountServiceTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(FolkDimens.Size4))
                                Text(
                                    text = umountServiceSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        ExpressiveSwitch(
                            checked = isUmountEnabled,
                            onCheckedChange = onUmountEnabledChange,
                        )
                    }

                    AnimatedVisibility(visible = isUmountEnabled) {
                        Column(modifier = Modifier.padding(top = FolkDimens.Size12)) {
                            OutlinedTextField(
                                value = umountPaths,
                                onValueChange = onUmountPathsChange,
                                modifier = Modifier.fillMaxWidth().height(FolkDimens.Size160),
                                label = { Text(umountPathsLabel) },
                                placeholder = { Text(umountPathsPlaceholder) },
                                supportingText = { Text(umountPathsHelper) },
                                minLines = 4,
                                maxLines = Int.MAX_VALUE,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.None),
                            )

                            Spacer(modifier = Modifier.height(FolkDimens.Size12))

                            Button(
                                onClick = onUmountSave,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.umount_config_save))
                            }
                        }
                    }
                }
            }
        }

     }
    }
    }

    FunctionSettingsHideSection(
        flat = flat,
        highlightKey = highlightKey,
        kPatchReady = kPatchReady,
        aPatchReady = aPatchReady,
        isKernelSpoofEnabled = isKernelSpoofEnabled,
        onKernelSpoofChange = onKernelSpoofChange,
        kernelSpoofVersion = kernelSpoofVersion,
        onKernelSpoofVersionChange = onKernelSpoofVersionChange,
        kernelSpoofBuildTime = kernelSpoofBuildTime,
        onKernelSpoofBuildTimeChange = onKernelSpoofBuildTimeChange,
        onKernelSpoofSave = onKernelSpoofSave,
        onKernelSpoofRestore = onKernelSpoofRestore,
        isPathHideEnabled = isPathHideEnabled,
        onPathHideChange = onPathHideChange,
        pathHidePaths = pathHidePaths,
        onPathHidePathsChange = onPathHidePathsChange,
        onPathHideSave = onPathHideSave,
        isPathHideUidMode = isPathHideUidMode,
        onPathHideUidModeChange = onPathHideUidModeChange,
        isPathHideFilterSystem = isPathHideFilterSystem,
        onPathHideFilterSystemChange = onPathHideFilterSystemChange,
        selectedUids = selectedUids,
        onUidToggle = onUidToggle,
        isNetIsolateEnabled = isNetIsolateEnabled,
        onNetIsolateChange = onNetIsolateChange,
        niSelectedUids = niSelectedUids,
        onNiUidToggle = onNiUidToggle,
    )
}

