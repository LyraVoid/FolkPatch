package me.bmax.apatch.ui.screen.settings

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.ui.component.folk.folkInsetColor
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import coil.compose.AsyncImage
import coil.request.ImageRequest
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.tokens.FolkShape
import androidx.compose.material.icons.outlined.*

@Composable
fun SelectedPathHideAppItem(
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
        shape = FolkShape.Corner16,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        tonalElevation = FolkDimens.Size0,
        onClick = onRemove,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(packageInfo)
                    .crossfade(true)
                    .build(),
                contentDescription = label,
                modifier = Modifier
                    .size(FolkDimens.Size42)
                    .clip(CircleShape),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = FolkDimens.Size12),
                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size2),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
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
                    horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size8),
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
                    .background(folkInsetColor())
                    .padding(FolkDimens.Size8),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.size(FolkDimens.Size18),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun MiniBadge(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = FolkDimens.Size8, vertical = FolkDimens.Size3),
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
fun AppPickerButton(
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
        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(FolkDimens.Size18))
        Spacer(Modifier.width(FolkDimens.Size8))
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
fun AppPickerSheet(
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = FolkDimens.Size16),
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

            Spacer(Modifier.height(FolkDimens.Size8))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FolkDimens.Size16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(searchHint) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = showSystem, onCheckedChange = { showSystem = it })
                Spacer(Modifier.width(FolkDimens.Size4))
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
                            .padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size8)
                            .drawBehind {
                                drawLine(
                                    dividerColor,
                                    Offset(0f, size.height),
                                    Offset(size.width, size.height),
                                    strokeWidth = FolkDimens.SizeHalf.toPx(),
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onUidToggle(app.uid) },
                        )
                        Spacer(Modifier.width(FolkDimens.Size8))
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(app.packageInfo)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.size(FolkDimens.Size36).clip(CircleShape),
                        )
                        Spacer(Modifier.width(FolkDimens.Size12))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.label,
                                style = MaterialTheme.typography.bodySmall,
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
    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
    ) {
        Column(modifier = Modifier.padding(PaddingValues(all = FolkDimens.Size24))) {
            Text(
                text = stringResource(R.string.path_hide_filter_system_warning_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = FolkDimens.Size16)
            )
            Text(
                text = stringResource(R.string.path_hide_filter_system_warning_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = FolkDimens.Size24)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(Modifier.width(FolkDimens.Size8))
                Button(onClick = {
                    showDialog.value = false
                    onConfirm()
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}
