package me.bmax.apatch.ui.screen.plugin

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.component.folk.navOrigin
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.ui.viewmodel.PluginViewModel
import me.bmax.apatch.util.ui.showToast

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginScreen(navigator: DestinationsNavigator) {
    val viewModel: PluginViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val confirmDialog = rememberConfirmDialog()

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val apdReady = (state == APApplication.State.ANDROIDPATCH_INSTALLING || state == APApplication.State.ANDROIDPATCH_INSTALLED || state == APApplication.State.ANDROIDPATCH_NEED_UPDATE)

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

    FolkScaffold(
        title = stringResource(R.string.plugin_title),
        titleStyle = FolkTitleStyle.Flexible,
        subtitle = stringResource(R.string.plugin_subtitle),
        onBack = { navigator.popBackStack() },
        actions = {
            IconButton(
                onClick = dropUnlessResumed {
                    navigator.navigate(com.ramcosta.composedestinations.generated.destinations.OnlinePluginScreenDestination)
                },
                modifier = Modifier.navOrigin(captureBitmap = true),
            ) {
                Icon(Icons.Outlined.Storefront, contentDescription = stringResource(R.string.online_plugin_title))
            }
            IconButton(
                onClick = dropUnlessResumed {
                    navigator.navigate(com.ramcosta.composedestinations.generated.destinations.PluginLogScreenDestination)
                },
                modifier = Modifier.navOrigin(captureBitmap = true),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Article, contentDescription = stringResource(R.string.plugin_log_title))
            }
            IconButton(onClick = { viewModel.fetchPlugins() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.plugin_refresh))
            }
        },
        floatingActionButton = {
            if (apdReady) {
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
            }
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
            if (!apdReady) {
                ApdNotInstalled()
            } else if (viewModel.plugins.isEmpty() && !viewModel.isRefreshing) {
                EmptyPlugins(
                    errorMessage = viewModel.errorMessage,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(top = FolkDimens.Size8, bottom = FolkDimens.Size96),
                ) {
                    splicedLazyColumnGroup(
                        items = viewModel.plugins,
                        key = { _, plugin -> plugin.id },
                    ) { _, plugin ->
                        PluginCard(
                            plugin = plugin,
                            onToggle = { enabled ->
                                scope.launch {
                                    val ok = viewModel.setPluginEnabled(plugin.id, enabled)
                                    val msg = if (ok) {
                                        context.getString(
                                            if (enabled) R.string.plugin_state_enabled else R.string.plugin_state_disabled
                                        )
                                    } else {
                                        context.getString(R.string.plugin_toggle_failed)
                                    }
                                    showToast(context, msg)
                                }
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

