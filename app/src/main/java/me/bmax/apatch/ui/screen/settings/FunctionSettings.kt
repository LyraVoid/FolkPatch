package me.bmax.apatch.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import coil.imageLoader
import me.bmax.apatch.APApplication
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
    selectedUids: Set<Int>,
    onUidToggle: (Int) -> Unit,
    onUidRemoveStale: () -> Unit,
    isUmountEnabled: Boolean,
    onUmountEnabledChange: (Boolean) -> Unit,
    umountPaths: String,
    onUmountPathsChange: (String) -> Unit,
    onUmountSave: () -> Unit,
    flat: Boolean = false,
) {
    val context = LocalContext.current
    val prefs = APApplication.sharedPreferences

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

                            Spacer(modifier = Modifier.height(16.dp))

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

                            AnimatedVisibility(visible = isPathHideUidMode) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    val pm = context.packageManager
                                    val noAppsText = stringResource(R.string.path_hide_no_apps_selected)

                                    if (selectedUids.isNotEmpty()) {
                                        @OptIn(ExperimentalLayoutApi::class)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            selectedUids.forEach { uid ->
                                                val pkgs = pm.getPackagesForUid(uid)
                                                val label = pkgs?.firstOrNull()?.let {
                                                    try { pm.getApplicationInfo(it, 0).loadLabel(pm).toString() }
                                                    catch (_: Exception) { it }
                                                } ?: "UID $uid"
                                                FilterChip(
                                                    selected = true,
                                                    onClick = { onUidToggle(uid) },
                                                    label = {
                                                        Text(
                                                            label,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            style = MaterialTheme.typography.bodySmall,
                                                        )
                                                    },
                                                    trailingIcon = {
                                                        Icon(
                                                            Icons.Filled.Close,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                                                        )
                                                    },
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
    }
}

data class AppListEntry(
    val uid: Int,
    val packageName: String,
    val label: String,
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
            .filter { showSystem || it.uid >= 10000 }
            .map { appInfo ->
                AppListEntry(
                    uid = appInfo.uid,
                    packageName = appInfo.packageName,
                    label = appInfo.loadLabel(pm).toString(),
                )
            }
            .distinctBy { it.uid }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    val filteredApps = remember(searchQuery, showSystem) {
        val base = if (showSystem) {
            pm.getInstalledApplications(0)
        } else {
            pm.getInstalledApplications(0).filter { it.uid >= 10000 }
        }
        base
            .let { apps ->
                if (searchQuery.isBlank()) apps
                else apps.filter {
                    it.packageName.contains(searchQuery, true) ||
                        it.loadLabel(pm).toString().contains(searchQuery, true)
                }
            }
            .map { AppListEntry(it.uid, it.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.uid }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
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
                            model = app.packageName,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            imageLoader = context.imageLoader,
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
