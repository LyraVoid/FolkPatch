package me.bmax.apatch.ui.screen.settings.appearance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TabletAndroid
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
import androidx.annotation.StringRes
import androidx.core.content.edit
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.theme.ThemeManager
import me.bmax.apatch.ui.theme.refreshTheme
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.showToast

private data class APColor(
    val name: String, @param:StringRes val nameId: Int
)

private fun colorsList(): List<APColor> {
    return listOf(
        APColor("amber", R.string.amber_theme),
        APColor("blue_grey", R.string.blue_grey_theme),
        APColor("blue", R.string.blue_theme),
        APColor("brown", R.string.brown_theme),
        APColor("cyan", R.string.cyan_theme),
        APColor("deep_orange", R.string.deep_orange_theme),
        APColor("deep_purple", R.string.deep_purple_theme),
        APColor("green", R.string.green_theme),
        APColor("indigo", R.string.indigo_theme),
        APColor("light_blue", R.string.light_blue_theme),
        APColor("light_green", R.string.light_green_theme),
        APColor("lime", R.string.lime_theme),
        APColor("orange", R.string.orange_theme),
        APColor("pink", R.string.pink_theme),
        APColor("purple", R.string.purple_theme),
        APColor("red", R.string.red_theme),
        APColor("sakura", R.string.sakura_theme),
        APColor("teal", R.string.teal_theme),
        APColor("yellow", R.string.yellow_theme),
        APColor("ink_wash", R.string.ink_wash_theme),
    )
}

@Composable
fun colorNameToString(colorName: String): Int {
    return colorsList().find { it.name == colorName }?.nameId ?: R.string.blue_theme
}

