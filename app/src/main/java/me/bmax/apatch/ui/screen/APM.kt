package me.bmax.apatch.ui.screen

import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.automirrored.outlined.Wysiwyg
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ExecuteAPMActionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.WebUIActivity
import me.bmax.apatch.ui.component.AdaptiveModuleButtonRow
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.ModuleButtonConfig
import me.bmax.apatch.ui.component.ModuleRemoveButton
import me.bmax.apatch.ui.component.ModuleStateIndicator
import me.bmax.apatch.ui.component.ModuleUpdateButton
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.util.DownloadListener
import me.bmax.apatch.util.download
import me.bmax.apatch.util.hasMagisk
import me.bmax.apatch.util.ModuleShortcut
import me.bmax.apatch.util.getRootShell
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.toggleModule
import me.bmax.apatch.util.ui.LocalSnackbarHost
import me.bmax.apatch.util.uninstallModule
import me.bmax.apatch.util.undoUninstallModule

import com.ramcosta.composedestinations.generated.destinations.ApmBulkInstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OnlineModuleScreenDestination
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenu
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenuItem
import me.bmax.apatch.util.ModuleBackupUtils
import me.bmax.apatch.ui.theme.BackgroundConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.io.File

import me.bmax.apatch.util.BiometricUtils

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun APModuleScreen(navigator: DestinationsNavigator) {
    val snackBarHost = LocalSnackbarHost.current
    val context = LocalContext.current

    // First use dialog state
    val prefs = remember { APApplication.sharedPreferences }
    var showFirstTimeDialog by remember { 
        mutableStateOf(!prefs.getBoolean("apm_first_use_shown", false)) 
    }
    var dontShowAgain by remember { mutableStateOf(false) }

    var showMoreModuleInfo by remember { mutableStateOf(prefs.getBoolean("show_more_module_info", true)) }
    var foldSystemModule by remember { mutableStateOf(prefs.getBoolean("fold_system_module", true)) }
    var simpleListBottomBar by remember { mutableStateOf(prefs.getBoolean("simple_list_bottom_bar", false)) }

    val viewModel = viewModel<APModuleViewModel>()

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == "show_more_module_info") {
                showMoreModuleInfo = sharedPrefs.getBoolean("show_more_module_info", true)
            } else if (key == "fold_system_module") {
                foldSystemModule = sharedPrefs.getBoolean("fold_system_module", false)
            } else if (key == "simple_list_bottom_bar") {
                simpleListBottomBar = sharedPrefs.getBoolean("simple_list_bottom_bar", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    val scope = rememberCoroutineScope()

    suspend fun checkStrongBiometric(): Boolean {
        val prefs = APApplication.sharedPreferences
        if (prefs.getBoolean("strong_biometric", false) && prefs.getBoolean("biometric_login", false)) {
            val activity = context as? androidx.fragment.app.FragmentActivity
            return if (activity != null) {
                BiometricUtils.authenticate(activity)
            } else {
                true
            }
        }
        return true
    }

    if (state != APApplication.State.ANDROIDPATCH_INSTALLED && state != APApplication.State.ANDROIDPATCH_NEED_UPDATE) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(id = R.string.apm_not_installed),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    LaunchedEffect(Unit) {
        if (viewModel.moduleList.isEmpty() || viewModel.isNeedRefresh) {
            viewModel.fetchModuleList()
        }
    }

    var pendingInstallUri by remember { mutableStateOf<Uri?>(null) }
    val installConfirmDialog = rememberConfirmDialog(
        onConfirm = {
            pendingInstallUri?.let { uri ->
                navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))
                viewModel.markNeedRefresh()
            }
            pendingInstallUri = null
        },
        onDismiss = {
            pendingInstallUri = null
        }
    )

    val webUILauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.fetchModuleList() }
    //TODO: FIXME -> val isSafeMode = Natives.getSafeMode()
    val isSafeMode = false
    val hasMagisk = hasMagisk()
    val hideInstallButton = isSafeMode || hasMagisk

    val moduleListState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    val filteredModuleList = remember(viewModel.moduleList, searchQuery) {
        if (searchQuery.isEmpty()) {
            viewModel.moduleList
        } else {
            viewModel.moduleList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true) ||
                        it.author.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
        TopBar(
            navigator,
            viewModel,
            snackBarHost,
            searchQuery,
            ::checkStrongBiometric,
            onSearchQueryChange = { searchQuery = it },
            onToggleModuleBanner = {
                val newValue = !BackgroundConfig.isBannerEnabled
                BackgroundConfig.setBannerEnabledState(newValue)
                BackgroundConfig.save(context)
            }
        )
    }, floatingActionButton = if (hideInstallButton) {
        { /* Empty */ }
    } else {
        {
            val selectZipLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode != RESULT_OK) {
                    return@rememberLauncherForActivityResult
                }
                val data = it.data ?: return@rememberLauncherForActivityResult
                val uri = data.data ?: return@rememberLauncherForActivityResult

                Log.i("ModuleScreen", "select zip result: $uri")

                val prefs = APApplication.sharedPreferences
                if (prefs.getBoolean("apm_install_confirm_enabled", true)) {
                    pendingInstallUri = uri
                    val fileName = try {
                        var name = uri.path ?: "Module"
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex >= 0) {
                                name = cursor.getString(nameIndex)
                            }
                        }
                        name
                    } catch (e: Exception) {
                        "Module"
                    }
                    installConfirmDialog.showConfirm(
                        title = context.getString(R.string.apm_install_confirm_title),
                        content = context.getString(R.string.apm_install_confirm_content, fileName),
                        markdown = false
                    )
                } else {
                    navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))
                    viewModel.markNeedRefresh()
                }
            }

            FloatingActionButton(
                contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1f),
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                onClick = {
                    scope.launch {
                        if (checkStrongBiometric()) {
                            // select the zip file to install
                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                            intent.type = "application/zip"
                            intent.addCategory(Intent.CATEGORY_OPENABLE)
                            selectZipLauncher.launch(intent)
                        }
                    }
                }) {
                Icon(
                    painter = painterResource(id = R.drawable.package_import),
                    contentDescription = null
                )
            }
        }
    }, snackbarHost = { SnackbarHost(snackBarHost) }) { innerPadding ->
        when {
            hasMagisk -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.apm_magisk_conflict),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            else -> {
                ModuleList(
                    navigator,
                    viewModel = viewModel,
                    modules = filteredModuleList,
                    showMoreModuleInfo = showMoreModuleInfo,
                    foldSystemModule = foldSystemModule,
                    simpleListBottomBar = simpleListBottomBar,
                    checkStrongBiometric = ::checkStrongBiometric,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    state = moduleListState,
                    onInstallModule = {
                        navigator.navigate(InstallScreenDestination(it, MODULE_TYPE.APM))
                    },
                    onClickModule = { id, name, hasWebUi ->
                        if (hasWebUi) {
                            webUILauncher.launch(
                                Intent(
                                    context, WebUIActivity::class.java
                                ).setData("apatch://webui/$id".toUri()).putExtra("id", id)
                                    .putExtra("name", name)
                            )
                        }
                    },
                    snackBarHost = snackBarHost,
                    context = context
                )
            }
        }
    }

    // First Use Dialog
    if (showFirstTimeDialog) {
        BasicAlertDialog(
            onDismissRequest = {
                if (dontShowAgain) {
                    prefs.edit().putBoolean("apm_first_use_shown", true).apply()
                }
                showFirstTimeDialog = false
            },
            properties = DialogProperties(
                dismissOnClickOutside = false,
                dismissOnBackPress = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(350.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = AlertDialogDefaults.TonalElevation,
                color = AlertDialogDefaults.containerColor,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.apm_first_use_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = stringResource(R.string.apm_first_use_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Text(
                            text = stringResource(R.string.kpm_autoload_do_not_show_again),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = {
                            if (dontShowAgain) {
                                prefs.edit().putBoolean("apm_first_use_shown", true).apply()
                            }
                            showFirstTimeDialog = false
                        }) {
                            Text(stringResource(R.string.kpm_autoload_first_time_confirm))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleList(
    navigator: DestinationsNavigator,
    viewModel: APModuleViewModel,
    modules: List<APModuleViewModel.ModuleInfo>,
    showMoreModuleInfo: Boolean,
    foldSystemModule: Boolean,
    simpleListBottomBar: Boolean,
    checkStrongBiometric: suspend () -> Boolean,
    modifier: Modifier = Modifier,
    state: LazyListState,
    onInstallModule: (Uri) -> Unit,
    onClickModule: (id: String, name: String, hasWebUi: Boolean) -> Unit,
    snackBarHost: SnackbarHostState,
    context: Context
) {
    var expandedModuleId by rememberSaveable { mutableStateOf<String?>(null) }

    // Warning Banner State
    val prefs = remember { APApplication.sharedPreferences }
    var showMountWarning by remember {
        mutableStateOf(!prefs.getBoolean("apm_mount_warning_shown", false))
    }
    val failedEnable = stringResource(R.string.apm_failed_to_enable)
    val failedDisable = stringResource(R.string.apm_failed_to_disable)
    val failedUninstall = stringResource(R.string.apm_uninstall_failed)
    val successUninstall = stringResource(R.string.apm_uninstall_success)
    val reboot = stringResource(id = R.string.reboot)
    val rebootToApply = stringResource(id = R.string.apm_reboot_to_apply)
    val moduleStr = stringResource(id = R.string.apm)
    val uninstall = stringResource(id = R.string.apm_remove)
    val cancel = stringResource(id = android.R.string.cancel)
    val moduleUninstallConfirm = stringResource(id = R.string.apm_uninstall_confirm)
    val updateText = stringResource(R.string.apm_update)
    val changelogText = stringResource(R.string.apm_changelog)
    val downloadingText = stringResource(R.string.apm_downloading)
    val startDownloadingText = stringResource(R.string.apm_start_downloading)

    // Enable Module Shortcut Add
    var enableModuleShortcutAdd by remember {
        mutableStateOf(prefs.getBoolean("enable_module_shortcut_add", true))
    }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "enable_module_shortcut_add") {
                enableModuleShortcutAdd = sharedPreferences.getBoolean("enable_module_shortcut_add", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val loadingDialog = rememberLoadingDialog()
    val confirmDialog = rememberConfirmDialog()

    suspend fun onModuleUpdate(
        module: APModuleViewModel.ModuleInfo,
        changelogUrl: String,
        downloadUrl: String,
        fileName: String
    ) {
        val changelog = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                if (Patterns.WEB_URL.matcher(changelogUrl).matches()) {
                    apApp.okhttpClient.newCall(
                        okhttp3.Request.Builder().url(changelogUrl).build()
                    ).execute().body!!.string()
                } else {
                    changelogUrl
                }
            }
        }


        if (changelog.isNotEmpty()) {
            // changelog is not empty, show it and wait for confirm
            val confirmResult = confirmDialog.awaitConfirm(
                changelogText,
                content = changelog,
                markdown = true,
                confirm = updateText,
            )

            if (confirmResult != ConfirmResult.Confirmed) {
                return
            }
        }

        withContext(Dispatchers.Main) {
            Toast.makeText(
                context, startDownloadingText.format(module.name), Toast.LENGTH_SHORT
            ).show()
        }

        val downloading = downloadingText.format(module.name)
        withContext(Dispatchers.IO) {
            download(
                context,
                downloadUrl,
                fileName,
                downloading,
                onDownloaded = onInstallModule,
                onDownloading = {
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, downloading, Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    suspend fun onModuleUninstall(module: APModuleViewModel.ModuleInfo) {
        if (!checkStrongBiometric()) return
        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = moduleUninstallConfirm.format(module.name),
            confirm = uninstall,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                uninstallModule(module.id)
            }
        }

        if (success) {
            viewModel.fetchModuleList()
        }
        val message = if (success) {
            successUninstall.format(module.name)
        } else {
            failedUninstall.format(module.name)
        }
        snackBarHost.showSnackbar(
            message = message, duration = SnackbarDuration.Short
        )
    }

    suspend fun onModuleUndoUninstall(module: APModuleViewModel.ModuleInfo) {
        if (!checkStrongBiometric()) return

        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                undoUninstallModule(module.id)
            }
        }

        if (success) {
            viewModel.fetchModuleList()
        }
        val message = if (success) {
            context.getString(R.string.apm_undo_uninstall_success).format(module.name)
        } else {
            context.getString(R.string.apm_undo_uninstall_failed).format(module.name)
        }
        snackBarHost.showSnackbar(
            message = message, duration = SnackbarDuration.Short
        )
    }

    PullToRefreshBox(
        modifier = modifier,
        onRefresh = { viewModel.fetchModuleList() },
        isRefreshing = viewModel.isRefreshing
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = remember {
                PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + 16.dp + 56.dp /*  Scaffold Fab Spacing + Fab container height */
                )
            },
        ) {
            // Warning Banner
            if (showMountWarning) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.apm_mount_warning_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = stringResource(R.string.apm_mount_warning_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        prefs.edit()
                                            .putBoolean("apm_mount_warning_shown", true)
                                            .apply()
                                        showMountWarning = false
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Text(stringResource(R.string.apm_mount_warning_button))
                                }
                            }
                        }
                    }
                }
            }

            when {
                modules.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.apm_empty), textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    itemsIndexed(modules, key = { index, module -> "${index}_${module.id}" }) { index, module ->
                        var isChecked by rememberSaveable(module) { mutableStateOf(module.enabled) }
                        val scope = rememberCoroutineScope()
                        val updatedModule by produceState(initialValue = Triple("", "", ""), key1 = module.id) {
                            scope.launch(Dispatchers.IO) {
                                value = viewModel.checkUpdate(module)
                            }
                        }

                        ModuleItem(
                            navigator,
                            module,
                            isChecked,
                            updatedModule.first,
                            showMoreModuleInfo = showMoreModuleInfo,
                            foldSystemModule = foldSystemModule,
                            simpleListBottomBar = simpleListBottomBar,
                            enableModuleShortcutAdd = enableModuleShortcutAdd,
                            expanded = expandedModuleId == module.id,
                            onExpandToggle = {
                                expandedModuleId = if (expandedModuleId == module.id) null else module.id
                            },
                            onUninstall = {
                                scope.launch { onModuleUninstall(module) }
                            },
                            onUndoUninstall = {
                                scope.launch { onModuleUndoUninstall(module) }
                            },
                            onCheckChanged = { checked ->
                                scope.launch {
                                    if (!checkStrongBiometric()) return@launch
                                    val success = loadingDialog.withLoading {
                                        withContext(Dispatchers.IO) {
                                            toggleModule(module.id, !isChecked)
                                        }
                                    }
                                    if (success) {
                                        isChecked = checked
                                        viewModel.fetchModuleList()

                                        val result = snackBarHost.showSnackbar(
                                            message = rebootToApply,
                                            actionLabel = reboot,
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            reboot()
                                        }
                                    } else {
                                        val message = if (isChecked) failedDisable else failedEnable
                                        snackBarHost.showSnackbar(message.format(module.name))
                                    }
                                }
                            },
                            onUpdate = {
                                scope.launch {
                                    onModuleUpdate(
                                        module,
                                        updatedModule.third,
                                        updatedModule.first,
                                        "${module.name}-${updatedModule.second}.zip"
                                    )
                                }
                            },
                            onClick = { clickedModule ->
                                onClickModule(clickedModule.id, clickedModule.name, clickedModule.hasWebUi)
                            })
                        // fix last item shadow incomplete in LazyColumn
                        Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }

        DownloadListener(context, onInstallModule)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    navigator: DestinationsNavigator,
    viewModel: APModuleViewModel,
    snackBarHost: SnackbarHostState,
    searchQuery: String,
    checkStrongBiometric: suspend () -> Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleModuleBanner: () -> Unit
) {
    val confirmDialog = rememberConfirmDialog()
    val scope = rememberCoroutineScope()
    val disableAllTitle = stringResource(R.string.apm_disable_all_title)
    val disableAllConfirm = stringResource(R.string.apm_disable_all_confirm)
    val confirm = stringResource(android.R.string.ok)
    val cancel = stringResource(android.R.string.cancel)
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/gzip")) { uri ->
        uri?.let {
            scope.launch {
                ModuleBackupUtils.backupModules(context, snackBarHost, it)
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                ModuleBackupUtils.restoreModules(context, snackBarHost, it)
                viewModel.fetchModuleList()
            }
        }
    }

    SearchAppBar(
        title = { Text(stringResource(R.string.apm)) },
        searchText = searchQuery,
        onSearchTextChange = onSearchQueryChange,
        onClearClick = { onSearchQueryChange("") },
        leadingActions = {
            androidx.compose.material3.IconButton(onClick = {
                navigator.navigate(OnlineModuleScreenDestination)
            }) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Online Modules"
                )
            }
            androidx.compose.material3.IconButton(onClick = {
                navigator.navigate(ApmBulkInstallScreenDestination())
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Bulk Install"
                )
            }
        },
        dropdownContent = {
            androidx.compose.material3.IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                WallpaperAwareDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.apm_disable_all_title)) },
                        onClick = {
                            showMenu = false
                            scope.launch {
                                if (!checkStrongBiometric()) return@launch
                                val result = confirmDialog.awaitConfirm(
                                    title = disableAllTitle,
                                    content = disableAllConfirm,
                                    confirm = confirm,
                                    dismiss = cancel
                                )
                                if (result == ConfirmResult.Confirmed) {
                                    viewModel.disableAllModules()
                                }
                            }
                        }
                    )
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.apm_backup_title)) },
                        onClick = {
                            showMenu = false
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            backupLauncher.launch("FolkPatch_Modules_Backup_$timeStamp.tar.gz")
                        }
                    )
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.apm_restore_title)) },
                        onClick = {
                            showMenu = false
                            restoreLauncher.launch(arrayOf("application/gzip", "application/x-gzip", "application/x-tar"))
                        }
                    )
                    WallpaperAwareDropdownMenuItem(
                        text = { Text(stringResource(R.string.apm_copy_list_title)) },
                        onClick = {
                            showMenu = false
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val moduleNames = viewModel.moduleList.joinToString("\n") { it.name }
                            val clip = ClipData.newPlainText("Module List", moduleNames)
                            clipboardManager.setPrimaryClip(clip)
                            scope.launch {
                                snackBarHost.showSnackbar(
                                    message = context.getString(R.string.apm_copy_list_success),
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun ModuleLabel(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private const val FOLK_BANNER_FILE_NAME = "FolkBanner"
private const val FOLK_BANNER_DIR_NAME = "folk_banners"

private fun sanitizeBannerKey(raw: String): String {
    return raw.replace(Regex("[^a-zA-Z0-9._-]"), "_")
}

private fun getFolkBannerFile(context: Context, moduleId: String): File {
    val dir = File(context.filesDir, FOLK_BANNER_DIR_NAME)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return File(dir, sanitizeBannerKey(moduleId))
}

private fun resolveModuleDir(rootShell: Shell, moduleId: String): String {
    val suFile = { path: String ->
        SuFile(path).apply { shell = rootShell }
    }
    val defaultDir = "/data/adb/modules/$moduleId"
    return runCatching {
        val direct = suFile(defaultDir)
        if (direct.exists()) {
            direct.path
        } else {
            val modulesRoot = suFile("/data/adb/modules")
            val dirs = modulesRoot.listFiles() ?: return@runCatching defaultDir
            for (dir in dirs) {
                if (!dir.isDirectory) continue
                val propFile = suFile("${dir.path}/module.prop")
                if (!propFile.exists()) continue
                val props = Properties()
                props.load(propFile.newInputStream())
                val id = props.getProperty("id")?.trim()
                if (id == moduleId) {
                    return@runCatching dir.path
                }
            }
            defaultDir
        }
    }.getOrDefault(defaultDir)
}

private fun readFolkBanner(context: Context, moduleId: String): ByteArray? {
    return runCatching {
        val file = getFolkBannerFile(context, moduleId)
        if (file.exists()) {
            file.readBytes().takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }.getOrNull()
}

private fun readModulePropBanner(rootShell: Shell, resolvedDir: String): String? {
    return runCatching {
        val propFile = SuFile("$resolvedDir/module.prop").apply { shell = rootShell }
        if (propFile.exists()) {
            val props = Properties()
            props.load(propFile.newInputStream())
            props.getProperty("banner")?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }.getOrNull()
}

private fun writeFolkBanner(
    context: Context,
    moduleId: String,
    uri: Uri
): ByteArray? {
    val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val file = getFolkBannerFile(context, moduleId)
    file.outputStream().use { it.write(data) }
    return data
}

private fun clearFolkBanner(context: Context, moduleId: String): Boolean {
    val file = getFolkBannerFile(context, moduleId)
    return !file.exists() || file.delete()
}

private fun clearLegacyFolkBanner(rootShell: Shell, resolvedDir: String): Boolean {
    val file = SuFile("$resolvedDir/$FOLK_BANNER_FILE_NAME").apply { shell = rootShell }
    return !file.exists() || file.delete()
}

@Composable
private fun ModuleItem(
    navigator: DestinationsNavigator,
    module: APModuleViewModel.ModuleInfo,
    isChecked: Boolean,
    updateUrl: String,
    showMoreModuleInfo: Boolean,
    foldSystemModule: Boolean,
    simpleListBottomBar: Boolean,
    enableModuleShortcutAdd: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onUninstall: (APModuleViewModel.ModuleInfo) -> Unit,
    onUndoUninstall: (APModuleViewModel.ModuleInfo) -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onUpdate: (APModuleViewModel.ModuleInfo) -> Unit,
    onClick: (APModuleViewModel.ModuleInfo) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val context = LocalContext.current
    val viewModel = viewModel<APModuleViewModel>()
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val shortcutAdd = stringResource(id = R.string.module_shortcut_add)
    val folkBannerTitle = stringResource(R.string.apm_folk_banner_title)
    val folkBannerSelect = stringResource(R.string.apm_folk_banner_select)
    val folkBannerClear = stringResource(R.string.apm_folk_banner_clear)
    val folkBannerSaved = stringResource(R.string.apm_folk_banner_saved)
    val folkBannerCleared = stringResource(R.string.apm_folk_banner_cleared)
    val folkBannerFailed = stringResource(R.string.apm_folk_banner_failed)
    
    var showFolkBannerDialog by remember { mutableStateOf(false) }
    var hasFolkBanner by remember { mutableStateOf(false) }
    var bannerReloadKey by rememberSaveable(module.id) { mutableStateOf(0) }
    
    LaunchedEffect(showFolkBannerDialog) {
        if (showFolkBannerDialog) {
            hasFolkBanner = withContext(Dispatchers.IO) {
                readFolkBanner(context, module.id) != null
            }
        }
    }
    
    val pickFolkBannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val data = writeFolkBanner(context, module.id, it)
                        if (data != null) {
                            runCatching {
                                val rootShell = getRootShell(true)
                                val resolvedDir = resolveModuleDir(rootShell, module.id)
                                clearLegacyFolkBanner(rootShell, resolvedDir)
                            }
                        }
                        data
                    }.getOrNull()
                }
                loadingDialog.hide()
                if (result != null) {
                    viewModel.putBannerInfo(module.id, APModuleViewModel.BannerInfo(result, null))
                    bannerReloadKey++
                    snackBarHost.showSnackbar(folkBannerSaved.format(module.name))
                } else {
                    snackBarHost.showSnackbar(folkBannerFailed.format(module.name))
                }
            }
        }
    }

    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled
    val opacity = if (isWallpaperMode) {
        BackgroundConfig.customBackgroundOpacity.coerceAtLeast(0.35f)
    } else {
        1f
    }

    val bannerImageAlpha = if (BackgroundConfig.isBannerCustomOpacityEnabled) {
        BackgroundConfig.bannerCustomOpacity
    } else {
        if (isWallpaperMode) {
            (0.35f + (opacity - 0.2f) * 0.5f).coerceIn(0.25f, 0.6f)
        } else {
            0.18f
        }
    }
    
    var showShortcutDialog by remember { mutableStateOf(false) }
    var shortcutName by rememberSaveable(module.id) { mutableStateOf(module.name) }
    var shortcutIconUri by remember { mutableStateOf<String?>(null) }
    var shortcutType by rememberSaveable(module.id) { mutableStateOf(if (module.hasWebUi) "webui" else "action") }
    val appIcon = remember(context) { context.packageManager.getApplicationIcon(context.packageName) }
    val pickShortcutIconLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        shortcutIconUri = uri?.toString()
    }

    fun toSuIconUri(path: String?): String? {
        val trimmed = path?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return if (trimmed.startsWith("su://", true)) trimmed else "su://$trimmed"
    }

    val moduleDefaultIconPath = remember(
        module.id,
        shortcutType,
        module.webuiIcon,
        module.actionIcon
    ) {
        val preferred = if (shortcutType == "webui") module.webuiIcon else module.actionIcon
        preferred?.takeIf { it.isNotBlank() }
            ?: module.webuiIcon?.takeIf { it.isNotBlank() }
            ?: module.actionIcon?.takeIf { it.isNotBlank() }
    }
    val moduleDefaultIconUri = remember(moduleDefaultIconPath) { toSuIconUri(moduleDefaultIconPath) }
    val effectiveShortcutIconUri = shortcutIconUri ?: moduleDefaultIconUri

    val shortcutPreviewBitmap by produceState<Bitmap?>(initialValue = null, key1 = effectiveShortcutIconUri) {
        value = if (effectiveShortcutIconUri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                ModuleShortcut.loadShortcutBitmap(context, effectiveShortcutIconUri)
            }
        }
    }
    
    val sizeStr by produceState(initialValue = "0 KB", key1 = module.id) {
        value = withContext(Dispatchers.IO) {
            viewModel.getModuleSize(module.id)
        }
    }

    val bannerInfo by produceState<APModuleViewModel.BannerInfo?>(
        initialValue = viewModel.getBannerInfo(module.id),
        module.id,
        BackgroundConfig.isBannerEnabled,
        BackgroundConfig.isFolkBannerEnabled,
        BackgroundConfig.isBannerApiModeEnabled,
        BackgroundConfig.bannerApiSource,
        bannerReloadKey
    ) {
        if (!BackgroundConfig.isBannerEnabled) {
            value = null
            return@produceState
        }

        // Get effective API source
        val effectiveApiSource = BackgroundConfig.getEffectiveBannerApiSource()

        if (BackgroundConfig.isBannerApiModeEnabled && effectiveApiSource.isNotBlank()) {
            val apiBanner = withContext(Dispatchers.IO) {
                BannerApiService.getModuleBanner(
                    context = context,
                    moduleId = module.id,
                    source = effectiveApiSource
                )
            }
            if (apiBanner != null) {
                viewModel.putBannerInfo(module.id, APModuleViewModel.BannerInfo(apiBanner, null))
                value = APModuleViewModel.BannerInfo(apiBanner, null)
                return@produceState
            }
        }

        val cached = viewModel.getBannerInfo(module.id)
        if (cached != null && (BackgroundConfig.isFolkBannerEnabled || cached.bytes == null)) {
            value = cached
            return@produceState
        }

        val loaded = withContext(Dispatchers.IO) {
            try {
                val folkBanner = if (BackgroundConfig.isFolkBannerEnabled) readFolkBanner(context, module.id) else null
                if (folkBanner != null) {
                    return@withContext APModuleViewModel.BannerInfo(folkBanner, null)
                }
                val rootShell = getRootShell(true)
                val suFile = { path: String ->
                    SuFile(path).apply { shell = rootShell }
                }
                val resolvedDir = resolveModuleDir(rootShell, module.id)
                val propBanner = readModulePropBanner(rootShell, resolvedDir)

                if (!propBanner.isNullOrEmpty() && propBanner.startsWith("http", true)) {
                    return@withContext APModuleViewModel.BannerInfo(null, propBanner)
                }

                val candidates = buildList {
                    if (!propBanner.isNullOrEmpty()) {
                        add(propBanner)
                    }
                    addAll(listOf("banner", "banner.png", "banner.jpg", "banner.jpeg", "banner.webp"))
                }.distinct()

                for (name in candidates) {
                    val file = if (name.startsWith("/")) {
                        suFile(name)
                    } else {
                        suFile("$resolvedDir/$name")
                    }
                    if (file.exists()) {
                        return@withContext APModuleViewModel.BannerInfo(file.newInputStream().use { it.readBytes() }, null)
                    }
                }
                APModuleViewModel.BannerInfo(null, null)
            } catch (e: Exception) {
                APModuleViewModel.BannerInfo(null, null)
            }
        }

        viewModel.putBannerInfo(module.id, loaded)
        value = loaded
    }

    val cardColor = if (isWallpaperMode) {
        MaterialTheme.colorScheme.surface.copy(alpha = opacity)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    }

    val cardShape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .combinedClickable(
                onClick = {
                    if (foldSystemModule) {
                        onExpandToggle()
                    } else {
                        onClick(module)
                    }
                },
                onLongClick = {
                    if (BackgroundConfig.isBannerEnabled && BackgroundConfig.isFolkBannerEnabled) {
                        showFolkBannerDialog = true
                    }
                }
            ),
        shape = cardShape,
        color = cardColor,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            val bannerUrl = bannerInfo?.url
            val bannerData = bannerInfo?.bytes
            val hasBannerUrl = !bannerUrl.isNullOrEmpty()
            if (bannerData != null || hasBannerUrl) {
                val isDark = isSystemInDarkTheme()
                val colorScheme = MaterialTheme.colorScheme
                val isDynamic = colorScheme.primary != colorScheme.secondary
                val fadeColor = when {
                    isDynamic -> colorScheme.surface
                    isDark -> Color(0xFF222222)
                    else -> Color.White
                }

                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = if (hasBannerUrl) {
                            bannerUrl
                        } else {
                            ImageRequest.Builder(context)
                                .data(bannerData)
                                .build()
                         },
                         contentDescription = null,
                         modifier = Modifier.fillMaxSize(),
                         contentScale = ContentScale.Crop,
                         alpha = bannerImageAlpha
                     )
                    val gradientAlpha = if (isWallpaperMode) 0.5f else 0.8f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        fadeColor.copy(alpha = 0.0f),
                                        fadeColor.copy(alpha = gradientAlpha)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val hasAnyLabel = showMoreModuleInfo || module.remove || (updateUrl.isNotEmpty() && !module.update) || module.update
                        if (hasAnyLabel) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                val labelOpacity = (opacity + 0.1f).coerceAtMost(1f)
                                if (showMoreModuleInfo) {
                                    ModuleLabel(
                                        text = sizeStr,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    ModuleLabel(
                                        text = module.id,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                if (module.remove) {
                                    ModuleLabel(
                                        text = stringResource(R.string.apm_remove),
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                } else if (updateUrl.isNotEmpty() && !module.update) {
                                    ModuleLabel(
                                        text = stringResource(R.string.apm_update),
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                } else if (module.update) {
                                    ModuleLabel(
                                        text = "Updated",
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                                
                                if (showMoreModuleInfo && module.hasWebUi && module.enabled && !module.remove) {
                                    ModuleLabel(
                                        text = "WebUI",
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                if (showMoreModuleInfo && module.hasActionScript && module.enabled && !module.remove) {
                                    ModuleLabel(
                                        text = "Action",
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                
                                if (module.isMetamodule && !module.remove) {
                                    ModuleLabel(
                                        text = "META",
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        Text(
                            text = module.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None
                        )

                        Text(
                            text = module.version,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None
                        )

                        Text(
                            text = module.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }

                    Switch(
                        enabled = !module.update,
                        checked = isChecked,
                        onCheckedChange = onCheckChanged
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = !foldSystemModule || expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = shrinkVertically() + fadeOut()
                ) {
           
                    val buttons = mutableListOf<ModuleButtonConfig>()
                    
                    if (module.hasWebUi && module.enabled && !module.remove) {
                        buttons.add(ModuleButtonConfig(
                            icon = Icons.AutoMirrored.Outlined.Wysiwyg,
                            text = stringResource(R.string.apm_webui_open),
                            contentDescription = stringResource(R.string.apm_webui_open),
                            onClick = { onClick(module) }
                        ))
                    }
                    
                    if (module.hasActionScript && module.enabled && !module.remove) {
                        buttons.add(ModuleButtonConfig(
                            icon = Icons.Outlined.Terminal,
                            text = stringResource(R.string.apm_action),
                            contentDescription = stringResource(R.string.apm_action),
                            onClick = {
                                navigator.navigate(ExecuteAPMActionScreenDestination(module.id))
                                viewModel.markNeedRefresh()
                            }
                        ))
                    }
                    
                    if (enableModuleShortcutAdd && module.enabled && !module.remove && (module.hasWebUi || module.hasActionScript)) {
                        buttons.add(ModuleButtonConfig(
                            icon = Icons.Outlined.Add,
                            text = shortcutAdd,
                            contentDescription = shortcutAdd,
                            onClick = { 
                                shortcutName = module.name
                                shortcutIconUri = null
                                shortcutType = if (module.hasWebUi) "webui" else "action"
                                showShortcutDialog = true 
                            }
                        ))
                    }
                    
                    if (updateUrl.isNotEmpty() && !module.remove && !module.update) {
                        buttons.add(ModuleButtonConfig(
                            icon = Icons.Outlined.Download,
                            text = stringResource(R.string.apm_update),
                            contentDescription = stringResource(R.string.apm_update),
                            onClick = { onUpdate(module) }
                        ))
                    }
                    

                    val deleteButton = ModuleButtonConfig(
                        icon = if (module.remove) Icons.Outlined.Restore else Icons.Outlined.Delete,
                        text = if (module.remove) stringResource(R.string.apm_undo) else stringResource(R.string.apm_remove),
                        contentDescription = if (module.remove) stringResource(R.string.apm_undo) else stringResource(R.string.apm_remove),
                        onClick = {
                            if (module.remove) {
                                onUndoUninstall(module)
                            } else {
                                onUninstall(module)
                            }
                        },
                        colors = if (simpleListBottomBar) ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f))
                        ) else if (module.remove) ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f)),
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) else ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f)),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                    
                    AdaptiveModuleButtonRow(
                        buttons = buttons,
                        trailingButton = deleteButton,
                        simpleListBottomBar = simpleListBottomBar,
                        spacing = if (simpleListBottomBar) 12 else 8,
                        opacity = opacity
                    )
                }
            }
        }
    }

    if (showShortcutDialog) {
        AlertDialog(
            onDismissRequest = { showShortcutDialog = false },
            title = { Text(stringResource(R.string.module_shortcut_add)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = shortcutName,
                        onValueChange = { shortcutName = it },
                        label = { Text(stringResource(R.string.module_shortcut_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.module_shortcut_icon))
                        Spacer(Modifier.width(12.dp))
                        if (shortcutPreviewBitmap != null) {
                            Image(
                                bitmap = shortcutPreviewBitmap!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        } else if (shortcutIconUri != null) {
                            AsyncImage(
                                model = shortcutIconUri,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            AsyncImage(
                                model = appIcon,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { pickShortcutIconLauncher.launch("image/*") }) {
                            Text(stringResource(R.string.module_shortcut_icon_select))
                        }
                        TextButton(onClick = { shortcutIconUri = null }) {
                            Text(stringResource(R.string.module_shortcut_icon_default))
                        }
                    }
                    if (module.hasWebUi && module.hasActionScript) {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.module_shortcut_type))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = shortcutType == "webui",
                                onClick = { shortcutType = "webui" }
                            )
                            Text(stringResource(R.string.module_shortcut_type_webui))
                            Spacer(Modifier.width(24.dp))
                            RadioButton(
                                selected = shortcutType == "action",
                                onClick = { shortcutType = "action" }
                            )
                            Text(stringResource(R.string.module_shortcut_type_action))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (shortcutType == "webui" && module.hasWebUi) {
                        ModuleShortcut.createModuleWebUiShortcut(context, module.id, shortcutName.ifEmpty { module.name }, effectiveShortcutIconUri)
                    } else if (module.hasActionScript) {
                        ModuleShortcut.createModuleActionShortcut(context, module.id, shortcutName.ifEmpty { module.name }, effectiveShortcutIconUri)
                    }
                    showShortcutDialog = false
                }) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showShortcutDialog = false }) {
                    Text(text = stringResource(id = android.R.string.cancel))
                }
            }
        )
    }

    if (showFolkBannerDialog) {
        AlertDialog(
            onDismissRequest = { showFolkBannerDialog = false },
            title = { Text(folkBannerTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            showFolkBannerDialog = false
                            pickFolkBannerLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(folkBannerSelect)
                    }
                    if (hasFolkBanner) {
                        Button(
                            onClick = {
                                showFolkBannerDialog = false
                                scope.launch {
                                    loadingDialog.show()
                                    val success = withContext(Dispatchers.IO) {
                                        runCatching {
                                            val localCleared = clearFolkBanner(context, module.id)
                                            val legacyCleared = runCatching {
                                                val rootShell = getRootShell(true)
                                                val resolvedDir = resolveModuleDir(rootShell, module.id)
                                                clearLegacyFolkBanner(rootShell, resolvedDir)
                                            }.getOrDefault(false)
                                            localCleared || legacyCleared
                                        }.getOrDefault(false)
                                    }
                                    loadingDialog.hide()
                                    if (success) {
                                        viewModel.removeBannerInfo(module.id)
                                        bannerReloadKey++
                                        snackBarHost.showSnackbar(folkBannerCleared.format(module.name))
                                    } else {
                                        snackBarHost.showSnackbar(folkBannerFailed.format(module.name))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(folkBannerClear)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolkBannerDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
