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
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.PersonPin
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import android.content.pm.ApplicationInfo
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.component.ExpressiveSwitch
import androidx.compose.material.icons.outlined.*
import android.content.Context
import me.bmax.apatch.ui.component.SegmentedColumnScope

fun SegmentedColumnScope.functionPathHideItem(
    context: Context,
    flat: Boolean,
    kPatchReady: Boolean,
    aPatchReady: Boolean,
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
) {
    item(key = "function_path_hide", visible = kPatchReady && aPatchReady) {
        val pathHideTitle = stringResource(id = R.string.settings_path_hide)
        val pathHideSummary = stringResource(id = R.string.settings_path_hide_summary)
        val pathsLabel = stringResource(id = R.string.path_hide_paths_label)
        val pathsPlaceholder = stringResource(id = R.string.path_hide_paths_placeholder)
        val pathsHelper = stringResource(id = R.string.path_hide_paths_helper)

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
                            imageVector = Icons.Outlined.HideSource,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(FolkDimens.Size24),
                        )
                        Spacer(Modifier.width(FolkDimens.Size16))
                        Column {
                            Text(
                                text = pathHideTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(FolkDimens.Size4))
                            Text(
                                text = pathHideSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    ExpressiveSwitch(
                        checked = isPathHideEnabled,
                        onCheckedChange = onPathHideChange,
                    )
                }

                AnimatedVisibility(
                    visible = isPathHideEnabled,
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
                ) {
                    Column(modifier = Modifier.padding(top = FolkDimens.Size12)) {
                        OutlinedTextField(
                            value = pathHidePaths,
                            onValueChange = onPathHidePathsChange,
                            modifier = Modifier.fillMaxWidth().height(FolkDimens.Size160),
                            label = { Text(pathsLabel) },
                            placeholder = { Text(pathsPlaceholder) },
                            supportingText = { Text(pathsHelper) },
                            minLines = 4,
                            maxLines = Int.MAX_VALUE,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.None),
                        )

                        Spacer(modifier = Modifier.height(FolkDimens.Size12))

                        Button(
                            onClick = onPathHideSave,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.path_hide_save))
                        }

                        Spacer(modifier = Modifier.height(FolkDimens.Size12))

                        val pathCount = pathHidePaths.lines().count { it.isNotBlank() }
                        val appCount = selectedUids.size
                        if (pathCount > 0 || appCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (pathCount > 0) {
                                    Icon(
                                        Icons.Outlined.FolderOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(FolkDimens.Size16),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(FolkDimens.Size4))
                                    Text(
                                        "$pathCount paths",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (pathCount > 0 && appCount > 0) {
                                    Spacer(Modifier.width(FolkDimens.Size12))
                                }
                                if (appCount > 0) {
                                    Icon(
                                        Icons.Outlined.PersonPin,
                                        contentDescription = null,
                                        modifier = Modifier.size(FolkDimens.Size16),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(FolkDimens.Size4))
                                    Text(
                                        "$appCount apps",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(FolkDimens.Size12))
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
                                    imageVector = Icons.Outlined.PersonPin,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(FolkDimens.Size20),
                                )
                                Spacer(Modifier.width(FolkDimens.Size12))
                                Column {
                                    Text(
                                        text = uidModeTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
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
                                    imageVector = Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(FolkDimens.Size20),
                                )
                                Spacer(Modifier.width(FolkDimens.Size12))
                                Column {
                                    Text(
                                        text = filterSystemTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
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

                        AnimatedVisibility(
                            visible = isPathHideUidMode,
                            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
                        ) {
                            Column(modifier = Modifier.padding(top = FolkDimens.Size8)) {
                                val pm = context.packageManager
                                val noAppsText = stringResource(R.string.path_hide_no_apps_selected)

                                if (selectedUids.isNotEmpty()) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(FolkDimens.Size8),
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

                                Spacer(modifier = Modifier.height(FolkDimens.Size8))

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

fun SegmentedColumnScope.functionNetIsolateItem(
    context: Context,
    flat: Boolean,
    kPatchReady: Boolean,
    aPatchReady: Boolean,
    isNetIsolateEnabled: Boolean,
    onNetIsolateChange: (Boolean) -> Unit,
    niSelectedUids: Set<Int>,
    onNiUidToggle: (Int) -> Unit,
) {
    item(key = "function_net_isolate", visible = kPatchReady && aPatchReady) {
        val niTitle = stringResource(id = R.string.netisolate_title)
        val niSummary = stringResource(id = R.string.netisolate_enable_summary)
        val noAppsText = stringResource(R.string.netisolate_no_uids)

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
                            imageVector = Icons.Outlined.WifiOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(FolkDimens.Size24),
                        )
                        Spacer(Modifier.width(FolkDimens.Size16))
                        Column {
                            Text(
                                text = niTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(FolkDimens.Size4))
                            Text(
                                text = niSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    ExpressiveSwitch(
                        checked = isNetIsolateEnabled,
                        onCheckedChange = onNetIsolateChange,
                    )
                }

                AnimatedVisibility(
                    visible = isNetIsolateEnabled,
                    enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
                ) {
                    Column(modifier = Modifier.padding(top = FolkDimens.Size12)) {
                        val pm = context.packageManager

                        if (niSelectedUids.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size8),
                            ) {
                                niSelectedUids.forEach { uid ->
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
                                        onRemove = { onNiUidToggle(uid) },
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

                        Spacer(modifier = Modifier.height(FolkDimens.Size8))

                        AppPickerButton(
                            selectedUids = niSelectedUids,
                            onUidToggle = onNiUidToggle,
                        )
                    }
                }
            }
        }
    }
}
