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
import org.json.JSONObject
import me.bmax.apatch.util.ui.showToast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.automirrored.outlined.Wysiwyg
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.component.LocalInsideSplicedGroup
import me.bmax.apatch.ui.component.ModuleLabel
import me.bmax.apatch.ui.component.TwoColumnGrid
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.ui.component.WarningCard
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ExecuteAPMActionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.WebUIActivity
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import me.bmax.apatch.ui.component.AdaptiveModuleButtonRow
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.ModuleButtonConfig
import me.bmax.apatch.ui.component.ModuleRemoveButton
import me.bmax.apatch.ui.component.ModuleStateIndicator
import me.bmax.apatch.ui.component.ModuleUpdateButton
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.ModuleInfoData
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.util.DownloadListener
import me.bmax.apatch.util.download
import me.bmax.apatch.util.hasMagisk
import me.bmax.apatch.util.isJailbreakMode
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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenu
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenuItem
import me.bmax.apatch.util.SafeUriResolver
import me.bmax.apatch.util.ModuleBannerStorage
import me.bmax.apatch.util.apmBannerStorage
import me.bmax.apatch.util.resolveModuleDir
import me.bmax.apatch.util.readModulePropBanner
import me.bmax.apatch.util.clearLegacyFolkBanner
import me.bmax.apatch.util.CustomModuleInfo
import me.bmax.apatch.util.apmCustomModuleInfoStorage
import me.bmax.apatch.util.sanitizeModuleKey
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.bannerFadeColor
import me.bmax.apatch.ui.navigation.LocalBottomBarVisible
import me.bmax.apatch.ui.navigation.LocalIsFloatingNavMode
import me.bmax.apatch.ui.navigation.fabNavBottomClearance
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Properties
import java.io.File

