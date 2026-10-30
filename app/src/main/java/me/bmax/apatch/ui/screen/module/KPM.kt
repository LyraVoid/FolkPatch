package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.screen.patches.MODULE_TYPE

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.util.Log
import me.bmax.apatch.util.ui.showToast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.viewmodel.KPModuleViewModel
import me.bmax.apatch.ui.viewmodel.PatchesViewModel

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalConfiguration
import me.bmax.apatch.ui.navigation.LocalBottomBarVisible
import me.bmax.apatch.ui.navigation.LocalIsFloatingNavMode
import me.bmax.apatch.ui.navigation.fabNavBottomClearance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Settings
import android.widget.Toast

import android.content.SharedPreferences
import androidx.compose.runtime.DisposableEffect

import me.bmax.apatch.util.BiometricUtils
import me.bmax.apatch.util.isJailbreakMode
import me.bmax.apatch.util.kpmCustomModuleInfoStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.bmax.apatch.ui.LocalLayoutVariant
import me.bmax.apatch.ui.LayoutVariant
import me.bmax.apatch.ui.component.rememberSearchAppBarScrollBehavior
import me.bmax.apatch.core.ui.R as CoreR


private const val TAG = "KernelPatchModule"
private val kpmInstallMutex = Mutex()

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KPModuleScreen(navigator: DestinationsNavigator) {
    var jailbreakMode by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        jailbreakMode = withContext(Dispatchers.IO) { isJailbreakMode() }
    }

    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    if (state == APApplication.State.UNKNOWN_STATE) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(FolkDimens.Size12),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(id = R.string.kpm_kp_not_installed),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    val viewModel = viewModel<KPModuleViewModel>()
    val m3eLayout = LocalLayoutVariant.current == LayoutVariant.M3E
    val topAppBarScrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val searchBarScrollBehavior = rememberSearchAppBarScrollBehavior(topAppBarScrollBehavior)

    val context = LocalContext.current

    val prefs = remember { APApplication.sharedPreferences }
    var showMoreModuleInfo by remember { mutableStateOf(prefs.getBoolean("show_more_module_info", true)) }
    var foldSystemModule by remember { mutableStateOf(prefs.getBoolean("fold_system_module", true)) }
    var simpleListBottomBar by remember { mutableStateOf(prefs.getBoolean("simple_list_bottom_bar", false)) }
    var splicedCardGroup by remember { mutableStateOf(prefs.getBoolean("spliced_card_group", true)) }
    var showKpmStatusBadge by remember { mutableStateOf(prefs.getBoolean("show_kpm_status_badge", true)) }

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
            } else if (key == "show_kpm_status_badge") {
                showKpmStatusBadge = sharedPrefs.getBoolean("show_kpm_status_badge", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.moduleList.isEmpty() || viewModel.isNeedRefresh || viewModel.embeddedKpmNames == null) {
            viewModel.fetchModuleList()
        }
    }

    val kpModuleListState = rememberLazyListState()
    val searchKpModuleListState = rememberLazyListState()

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
                kpmCustomModuleInfoStorage.prune(viewModel.moduleList.map { it.name }.toSet())
            }
        }
    }

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

    FolkScaffold(
        topBar = {
            KPMTopBar(
                navigator,
                searchQuery,
                onSearchQueryChange = { searchQuery = it },
                scrollBehavior = if (m3eLayout) searchBarScrollBehavior else null,
                searchContent = { bottomPadding, _ ->
                    KPModuleList(
                        viewModel = viewModel,
                        moduleList = filteredModuleList,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPadding),
                        state = searchKpModuleListState,
                        showMoreModuleInfo = showMoreModuleInfo,
                        foldSystemModule = foldSystemModule,
                        simpleListBottomBar = simpleListBottomBar,
                        splicedCardGroup = splicedCardGroup,
                        showKpmStatusBadge = showKpmStatusBadge,
                        checkStrongBiometric = ::checkStrongBiometric,
                    )
                },
            )
        },
        floatingActionButton = run {
        {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            val moduleLoad = stringResource(id = R.string.kpm_load)
            val moduleInstall = stringResource(id = R.string.kpm_install)
            val moduleEmbed = stringResource(id = R.string.kpm_embed)
            val successToastText = stringResource(id = R.string.kpm_load_toast_succ)
            val installSuccessToastText = stringResource(id = R.string.kpm_install_toast_succ)
            val failToastText = stringResource(id = R.string.kpm_load_toast_failed)
            val loadingDialog = rememberLoadingDialog()

            val selectZipLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode != RESULT_OK) {
                    return@rememberLauncherForActivityResult
                }
                val data = it.data ?: return@rememberLauncherForActivityResult
                val uri = data.data ?: return@rememberLauncherForActivityResult

                Log.i(TAG, "select zip result: $uri")

                navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.KPM))
            }

            val selectKpmLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode != RESULT_OK) {
                    return@rememberLauncherForActivityResult
                }
                val data = it.data ?: return@rememberLauncherForActivityResult
                val uri = data.data ?: return@rememberLauncherForActivityResult

                // todo: args
                scope.launch {
                    val rc = loadModule(loadingDialog, uri, "")
                    val toastText = if (rc == 0) successToastText else "$failToastText: $rc"
                    withContext(Dispatchers.Main) {
                        showToast(context, toastText)
                    }
                    viewModel.markNeedRefresh()
                    viewModel.fetchModuleList()
                }
            }

            val selectInstallKpmLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode != RESULT_OK) return@rememberLauncherForActivityResult
                val uri = it.data?.data ?: return@rememberLauncherForActivityResult
                scope.launch {
                    val rc = kpmInstallMutex.withLock { installKpm(uri) }
                    Toast.makeText(context, if (rc == 0) installSuccessToastText else "$failToastText: $rc", Toast.LENGTH_SHORT).show()
                    viewModel.markNeedRefresh()
                }
            }

            var expanded by remember { mutableStateOf(false) }
            val isFloatingMode = LocalIsFloatingNavMode.current

            val fabContent: @Composable () -> Unit = {
                FloatingActionButtonMenu(
                    expanded = expanded,
                    button = {
                        FloatingActionButton(
                            onClick = { expanded = !expanded },
                            shape = CircleShape,
                            contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1f),
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                        ) {
                            Crossfade(
                                targetState = expanded,
                                animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                                label = "fabIconCrossfade"
                            ) { isExpanded ->
                                if (isExpanded) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = stringResource(CoreR.string.core_action_close),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.package_import),
                                        contentDescription = stringResource(R.string.apm_install_confirm_title),
                                    )
                                }
                            }
                        }
                    },
                ) {
                    // Jailbreak mode only supports loading, so auto-load config and
                    // embedding (which needs boot patching) are hidden there.
                    if (jailbreakMode != true) {
                        // 嵌入 (Embed)
                        FloatingActionButtonMenuItem(
                            onClick = {
                                expanded = false
                                scope.launch {
                                    if (!checkStrongBiometric()) return@launch
                                    navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_AND_INSTALL))
                                }
                            },
                            icon = { Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(FolkDimens.Size18)) },
                            text = { Text(text = moduleEmbed, style = MaterialTheme.typography.bodyMedium) },
                        )
                    }
                    // 安装 (Install): copy into the boot-time loader dir without loading
                    FloatingActionButtonMenuItem(
                        onClick = {
                            expanded = false
                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                            intent.type = "*/*"
                            intent.addCategory(Intent.CATEGORY_OPENABLE)
                            selectInstallKpmLauncher.launch(intent)
                        },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(FolkDimens.Size18)) },
                        text = { Text(text = moduleInstall, style = MaterialTheme.typography.bodyMedium) },
                    )
                    // 加载 (Load)
                    FloatingActionButtonMenuItem(
                        onClick = {
                            expanded = false
                            scope.launch {
                                if (!checkStrongBiometric()) return@launch
                                val intent = Intent(Intent.ACTION_GET_CONTENT)
                                intent.type = "*/*"
                                intent.addCategory(Intent.CATEGORY_OPENABLE)
                                selectKpmLauncher.launch(intent)
                            }
                        },
                        icon = { Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(FolkDimens.Size18)) },
                        text = { Text(text = moduleLoad, style = MaterialTheme.typography.bodyMedium) },
                    )
                }
            }
            val bottomBarVisible = LocalBottomBarVisible.current.value
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val animatedOffset by animateDpAsState(
                targetValue = if (isFloatingMode && bottomBarVisible && !isLandscape) (-88).dp else FolkDimens.Size0,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "fabOffset"
            )
            if (isFloatingMode) {
                Box(modifier = Modifier.offset(y = animatedOffset)) {
                    fabContent()
                }
            } else {
                fabContent()
            }
        }
        },
        // The list already reserves room for the FAB and the floating bar via
        // fabNavBottomClearance, so the scaffold must not add more.
        addBottomClearance = false,
    ) { innerPadding ->

        KPModuleList(
            viewModel = viewModel,
            moduleList = filteredModuleList,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            scrollConnection = if (m3eLayout) {
                searchBarScrollBehavior.nestedScrollConnection
            } else {
                null
            },
            state = kpModuleListState,
            showMoreModuleInfo = showMoreModuleInfo,
            foldSystemModule = foldSystemModule,
            simpleListBottomBar = simpleListBottomBar,
            splicedCardGroup = splicedCardGroup,
            showKpmStatusBadge = showKpmStatusBadge,
            checkStrongBiometric = ::checkStrongBiometric
        )
    }
}


