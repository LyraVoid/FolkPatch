package me.bmax.apatch.ui.screen

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import me.bmax.apatch.util.ui.showToast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OnlineKPMScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.component.ModuleLabel
import me.bmax.apatch.ui.component.TwoColumnGrid
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.viewmodel.KPModel
import me.bmax.apatch.ui.viewmodel.KPModuleViewModel
import me.bmax.apatch.ui.viewmodel.safeKpmModuleId
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.inputStream
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.writeTo
import me.bmax.apatch.util.rootShellForResult
import java.io.IOException

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.bannerFadeColor
import me.bmax.apatch.ui.navigation.LocalBottomBarVisible
import me.bmax.apatch.ui.navigation.LocalIsFloatingNavMode
import me.bmax.apatch.ui.navigation.fabNavBottomClearance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ButtonDefaults
import android.widget.Toast

import android.content.SharedPreferences
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

import me.bmax.apatch.util.BiometricUtils
import me.bmax.apatch.util.ModuleBackupUtils
import me.bmax.apatch.util.ModuleBannerStorage
import me.bmax.apatch.util.kpmBannerStorage
import me.bmax.apatch.util.SafeUriResolver
import me.bmax.apatch.util.getFileNameFromUri
import me.bmax.apatch.util.CustomModuleInfo
import me.bmax.apatch.util.isJailbreakMode
import me.bmax.apatch.util.kpmCustomModuleInfoStorage
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.ModuleInfoData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import coil.compose.AsyncImage
import coil.request.ImageRequest

import java.io.StringReader
import org.ini4j.Ini

private const val TAG = "KernelPatchModule"
private val kpmInstallMutex = Mutex()
private lateinit var targetKPMToControl: KPModel.KPMInfo

private data class UninstallResult(
    val unloaded: Boolean,
    val removed: Boolean,
)

