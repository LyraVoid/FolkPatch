package me.bmax.apatch.ui.screen.theme

import me.bmax.apatch.ui.component.folk.navOrigin
import me.bmax.apatch.ui.theme.tokens.FolkColors
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.MyThemesScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkLoadingIndicator
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.viewmodel.ThemeStoreViewModel
import me.bmax.apatch.util.DownloadProgress
import me.bmax.apatch.util.DownloadStatus
import me.bmax.apatch.core.ui.R as CoreR
import me.bmax.apatch.ui.theme.tokens.FolkShape

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeStoreScreen(
    navigator: DestinationsNavigator
) {
    val viewModel = viewModel<ThemeStoreViewModel>(
        factory = ThemeStoreViewModel.Factory(LocalContext.current)
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val prefs = APApplication.sharedPreferences
    
    // Theme mode
    val themeModeKey = "theme_mode"
    var themeMode by remember { mutableStateOf(prefs.getString(themeModeKey, null)) }
    val isCompatMode = themeMode == "compat"
    var showOnboardingDialog by remember { mutableStateOf(false) }

    // First-run: check if onboarding needed
    LaunchedEffect(Unit) {
        if (themeMode == null) {
            showOnboardingDialog = true
        }
    }
    
    var selectedTheme by remember { mutableStateOf<ThemeStoreViewModel.RemoteTheme?>(null) }
    var downloadingTheme by remember { mutableStateOf<ThemeStoreViewModel.RemoteTheme?>(null) }
    var downloadCompletedTheme by remember { mutableStateOf<ThemeStoreViewModel.RemoteTheme?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Onboarding dialog for first-time theme store entry
    if (showOnboardingDialog) {
        AlertDialog(
            onDismissRequest = { showOnboardingDialog = false },
            title = { Text(stringResource(R.string.theme_mode_onboarding_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.theme_mode_onboarding_msg))
                    Spacer(Modifier.height(FolkDimens.Size16))
                    // Built-in option
                    OutlinedButton(
                        onClick = {
                            prefs.edit { putString(themeModeKey, "builtin") }
                            themeMode = "builtin"
                            showOnboardingDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(FolkDimens.Size8)) {
                            Text(
                                stringResource(R.string.theme_mode_builtin_label),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.theme_mode_builtin_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(FolkDimens.Size12))
                    // Compat option
                    OutlinedButton(
                        onClick = {
                            prefs.edit { putString(themeModeKey, "compat") }
                            themeMode = "compat"
                            showOnboardingDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(FolkDimens.Size8)) {
                            Text(
                                stringResource(R.string.theme_mode_compat_label),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.theme_mode_compat_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOnboardingDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // 监听下载进度
    val downloadProgressFlow = remember { viewModel.getDownloadProgressFlow() }
    var downloadProgress by remember { mutableStateOf<DownloadProgress?>(null) }

    LaunchedEffect(downloadProgressFlow) {
        downloadProgressFlow.collect { progressMap ->
            downloadingTheme?.let { theme ->
                downloadProgress = progressMap[theme.id]
                
                // 检查下载是否完成
                if (downloadProgress?.status == DownloadStatus.COMPLETED) {
                    downloadCompletedTheme = downloadingTheme
                    downloadingTheme = null
                    downloadProgress = null
                } else if (downloadProgress?.status == DownloadStatus.FAILED) {
                    // 下载失败：errorMessage 缺失时不拼接 ":null"，避免显示无意义信息
                    val detail = downloadProgress?.errorMessage?.takeIf { it.isNotBlank() }
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (detail != null) {
                                context.getString(R.string.theme_download_failed) + ": $detail"
                            } else {
                                context.getString(R.string.theme_download_failed)
                            }
                        )
                    }
                    downloadingTheme = null
                    downloadProgress = null
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.themes.isEmpty()) {
            viewModel.fetchThemes()
        }
    }

    // 下载对话框
    if (downloadingTheme != null && downloadProgress != null) {
        ThemeDownloadDialog(
            theme = downloadingTheme!!,
            progress = downloadProgress!!,
            onCancel = {
                viewModel.cancelDownload(downloadingTheme!!.id)
                downloadingTheme = null
                downloadProgress = null
            },
            onPause = {
                if (downloadProgress?.status == DownloadStatus.PAUSED) {
                    viewModel.startDownload(downloadingTheme!!)
                } else {
                    viewModel.pauseDownload(downloadingTheme!!.id)
                }
            }
        )
    }

    // 下载完成对话框
    if (downloadCompletedTheme != null) {
        val completedTheme = downloadCompletedTheme!!
        ThemeDownloadCompleteDialog(
            theme = completedTheme,
            onApply = {
                scope.launch {
                    // 重新加载本地主题列表以确保最新
                    viewModel.loadLocalThemes()
                    // 等待一小段时间让列表更新
                    kotlinx.coroutines.delay(100)
                    val localTheme = viewModel.localThemes.find { it.id == completedTheme.id }
                    if (localTheme != null) {
                                    val success = viewModel.applyTheme(localTheme)
                                    if (success) {
                                        snackbarHostState.showSnackbar(context.getString(R.string.my_themes_applied))
                                    } else {
                                        snackbarHostState.showSnackbar(context.getString(R.string.my_themes_apply_failed))
                                    }
                    } else {
                        snackbarHostState.showSnackbar("Theme not found in local list")
                    }
                }
                downloadCompletedTheme = null
            },
            onGoToMyThemes = {
                navigator.navigate(MyThemesScreenDestination)
                downloadCompletedTheme = null
            },
            onDismiss = {
                downloadCompletedTheme = null
            }
        )
    }

    // 主题详情对话框
    if (selectedTheme != null) {
        val theme = selectedTheme!!
        val typeString = if (theme.type == "tablet") stringResource(R.string.theme_type_tablet) else stringResource(R.string.theme_type_phone)
        val sourceString = if (theme.source == "official") stringResource(R.string.theme_source_official) else stringResource(R.string.theme_source_third_party)
        val isDownloaded = viewModel.isThemeDownloaded(theme.id)
        val isDownloading = viewModel.isThemeDownloading(theme.id)

        AlertDialog(
            onDismissRequest = { selectedTheme = null },
            title = { Text(text = theme.name) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.theme_store_author, theme.author),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.theme_store_version, theme.version),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${stringResource(R.string.theme_type)}: $typeString",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${stringResource(R.string.theme_source)}: $sourceString",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(FolkDimens.Size8))
                    Text(
                        text = theme.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isDownloaded) {
                        Spacer(modifier = Modifier.height(FolkDimens.Size8))
                        Text(
                            text = "✓ Already downloaded",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                if (isDownloaded) {
                    Button(
                        onClick = {
                            scope.launch {
                                val localTheme = viewModel.localThemes.find { it.id == theme.id }
                                if (localTheme != null) {
                                    val success = viewModel.applyTheme(localTheme)
                                    if (success) {
                                        snackbarHostState.showSnackbar(context.getString(R.string.my_themes_applied))
                                    }
                                }
                            }
                            selectedTheme = null
                        }
                    ) {
                        Text(stringResource(R.string.my_themes_apply))
                    }
                } else if (isDownloading) {
                    OutlinedButton(
                        onClick = { selectedTheme = null },
                        enabled = false
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(FolkDimens.Size16))
                        Spacer(modifier = Modifier.width(FolkDimens.Size8))
                        Text("Downloading...")
                    }
                } else {
                    Button(
                        onClick = {
                            if (isCompatMode) {
                                // Compat mode: open download URL in browser
                                try {
                                    uriHandler.openUri(theme.downloadUrl)
                                } catch (_: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.theme_download_failed)
                                        )
                                    }
                                }
                            } else {
                                downloadingTheme = theme
                                viewModel.startDownload(theme)
                            }
                            selectedTheme = null
                        }
                    ) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(FolkDimens.Size8))
                        Text(stringResource(R.string.theme_store_download))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTheme = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // 过滤器对话框
    if (showFilterSheet) {
        ExpressiveDialog(
            onDismissRequest = { showFilterSheet = false },
            width = FolkDimens.Size320,
            shape = FolkShape.Corner28,
            blurBehind = false,
        ) {
            ThemeFilterSheetContent(
                currentAuthor = viewModel.filterAuthor,
                currentSource = viewModel.filterSource,
                currentTypePhone = viewModel.filterTypePhone,
                currentTypeTablet = viewModel.filterTypeTablet,
                onApply = { author, source, phone, tablet ->
                    viewModel.updateFilters(author, source, phone, tablet)
                    showFilterSheet = false
                },
                onReset = {
                    viewModel.updateFilters("", "all", phone = true, tablet = true)
                }
            )
        }
    }

    FolkScaffold(
        title = stringResource(R.string.theme_store_title),
        titleStyle = if (isSearchActive) FolkTitleStyle.Inline else FolkTitleStyle.Flexible,
        subtitle = stringResource(R.string.theme_store_subtitle),
        onBack = {
            if (isSearchActive) {
                isSearchActive = false
                viewModel.onSearchQueryChange("")
            } else {
                navigator.popBackStack()
            }
        },
        titleContent = if (isSearchActive) {
            {
                TextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text(stringResource(R.string.theme_store_search_hint)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FolkColors.Transparent,
                        unfocusedContainerColor = FolkColors.Transparent,
                        disabledContainerColor = FolkColors.Transparent,
                        focusedIndicatorColor = FolkColors.Transparent,
                        unfocusedIndicatorColor = FolkColors.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            null
        },
        actions = {
            // "我的主题"按钮 — hidden in compat mode
            if (!isCompatMode) {
                IconButton(
                    onClick = { navigator.navigate(MyThemesScreenDestination) },
                    modifier = Modifier.navOrigin(captureBitmap = true),
                ) {
                    Icon(Icons.Outlined.ColorLens, contentDescription = stringResource(R.string.my_themes_title))
                }
            }
            if (isSearchActive) {
                if (viewModel.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(CoreR.string.core_action_clear))
                    }
                }
            } else {
                IconButton(onClick = { isSearchActive = true }) {
                    Icon(Icons.Outlined.Search, contentDescription = stringResource(CoreR.string.core_action_search))
                }
            }
            IconButton(onClick = { showFilterSheet = true }) {
                Icon(Icons.Outlined.FilterList, contentDescription = stringResource(CoreR.string.core_action_filter))
            }
        },
        snackbarHostState = snackbarHostState,
    ) { paddingValues ->
        if (viewModel.isRefreshing) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                FolkLoadingIndicator(
                    text = stringResource(R.string.loading_themes),
                )
            }
        } else if (viewModel.errorMessage != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = viewModel.errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(FolkDimens.Size16)
                )
                Button(onClick = { viewModel.fetchThemes() }) {
                    Text(stringResource(R.string.retry))
                }
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = FolkDimens.Size128),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(FolkDimens.Size16),
                verticalItemSpacing = FolkDimens.Size16,
                horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
            ) {
                items(
                    items = viewModel.themes.distinctBy { it.id },
                    key = { it.id }
                ) { theme ->
                    val localTheme = viewModel.localThemes.find { it.id == theme.id }
                    ThemeGridItem(
                        theme = theme,
                        localPreviewPath = localTheme?.previewImagePath,
                        onClick = { selectedTheme = theme }
                    )
                }
            }
        }
    }
}

/**
 * 主题下载对话框
 */
