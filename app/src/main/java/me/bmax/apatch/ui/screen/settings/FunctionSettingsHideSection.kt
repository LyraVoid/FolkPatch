package me.bmax.apatch.ui.screen.settings

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSection
import androidx.compose.material.icons.outlined.*

@Composable
fun FunctionSettingsHideSection(
    flat: Boolean,
    highlightKey: String?,
    kPatchReady: Boolean,
    aPatchReady: Boolean,
    isKernelSpoofEnabled: Boolean,
    onKernelSpoofChange: (Boolean) -> Unit,
    kernelSpoofVersion: String,
    onKernelSpoofVersionChange: (String) -> Unit,
    kernelSpoofBuildTime: String,
    onKernelSpoofBuildTimeChange: (String) -> Unit,
    onKernelSpoofSave: () -> Unit,
    onKernelSpoofRestore: () -> Unit,
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
    isNetIsolateEnabled: Boolean,
    onNetIsolateChange: (Boolean) -> Unit,
    niSelectedUids: Set<Int>,
    onNiUidToggle: (Int) -> Unit,
) {
    val context = LocalContext.current
    if (kPatchReady && aPatchReady) {
    SegmentedSection(title = stringResource(R.string.settings_section_function_hide)) {
     SegmentedColumn(flat = flat, highlightKey = highlightKey) {
        item(key = "function_kernel_spoof", visible = kPatchReady && aPatchReady) {
            val kernelSpoofTitle = stringResource(id = R.string.settings_kernel_spoof)
            val kernelSpoofSummary = stringResource(id = R.string.settings_kernel_spoof_summary)
            val versionLabel = stringResource(id = R.string.settings_kernel_spoof_version)
            val buildTimeLabel = stringResource(id = R.string.settings_kernel_spoof_build_time)

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
                                imageVector = Icons.Outlined.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(FolkDimens.Size24),
                            )
                            Spacer(Modifier.width(FolkDimens.Size16))
                            Column {
                                Text(
                                    text = kernelSpoofTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(FolkDimens.Size4))
                                Text(
                                    text = kernelSpoofSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        ExpressiveSwitch(
                            checked = isKernelSpoofEnabled,
                            onCheckedChange = onKernelSpoofChange,
                        )
                    }

                    AnimatedVisibility(
                        visible = isKernelSpoofEnabled,
                        enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                        exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
                    ) {
                        Column(modifier = Modifier.padding(top = FolkDimens.Size12)) {
                            OutlinedTextField(
                                value = kernelSpoofVersion,
                                onValueChange = onKernelSpoofVersionChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(versionLabel) },
                                singleLine = true,
                            )

                            Spacer(modifier = Modifier.height(FolkDimens.Size8))

                            OutlinedTextField(
                                value = kernelSpoofBuildTime,
                                onValueChange = onKernelSpoofBuildTimeChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(buildTimeLabel) },
                                singleLine = true,
                            )

                            Spacer(modifier = Modifier.height(FolkDimens.Size12))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size8),
                            ) {
                                Button(
                                    onClick = onKernelSpoofSave,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.save))
                                }
                                OutlinedButton(
                                    onClick = onKernelSpoofRestore,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(stringResource(R.string.settings_kernel_spoof_restore))
                                }
                            }
                        }
                    }
                }
            }
        }

        functionPathHideItem(
            context = context,
            flat = flat,
            kPatchReady = kPatchReady,
            aPatchReady = aPatchReady,
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
        )
        functionNetIsolateItem(
            context = context,
            flat = flat,
            kPatchReady = kPatchReady,
            aPatchReady = aPatchReady,
            isNetIsolateEnabled = isNetIsolateEnabled,
            onNetIsolateChange = onNetIsolateChange,
            niSelectedUids = niSelectedUids,
            onNiUidToggle = onNiUidToggle,
        )
     }
    }
    }
}