@Destination<RootGraph>
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
                .padding(12.dp),
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

    var showOrderDialog by remember { mutableStateOf(false) }
    var orderedModules by remember { mutableStateOf(viewModel.moduleList) }

    Scaffold(topBar = {
        TopBar(
            navigator,
            searchQuery,
            showCustomOrder = viewModel.moduleList.isNotEmpty(),
            onCustomOrderClick = {
                orderedModules = viewModel.moduleList
                showOrderDialog = true
            }
        ) { searchQuery = it }
    }, floatingActionButton = run {
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
                                animationSpec = tween(durationMillis = 200),
                                label = "fabIconCrossfade"
                            ) { isExpanded ->
                                if (isExpanded) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Close",
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.package_import),
                                        contentDescription = "Install module",
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
                            icon = { Icon(Icons.Outlined.Code, contentDescription = null, modifier = Modifier.size(18.dp)) },
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
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) },
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
                        icon = { Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        text = { Text(text = moduleLoad, style = MaterialTheme.typography.bodyMedium) },
                    )
                }
            }
            val bottomBarVisible = LocalBottomBarVisible.current.value
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val animatedOffset by animateDpAsState(
                targetValue = if (isFloatingMode && bottomBarVisible && !isLandscape) (-88).dp else 0.dp,
                animationSpec = tween(durationMillis = 300),
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
    }) { innerPadding ->

        KPModuleList(
            viewModel = viewModel,
            moduleList = filteredModuleList,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            state = kpModuleListState,
            showMoreModuleInfo = showMoreModuleInfo,
            foldSystemModule = foldSystemModule,
            simpleListBottomBar = simpleListBottomBar,
            splicedCardGroup = splicedCardGroup,
            showKpmStatusBadge = showKpmStatusBadge,
            checkStrongBiometric = ::checkStrongBiometric
        )
    }



    if (showOrderDialog) {
        val reorderThreshold = with(LocalDensity.current) { 40.dp.toPx() }
        val dragToReorderDescription = stringResource(R.string.apm_drag_to_reorder)
        var draggedModuleName by remember { mutableStateOf<String?>(null) }
        var draggedDistance by remember { mutableStateOf(0f) }
        AlertDialog(
            onDismissRequest = { showOrderDialog = false },
            title = { Text(stringResource(R.string.apm_custom_order)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    itemsIndexed(orderedModules, key = { _, module -> module.name }) { _, module ->
                        val isDragging = draggedModuleName == module.name
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
                                    .pointerInput(module.name) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggedModuleName = module.name
                                                draggedDistance = 0f
                                            },
                                            onDragEnd = {
                                                draggedModuleName = null
                                                draggedDistance = 0f
                                            },
                                            onDragCancel = {
                                                draggedModuleName = null
                                                draggedDistance = 0f
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            draggedDistance += dragAmount.y
                                            val currentIndex = orderedModules.indexOfFirst { it.name == module.name }
                                            val targetIndex = when {
                                                draggedDistance > reorderThreshold -> currentIndex + 1
                                                draggedDistance < -reorderThreshold -> currentIndex - 1
                                                else -> currentIndex
                                            }
                                            if (currentIndex >= 0 && targetIndex in orderedModules.indices && targetIndex != currentIndex) {
                                                orderedModules = orderedModules.toMutableList().apply {
                                                    add(targetIndex, removeAt(currentIndex))
                                                }
                                                viewModel.setCustomModuleOrder(orderedModules.map { it.name })
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

suspend fun loadModule(loadingDialog: LoadingDialogHandle, uri: Uri, args: String): Int {
    val rc = loadingDialog.withLoading {
        withContext(Dispatchers.IO) {
            run {
                val kpmDir: ExtendedFile = FileSystemManager.getLocal().getFile(apApp.cacheDir.path, "kpm")
                kpmDir.deleteRecursively()
                kpmDir.mkdirs()
                val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
                val kpm = kpmDir.getChildFile("${rand}.kpm")
                Log.d(TAG, "save tmp kpm: ${kpm.path}")
                var rc = -1
                try {
                    uri.inputStream().buffered().writeTo(kpm)

                    // Auto Backup Logic for KPM Load
                    val fileName = getFileNameFromUri(apApp, uri)
                    // Launch backup asynchronously
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = ModuleBackupUtils.autoBackupModule(apApp, kpm, fileName, "KPM")
                            if (result != null && !result.startsWith("Duplicate")) {
                                Log.e(TAG, "KPM Auto backup failed: $result")
                            } else {
                                Log.d(TAG, "KPM Auto backup success")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "KPM Auto backup error: ${e.message}")
                        }
                    }

                    rc = Natives.loadKernelPatchModule(kpm.path, args).toInt()
                } catch (e: IOException) {
                    Log.e(TAG, "Copy kpm error: $e")
                }
                Log.d(TAG, "load ${kpm.path} rc: $rc")
                rc
            }
        }
    }
    return rc
}

/** Install a KPM from an app-local temporary file; it takes effect after reboot. */
suspend fun installKpm(uri: Uri): Int = withContext(Dispatchers.IO) {
    val tempDir: ExtendedFile =
        FileSystemManager.getLocal().getFile(apApp.cacheDir.path, "kpm-install")
    tempDir.deleteRecursively()
    tempDir.mkdirs()
    val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
    val temp = tempDir.getChildFile("$rand.kpm")
    try {
        Log.d(TAG, "save temporary KPM: ${temp.path}")
        uri.inputStream().buffered().writeTo(temp)
        val infoResult = rootShellForResult(
            "${APApplication.APATCH_FOLDER}bin/kptools -l -M '${temp.path}'"
        )
        if (!infoResult.isSuccess) return@withContext -2
        val section = Ini(StringReader(infoResult.out.joinToString("\n")))["kpm"] ?: return@withContext -3
        val name = section["name"]?.toString()?.trim().orEmpty()
        if (name.isEmpty()) return@withContext -4
        val id = safeKpmModuleId(name)
        val dir = "${APApplication.KPMS_DIR}$id"
        val destination = "$dir/$id.kpm"
        val result = rootShellForResult(
            "mkdir -p '$dir' && cp -f '${temp.path}' '$destination'"
        )
        if (!result.isSuccess) return@withContext -5

        // Installed KPMs are loaded by the boot-time loader. Do not load them in
        // the current session; installation takes effect after reboot.
        Log.i(TAG, "install KPM $name to $destination; reboot required")
        0
    } catch (e: Exception) {
        Log.e(TAG, "install KPM failed", e)
        -1
    } finally {
        tempDir.deleteRecursively()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KPMControlDialog(showDialog: MutableState<Boolean>, onConfirm: (String) -> Unit) {
    var controlParam by remember { mutableStateOf("") }
    var enable by remember { mutableStateOf(false) }

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
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.kpm_control_dialog_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.kpm_control_dialog_content),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Box(
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    OutlinedTextField(
                        value = controlParam,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        onValueChange = {
                            controlParam = it
                            enable = controlParam.isNotBlank()
                        },
                        shape = RoundedCornerShape(50.0f),
                        label = { Text(stringResource(id = R.string.kpm_control_paramters)) },
                        visualTransformation = VisualTransformation.None,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }

                    Button(onClick = {
                        showDialog.value = false

                        // Run the control on the caller's scope: this dialog
                        // leaves composition here, cancelling any scope it owns.
                        onConfirm(controlParam)

                    }, enabled = enable) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                }
            }
        }
        val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
        APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun KPModuleList(
    viewModel: KPModuleViewModel,
    moduleList: List<KPModel.KPMInfo>,
    modifier: Modifier = Modifier,
    state: LazyListState,
    showMoreModuleInfo: Boolean,
    foldSystemModule: Boolean,
    simpleListBottomBar: Boolean,
    splicedCardGroup: Boolean,
    showKpmStatusBadge: Boolean,
    checkStrongBiometric: suspend () -> Boolean
) {
    val moduleStr = stringResource(id = R.string.kpm)
    val moduleUninstallConfirm = stringResource(id = R.string.kpm_unload_confirm)
    val embeddedUnloadInvalid = stringResource(id = R.string.kpm_embedded_unload_invalid)
    val uninstall = stringResource(id = R.string.kpm_unload)
    val cancel = stringResource(id = android.R.string.cancel)

    var expandedModuleId by remember { mutableStateOf<String?>(null) }

    val confirmDialog = rememberConfirmDialog()
    val loadingDialog = rememberLoadingDialog()
    val outMsgStringRes = stringResource(id = R.string.kpm_control_outMsg)
    val okStringRes = stringResource(id = R.string.kpm_control_ok)
    val failedStringRes = stringResource(id = R.string.kpm_control_failed)

    // Run on the caller's (ViewModel) scope: KPMControlDialog leaves
    // composition on OK, cancelling any scope it owns.
    suspend fun onModuleControl(module: KPModel.KPMInfo, param: String) {
        lateinit var controlResult: Natives.KPMCtlRes
        loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                controlResult = Natives.kernelPatchModuleControl(module.name, param)
            }
        }

        if (controlResult.rc >= 0) {
            showToast(apApp, "$okStringRes\n${outMsgStringRes}: ${controlResult.outMsg}")
        } else {
            showToast(apApp, "$failedStringRes\n${outMsgStringRes}: ${controlResult.outMsg}")
        }
    }

    val showKPMControlDialog = remember { mutableStateOf(false) }
    if (showKPMControlDialog.value) {
        KPMControlDialog(showDialog = showKPMControlDialog, onConfirm = { param ->
            viewModel.viewModelScope.launch { onModuleControl(targetKPMToControl, param) }
        })
    }

    suspend fun onModuleUninstall(module: KPModel.KPMInfo) {
        if (!checkStrongBiometric()) return
        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = if (module.loadSource == "embedded") {
                embeddedUnloadInvalid
            } else {
                moduleUninstallConfirm.format(module.name)
            },
            confirm = uninstall,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val result = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                val unloaded = module.loadSource.isBlank() || Natives.unloadKernelPatchModule(module.name) == 0L
                val removed = if (module.installed && module.loadSource != "embedded") {
                    val id = safeKpmModuleId(module.moduleId.ifBlank { module.name })
                    val dir = "${APApplication.KPMS_DIR}$id"
                    rootShellForResult("rm -rf '$dir' && test ! -e '$dir'").isSuccess
                } else true
                UninstallResult(unloaded, removed)
            }
        }

        // Refresh even when the live kernel instance could not be unloaded:
        // the persistent file may still have been removed and must not remain
        // represented as installed in the UI.
        if (result.removed) {
            viewModel.fetchModuleList()
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = modifier,
        onRefresh = { viewModel.fetchModuleList(forceEmbeddedRefresh = true) },
        isRefreshing = viewModel.isRefreshing,
        state = pullToRefreshState,
        indicator = { PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = viewModel.isRefreshing, modifier = Modifier.align(Alignment.TopCenter)) }
    ) {
        val configuration = LocalConfiguration.current
        val isWideScreen = configuration.screenWidthDp >= 600

        if (isWideScreen) {
            TwoColumnGrid(
                modifier = Modifier.fillMaxSize(),
                items = if (moduleList.isEmpty()) emptyList() else moduleList,
                key = { module -> module.name },
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
                    if (moduleList.isEmpty()) {
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
                                        text = viewModel.errorMessage ?: stringResource(R.string.kpm_load_failed),
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
                                    stringResource(R.string.kpm_apm_empty), textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                },
                itemContent = { module ->
                    val scope = rememberCoroutineScope()
                    KPModuleItem(
                        module,
                        onUninstall = {
                            scope.launch { onModuleUninstall(module) }
                        },
                        onControl = {
                            scope.launch {
                                if (checkStrongBiometric()) {
                                    targetKPMToControl = module
                                    showKPMControlDialog.value = true
                                }
                            }
                        },
                        onToggle = { enabled ->
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val id = safeKpmModuleId(module.moduleId.ifBlank { module.name })
                                    if (enabled) {
                                        rootShellForResult("rm -f '${APApplication.KPMS_DIR}$id/disable'")
                                    } else {
                                        rootShellForResult("touch '${APApplication.KPMS_DIR}$id/disable'")
                                    }
                                }
                                viewModel.updateModuleDisabled(module.moduleId, !enabled)
                                viewModel.markNeedRefresh()
                                viewModel.fetchModuleList()
                            }
                        },
                        showMoreModuleInfo = showMoreModuleInfo,
                        simpleListBottomBar = simpleListBottomBar,
                        foldSystemModule = foldSystemModule,
                        expanded = expandedModuleId == module.name,
                        onExpandToggle = {
                            expandedModuleId = if (expandedModuleId == module.name) null else module.name
                        },
                        isEmbedded = if (showKpmStatusBadge) viewModel.embeddedKpmNames?.let { module.name.trim() in it } else null
                    )
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
                                        text = viewModel.errorMessage ?: stringResource(R.string.kpm_load_failed),
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

                    moduleList.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillParentMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.kpm_apm_empty), textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    else -> {
                        if (splicedCardGroup) {
                            item { Spacer(Modifier.height(8.dp)) }
                            splicedLazyColumnGroup(
                                items = moduleList,
                                key = { _, module -> module.name },
                                contentType = { _, _ -> "KPModuleItem" },
                            ) { _, module ->
                                val scope = rememberCoroutineScope()
                                KPModuleItem(
                                    module,
                                    onUninstall = {
                                        scope.launch { onModuleUninstall(module) }
                                    },
                                    onControl = {
                                        scope.launch {
                                            if (checkStrongBiometric()) {
                                                targetKPMToControl = module
                                                showKPMControlDialog.value = true
                                            }
                                        }
                                    },
                                    onToggle = { enabled ->
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val id = safeKpmModuleId(module.moduleId.ifBlank { module.name })
                                                if (enabled) {
                                                    rootShellForResult("rm -f '${APApplication.KPMS_DIR}$id/disable'")
                                                } else {
                                                    rootShellForResult("mkdir -p '${APApplication.KPMS_DIR}$id' && touch '${APApplication.KPMS_DIR}$id/disable'")
                                                }
                                            }
                                            viewModel.markNeedRefresh()
                                            viewModel.fetchModuleList()
                                        }
                                    },
                                    showMoreModuleInfo = showMoreModuleInfo,
                                    simpleListBottomBar = simpleListBottomBar,
                                    foldSystemModule = foldSystemModule,
                                    expanded = expandedModuleId == module.name,
                                    onExpandToggle = {
                                        expandedModuleId = if (expandedModuleId == module.name) null else module.name
                                    },
                                    isEmbedded = if (showKpmStatusBadge) viewModel.embeddedKpmNames?.let { module.name.trim() in it } else null
                                )
                            }
                            item { Spacer(Modifier.height(8.dp)) } // bottom clearance handled by contentPadding
                        } else {
                            item { Spacer(Modifier.height(8.dp)) }
                            itemsIndexed(moduleList, key = { _, module -> module.name }) { _, module ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                val scope = rememberCoroutineScope()
                                KPModuleItem(
                                    module,
                                    onUninstall = {
                                        scope.launch { onModuleUninstall(module) }
                                    },
                                    onControl = {
                                        scope.launch {
                                            if (checkStrongBiometric()) {
                                                targetKPMToControl = module
                                                showKPMControlDialog.value = true
                                            }
                                        }
                                    },
                                    onToggle = { enabled ->
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                val id = safeKpmModuleId(module.moduleId.ifBlank { module.name })
                                                if (enabled) {
                                                    rootShellForResult("rm -f '${APApplication.KPMS_DIR}$id/disable'")
                                                } else {
                                                    rootShellForResult("mkdir -p '${APApplication.KPMS_DIR}$id' && touch '${APApplication.KPMS_DIR}$id/disable'")
                                                }
                                            }
                                            viewModel.markNeedRefresh()
                                            viewModel.fetchModuleList()
                                        }
                                    },
                                    showMoreModuleInfo = showMoreModuleInfo,
                                    simpleListBottomBar = simpleListBottomBar,
                                    foldSystemModule = foldSystemModule,
                                    expanded = expandedModuleId == module.name,
                                    onExpandToggle = {
                                        expandedModuleId = if (expandedModuleId == module.name) null else module.name
                                    },
                                    isEmbedded = if (showKpmStatusBadge) viewModel.embeddedKpmNames?.let { module.name.trim() in it } else null
                                )
                                }
                            }
                            item { Spacer(Modifier.height(8.dp)) } // bottom clearance handled by contentPadding
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    navigator: DestinationsNavigator,
    searchQuery: String,
    showCustomOrder: Boolean,
    onCustomOrderClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var onSearch by remember { mutableStateOf(false) }

    if (onSearch) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    BackHandler(
        enabled = onSearch,
        onBack = {
            keyboardController?.hide()
            onSearchQueryChange("")
            onSearch = false
        }
    )

    TopAppBar(
        title = {
            Box {
                // 标题（搜索框未显示时）
                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.CenterStart),
                    visible = !onSearch,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    content = { Text(stringResource(R.string.kpm)) }
                )

                // 搜索框（搜索时显示）
                AnimatedVisibility(
                    visible = onSearch,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp, end = 14.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) onSearch = true
                            },
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        shape = RoundedCornerShape(15.dp),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    onSearch = false
                                    keyboardController?.hide()
                                    onSearchQueryChange("")
                                },
                                content = { Icon(Icons.Filled.Close, "Close") }
                            )
                        },
                        maxLines = 1,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions {
                            keyboardController?.hide()
                        },
                    )
                }
            }
        },
        actions = {
            AnimatedVisibility(
                visible = !onSearch
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 搜索按钮
                    IconButton(onClick = { onSearch = true }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }
                    // 下载按钮
                    IconButton(onClick = dropUnlessResumed { navigator.navigate(OnlineKPMScreenDestination) }) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = "Online KPM"
                        )
                    }
                    // 自定义排序按钮（无模块时隐藏）
                    if (showCustomOrder) {
                        IconButton(onClick = onCustomOrderClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(R.string.kpm_custom_order)
                            )
                        }
                    }
                }
            }
        }
    )
}