@Composable
fun homeLayoutStyleToString(style: String): Int {
    return when (style) {
        "kernelsu" -> R.string.settings_home_layout_grid
        "focus" -> R.string.settings_home_layout_focus
        "circle" -> R.string.settings_home_layout_circle
        "dashboard_ui" -> R.string.settings_home_layout_dashboard_pro
        "stats" -> R.string.settings_home_layout_stats
        else -> R.string.settings_home_layout_default
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeChooseDialog(showDialog: MutableState<Boolean>) {
    val prefs = APApplication.sharedPreferences
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
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
            LazyColumn {
                items(colorsList(), key = { it.name }) {
                    ListItem(
                        headlineContent = { Text(text = stringResource(it.nameId)) },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit { putString("custom_color", it.name) }
                            refreshTheme.value = true
                        })
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLayoutChooseDialog(showDialog: MutableState<Boolean>, onLayoutSelected: (String) -> Unit) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
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
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_home_layout_style),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val currentStyle = prefs.getString("home_layout_style", "circle")

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_default)) },
                            leadingContent = { RadioButton(selected = currentStyle == "default", onClick = null) },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "default").apply()
                                onLayoutSelected("default")
                                showDialog.value = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_grid)) },
                            leadingContent = { RadioButton(selected = currentStyle == "kernelsu", onClick = null) },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "kernelsu").apply()
                                onLayoutSelected("kernelsu")
                                showDialog.value = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_focus)) },
                            leadingContent = { RadioButton(selected = currentStyle == "focus", onClick = null) },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "focus").apply()
                                onLayoutSelected("focus")
                                showDialog.value = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_circle)) },
                            leadingContent = { RadioButton(selected = currentStyle == "circle", onClick = null) },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "circle").apply()
                                onLayoutSelected("circle")
                                showDialog.value = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_dashboard_pro)) },
                            leadingContent = { RadioButton(selected = currentStyle == "dashboard_ui", onClick = null) },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "dashboard_ui").apply()
                                onLayoutSelected("dashboard_ui")
                                showDialog.value = false
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_stats)) },
                            leadingContent = { RadioButton(selected = currentStyle == "stats", onClick = null) },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "stats").apply()
                                onLayoutSelected("stats")
                                showDialog.value = false
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

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
                .widthIn(max = 440.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.theme_export_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.theme_name)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.theme_type),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        listOf(
                            Triple("phone", Icons.Default.PhoneAndroid, R.string.theme_type_phone),
                            Triple("tablet", Icons.Default.TabletAndroid, R.string.theme_type_tablet)
                        ).forEachIndexed { index, (value, icon, label) ->
                            val selected = type == value
                            Surface(
                                onClick = { type = value },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = when (index) {
                                    0 -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                                    else -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
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
                                    width = 1.dp,
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
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(label),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    if (selected) {
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
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
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text(stringResource(R.string.theme_author)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.theme_description)) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
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
                        enabled = name.isNotEmpty()
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
                .widthIn(max = 440.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = stringResource(R.string.theme_import_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = stringResource(R.string.theme_import_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(metadata.name, style = MaterialTheme.typography.titleLarge)
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        if (metadata.description.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Text(
                                text = metadata.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showDialog.value = false }) { Text(stringResource(android.R.string.cancel)) }
                    Button(onClick = { showDialog.value = false; onConfirm() }) { Text(stringResource(R.string.theme_import_action)) }
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
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = true, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.width(310.dp).wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = stringResource(R.string.settings_nav_scheme), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = AlertDialogDefaults.containerColor, tonalElevation = 2.dp) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_nav_mode_floating)) },
                            leadingContent = { RadioButton(selected = currentMode == "floating", onClick = null) },
                            modifier = Modifier.clickable { onModeSelected("floating") }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_nav_mode_auto)) },
                            leadingContent = { RadioButton(selected = currentMode == "auto", onClick = null) },
                            modifier = Modifier.clickable { onModeSelected("auto") }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_nav_mode_bottom)) },
                            leadingContent = { RadioButton(selected = currentMode == "bottom", onClick = null) },
                            modifier = Modifier.clickable { onModeSelected("bottom") }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_nav_mode_rail)) },
                            leadingContent = { RadioButton(selected = currentMode == "rail", onClick = null) },
                            modifier = Modifier.clickable { onModeSelected("rail") }
                        )
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
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
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(decorFitsSystemWindows = true, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.width(310.dp).wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = stringResource(R.string.settings_stats_top_layout), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = AlertDialogDefaults.containerColor, tonalElevation = 2.dp) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_stats_top_layout_list)) },
                            leadingContent = { RadioButton(selected = currentMode == "list", onClick = null) },
                            modifier = Modifier.clickable { onModeSelected("list") }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_stats_top_layout_grid)) },
                            leadingContent = { RadioButton(selected = currentMode == "grid", onClick = null) },
                            modifier = Modifier.clickable { onModeSelected("grid") }
                        )
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
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

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false },
        properties = DialogProperties(decorFitsSystemWindows = true, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.width(340.dp).wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(text = stringResource(R.string.apm_banner_api_config_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
                Text(text = stringResource(R.string.apm_banner_api_config_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
                OutlinedTextField(
                    value = sourceText, onValueChange = { sourceText = it },
                    label = { Text(stringResource(R.string.apm_banner_api_source)) },
                    placeholder = { Text(stringResource(R.string.apm_banner_api_source_hint), style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    trailingIcon = {
                        if (sourceText.isNotEmpty()) {
                            IconButton(onClick = { sourceText = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = stringResource(R.string.apm_banner_api_examples_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = stringResource(R.string.apm_banner_api_examples), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onClearCache() }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.apm_banner_clear_cache)) }
                    Button(
                        onClick = { onConfirm(sourceText); showDialog.value = false; showToast(context, context.getString(R.string.apm_banner_api_source_saved)) },
                        enabled = sourceText.isNotBlank(), modifier = Modifier.weight(1f)
                    ) { Text(stringResource(android.R.string.ok)) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showDialog.value = false }) { Text(stringResource(android.R.string.cancel)) }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}
