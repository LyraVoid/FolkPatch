package me.bmax.apatch.ui.screen

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.viewmodel.PluginViewModel
import me.bmax.apatch.util.ui.showToast

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginScreen(navigator: DestinationsNavigator) {
    val viewModel: PluginViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val confirmDialog = rememberConfirmDialog()

    var pendingInstallUri by remember { mutableStateOf<Uri?>(null) }
    var configPlugin by remember { mutableStateOf<PluginViewModel.PluginInfo?>(null) }
    var configValues by remember { mutableStateOf<Map<String, String>?>(null) }
    var logOutput by remember { mutableStateOf<String?>(null) }

    val installLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
        val data = it.data ?: return@rememberLauncherForActivityResult
        val uri = data.data ?: return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                val cached = java.io.File(context.cacheDir, "plugin_install.zip")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    cached.outputStream().use { output -> input.copyTo(output) }
                }
                viewModel.installPluginZip(cached.absolutePath).also {
                    cached.delete()
                }
            }
            val msg = if (ok) {
                context.getString(R.string.plugin_install_success)
            } else {
                context.getString(R.string.plugin_install_failed)
            }
            showToast(context, msg)
            viewModel.fetchPlugins()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.plugins.isEmpty()) viewModel.fetchPlugins()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plugin_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = dropUnlessResumed { navigator.navigate(com.ramcosta.composedestinations.generated.destinations.PluginLogScreenDestination) }) {
                        Icon(Icons.Outlined.Article, contentDescription = stringResource(R.string.plugin_log_title))
                    }
                    IconButton(onClick = { viewModel.fetchPlugins() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.plugin_refresh))
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = dropUnlessResumed {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "application/zip"
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    installLauncher.launch(intent)
                },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.plugin_install)) },
            )
        },
    ) { paddingValues ->
        val listState = rememberLazyListState()
        val pullToRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            onRefresh = { viewModel.fetchPlugins() },
            isRefreshing = viewModel.isRefreshing,
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = viewModel.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            if (viewModel.plugins.isEmpty() && !viewModel.isRefreshing) {
                EmptyPlugins(viewModel.errorMessage)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        PluginSummaryCard(
                            enabledCount = viewModel.plugins.count { it.enabled },
                            totalCount = viewModel.plugins.size,
                        )
                    }
                    items(viewModel.plugins, key = { it.id }) { plugin ->
                        PluginCard(
                            plugin = plugin,
                            onToggle = { enabled ->
                                viewModel.setPluginEnabled(plugin.id, enabled)
                            },
                            onAction = {
                                scope.launch {
                                    val (ok, output) = viewModel.runCallback(plugin.id, "action")
                                    if (output.isNotBlank()) {
                                        logOutput = output
                                    } else {
                                        val msg = if (ok) {
                                            context.getString(R.string.plugin_action_success)
                                        } else {
                                            context.getString(R.string.plugin_action_failed)
                                        }
                                        showToast(context, msg)
                                    }
                                }
                            },
                            onQuickAction = {
                                scope.launch {
                                    val fn = plugin.quickAction?.function ?: "action"
                                    val (ok, output) = viewModel.runCallback(plugin.id, fn)
                                    if (output.isNotBlank()) {
                                        logOutput = output
                                    } else {
                                        val msg = if (ok) {
                                            context.getString(R.string.plugin_quick_action_success)
                                        } else {
                                            context.getString(R.string.plugin_quick_action_failed)
                                        }
                                        showToast(context, msg)
                                    }
                                }
                            },
                            onConfig = {
                                scope.launch {
                                    val values = withContext(Dispatchers.IO) {
                                        loadConfigValues(viewModel, plugin)
                                    }
                                    configValues = values
                                    configPlugin = plugin
                                }
                            },
                            onViewLog = {
                                scope.launch {
                                    val log = viewModel.fetchLog(plugin.id)
                                    logOutput = log.ifBlank { context.getString(R.string.plugin_log_empty) }
                                }
                            },
                            onRemove = {
                                scope.launch {
                                    val result = confirmDialog.awaitConfirm(
                                        title = context.getString(R.string.plugin_uninstall_title),
                                        content = context.getString(R.string.plugin_uninstall_confirm, plugin.name),
                                        confirm = context.getString(R.string.plugin_uninstall),
                                        dismiss = context.getString(android.R.string.cancel),
                                    )
                                    if (result == ConfirmResult.Confirmed) {
                                        val ok = withContext(Dispatchers.IO) { viewModel.removePlugin(plugin.id) }
                                        val msg = if (ok) {
                                            context.getString(R.string.plugin_uninstall_success)
                                        } else {
                                            context.getString(R.string.plugin_uninstall_failed)
                                        }
                                        showToast(context, msg)
                                        viewModel.fetchPlugins()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    configPlugin?.let { plugin ->
        val initial = configValues ?: emptyMap()
        PluginConfigDialog(
            plugin = plugin,
            initial = initial,
            onDismiss = {
                configPlugin = null
                configValues = null
            },
            onConfirm = { values ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        values.forEach { (key, value) ->
                            viewModel.saveConfigValue(plugin.id, key, value)
                        }
                    }
                    viewModel.fetchPlugins()
                    showToast(context, context.getString(R.string.plugin_config_saved))
                    configPlugin = null
                    configValues = null
                }
            },
        )
    }

    // Plugin execution log output dialog
    logOutput?.let { output ->
        PluginLogDialog(
            output = output,
            onDismiss = { logOutput = null },
        )
    }
}

@Composable
private fun PluginSummaryCard(enabledCount: Int, totalCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Extension,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = stringResource(R.string.plugin_summary_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.plugin_summary_desc, enabledCount, totalCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun PluginCard(
    plugin: PluginViewModel.PluginInfo,
    onToggle: (Boolean) -> Unit,
    onAction: () -> Unit,
    onQuickAction: () -> Unit,
    onConfig: () -> Unit,
    onViewLog: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Extension,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(8.dp).size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    val meta = buildList {
                        add(plugin.id)
                        if (plugin.version.isNotEmpty()) add(plugin.version)
                        if (plugin.author.isNotEmpty()) add(plugin.author)
                    }.joinToString(" · ")
                    if (meta.isNotEmpty()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                IconButton(onClick = onViewLog) {
                    Icon(
                        imageVector = Icons.Outlined.Article,
                        contentDescription = stringResource(R.string.plugin_log_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.plugin_uninstall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = plugin.enabled, onCheckedChange = onToggle)
            }

            if (plugin.description.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (plugin.hasAction || plugin.quickAction != null || plugin.config.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (plugin.quickAction != null) {
                        // Prominent one-tap quick action button.
                        androidx.compose.material3.Button(
                            onClick = onQuickAction,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = quickActionLabel(plugin.quickAction),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else if (plugin.hasAction) {
                        androidx.compose.material3.Button(
                            onClick = onAction,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.plugin_action))
                        }
                    }
                    if (plugin.config.isNotEmpty()) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onConfig,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.plugin_config))
                        }
                    }
                }
            }


        }
    }
}

@Composable
private fun EmptyPlugins(errorMessage: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (errorMessage != null) Icons.Outlined.Warning else Icons.Outlined.Extension,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(
                if (errorMessage != null) R.string.plugin_load_failed else R.string.plugin_empty
            ),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.plugin_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Load current values for all config fields of a plugin (from apd). */
private fun loadConfigValues(
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
private fun configFieldLabel(field: PluginViewModel.PluginConfigField): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language ?: "en"
    return field.labels[locale]?.ifBlank { null } ?: field.label
}

/** Choose the display label for a quick action based on the system locale. */
@Composable
private fun quickActionLabel(action: PluginViewModel.PluginQuickAction): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language ?: "en"
    return action.labels[locale]?.ifBlank { null } ?: action.label
}

/** Dialog that lets the user edit a plugin's config fields. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginConfigDialog(
    plugin: PluginViewModel.PluginInfo,
    initial: Map<String, String>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit,
) {
    val values = remember(plugin.id, initial) {
        mutableStateMapOf<String, String>().apply { putAll(initial) }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.plugin_config_title, plugin.name),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                androidx.compose.material3.Switch(
                                    checked = checked,
                                    onCheckedChange = { values[field.key] = it.toString() },
                                )
                            }
                        }
                        "select" -> {
                            var expanded by remember(field.key) { mutableStateOf(false) }
                            val current = values[field.key] ?: field.default
                            Text(text = configFieldLabel(field), style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                            ) {
                                androidx.compose.material3.OutlinedTextField(
                                    value = current,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
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
                            Spacer(Modifier.height(4.dp))
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
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(values.toMap()) }
            ) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

/** Dialog that displays plugin execution log output. */
@Composable
private fun PluginLogDialog(
    output: String,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.plugin_log_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
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
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}