import me.bmax.apatch.util.BiometricUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    var splicedCardGroup by remember { mutableStateOf(prefs.getBoolean("spliced_card_group", true)) }

    val viewModel = viewModel<APModuleViewModel>()

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == "show_more_module_info") {
                showMoreModuleInfo = sharedPrefs.getBoolean("show_more_module_info", true)
            } else if (key == "fold_system_module") {
                foldSystemModule = sharedPrefs.getBoolean("fold_system_module", false)
            } else if (key == "simple_list_bottom_bar") {
                simpleListBottomBar = sharedPrefs.getBoolean("simple_list_bottom_bar", false)
            } else if (key == "spliced_card_group") {
                splicedCardGroup = sharedPrefs.getBoolean("spliced_card_group", true)
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
    val hasMagisk by produceState(initialValue = false) {
        value = withContext(Dispatchers.IO) { hasMagisk() }
    }
    val hideInstallButton = hasMagisk

    val moduleListState = rememberLazyListState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
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

    LaunchedEffect(viewModel.moduleList) {
        if (viewModel.moduleList.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                apmCustomModuleInfoStorage.prune(viewModel.moduleList.map { it.id }.toSet())
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

            val isFloatingMode = LocalIsFloatingNavMode.current
            val bottomBarVisible = LocalBottomBarVisible.current.value
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val animatedOffset by animateDpAsState(
                targetValue = if (isFloatingMode && bottomBarVisible && !isLandscape) (-88).dp else 0.dp,
                animationSpec = tween(durationMillis = 300),
                label = "fabOffset"
            )

            var fabExpanded by remember { mutableStateOf(false) }

            val fabContent: @Composable () -> Unit = {
                FloatingActionButtonMenu(
                    expanded = fabExpanded,
                    button = {
                        FloatingActionButton(
                            onClick = { fabExpanded = !fabExpanded },
                            shape = CircleShape,
                            contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1f),
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                        ) {
                            Crossfade(
                                targetState = fabExpanded,
                                animationSpec = tween(durationMillis = 200),
                                label = "fabIconCrossfade"
                            ) { isExpanded ->
                                if (isExpanded) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = null,
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.package_import),
                                        contentDescription = null,
                                    )
                                }
                            }
                        }
                    },
                ) {
                    // 批量刷入 (Bulk Install)
                    FloatingActionButtonMenuItem(
                        onClick = dropUnlessResumed {
                            fabExpanded = false
                            navigator.navigate(ApmBulkInstallScreenDestination())
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text(text = stringResource(R.string.apm_bulk_install_action), style = MaterialTheme.typography.bodyMedium) },
                    )
                    // 安装 (Install)
                    FloatingActionButtonMenuItem(
                        onClick = {
                            fabExpanded = false
                            scope.launch {
                                if (checkStrongBiometric()) {
                                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                                    intent.type = "application/zip"
                                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                                    selectZipLauncher.launch(intent)
                                }
                            }
                        },
                        icon = { Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text(text = stringResource(R.string.apm_install), style = MaterialTheme.typography.bodyMedium) },
                    )
                }
            }
            if (isFloatingMode) {
                Box(modifier = Modifier.offset(y = animatedOffset)) {
                    fabContent()
                }
            } else {
                fabContent()
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
                    splicedCardGroup = splicedCardGroup,
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
                            text = stringResource(R.string.dont_show_again),
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
                            Text(stringResource(R.string.got_it))
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
    splicedCardGroup: Boolean,
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
            showToast(context, startDownloadingText.format(module.name))
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
                        showToast(context, downloading)
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

    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = modifier,
        onRefresh = { viewModel.fetchModuleList() },
        isRefreshing = viewModel.isRefreshing,
        state = pullToRefreshState,
        indicator = { PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = viewModel.isRefreshing, modifier = Modifier.align(Alignment.TopCenter)) }
    ) {
        val configuration = LocalConfiguration.current
        val isWideScreen = configuration.screenWidthDp >= 600

        if (isWideScreen) {
            TwoColumnGrid(
                modifier = Modifier.fillMaxSize(),
                items = if (modules.isEmpty()) emptyList() else modules,
                key = { module -> module.id },
                verticalSpacing = 16.dp,
                horizontalSpacing = 16.dp,
                contentPadding = run {
                    val bottomClearance = fabNavBottomClearance()
                    remember(bottomClearance) {
                        PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            end = 16.dp,
                            bottom = bottomClearance
                        )
                    }
                },
                beforeItems = {
                    if (showMountWarning) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            WarningCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                message = stringResource(R.string.apm_mount_warning_message),
                                onClose = {
                                    prefs.edit()
                                        .putBoolean("apm_mount_warning_shown", true)
                                        .apply()
                                    showMountWarning = false
                                }
                            )
                        }
                    }
                    if (modules.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.errorMessage != null && !viewModel.isRefreshing) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = viewModel.errorMessage ?: stringResource(R.string.apm_load_failed),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { viewModel.fetchModuleList() }) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            } else {
                                Text(
                                    stringResource(R.string.apm_empty), textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                },
                itemContent = { module ->
                    var isChecked by rememberSaveable(module) { mutableStateOf(module.enabled) }
                    val scope = rememberCoroutineScope()
                    val updatedModule = viewModel.getCachedUpdate(module.id)

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

                                    // In jailbreak mode a full reboot would unload the
                                    // runtime-loaded module, so apply without the prompt.
                                    if (!withContext(Dispatchers.IO) { isJailbreakMode() }) {
                                        val result = snackBarHost.showSnackbar(
                                            message = rebootToApply,
                                            actionLabel = reboot,
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            reboot()
                                        }
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
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = run {
                    val bottomClearance = fabNavBottomClearance()
                    remember(bottomClearance) {
                        PaddingValues(
                            start = 0.dp,
                            top = 16.dp,
                            end = 0.dp,
                            bottom = bottomClearance
                        )
                    }
                },
            ) {
                // Warning Banner
                if (showMountWarning) {
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            WarningCard(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                message = stringResource(R.string.apm_mount_warning_message),
                                onClose = {
                                    prefs.edit()
                                        .putBoolean("apm_mount_warning_shown", true)
                                        .apply()
                                    showMountWarning = false
                                }
                            )
                        }
                    }
                }

                when {
                    viewModel.errorMessage != null && !viewModel.isRefreshing -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillParentMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = viewModel.errorMessage ?: stringResource(R.string.apm_load_failed),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { viewModel.fetchModuleList() }) {
                                        Text(stringResource(R.string.retry))
                                    }
                                }
                            }
                        }
                    }

                    modules.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillParentMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.apm_empty), textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    else -> {
                        if (splicedCardGroup) {
                            item { Spacer(Modifier.height(8.dp)) }
                            splicedLazyColumnGroup(
                                items = modules,
                                key = { _, module -> module.id },
                                contentType = { _, _ -> "ModuleItem" },
                            ) { _, module ->
                                var isChecked by rememberSaveable(module) { mutableStateOf(module.enabled) }
                                val scope = rememberCoroutineScope()
                                val updatedModule = viewModel.getCachedUpdate(module.id)

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

                                                // In jailbreak mode a full reboot would unload the
                                                // runtime-loaded module, so apply without the prompt.
                                                if (!withContext(Dispatchers.IO) { isJailbreakMode() }) {
                                                    val result = snackBarHost.showSnackbar(
                                                        message = rebootToApply,
                                                        actionLabel = reboot,
                                                        duration = SnackbarDuration.Long
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        reboot()
                                                    }
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
                            }
                            item { Spacer(Modifier.height(8.dp)) } // bottom clearance handled by contentPadding
                        } else {
                            item { Spacer(Modifier.height(8.dp)) }
                            itemsIndexed(modules, key = { _, module -> module.id }) { _, module ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                var isChecked by rememberSaveable(module) { mutableStateOf(module.enabled) }
                                val scope = rememberCoroutineScope()
                                val updatedModule = viewModel.getCachedUpdate(module.id)

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

                                                // In jailbreak mode a full reboot would unload the
                                                // runtime-loaded module, so apply without the prompt.
                                                if (!withContext(Dispatchers.IO) { isJailbreakMode() }) {
                                                    val result = snackBarHost.showSnackbar(
                                                        message = rebootToApply,
                                                        actionLabel = reboot,
                                                        duration = SnackbarDuration.Long
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        reboot()
                                                    }
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

                                }
                            }
                            item { Spacer(Modifier.height(8.dp)) } // bottom clearance handled by contentPadding
                        }
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
    var showOrderDialog by remember { mutableStateOf(false) }
    var orderedModules by remember { mutableStateOf(viewModel.moduleList) }

    SearchAppBar(
        title = { Text(stringResource(R.string.apm)) },
        searchText = searchQuery,
        onSearchTextChange = onSearchQueryChange,
        onClearClick = { onSearchQueryChange("") },
        dropdownContent = {
            androidx.compose.material3.IconButton(onClick = {
                navigator.navigate(OnlineModuleScreenDestination)
            }) {
                Icon(
                    imageVector = Icons.Outlined.Storefront,
                    contentDescription = "Online Modules"
                )
            }
            androidx.compose.material3.IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                WallpaperAwareDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (viewModel.moduleList.isNotEmpty()) {
                        WallpaperAwareDropdownMenuItem(
                            text = { Text(stringResource(R.string.apm_custom_order)) },
                            onClick = {
                                showMenu = false
                                orderedModules = viewModel.moduleList
                                showOrderDialog = true
                            }
                        )
                    }
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

    if (showOrderDialog) {
        val reorderThreshold = with(LocalDensity.current) { 40.dp.toPx() }
        val dragToReorderDescription = stringResource(R.string.apm_drag_to_reorder)
        var draggedModuleId by remember { mutableStateOf<String?>(null) }
        var draggedDistance by remember { mutableStateOf(0f) }
        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = { Text(stringResource(R.string.apm_custom_order)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    itemsIndexed(orderedModules, key = { _, module -> module.id }) { _, module ->
                        val isDragging = draggedModuleId == module.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isDragging) {
                                        Modifier
                                            .zIndex(1f)
                                            .graphicsLayer { translationY = draggedDistance }
                                    } else {
                                        Modifier.animateItem(
                                            placementSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = module.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Column(
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 4.dp)
                                    .semantics { contentDescription = dragToReorderDescription }
                                    .pointerInput(module.id) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggedModuleId = module.id
                                                draggedDistance = 0f
                                            },
                                            onDragEnd = {
                                                draggedModuleId = null
                                                draggedDistance = 0f
                                            },
                                            onDragCancel = {
                                                draggedModuleId = null
                                                draggedDistance = 0f
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            draggedDistance += dragAmount.y
                                            val currentIndex = orderedModules.indexOfFirst { it.id == module.id }
                                            val targetIndex = when {
                                                draggedDistance > reorderThreshold -> currentIndex + 1
                                                draggedDistance < -reorderThreshold -> currentIndex - 1
                                                else -> currentIndex
                                            }
                                            if (currentIndex >= 0 && targetIndex in orderedModules.indices && targetIndex != currentIndex) {
                                                orderedModules = orderedModules.toMutableList().apply {
                                                    add(targetIndex, removeAt(currentIndex))
                                                }
                                                viewModel.setCustomModuleOrder(orderedModules.map { it.id })
                                                draggedDistance -= if (targetIndex > currentIndex) {
                                                    reorderThreshold
                                                } else {
                                                    -reorderThreshold
                                                }
                                            }
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(2) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 24.dp, height = 3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrderDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.resetCustomModuleOrder()
                    orderedModules = viewModel.moduleList
                }) {
                    Text(stringResource(R.string.apm_reset_order))
                }
            }
        )
    }
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
    val customInfoReloadKeyState = remember { mutableStateOf(0) }
    var customInfoReloadKey by customInfoReloadKeyState
    
    LaunchedEffect(showFolkBannerDialog) {
        if (showFolkBannerDialog) {
            hasFolkBanner = withContext(Dispatchers.IO) {
                apmBannerStorage.read(module.id) != null
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
                        val data = apmBannerStorage.write(context, module.id, it)
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

    val shortcutPreviewBitmap by produceState<Bitmap?>(initialValue = null, key1 = if (showShortcutDialog) effectiveShortcutIconUri else null) {
        if (!showShortcutDialog || effectiveShortcutIconUri.isNullOrBlank()) {
            value = null
        } else {
            value = withContext(Dispatchers.IO) {
                ModuleShortcut.loadShortcutBitmap(context, effectiveShortcutIconUri)
            }
        }
    }
    
    val customInfo by produceState(initialValue = null as CustomModuleInfo?, key1 = module.id, key2 = customInfoReloadKey) {
        value = withContext(Dispatchers.IO) {
            apmCustomModuleInfoStorage.read(module.id)
        }
    }

    val sizeStr = if (showMoreModuleInfo) viewModel.getModuleSize(module.id) else "0 KB"

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

        viewModel.bannerSemaphore.withPermit {
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
        if (cached != null && (cached.bytes != null || cached.url != null)) {
            value = cached
            return@produceState
        }

        val loaded = withContext(Dispatchers.IO) {
            try {
                val folkBanner = if (BackgroundConfig.isFolkBannerEnabled) apmBannerStorage.read(module.id) else null
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
                null
            } catch (e: Exception) {
                null
            }
        }

        if (loaded != null) {
            viewModel.putBannerInfo(module.id, loaded)
            value = loaded
        } else if (cached != null) {
            value = cached
        } else {
            viewModel.putBannerInfo(module.id, APModuleViewModel.BannerInfo(null, null))
            value = APModuleViewModel.BannerInfo(null, null)
        }
        }
    }

    val insideSplicedGroup = LocalInsideSplicedGroup.current

    val cardColor = if (isWallpaperMode) {
        MaterialTheme.colorScheme.surface.copy(alpha = opacity)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
    }

    val cardShape = RoundedCornerShape(20.dp)

    val clickModifier = Modifier
        .fillMaxWidth()
        .animateContentSize()
        .combinedClickable(
            onClick = {
                if (foldSystemModule) {
                    onExpandToggle()
                } else {
                    onClick(module)
                }
            },
            onLongClick = {
                showFolkBannerDialog = true
            }
        )

    val contentBlock: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxWidth()) {
            val bannerUrl = bannerInfo?.url
            val bannerData = bannerInfo?.bytes
            val hasBannerUrl = !bannerUrl.isNullOrEmpty()
            if (bannerData != null || hasBannerUrl) {
                val fadeColor = bannerFadeColor()

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
                            text = customInfo?.name?.takeIf { it.isNotBlank() } ?: module.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None
                        )

                        Text(
                            text = customInfo?.version?.takeIf { it.isNotBlank() } ?: module.version,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None
                        )

                        Text(
                            text = customInfo?.author?.takeIf { it.isNotBlank() } ?: module.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = if (module.remove) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }

                    ExpressiveSwitch(
                        enabled = !module.update,
                        checked = isChecked,
                        onCheckedChange = onCheckChanged
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = customInfo?.description?.takeIf { it.isNotBlank() } ?: module.description,
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

                    val hasUpdateButton = updateUrl.isNotEmpty() && !module.remove && !module.update
                    
                    if (enableModuleShortcutAdd && module.enabled && !module.remove && (module.hasWebUi || module.hasActionScript) && !hasUpdateButton) {
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
                    
                    if (hasUpdateButton) {
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

    // Render: inside spliced group → no Surface wrapper; standalone → Surface card
    if (insideSplicedGroup) {
        Box(modifier = modifier.then(clickModifier)) {
            contentBlock()
        }
    } else {
        Surface(
            modifier = modifier
                .clip(cardShape)
                .then(clickModifier),
            shape = cardShape,
            color = cardColor,
            tonalElevation = 0.dp
        ) {
            contentBlock()
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

    // 自定义模块信息状态
    var customName by remember { mutableStateOf("") }
    var customVersion by remember { mutableStateOf("") }
    var customAuthor by remember { mutableStateOf("") }
    var customDescription by remember { mutableStateOf("") }

    // 弹窗打开时加载自定义信息
    LaunchedEffect(showFolkBannerDialog) {
        if (showFolkBannerDialog) {
            val info = withContext(Dispatchers.IO) {
                apmCustomModuleInfoStorage.read(module.id)
            }
            customName = info?.name?.takeIf { it.isNotBlank() } ?: module.name
            customVersion = info?.version?.takeIf { it.isNotBlank() } ?: module.version
            customAuthor = info?.author?.takeIf { it.isNotBlank() } ?: module.author
            customDescription = info?.description?.takeIf { it.isNotBlank() } ?: module.description
        }
    }

    val customInfoTitle = stringResource(R.string.folk_banner_custom_info_title)
    val customInfoNameLabel = stringResource(R.string.folk_banner_custom_info_name)
    val customInfoVersionLabel = stringResource(R.string.folk_banner_custom_info_version)
    val customInfoAuthorLabel = stringResource(R.string.folk_banner_custom_info_author)
    val customInfoDescriptionLabel = stringResource(R.string.folk_banner_custom_info_description)
    val customInfoSaveLabel = stringResource(R.string.folk_banner_custom_info_save)
    val customInfoResetLabel = stringResource(R.string.folk_banner_custom_info_reset)
    val customInfoSavedMsg = stringResource(R.string.folk_banner_custom_info_saved)
    val customInfoResetMsg = stringResource(R.string.folk_banner_custom_info_reset_done)

    BackgroundOptionsDialog(
        showDialog = showFolkBannerDialog,
        onDismiss = { showFolkBannerDialog = false },
        title = folkBannerTitle,
        showBannerSection = BackgroundConfig.isBannerEnabled && BackgroundConfig.isFolkBannerEnabled,
        selectLabel = folkBannerSelect,
        clearLabel = folkBannerClear,
        hasExisting = hasFolkBanner,
        onSelectImage = {
            pickFolkBannerLauncher.launch("image/*")
        },
        onClearImage = {
            scope.launch {
                loadingDialog.show()
                val success = withContext(Dispatchers.IO) {
                    runCatching {
                        val localCleared = apmBannerStorage.clear(module.id)
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
        customInfoTitle = customInfoTitle,
        customInfoNameLabel = customInfoNameLabel,
        customInfoVersionLabel = customInfoVersionLabel,
        customInfoAuthorLabel = customInfoAuthorLabel,
        customInfoDescriptionLabel = customInfoDescriptionLabel,
        saveLabel = customInfoSaveLabel,
        resetLabel = customInfoResetLabel,
        initialModuleInfo = ModuleInfoData(
            name = customName,
            version = customVersion,
            author = customAuthor,
            description = customDescription
        ),
        hasSavedCustomInfo = customInfo?.hasAnyInfo() == true,
        customInfoReloadKey = customInfoReloadKeyState,
        onSaveModuleInfo = { info ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    apmCustomModuleInfoStorage.write(module.id, CustomModuleInfo(
                        name = info.name.takeIf { it.isNotBlank() },
                        version = info.version.takeIf { it.isNotBlank() },
                        author = info.author.takeIf { it.isNotBlank() },
                        description = info.description.takeIf { it.isNotBlank() },
                    ))
                }
                snackBarHost.showSnackbar(
                    customInfoSavedMsg.format(module.name)
                )
            }
        },
        onResetModuleInfo = {
            scope.launch {
                withContext(Dispatchers.IO) {
                    apmCustomModuleInfoStorage.clear(module.id)
                }
                snackBarHost.showSnackbar(
                    customInfoResetMsg.format(module.name)
                )
            }
        }
    )
}
