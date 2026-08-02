package me.bmax.apatch.ui.screen

import android.content.SharedPreferences
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ScriptExecutionLogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.OnlineScriptScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.data.ScriptInfo
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import me.bmax.apatch.ui.component.FilePickerDialog
import me.bmax.apatch.ui.component.TwoColumnGrid
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import me.bmax.apatch.ui.component.LocalInsideSplicedGroup
import me.bmax.apatch.ui.component.BackgroundOptionsDialog
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.bannerFadeColor
import me.bmax.apatch.ui.viewmodel.ScriptLibraryViewModel
import me.bmax.apatch.util.ModuleShortcut
import me.bmax.apatch.util.scriptBannerStorage
import me.bmax.apatch.util.ui.showToast
import me.bmax.apatch.util.ui.LocalSnackbarHost
import java.io.File

private val scriptBannerSemaphore = Semaphore(4)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.script_library_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.script_library_add))
                    }
                    IconButton(onClick = { navigator.navigate(OnlineScriptScreenDestination) }) {
                        Icon(Icons.Outlined.Storefront, contentDescription = stringResource(R.string.online_script_title))
                    }
                }
            )
        }
    ) { innerPadding ->
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            modifier = Modifier.padding(innerPadding),
            onRefresh = { viewModel.loadScripts() },
            isRefreshing = isLoading,
            state = pullToRefreshState,
            indicator = { PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = isLoading, modifier = Modifier.align(Alignment.TopCenter)) }
        ) {
            if (scripts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.script_library_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600
                if (isWideScreen) {
                    TwoColumnGrid(
                        modifier = Modifier.fillMaxSize(),
                        items = scripts,
                        key = { it.id },
                        verticalSpacing = 16.dp,
                        horizontalSpacing = 16.dp,
                        contentPadding = PaddingValues(16.dp),
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
                        contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp)
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
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
private fun ScriptLabel(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScriptItem(
    script: ScriptInfo,
    enableShortcut: Boolean,
    simpleListBottomBar: Boolean,
    foldCard: Boolean,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    onRun: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showShortcutDialog by remember { mutableStateOf(false) }
    var shortcutName by rememberSaveable(script.id) { mutableStateOf(script.alias) }
    var shortcutIconUri by remember { mutableStateOf<String?>(null) }
    val appIcon = remember(context) { context.packageManager.getApplicationIcon(context.packageName) }
    val pickShortcutIconLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        shortcutIconUri = uri?.toString()
    }

    val shortcutPreviewBitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = shortcutIconUri) {
        value = if (shortcutIconUri.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                ModuleShortcut.loadShortcutBitmap(context, shortcutIconUri)
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

    val labelOpacity = (opacity + 0.1f).coerceAtMost(1f)

    val bannerImageAlpha = if (BackgroundConfig.isBannerCustomOpacityEnabled) {
        BackgroundConfig.bannerCustomOpacity
    } else if (isWallpaperMode) {
        (0.35f + (opacity - 0.2f) * 0.5f).coerceIn(0.25f, 0.6f)
    } else {
        0.18f
    }
    var showBannerDialog by remember { mutableStateOf(false) }
    var hasBanner by remember { mutableStateOf(false) }
    var bannerReloadKey by rememberSaveable(script.id) { mutableStateOf(0) }
    val loadingDialog = rememberLoadingDialog()
    val bannerTitle = stringResource(R.string.apm_folk_banner_title)
    val bannerSelect = stringResource(R.string.apm_folk_banner_select)
    val bannerClear = stringResource(R.string.apm_folk_banner_clear)
    val bannerSaved = stringResource(R.string.apm_folk_banner_saved)
    val bannerCleared = stringResource(R.string.apm_folk_banner_cleared)
    val bannerFailed = stringResource(R.string.apm_folk_banner_failed)

    LaunchedEffect(showBannerDialog) {
        if (showBannerDialog) {
            hasBanner = withContext(Dispatchers.IO) { scriptBannerStorage.read(script.id) != null }
        }
    }

    val pickBannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val data = withContext(Dispatchers.IO) {
                    runCatching { scriptBannerStorage.write(context, script.id, it) }.getOrNull()
                }
                loadingDialog.hide()
                if (data != null) {
                    bannerReloadKey++
                    showToast(context, bannerSaved.format(script.alias))
                } else {
                    showToast(context, bannerFailed.format(script.alias))
                }
            }
        }
    }

    val bannerData by produceState<ByteArray?>(
        initialValue = null,
        script.id,
        BackgroundConfig.isBannerEnabled,
        BackgroundConfig.isBannerApiModeEnabled,
        BackgroundConfig.bannerApiSource,
        BackgroundConfig.isFolkBannerEnabled,
        bannerReloadKey
    ) {
        if (!BackgroundConfig.isBannerEnabled) {
            value = null
            return@produceState
        }
        scriptBannerSemaphore.withPermit {
            val apiSource = BackgroundConfig.getEffectiveBannerApiSource()
            value = if (BackgroundConfig.isBannerApiModeEnabled && apiSource.isNotBlank()) {
                BannerApiService.getModuleBanner(context, "script_${script.id}", apiSource)
                    ?: if (BackgroundConfig.isFolkBannerEnabled) withContext(Dispatchers.IO) { scriptBannerStorage.read(script.id) } else null
            } else if (BackgroundConfig.isFolkBannerEnabled) {
                withContext(Dispatchers.IO) { scriptBannerStorage.read(script.id) }
            } else null
        }
    }

    val cardShape = RoundedCornerShape(20.dp)
    val clickModifier = Modifier
        .fillMaxWidth()
        .animateContentSize()
        .combinedClickable(
            onClick = { if (foldCard) onExpandToggle() else onRun() },
            onLongClick = { showBannerDialog = true }
        )

    val contentBlock: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (bannerData != null) {
                val fadeColor = bannerFadeColor()
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(context).data(bannerData).build(),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    alpha = bannerImageAlpha
                )
                Box(
                    modifier = Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            listOf(fadeColor.copy(alpha = 0f), fadeColor.copy(alpha = if (isWallpaperMode) 0.5f else 0.8f))
                        )
                    )
                )
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            ScriptLabel(
                                text = "Shell",
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = labelOpacity),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = script.alias,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = File(script.path).name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = script.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = !foldCard || expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onRun,
                        contentPadding = if (simpleListBottomBar) PaddingValues(12.dp) else ButtonDefaults.TextButtonContentPadding,
                        modifier = if (simpleListBottomBar) Modifier else Modifier.height(36.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f))
                        )
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        if (!simpleListBottomBar) {
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.script_library_run))
                        }
                    }

                    if (enableShortcut) {
                        FilledTonalButton(
                            onClick = {
                                shortcutName = script.alias
                                shortcutIconUri = null
                                showShortcutDialog = true
                            },
                            contentPadding = if (simpleListBottomBar) PaddingValues(12.dp) else ButtonDefaults.TextButtonContentPadding,
                            modifier = if (simpleListBottomBar) Modifier else Modifier.height(36.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f)),
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            if (!simpleListBottomBar) {
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.module_shortcut_add))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    FilledTonalButton(
                        onClick = onDelete,
                        contentPadding = if (simpleListBottomBar) PaddingValues(12.dp) else ButtonDefaults.TextButtonContentPadding,
                        modifier = if (simpleListBottomBar) Modifier else Modifier.height(36.dp),
                        colors = if (simpleListBottomBar) ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f))
                        ) else ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = (opacity + 0.3f).coerceAtMost(1f)),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        if (!simpleListBottomBar) {
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.script_library_delete))
                        }
                    }
                }
            }
        }
    }
    }

    if (LocalInsideSplicedGroup.current) {
        Box(modifier = clickModifier) { contentBlock() }
    } else {
        Surface(
            modifier = Modifier.clip(cardShape).then(clickModifier),
            shape = cardShape,
            color = cardColor,
            tonalElevation = 0.dp,
        ) { contentBlock() }
    }

    BackgroundOptionsDialog(
        showDialog = showBannerDialog,
        onDismiss = { showBannerDialog = false },
        title = bannerTitle,
        selectLabel = bannerSelect,
        clearLabel = bannerClear,
        hasExisting = hasBanner,
        onSelectImage = { pickBannerLauncher.launch("image/*") },
        onClearImage = {
            scope.launch {
                loadingDialog.show()
                val cleared = withContext(Dispatchers.IO) { scriptBannerStorage.clear(script.id) }
                loadingDialog.hide()
                if (cleared) {
                    bannerReloadKey++
                    showToast(context, bannerCleared.format(script.alias))
                } else {
                    showToast(context, bannerFailed.format(script.alias))
                }
            }
        }
    )

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
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = shortcutName.ifBlank { script.alias }
                    ModuleShortcut.createScriptShortcut(
                        context,
                        script.id,
                        name,
                        shortcutIconUri
                    )
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
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.script_library_select_file))
                }

                if (selectedFile != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedFile?.absolutePath ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                enabled = selectedFile != null
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