@Composable
private fun KPModuleItem(
    module: KPModel.KPMInfo,
    onUninstall: (KPModel.KPMInfo) -> Unit,
    onControl: (KPModel.KPMInfo) -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    showMoreModuleInfo: Boolean,
    simpleListBottomBar: Boolean,
    foldSystemModule: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    isEmbedded: Boolean? = null
) {
    val moduleAuthor = stringResource(id = R.string.kpm_author)
    val moduleArgs = stringResource(id = R.string.kpm_args)
    val decoration = TextDecoration.None
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val folkBannerTitle = stringResource(R.string.apm_folk_banner_title)
    val folkBannerSelect = stringResource(R.string.apm_folk_banner_select)
    val folkBannerClear = stringResource(R.string.apm_folk_banner_clear)
    val folkBannerSaved = stringResource(R.string.apm_folk_banner_saved)
    val folkBannerCleared = stringResource(R.string.apm_folk_banner_cleared)
    val folkBannerFailed = stringResource(R.string.apm_folk_banner_failed)
    var showFolkBannerDialog by remember { mutableStateOf(false) }
    var hasFolkBanner by remember { mutableStateOf(false) }
    var bannerReloadKey by remember { mutableStateOf(0) }
    val customInfoReloadKeyState = remember { mutableStateOf(0) }
    var customInfoReloadKey by customInfoReloadKeyState
    
    LaunchedEffect(showFolkBannerDialog) {
        if (showFolkBannerDialog) {
            hasFolkBanner = withContext(Dispatchers.IO) {
                kpmBannerStorage.read(module.name) != null
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
                    runCatching { kpmBannerStorage.write(context, module.name, it) }.getOrNull()
                }
                loadingDialog.hide()
                val message = if (result != null) {
                    bannerReloadKey++
                    folkBannerSaved.format(module.name)
                } else {
                    folkBannerFailed.format(module.name)
                }
                showToast(context, message)
            }
        }
    }

    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled
    val opacity = if (isWallpaperMode) {
        BackgroundConfig.customBackgroundOpacity.coerceAtLeast(0.35f)
    } else {
        1f
    }
    
    val cardColor = if (isWallpaperMode) {
        MaterialTheme.colorScheme.surface.copy(alpha = opacity)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
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

    val cachedBanner = if (BackgroundConfig.isBannerApiModeEnabled && BackgroundConfig.getEffectiveBannerApiSource().isNotBlank()) {
        BannerApiService.loadSync(context, "kpm_${module.name}", BackgroundConfig.getEffectiveBannerApiSource())
    } else null

    val bannerData by produceState<ByteArray?>(
        initialValue = cachedBanner,
        module.name,
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

        KPModuleViewModel.bannerSemaphore.withPermit {
            val effectiveApiSource = BackgroundConfig.getEffectiveBannerApiSource()

        if (BackgroundConfig.isBannerApiModeEnabled && effectiveApiSource.isNotBlank()) {
            val apiBanner = withContext(Dispatchers.IO) {
                BannerApiService.getModuleBanner(
                    context = context,
                    moduleId = "kpm_${module.name}",
                    source = effectiveApiSource
                )
            }
            if (apiBanner != null) {
                value = apiBanner
                return@produceState
            }
        }

        value = if (BackgroundConfig.isFolkBannerEnabled) {
            withContext(Dispatchers.IO) { kpmBannerStorage.read(module.name) }
        } else {
            null
        }
        }
    }

    val customInfo by produceState(initialValue = null as CustomModuleInfo?, key1 = module.name, key2 = customInfoReloadKey) {
        value = withContext(Dispatchers.IO) {
            kpmCustomModuleInfoStorage.read(module.name)
        }
    }

    val insideSplicedGroup = me.bmax.apatch.ui.component.LocalInsideSplicedGroup.current

    val cardShape = RoundedCornerShape(20.dp)

    val clickModifier = Modifier
        .fillMaxWidth()
        .animateContentSize()
        .combinedClickable(
            onClick = {
                if (foldSystemModule) {
                    onExpandToggle()
                }
            },
            onLongClick = {
                showFolkBannerDialog = true
            }
        )

    val contentBlock: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (bannerData != null) {
                val fadeColor = bannerFadeColor()

                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(bannerData)
                            .build(),
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
                        val hasAnyLabel = showMoreModuleInfo || isEmbedded != null
                        if (hasAnyLabel) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                 val labelOpacity = (opacity + 0.1f).coerceAtMost(1f)

                                 if (showMoreModuleInfo) {
                                     ModuleLabel(
                                        text = "KPM",
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                     )

                                     if (module.args.isNotBlank()) {
                                         ModuleLabel(
                                            text = "Args",
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = labelOpacity),
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                         )
                                     }
                                 }

                                 isEmbedded?.let { embedded ->
                                     ModuleLabel(
                                        text = stringResource(if (embedded) R.string.kpm_embedded else R.string.kpm_loaded),
                                        containerColor = if (embedded) {
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = labelOpacity)
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = labelOpacity)
                                        },
                                        contentColor = if (embedded) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                     )
                                 }
                            }
                        }
                    
                        Text(
                            text = customInfo?.name?.takeIf { it.isNotBlank() } ?: module.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textDecoration = decoration
                        )

                        Text(
                            text = customInfo?.version?.takeIf { it.isNotBlank() } ?: module.version,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = decoration
                        )

                        Text(
                            text = customInfo?.author?.takeIf { it.isNotBlank() } ?: module.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = decoration
                        )
                        
                        if (showMoreModuleInfo && module.args.isNotBlank()) {
                             Text(
                                text = "$moduleArgs: ${module.args}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = decoration,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (simpleListBottomBar) 12.dp else 8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onControl(module) },
                            enabled = true,
                            contentPadding = if (simpleListBottomBar) PaddingValues(12.dp) else PaddingValues(horizontal = 12.dp),
                            modifier = if (simpleListBottomBar) Modifier else Modifier.height(36.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f))
                            )
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.settings),
                                contentDescription = stringResource(id = R.string.kpm_control)
                            )
                            if (!simpleListBottomBar) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.kpm_control))
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (module.installed && module.loadSource != "embedded") {
                            Switch(checked = !module.disabled, onCheckedChange = onToggle)
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        FilledTonalButton(
                            onClick = { onUninstall(module) },
                            enabled = true,
                            contentPadding = if (simpleListBottomBar) PaddingValues(12.dp) else PaddingValues(horizontal = 12.dp),
                            modifier = if (simpleListBottomBar) Modifier else Modifier.height(36.dp),
                            colors = if (simpleListBottomBar) ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f))
                            ) else ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f)),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.trash),
                                contentDescription = stringResource(id = R.string.kpm_unload)
                            )
                            if (!simpleListBottomBar) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.kpm_unload))
                            }
                        }
                    }
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

    // 自定义模块信息状态
    var customName by remember { mutableStateOf("") }
    var customVersion by remember { mutableStateOf("") }
    var customAuthor by remember { mutableStateOf("") }
    var customDescription by remember { mutableStateOf("") }

    // 弹窗打开时加载自定义信息
    LaunchedEffect(showFolkBannerDialog) {
        if (showFolkBannerDialog) {
            val info = withContext(Dispatchers.IO) {
                kpmCustomModuleInfoStorage.read(module.name)
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
                    runCatching { kpmBannerStorage.clear(module.name) }.getOrDefault(false)
                }
                loadingDialog.hide()
                val message = if (success) {
                    bannerReloadKey++
                    folkBannerCleared.format(module.name)
                } else {
                    folkBannerFailed.format(module.name)
                }
                showToast(context, message)
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
                    kpmCustomModuleInfoStorage.write(module.name, CustomModuleInfo(
                        name = info.name.takeIf { it.isNotBlank() },
                        version = info.version.takeIf { it.isNotBlank() },
                        author = info.author.takeIf { it.isNotBlank() },
                        description = info.description.takeIf { it.isNotBlank() },
                    ))
                }
                showToast(context, customInfoSavedMsg.format(module.name))
            }
        },
        onResetModuleInfo = {
            scope.launch {
                withContext(Dispatchers.IO) {
                    kpmCustomModuleInfoStorage.clear(module.name)
                }
                showToast(context, customInfoResetMsg.format(module.name))
            }
        }
    )
}
