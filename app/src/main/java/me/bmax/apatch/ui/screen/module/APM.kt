package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.screen.patches.MODULE_TYPE

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.WebUIActivity
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.viewmodel.APModuleViewModel
import me.bmax.apatch.util.hasMagisk
import me.bmax.apatch.util.ui.LocalSnackbarHost

import com.ramcosta.composedestinations.generated.destinations.ApmBulkInstallScreenDestination
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import me.bmax.apatch.util.apmCustomModuleInfoStorage
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.navigation.LocalBottomBarVisible
import me.bmax.apatch.ui.navigation.LocalIsFloatingNavMode
import me.bmax.apatch.ui.navigation.fabNavBottomClearance
import androidx.compose.ui.platform.LocalConfiguration

import me.bmax.apatch.util.BiometricUtils
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.LocalLayoutVariant
import me.bmax.apatch.ui.LayoutVariant
import me.bmax.apatch.ui.component.rememberSearchAppBarScrollBehavior

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
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
    var showModuleSortSheet by remember { mutableStateOf(false) }

    var showMoreModuleInfo by remember { mutableStateOf(prefs.getBoolean("show_more_module_info", true)) }
    var foldSystemModule by remember { mutableStateOf(prefs.getBoolean("fold_system_module", true)) }
    var simpleListBottomBar by remember { mutableStateOf(prefs.getBoolean("simple_list_bottom_bar", false)) }
    var splicedCardGroup by remember { mutableStateOf(prefs.getBoolean("spliced_card_group", true)) }

    val viewModel = viewModel<APModuleViewModel>()
    val m3eLayout = LocalLayoutVariant.current == LayoutVariant.M3E
    val topAppBarScrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val searchBarScrollBehavior = rememberSearchAppBarScrollBehavior(topAppBarScrollBehavior)

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
                .padding(FolkDimens.Size12),
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
    val searchModuleListState = rememberLazyListState()

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

    FolkScaffold(
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
            },
            onSortClick = { showModuleSortSheet = true },
            scrollBehavior = if (m3eLayout) searchBarScrollBehavior else null,
            searchContent = { bottomPadding, _ ->
                ModuleList(
                    navigator = navigator,
                    viewModel = viewModel,
                    modules = filteredModuleList,
                    showMoreModuleInfo = showMoreModuleInfo,
                    foldSystemModule = foldSystemModule,
                    simpleListBottomBar = simpleListBottomBar,
                    splicedCardGroup = splicedCardGroup,
                    checkStrongBiometric = ::checkStrongBiometric,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding),
                    state = searchModuleListState,
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
                    context = context,
                )
            },
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
                targetValue = if (isFloatingMode && bottomBarVisible && !isLandscape) (-88).dp else FolkDimens.Size0,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
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
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "fabIconCrossfade"
                            ) { isExpanded ->
                                if (isExpanded) {
                                    Icon(
                                        Icons.Outlined.Close,
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
                        icon = { Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = null, modifier = Modifier.size(FolkDimens.Size18)) },
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
                        icon = { Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(FolkDimens.Size18)) },
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
    },
        snackbarHostState = snackBarHost,
        // The module lists already reserve room for the FAB and the floating
        // bar via fabNavBottomClearance.
        addBottomClearance = false,
    ) { innerPadding ->
        when {
            hasMagisk -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(FolkDimens.Size24),
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
                    scrollConnection = if (m3eLayout) {
                        searchBarScrollBehavior.nestedScrollConnection
                    } else {
                        null
                    },
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

    ModuleSortSheet(
        show = showModuleSortSheet,
        groups = viewModel.sortGroups,
        enabledFirst = viewModel.sortEnabledFirst,
        customOrder = viewModel.customOrder,
        modules = viewModel.moduleList,
        onDismiss = { showModuleSortSheet = false },
        onGroupChange = viewModel::setSortGroup,
        onEnabledFirstChange = viewModel::updateSortEnabledFirst,
        onCustomOrderChange = viewModel::setCustomModuleOrder,
        onResetCustomOrder = viewModel::resetCustomModuleOrder,
    )

    // First Use Dialog
    if (showFirstTimeDialog) {
        ExpressiveDialog(
            onDismissRequest = {
                if (dontShowAgain) {
                    prefs.edit().putBoolean("apm_first_use_shown", true).apply()
                }
                showFirstTimeDialog = false
            },
            width = FolkDimens.Size350,
            shape = FolkShape.Corner20,
            blurBehind = false,
            dialogProperties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
        ) {
            Column(modifier = Modifier.padding(FolkDimens.Size16)) {
                Text(
                    text = stringResource(R.string.apm_first_use_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = FolkDimens.Size16)
                )

                Text(
                    text = stringResource(R.string.apm_first_use_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FolkDimens.Size16),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it }
                    )
                    Text(
                        text = stringResource(R.string.dont_show_again),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = FolkDimens.Size8)
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

