package me.bmax.apatch.ui.screen.settings.appearance

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.ui.component.SegmentedSelectableItem
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.ThemeManager
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.showToast
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import me.bmax.apatch.core.ui.R as CoreR
import me.bmax.apatch.ui.theme.tokens.FolkShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeExportDialog(
    showDialog: MutableState<Boolean>,
    onConfirm: (ThemeManager.ThemeMetadata) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("phone") }
    var version by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false },
        properties = DialogProperties(decorFitsSystemWindows = true, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = FolkDimens.Size440)
                .wrapContentHeight(),
            shape = FolkShape.Corner28,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(FolkDimens.Size24)) {
                Text(
                    text = stringResource(R.string.theme_export_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = FolkDimens.Size20)
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(FolkDimens.Size12)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.theme_name)) },
                        singleLine = true,
                        shape = FolkShape.Corner16,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.theme_type),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size0)
                    ) {
                        listOf(
                            Triple("phone", Icons.Outlined.PhoneAndroid, R.string.theme_type_phone),
                            Triple("tablet", Icons.Outlined.TabletAndroid, R.string.theme_type_tablet)
                        ).forEachIndexed { index, (value, icon, label) ->
                            val selected = type == value
                            Surface(
                                onClick = { type = value },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(FolkDimens.Size56),
                                shape = when (index) {
                                    0 -> ContinuousCornerShape(topStart = FolkDimens.Size16, bottomStart = FolkDimens.Size16)
                                    else -> ContinuousCornerShape(topEnd = FolkDimens.Size16, bottomEnd = FolkDimens.Size16)
                                },
                                color = if (selected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                contentColor = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                border = BorderStroke(
                                    width = FolkDimens.Size1,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(FolkDimens.Size20))
                                    Spacer(Modifier.width(FolkDimens.Size8))
                                    Text(
                                        text = stringResource(label),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    if (selected) {
                                        Spacer(Modifier.width(FolkDimens.Size8))
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(FolkDimens.Size18)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = version,
                        onValueChange = { version = it },
                        label = { Text(stringResource(R.string.theme_version)) },
                        singleLine = true,
                        shape = FolkShape.Corner16,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text(stringResource(R.string.theme_author)) },
                        singleLine = true,
                        shape = FolkShape.Corner16,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.theme_description)) },
                        shape = FolkShape.Corner16,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = FolkDimens.Size20),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showDialog.value = false }) { Text(stringResource(android.R.string.cancel)) }
                    Button(
                        onClick = {
                            if (name.isNotEmpty()) {
                                showDialog.value = false
                                onConfirm(ThemeManager.ThemeMetadata(name = name, type = type, version = version, author = author, description = description))
                            }
                        },
                        enabled = name.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors()
                    ) { Text(stringResource(R.string.theme_export_action)) }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeImportDialog(
    showDialog: MutableState<Boolean>,
    metadata: ThemeManager.ThemeMetadata,
    onConfirm: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false },
        properties = DialogProperties(decorFitsSystemWindows = true, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = FolkDimens.Size440)
                .wrapContentHeight(),
            shape = FolkShape.Corner28,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(FolkDimens.Size24)) {
                Text(text = stringResource(R.string.theme_import_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.theme_import_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = FolkDimens.Size8, bottom = FolkDimens.Size20)
                )
                Surface(
                    shape = FolkShape.Corner16,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(FolkDimens.Size16)) {
                        Text(metadata.name, style = MaterialTheme.typography.titleLarge)
                        Row(
                            modifier = Modifier.padding(top = FolkDimens.Size8),
                            horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size8)
                        ) {
                            SuggestionChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(if (metadata.type == "tablet") stringResource(R.string.theme_type_tablet) else stringResource(R.string.theme_type_phone)) }
                            )
                            if (metadata.version.isNotEmpty()) {
                                SuggestionChip(onClick = {}, enabled = false, label = { Text(metadata.version) })
                            }
                        }
                        if (metadata.author.isNotEmpty()) {
                            Text(
                                text = metadata.author,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = FolkDimens.Size8)
                            )
                        }
                        if (metadata.description.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = FolkDimens.Size12))
                            Text(
                                text = metadata.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = FolkDimens.Size20), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showDialog.value = false }) { Text(stringResource(android.R.string.cancel)) }
                    Button(onClick = { showDialog.value = false; onConfirm() }, colors = ButtonDefaults.buttonColors()) { Text(stringResource(R.string.theme_import_action)) }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavModeChooseDialog(
    showDialog: MutableState<Boolean>,
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ExpressiveDialog(
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(FolkDimens.Size24)) {
            Text(text = stringResource(R.string.settings_nav_scheme), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = FolkDimens.Size16))
            Surface(shape = FolkShape.Corner12, color = AlertDialogDefaults.containerColor, tonalElevation = FolkDimens.Size2) {
                Column {
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_nav_mode_floating),
                        selected = currentMode == "floating",
                        onClick = { onModeSelected("floating") }
                    )
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_nav_mode_auto),
                        selected = currentMode == "auto",
                        onClick = { onModeSelected("auto") }
                    )
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_nav_mode_bottom),
                        selected = currentMode == "bottom",
                        onClick = { onModeSelected("bottom") }
                    )
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_nav_mode_rail),
                        selected = currentMode == "rail",
                        onClick = { onModeSelected("rail") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsTopLayoutChooseDialog(
    showDialog: MutableState<Boolean>,
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ExpressiveDialog(
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(FolkDimens.Size24)) {
            Text(text = stringResource(R.string.settings_stats_top_layout), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = FolkDimens.Size16))
            Surface(shape = FolkShape.Corner12, color = AlertDialogDefaults.containerColor, tonalElevation = FolkDimens.Size2) {
                Column {
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_stats_top_layout_list),
                        selected = currentMode == "list",
                        onClick = { onModeSelected("list") }
                    )
                    SegmentedSelectableItem(
                        title = stringResource(R.string.settings_stats_top_layout_grid),
                        selected = currentMode == "grid",
                        onClick = { onModeSelected("grid") }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerApiConfigDialog(
    showDialog: MutableState<Boolean>,
    currentSource: String,
    onConfirm: (String) -> Unit,
    onClearCache: () -> Unit
) {
    val context = LocalContext.current
    var sourceText by remember { mutableStateOf(currentSource) }

    ExpressiveDialog(
        onDismissRequest = { showDialog.value = false },
        width = FolkDimens.Size340,
        shape = FolkShape.Corner28,
    ) {
        Column(modifier = Modifier.padding(FolkDimens.Size24).verticalScroll(rememberScrollState())) {
            Text(text = stringResource(R.string.apm_banner_api_config_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = FolkDimens.Size16))
            Text(text = stringResource(R.string.apm_banner_api_config_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = FolkDimens.Size16))
            OutlinedTextField(
                value = sourceText, onValueChange = { sourceText = it },
                label = { Text(stringResource(R.string.apm_banner_api_source)) },
                placeholder = { Text(stringResource(R.string.apm_banner_api_source_hint), style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                trailingIcon = {
                    if (sourceText.isNotEmpty()) {
                        IconButton(onClick = { sourceText = "" }) { Icon(Icons.Outlined.Clear, contentDescription = stringResource(CoreR.string.core_action_clear)) }
                    }
                }
            )
            Spacer(modifier = Modifier.height(FolkDimens.Size12))
            Surface(shape = FolkShape.Corner12, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                Column(modifier = Modifier.padding(FolkDimens.Size12)) {
                    Text(text = stringResource(R.string.apm_banner_api_examples_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(FolkDimens.Size8))
                    Text(text = stringResource(R.string.apm_banner_api_examples), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(FolkDimens.Size16))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size8)) {
                OutlinedButton(onClick = { onClearCache() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.apm_banner_clear_cache)) }
                Button(
                    onClick = { onConfirm(sourceText); showDialog.value = false; showToast(context, context.getString(R.string.apm_banner_api_source_saved)) },
                    enabled = sourceText.isNotBlank(), modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors()
                ) { Text(stringResource(android.R.string.ok)) }
            }
            Spacer(modifier = Modifier.height(FolkDimens.Size8))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showDialog.value = false }) { Text(stringResource(android.R.string.cancel)) }
            }
        }
    }
}
