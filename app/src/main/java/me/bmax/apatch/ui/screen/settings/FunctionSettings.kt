package me.bmax.apatch.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.foundation.text.KeyboardOptions
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import coil.compose.AsyncImage
import coil.request.ImageRequest
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.SplicedColumnGroup
import me.bmax.apatch.ui.component.ToggleSettingCard
import me.bmax.apatch.util.setHideServiceEnabled

@Composable
fun FunctionSettingsContent(
    kPatchReady: Boolean,
    aPatchReady: Boolean,
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
    netIsolateUids: String,
    onNetIsolateUidsChange: (String) -> Unit,
    onNetIsolateSave: () -> Unit,
    flat: Boolean = false,
) {
    val context = LocalContext.current
    val hideServiceTitle = stringResource(id = R.string.settings_hide_service)
    val hideServiceSummary = stringResource(id = R.string.settings_hide_service_summary)
    val umountServiceTitle = stringResource(id = R.string.settings_umount_service)
    val umountServiceSummary = stringResource(id = R.string.settings_umount_service_summary)

    SplicedColumnGroup(flat = flat) {
        item(visible = kPatchReady && aPatchReady) {
            ToggleSettingCard(
                flat = flat,
                icon = Icons.Filled.VisibilityOff,
                title = hideServiceTitle,
                description = hideServiceSummary,
                checked = isHideServiceEnabled,
                onCheckedChange = {
                    setHideServiceEnabled(it)
                    onHideServiceChange(it)
                }
            )
        }

        item(visible = kPatchReady && aPatchReady) {
            val umountPathsLabel = stringResource(id = R.string.umount_config_paths_label)
            val umountPathsPlaceholder = stringResource(id = R.string.umount_config_paths_placeholder)
            val umountPathsHelper = stringResource(id = R.string.umount_config_paths_helper)

            ExpressiveCard(flat = flat) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                imageVector = Icons.Filled.FolderOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = umountServiceTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = umountServiceSummary,
                                    style = MaterialTheme.typography.bodyMedium,
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
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = umountPaths,
                                onValueChange = onUmountPathsChange,
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                label = { Text(umountPathsLabel) },
                                placeholder = { Text(umountPathsPlaceholder) },
                                supportingText = { Text(umountPathsHelper) },
                                minLines = 4,
                                maxLines = Int.MAX_VALUE,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            )

                            Spacer(modifier = Modifier.height(12.dp))

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

        item(visible = kPatchReady && aPatchReady) {
            val kernelSpoofTitle = stringResource(id = R.string.settings_kernel_spoof)
            val kernelSpoofSummary = stringResource(id = R.string.settings_kernel_spoof_summary)
            val versionLabel = stringResource(id = R.string.settings_kernel_spoof_version)
            val buildTimeLabel = stringResource(id = R.string.settings_kernel_spoof_build_time)

            ExpressiveCard(flat = flat) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                imageVector = Icons.Filled.Memory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = kernelSpoofTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = kernelSpoofSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        ExpressiveSwitch(
                            checked = isKernelSpoofEnabled,
                            onCheckedChange = onKernelSpoofChange,
                        )
                    }

                    AnimatedVisibility(visible = isKernelSpoofEnabled) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = kernelSpoofVersion,
                                onValueChange = onKernelSpoofVersionChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(versionLabel) },
                                singleLine = true,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = kernelSpoofBuildTime,
                                onValueChange = onKernelSpoofBuildTimeChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(buildTimeLabel) },
                                singleLine = true,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

        item(visible = kPatchReady && aPatchReady) {
            val pathHideTitle = stringResource(id = R.string.settings_path_hide)
            val pathHideSummary = stringResource(id = R.string.settings_path_hide_summary)
            val pathsLabel = stringResource(id = R.string.path_hide_paths_label)
            val pathsPlaceholder = stringResource(id = R.string.path_hide_paths_placeholder)
            val pathsHelper = stringResource(id = R.string.path_hide_paths_helper)

            ExpressiveCard(flat = flat) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                imageVector = Icons.Filled.HideSource,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = pathHideTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = pathHideSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        ExpressiveSwitch(
                            checked = isPathHideEnabled,
                            onCheckedChange = onPathHideChange,
                        )
                    }

                    AnimatedVisibility(visible = isPathHideEnabled) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = pathHidePaths,
                                onValueChange = onPathHidePathsChange,
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                label = { Text(pathsLabel) },
                                placeholder = { Text(pathsPlaceholder) },
                                supportingText = { Text(pathsHelper) },
                                minLines = 4,
                                maxLines = Int.MAX_VALUE,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onPathHideSave,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.path_hide_save))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val pathCount = pathHidePaths.lines().count { it.isNotBlank() }
                            val appCount = selectedUids.size
                            if (pathCount > 0 || appCount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (pathCount > 0) {
                                        Icon(
                                            Icons.Filled.FolderOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "$pathCount paths",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (pathCount > 0 && appCount > 0) {
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    if (appCount > 0) {
                                        Icon(
                                            Icons.Filled.PersonPin,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "$appCount apps",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // UID Execution Mode
                            val uidModeTitle = stringResource(id = R.string.path_hide_uid_mode)
                            val uidModeSummary = stringResource(id = R.string.path_hide_uid_mode_summary)

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
                                        imageVector = Icons.Filled.PersonPin,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = uidModeTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = uidModeSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                ExpressiveSwitch(
                                    checked = isPathHideUidMode,
                                    onCheckedChange = onPathHideUidModeChange,
                                )
                            }

                            // Filter system/root UID toggle
                            val filterSystemTitle = stringResource(id = R.string.path_hide_filter_system)
                            val filterSystemSummary = stringResource(id = R.string.path_hide_filter_system_summary)

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
                                        imageVector = Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = filterSystemTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = filterSystemSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                ExpressiveSwitch(
                                    checked = isPathHideFilterSystem,
                                    onCheckedChange = onPathHideFilterSystemChange,
                                )
                            }

                            AnimatedVisibility(visible = isPathHideUidMode) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    val pm = context.packageManager
                                    val noAppsText = stringResource(R.string.path_hide_no_apps_selected)

                                    if (selectedUids.isNotEmpty()) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            selectedUids.forEach { uid ->
                                                val pkgs = pm.getPackagesForUid(uid)
                                                val pkgName = pkgs?.firstOrNull()
                                                val pkgInfo = remember(pkgName) {
                                                    pkgName?.let {
                                                        try { pm.getPackageInfo(it, 0) } catch (_: Exception) { null }
                                                    }
                                                }
                                                val label = pkgName?.let {
                                                    try { pm.getApplicationInfo(it, 0).loadLabel(pm).toString() }
                                                    catch (_: Exception) { it }
                                                } ?: "UID $uid"
                                                val isSystemApp = remember(pkgName) {
                                                    pkgName?.let {
                                                        try {
                                                            val appInfo = pm.getApplicationInfo(it, 0)
                                                            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                                                                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                                                        } catch (_: Exception) {
                                                            false
                                                        }
                                                    } ?: false
                                                }

                                                SelectedPathHideAppItem(
                                                    label = label,
                                                    packageName = pkgName ?: "UID $uid",
                                                    uid = uid,
                                                    packageInfo = pkgInfo,
                                                    isSystemApp = isSystemApp,
                                                    onRemove = { onUidToggle(uid) },
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            noAppsText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    AppPickerButton(
                                        selectedUids = selectedUids,
                                        onUidToggle = onUidToggle,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item(visible = kPatchReady && aPatchReady) {
            val niTitle = stringResource(id = R.string.netisolate_title)
            val niSummary = stringResource(id = R.string.netisolate_enable_summary)
            val niUidsLabel = stringResource(id = R.string.netisolate_uids_label)
            val niUidsHint = stringResource(id = R.string.netisolate_uids_hint)

            ExpressiveCard(flat = flat) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                                imageVector = Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = niTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = niSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        ExpressiveSwitch(
                            checked = isNetIsolateEnabled,
                            onCheckedChange = onNetIsolateChange,
                        )
                    }

                    AnimatedVisibility(visible = isNetIsolateEnabled) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = netIsolateUids,
                                onValueChange = onNetIsolateUidsChange,
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                label = { Text(niUidsLabel) },
                                placeholder = { Text(niUidsHint) },
                                minLines = 3,
                                maxLines = Int.MAX_VALUE,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onNetIsolateSave,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedPathHideAppItem(
    label: String,
    packageName: String,
    uid: Int,
    packageInfo: PackageInfo?,
    isSystemApp: Boolean,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = 0.dp,
        onClick = onRemove,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(packageInfo)
                    .crossfade(true)
                    .build(),
                contentDescription = label,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "UID $uid",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isSystemApp) {
                        MiniBadge(text = stringResource(R.string.path_hide_show_system))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MiniBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

data class AppListEntry(
    val uid: Int,
    val packageName: String,
    val label: String,
    val packageInfo: PackageInfo?,
    val isSystemApp: Boolean,
)

@Composable
private fun AppPickerButton(
    selectedUids: Set<Int>,
    onUidToggle: (Int) -> Unit,
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    val selectText = stringResource(R.string.path_hide_select_apps)

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(selectText)
    }

    if (showPicker) {
        AppPickerSheet(
            selectedUids = selectedUids,
            onUidToggle = onUidToggle,
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    selectedUids: Set<Int>,
    onUidToggle: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var searchQuery by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }

    val allApps = remember {
        pm.getInstalledApplications(0)
            .map { appInfo ->
                AppListEntry(
                    uid = appInfo.uid,
                    packageName = appInfo.packageName,
                    label = appInfo.loadLabel(pm).toString(),
                    packageInfo = try { pm.getPackageInfo(appInfo.packageName, 0) } catch (_: Exception) { null },
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                )
            }
            .distinctBy { it.uid }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    val filteredApps = remember(searchQuery, showSystem) {
        allApps
            .filter { showSystem || !it.isSystemApp }
            .let { apps ->
                if (searchQuery.isBlank()) apps
                else apps.filter {
                    it.packageName.contains(searchQuery, true) ||
                        it.label.contains(searchQuery, true)
                }
            }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searchHint = stringResource(R.string.path_hide_search_apps)
    val systemLabel = stringResource(R.string.path_hide_show_system)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.path_hide_select_apps),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(searchHint) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = showSystem, onCheckedChange = { showSystem = it })
                Spacer(Modifier.width(4.dp))
                Text(systemLabel, style = MaterialTheme.typography.bodySmall)
            }

            val dividerColor = MaterialTheme.colorScheme.outlineVariant
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(filteredApps, key = { it.uid }) { app ->
                    val isSelected = app.uid in selectedUids
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUidToggle(app.uid) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .drawBehind {
                                drawLine(
                                    dividerColor,
                                    Offset(0f, size.height),
                                    Offset(size.width, size.height),
                                    strokeWidth = 0.5.dp.toPx(),
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onUidToggle(app.uid) },
                        )
                        Spacer(Modifier.width(8.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(app.packageInfo)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        Text(
                            "UID ${app.uid}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PathHideFilterSystemWarningDialog(
    showDialog: MutableState<Boolean>,
    onConfirm: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false },
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Text(
                    text = stringResource(R.string.path_hide_filter_system_warning_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = stringResource(R.string.path_hide_filter_system_warning_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        showDialog.value = false
                        onConfirm()
                    }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}
