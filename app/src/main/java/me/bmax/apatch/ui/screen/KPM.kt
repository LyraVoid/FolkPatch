package me.bmax.apatch.ui.screen

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.KpmAutoLoadConfigScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OnlineKPMScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.Natives
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.ui.component.ConfirmResult
import me.bmax.apatch.ui.component.KPModuleRemoveButton
import me.bmax.apatch.ui.component.KpmAutoLoadManager
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.component.ProvideMenuShape
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenu
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenuItem
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.viewmodel.KPModel
import me.bmax.apatch.ui.viewmodel.KPModuleViewModel
import me.bmax.apatch.ui.viewmodel.PatchesViewModel
import me.bmax.apatch.util.inputStream
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.writeTo
import java.io.IOException
import java.io.File

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import me.bmax.apatch.ui.theme.BackgroundConfig
import androidx.compose.material3.ButtonDefaults

import android.content.SharedPreferences
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

import me.bmax.apatch.util.BiometricUtils
import me.bmax.apatch.util.ModuleBackupUtils
import me.bmax.apatch.util.getFileNameFromUri
import kotlinx.coroutines.CoroutineScope
import coil.compose.AsyncImage
import coil.request.ImageRequest

private const val TAG = "KernelPatchModule"
private lateinit var targetKPMToControl: KPModel.KPMInfo
private const val KPM_BANNER_DIR_NAME = "kpm_banners"

private fun sanitizeKpmBannerKey(raw: String): String {
    return raw.replace(Regex("[^a-zA-Z0-9._-]"), "_")
}

private fun getKpmBannerFile(context: Context, moduleName: String): File {
    val dir = File(context.filesDir, KPM_BANNER_DIR_NAME)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return File(dir, sanitizeKpmBannerKey(moduleName))
}

