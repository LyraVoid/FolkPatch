package me.bmax.apatch.ui.screen.plugin

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenu
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenuItem
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.viewmodel.PluginViewModel
import me.bmax.apatch.util.pickLocalizedString
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import me.bmax.apatch.ui.component.folk.FolkStateView
import me.bmax.apatch.ui.component.folk.FolkStateTone
import me.bmax.apatch.ui.theme.tokens.FolkShape

@Composable
fun PluginCard(
    plugin: PluginViewModel.PluginInfo,
    onToggle: (Boolean) -> Unit,
    onAction: () -> Unit,
    onQuickAction: () -> Unit,
    onConfig: () -> Unit,
    onViewLog: () -> Unit,
    onRemove: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    // secondaryContainer 必须与其 on 色配对，这里保持不透明。
    val iconContainerColor = MaterialTheme.colorScheme.secondaryContainer
    val buttonColors = ButtonDefaults.filledTonalButtonColors()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FolkDimens.Size16)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = FolkShape.Corner12,
                color = iconContainerColor
            ) {
                Icon(
                    imageVector = Icons.Outlined.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(FolkDimens.Size8).size(FolkDimens.Size20)
                )
            }
            Spacer(Modifier.width(FolkDimens.Size12))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plugin.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val metadata = buildList {
                    if (plugin.version.isNotEmpty()) add("v${plugin.version}")
                    if (plugin.author.isNotEmpty()) add(plugin.author)
                }.joinToString(" · ")
                if (metadata.isNotEmpty()) {
                    Spacer(Modifier.height(FolkDimens.Size2))
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.home_stats_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                WallpaperAwareDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.plugin_log_title)) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Outlined.Article, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onViewLog()
                        },
                    )
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.plugin_uninstall)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onRemove()
                        },
                    )
                }
            }
            ExpressiveSwitch(checked = plugin.enabled, onCheckedChange = onToggle)
        }

        if (plugin.description.isNotEmpty()) {
            Spacer(Modifier.height(FolkDimens.Size12))
            Text(
                text = pluginDescription(plugin),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (plugin.hasAction || plugin.quickAction != null || plugin.config.isNotEmpty()) {
            Spacer(Modifier.height(FolkDimens.Size16))
            Row(horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size8)) {
                if (plugin.quickAction != null) {
                    // Prominent one-tap quick action button.
                    FilledTonalButton(
                        onClick = onQuickAction,
                        modifier = Modifier.weight(1f),
                        colors = buttonColors,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(FolkDimens.Size16)
                        )
                        Spacer(Modifier.width(FolkDimens.Size6))
                        Text(
                            text = quickActionLabel(plugin.quickAction),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                } else if (plugin.hasAction) {
                    FilledTonalButton(
                        onClick = onAction,
                        modifier = Modifier.weight(1f),
                        colors = buttonColors,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(FolkDimens.Size16)
                        )
                        Spacer(Modifier.width(FolkDimens.Size6))
                        Text(stringResource(R.string.plugin_action))
                    }
                }
                if (plugin.config.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = onConfig,
                        modifier = Modifier.weight(1f),
                        colors = buttonColors,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(FolkDimens.Size16)
                        )
                        Spacer(Modifier.width(FolkDimens.Size6))
                        Text(stringResource(R.string.plugin_config))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPlugins(errorMessage: String?, modifier: Modifier = Modifier) {
    FolkStateView(
        title = stringResource(
            if (errorMessage != null) R.string.plugin_load_failed else R.string.plugin_empty
        ),
        modifier = modifier,
        icon = if (errorMessage != null) Icons.Outlined.Warning else Icons.Outlined.Extension,
        hint = stringResource(R.string.plugin_empty_hint),
        tone = if (errorMessage != null) FolkStateTone.Critical else FolkStateTone.Neutral,
    )
}

/** Centered prompt shown when APD is not installed. */
@Composable
fun ApdNotInstalled() {
    FolkStateView(
        title = stringResource(R.string.plugin_summary_title),
        icon = Icons.Outlined.Extension,
        hint = stringResource(R.string.plugin_summary_not_installed),
    )
}

/** Load current values for all config fields of a plugin (from apd). */
fun loadConfigValues(
    viewModel: PluginViewModel,
    plugin: PluginViewModel.PluginInfo,
): Map<String, String> {
    return plugin.config.associate { field ->
        val saved = viewModel.getConfigValue(plugin.id, field.key)
        field.key to saved.ifEmpty { field.default }
    }
}

/** Choose the display label for a config field based on the system locale. */
@Composable
fun configFieldLabel(field: PluginViewModel.PluginConfigField): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    return pickLocalizedString(field.labels, locale) ?: field.label
}

/** Choose the display label for a quick action based on the system locale. */
@Composable
fun quickActionLabel(action: PluginViewModel.PluginQuickAction): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    return pickLocalizedString(action.labels, locale) ?: action.label
}

/** Choose the display description for a plugin based on the system locale. */
@Composable
fun pluginDescription(plugin: PluginViewModel.PluginInfo): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    return pickLocalizedString(plugin.descriptions, locale) ?: plugin.description
}

/** Dialog that lets the user edit a plugin's config fields. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginConfigDialog(
    plugin: PluginViewModel.PluginInfo,
    initial: Map<String, String>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    val values = remember(plugin.id, initial) {
        mutableStateMapOf<String, String>().apply { putAll(initial) }
    }

    ExpressiveDialog(
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(FolkDimens.Size24)) {
            Text(
                text = stringResource(R.string.plugin_config_title, plugin.name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = FolkDimens.Size16)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
            ) {
                plugin.config.forEach { field ->
                    when (field.type) {
                        "bool" -> {
                            val stored = values[field.key]
                            val checked = when {
                                stored != null -> stored == "true"
                                else -> field.default == "true"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = configFieldLabel(field),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                ExpressiveSwitch(
                                    checked = checked,
                                    onCheckedChange = { values[field.key] = it.toString() },
                                )
                            }
                        }
                        "select" -> {
                            var expanded by remember(field.key) { mutableStateOf(false) }
                            val current = values[field.key] ?: field.default
                            Text(text = configFieldLabel(field), style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(FolkDimens.Size4))
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                            ) {
                                androidx.compose.material3.OutlinedTextField(
                                    value = current,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().menuAnchor(
                                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    ),
                                    singleLine = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                )
                                androidx.compose.material3.DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                ) {
                                    field.options.forEach { option ->
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                values[field.key] = option
                                                expanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            Text(text = configFieldLabel(field), style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(FolkDimens.Size4))
                            androidx.compose.material3.OutlinedTextField(
                                value = values[field.key] ?: field.default,
                                onValueChange = { values[field.key] = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = if (field.type == "number") {
                                    androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    )
                                } else {
                                    androidx.compose.foundation.text.KeyboardOptions.Default
                                },
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FolkDimens.Size24),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(Modifier.width(FolkDimens.Size8))
                TextButton(onClick = { onConfirm(values.toMap()) }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}

/** Dialog that displays plugin execution log output. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginLogDialog(
    output: String,
    onDismiss: () -> Unit,
) {
    ExpressiveDialog(
        onDismissRequest = onDismiss,
    ) {
        Column(modifier = Modifier.padding(FolkDimens.Size24)) {
            Text(
                text = stringResource(R.string.plugin_log_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = FolkDimens.Size16)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = output,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = FolkDimens.Size24),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}
