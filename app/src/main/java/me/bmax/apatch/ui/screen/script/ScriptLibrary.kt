package me.bmax.apatch.ui.screen.script

import me.bmax.apatch.ui.component.folk.navOrigin
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ScriptExecutionLogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OnlineScriptScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.data.ScriptInfo
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import me.bmax.apatch.ui.component.FilePickerDialog
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.component.TwoColumnGrid
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.viewmodel.ScriptLibraryViewModel
import me.bmax.apatch.util.ui.LocalSnackbarHost
import java.io.File
import me.bmax.apatch.ui.component.folk.FolkStateView
import androidx.compose.material.icons.outlined.Code

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun ScriptLibraryScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<ScriptLibraryViewModel>()
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { APApplication.sharedPreferences }

    val scripts by viewModel.scripts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var scriptAlias by remember { mutableStateOf("") }
    var selectedScript by remember { mutableStateOf<ScriptInfo?>(null) }
    var expandedScriptId by rememberSaveable { mutableStateOf<String?>(null) }

    val confirmDialog = rememberConfirmDialog()

    val confirmDeleteTitle = stringResource(R.string.script_library_confirm_delete)
    val confirmDeleteLabel = stringResource(R.string.script_library_delete)
    val dismissLabel = stringResource(android.R.string.cancel)
    val deleteSuccessMsg = context.getString(R.string.script_library_delete_success)

    var enableModuleShortcutAdd by remember {
        mutableStateOf(prefs.getBoolean("enable_module_shortcut_add", true))
    }
    var foldSystemModule by remember { mutableStateOf(prefs.getBoolean("fold_system_module", true)) }
    var splicedCardGroup by remember { mutableStateOf(prefs.getBoolean("spliced_card_group", true)) }
    var simpleListBottomBar by remember { mutableStateOf(prefs.getBoolean("simple_list_bottom_bar", false)) }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "enable_module_shortcut_add") {
                enableModuleShortcutAdd = sharedPreferences.getBoolean("enable_module_shortcut_add", true)
            } else if (key == "fold_system_module") {
                foldSystemModule = sharedPreferences.getBoolean("fold_system_module", true)
            } else if (key == "spliced_card_group") {
                splicedCardGroup = sharedPreferences.getBoolean("spliced_card_group", true)
            } else if (key == "simple_list_bottom_bar") {
                simpleListBottomBar = sharedPreferences.getBoolean("simple_list_bottom_bar", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    FolkScaffold(
        title = stringResource(R.string.script_library_title),
        titleStyle = FolkTitleStyle.Flexible,
        subtitle = stringResource(R.string.script_library_subtitle),
        onBack = { navigator.navigateUp() },
        actions = {
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.script_library_add))
            }
            IconButton(
                onClick = { navigator.navigate(OnlineScriptScreenDestination) },
                modifier = Modifier.navOrigin(captureBitmap = true),
            ) {
                Icon(Icons.Outlined.Storefront, contentDescription = stringResource(R.string.online_script_title))
            }
        },
    ) { innerPadding ->
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onRefresh = { viewModel.loadScripts() },
            isRefreshing = isLoading,
            state = pullToRefreshState,
            indicator = { PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = isLoading, modifier = Modifier.align(Alignment.TopCenter)) }
        ) {
            if (scripts.isEmpty()) {
                FolkStateView(
                    title = stringResource(R.string.script_library_empty),
                    icon = Icons.Outlined.Code,
                )
            } else {
                val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600
                if (isWideScreen) {
                    TwoColumnGrid(
                        modifier = Modifier.fillMaxSize(),
                        items = scripts,
                        key = { it.id },
                        verticalSpacing = FolkDimens.Size16,
                        horizontalSpacing = FolkDimens.Size16,
                        contentPadding = PaddingValues(FolkDimens.Size16),
                    ) { script ->
                        ScriptItem(
                            script = script,
                            enableShortcut = enableModuleShortcutAdd,
                            simpleListBottomBar = simpleListBottomBar,
                            foldCard = foldSystemModule,
                            expanded = expandedScriptId == script.id,
                            onExpandToggle = {
                                expandedScriptId = if (expandedScriptId == script.id) null else script.id
                            },
                            onRun = {
                                navigator.navigate(ScriptExecutionLogScreenDestination(script))
                            },
                            onDelete = {
                                selectedScript = script
                                val confirmContent = "${script.alias}\n${script.path}"

                                scope.launch {
                                    val confirmResult = confirmDialog.awaitConfirm(
                                        title = confirmDeleteTitle,
                                        content = confirmContent,
                                        confirm = confirmDeleteLabel,
                                        dismiss = dismissLabel
                                    )
                                    if (confirmResult == me.bmax.apatch.ui.component.ConfirmResult.Confirmed) {
                                        viewModel.removeScript(
                                            script,
                                            onSuccess = {
                                                scope.launch {
                                                    snackBarHost.showSnackbar(deleteSuccessMsg)
                                                }
                                            },
                                            onError = { error ->
                                                scope.launch {
                                                    snackBarHost.showSnackbar(context.getString(R.string.script_library_delete_failed, error))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = FolkDimens.Size24, bottom = FolkDimens.Size16)
                    ) {
                        if (splicedCardGroup) {
                            splicedLazyColumnGroup(
                                items = scripts,
                                key = { _, script -> script.id },
                                contentType = { _, _ -> "ScriptItem" },
                            ) { _, script ->
                                ScriptItem(
                                    script = script,
                                    enableShortcut = enableModuleShortcutAdd,
                                    simpleListBottomBar = simpleListBottomBar,
                                    foldCard = foldSystemModule,
                                    expanded = expandedScriptId == script.id,
                                    onExpandToggle = {
                                        expandedScriptId = if (expandedScriptId == script.id) null else script.id
                                    },
                                    onRun = { navigator.navigate(ScriptExecutionLogScreenDestination(script)) },
                                    onDelete = {
                                        selectedScript = script
                                        scope.launch {
                                            val result = confirmDialog.awaitConfirm(
                                                title = confirmDeleteTitle,
                                                content = "${script.alias}\n${script.path}",
                                                confirm = confirmDeleteLabel,
                                                dismiss = dismissLabel
                                            )
                                            if (result == me.bmax.apatch.ui.component.ConfirmResult.Confirmed) {
                                                viewModel.removeScript(
                                                    script,
                                                    onSuccess = { scope.launch { snackBarHost.showSnackbar(deleteSuccessMsg) } },
                                                    onError = { error ->
                                                        scope.launch { snackBarHost.showSnackbar(context.getString(R.string.script_library_delete_failed, error)) }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            items(scripts, key = { it.id }) { script ->
                                Box(modifier = Modifier.padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size8)) {
                                    ScriptItem(
                                        script = script,
                                        enableShortcut = enableModuleShortcutAdd,
                                        simpleListBottomBar = simpleListBottomBar,
                                        foldCard = foldSystemModule,
                                        expanded = expandedScriptId == script.id,
                                        onExpandToggle = {
                                            expandedScriptId = if (expandedScriptId == script.id) null else script.id
                                        },
                                        onRun = { navigator.navigate(ScriptExecutionLogScreenDestination(script)) },
                                        onDelete = {
                                            selectedScript = script
                                            scope.launch {
                                                val result = confirmDialog.awaitConfirm(
                                                    title = confirmDeleteTitle,
                                                    content = "${script.alias}\n${script.path}",
                                                    confirm = confirmDeleteLabel,
                                                    dismiss = dismissLabel
                                                )
                                                if (result == me.bmax.apatch.ui.component.ConfirmResult.Confirmed) {
                                                    viewModel.removeScript(
                                                        script,
                                                        onSuccess = { scope.launch { snackBarHost.showSnackbar(deleteSuccessMsg) } },
                                                        onError = { error ->
                                                            scope.launch { snackBarHost.showSnackbar(context.getString(R.string.script_library_delete_failed, error)) }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddScriptDialog(
            onDismiss = {
                showAddDialog = false
                selectedFile = null
                scriptAlias = ""
            },
            onConfirm = { file, alias ->
                showAddDialog = false
                viewModel.addScript(
                    file,
                    alias,
                    onSuccess = {
                        scope.launch {
                            snackBarHost.showSnackbar(context.getString(R.string.script_library_add_success))
                        }
                    },
                    onError = { error ->
                        scope.launch {
                            snackBarHost.showSnackbar(context.getString(R.string.script_library_add_failed, error))
                        }
                    }
                )
                selectedFile = null
                scriptAlias = ""
            },
            selectedFile = selectedFile,
            onFileSelected = { selectedFile = it },
            scriptAlias = scriptAlias,
            onAliasChange = { scriptAlias = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScriptDialog(
    onDismiss: () -> Unit,
    onConfirm: (File, String) -> Unit,
    selectedFile: File?,
    onFileSelected: (File) -> Unit,
    scriptAlias: String,
    onAliasChange: (String) -> Unit
) {
    var showFilePicker by remember { mutableStateOf(false) }

    AnimatedVisibility(visible = showFilePicker) {
        FilePickerDialog(
            initialPath = null,
            allowedExtensions = listOf("sh"),
            onDismissRequest = { showFilePicker = false },
            onFileSelected = { file ->
                onFileSelected(file)
                showFilePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.script_library_add_title)) },
        text = {
            Column {
                OutlinedButton(
                    onClick = { showFilePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(FolkDimens.Size8))
                    Text(stringResource(R.string.script_library_select_file))
                }

                if (selectedFile != null) {
                    Spacer(modifier = Modifier.height(FolkDimens.Size8))
                    Text(
                        text = selectedFile?.absolutePath ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(FolkDimens.Size16))

                OutlinedTextField(
                    value = scriptAlias,
                    onValueChange = onAliasChange,
                    label = { Text(stringResource(R.string.script_library_alias)) },
                    placeholder = { Text(stringResource(R.string.script_library_alias_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedFile?.let { onConfirm(it, scriptAlias) }
                },
                enabled = selectedFile != null,
                colors = ButtonDefaults.buttonColors()
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