private fun readKpmBanner(context: Context, moduleName: String): ByteArray? {
    return runCatching {
        val file = getKpmBannerFile(context, moduleName)
        if (file.exists()) {
            file.readBytes().takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }.getOrNull()
}

private fun writeKpmBanner(context: Context, moduleName: String, uri: Uri): ByteArray? {
    val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val file = getKpmBannerFile(context, moduleName)
    file.outputStream().use { it.write(data) }
    return data
}

private fun clearKpmBanner(context: Context, moduleName: String): Boolean {
    val file = getKpmBannerFile(context, moduleName)
    return !file.exists() || file.delete()
}

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KPModuleScreen(navigator: DestinationsNavigator) {
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
    var showFirstTimeDialog by remember { mutableStateOf(KpmAutoLoadManager.isFirstTimeKpmPage(context)) }
    var dontShowAgain by remember { mutableStateOf(false) }

    val prefs = remember { APApplication.sharedPreferences }
    var showMoreModuleInfo by remember { mutableStateOf(prefs.getBoolean("show_more_module_info", true)) }
    var foldSystemModule by remember { mutableStateOf(prefs.getBoolean("fold_system_module", true)) }
    var simpleListBottomBar by remember { mutableStateOf(prefs.getBoolean("simple_list_bottom_bar", false)) }

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

    LaunchedEffect(Unit) {
        if (viewModel.moduleList.isEmpty() || viewModel.isNeedRefresh) {
            viewModel.fetchModuleList()
        }
    }

    val kpModuleListState = rememberLazyListState()

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

    Scaffold(topBar = {
        TopBar(navigator, searchQuery) { searchQuery = it }
    }, floatingActionButton = run {
        {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            val moduleLoad = stringResource(id = R.string.kpm_load)
            val moduleInstall = stringResource(id = R.string.kpm_install)
            val moduleEmbed = stringResource(id = R.string.kpm_embed)
            val successToastText = stringResource(id = R.string.kpm_load_toast_succ)
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
                        Toast.makeText(
                            context, toastText, Toast.LENGTH_SHORT
                        ).show()
                    }
                    viewModel.markNeedRefresh()
                    viewModel.fetchModuleList()
                }
            }

            var expanded by remember { mutableStateOf(false) }
            val options = listOf(moduleEmbed, moduleInstall, moduleLoad)

            Column {
                FloatingActionButton(
                    onClick = {
                        expanded = !expanded
                    },
                    contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1f),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.package_import),
                        contentDescription = null
                    )
                }

                WallpaperAwareDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    options.forEach { label ->
                        WallpaperAwareDropdownMenuItem(text = { Text(label) }, onClick = {
                            expanded = false
                            scope.launch {
                                if (!checkStrongBiometric()) return@launch
                                when (label) {
                                    moduleEmbed -> {
                                        navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_AND_INSTALL))
                                    }

                                    moduleInstall -> {
//                                        val intent = Intent(Intent.ACTION_GET_CONTENT)
//                                        intent.type = "application/zip"
//                                        selectZipLauncher.launch(intent)
                                        Toast.makeText(
                                            context,
                                            "Under development",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    moduleLoad -> {
                                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                                        intent.type = "*/*"
                                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                                        selectKpmLauncher.launch(intent)
                                    }
                                }
                            }
                        })
                    }
                }
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
            checkStrongBiometric = ::checkStrongBiometric
        )
    }

    if (showFirstTimeDialog) {
        BasicAlertDialog(
            onDismissRequest = {
                if (dontShowAgain) {
                    KpmAutoLoadManager.setFirstTimeKpmPageShown(context)
                }
                showFirstTimeDialog = false
            },
            properties = androidx.compose.ui.window.DialogProperties(
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
                        text = stringResource(R.string.kpm_page_first_time_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = stringResource(R.string.kpm_page_first_time_message),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.kpm_autoload_do_not_show_again),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = {
                            if (dontShowAgain) {
                                KpmAutoLoadManager.setFirstTimeKpmPageShown(context)
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

suspend fun loadModule(loadingDialog: LoadingDialogHandle, uri: Uri, args: String): Int {
    val rc = loadingDialog.withLoading {
        withContext(Dispatchers.IO) {
            run {
                val kpmDir: ExtendedFile =
                    FileSystemManager.getLocal().getFile(apApp.filesDir.parent, "kpm")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KPMControlDialog(showDialog: MutableState<Boolean>) {
    var controlParam by remember { mutableStateOf("") }
    var enable by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val context = LocalContext.current
    val outMsgStringRes = stringResource(id = R.string.kpm_control_outMsg)
    val okStringRes = stringResource(id = R.string.kpm_control_ok)
    val failedStringRes = stringResource(id = R.string.kpm_control_failed)

    lateinit var controlResult: Natives.KPMCtlRes

    suspend fun onModuleControl(module: KPModel.KPMInfo) {
        loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                controlResult = Natives.kernelPatchModuleControl(module.name, controlParam)
            }
        }

        if (controlResult.rc >= 0) {
            Toast.makeText(
                context,
                "$okStringRes\n${outMsgStringRes}: ${controlResult.outMsg}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "$failedStringRes\n${outMsgStringRes}: ${controlResult.outMsg}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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

                        scope.launch { onModuleControl(targetKPMToControl) }

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
    checkStrongBiometric: suspend () -> Boolean
) {
    val moduleStr = stringResource(id = R.string.kpm)
    val moduleUninstallConfirm = stringResource(id = R.string.kpm_unload_confirm)
    val uninstall = stringResource(id = R.string.kpm_unload)
    val cancel = stringResource(id = android.R.string.cancel)

    var expandedModuleId by remember { mutableStateOf<String?>(null) }

    val confirmDialog = rememberConfirmDialog()
    val loadingDialog = rememberLoadingDialog()

    val showKPMControlDialog = remember { mutableStateOf(false) }
    if (showKPMControlDialog.value) {
        KPMControlDialog(showDialog = showKPMControlDialog)
    }

    suspend fun onModuleUninstall(module: KPModel.KPMInfo) {
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
                Natives.unloadKernelPatchModule(module.name) == 0L
            }
        }

        if (success) {
            viewModel.fetchModuleList()
        }
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
            when {
                moduleList.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.kpm_apm_empty), textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    itemsIndexed(moduleList, key = { index, module -> "${index}_${module.name}" }) { index, module ->
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
                            showMoreModuleInfo = showMoreModuleInfo,
                            simpleListBottomBar = simpleListBottomBar,
                            foldSystemModule = foldSystemModule,
                            expanded = expandedModuleId == module.name,
                            onExpandToggle = {
                                expandedModuleId = if (expandedModuleId == module.name) null else module.name
                            }
                        )

                        // fix last item shadow incomplete in LazyColumn
                        Spacer(Modifier.height(1.dp))
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
                                content = { Icon(Icons.Filled.Close, null) }
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
                    IconButton(onClick = {
                        navigator.navigate(OnlineKPMScreenDestination)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Online KPM"
                        )
                    }
                    // 设置按钮
                    IconButton(onClick = {
                        navigator.navigate(KpmAutoLoadConfigScreenDestination)
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.kpm_autoload_title)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun KPModuleLabel(
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

@Composable
private fun KPModuleItem(
    module: KPModel.KPMInfo,
    onUninstall: (KPModel.KPMInfo) -> Unit,
    onControl: (KPModel.KPMInfo) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    showMoreModuleInfo: Boolean,
    simpleListBottomBar: Boolean,
    foldSystemModule: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit
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
    
    LaunchedEffect(showFolkBannerDialog) {
        if (showFolkBannerDialog) {
            hasFolkBanner = withContext(Dispatchers.IO) {
                readKpmBanner(context, module.name) != null
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
                    runCatching { writeKpmBanner(context, module.name, it) }.getOrNull()
                }
                loadingDialog.hide()
                val message = if (result != null) {
                    bannerReloadKey++
                    folkBannerSaved.format(module.name)
                } else {
                    folkBannerFailed.format(module.name)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isWallpaperMode = BackgroundConfig.isCustomBackgroundEnabled
    val opacity = if (isWallpaperMode) {
        BackgroundConfig.customBackgroundOpacity.coerceAtLeast(0.35f)
    } else {
        1f
    }
    
    val isDark = isSystemInDarkTheme()
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

    val bannerData by produceState<ByteArray?>(
        initialValue = null,
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

        // Get effective API source
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
            withContext(Dispatchers.IO) { readKpmBanner(context, module.name) }
        } else {
            null
        }
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
            if (bannerData != null) {
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
                        val hasAnyLabel = showMoreModuleInfo
                        if (hasAnyLabel) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                 val labelOpacity = (opacity + 0.1f).coerceAtMost(1f)
                                 
                                 KPModuleLabel(
                                    text = "KPM",
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = labelOpacity),
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                 )
                                 
                                 if (module.args.isNotEmpty()) {
                                     KPModuleLabel(
                                        text = "Args",
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = labelOpacity),
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                     )
                                 }
                            }
                        }
                    
                        Text(
                            text = module.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textDecoration = decoration
                        )

                        Text(
                            text = module.version,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = decoration
                        )

                        Text(
                            text = module.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = decoration
                        )
                        
                        if (showMoreModuleInfo && module.args.isNotEmpty()) {
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
                                        runCatching { clearKpmBanner(context, module.name) }.getOrDefault(false)
                                    }
                                    loadingDialog.hide()
                                    val message = if (success) {
                                        bannerReloadKey++
                                        folkBannerCleared.format(module.name)
                                    } else {
                                        folkBannerFailed.format(module.name)
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
