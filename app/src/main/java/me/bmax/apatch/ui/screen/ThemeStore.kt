package me.bmax.apatch.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.MyThemesScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.AppLoadingIndicator
import me.bmax.apatch.ui.viewmodel.ThemeStoreViewModel
import java.io.File
import me.bmax.apatch.util.DownloadProgress
import me.bmax.apatch.util.DownloadStatus
import me.bmax.apatch.util.ThemeDownloader

@Destination<RootGraph>
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
    
    var selectedTheme by remember { mutableStateOf<ThemeStoreViewModel.RemoteTheme?>(null) }
    var downloadingTheme by remember { mutableStateOf<ThemeStoreViewModel.RemoteTheme?>(null) }
    var downloadCompletedTheme by remember { mutableStateOf<ThemeStoreViewModel.RemoteTheme?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

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
                    // 下载失败
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.theme_download_failed) + 
                            ": ${downloadProgress?.errorMessage}"
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
                // TODO: 实现暂停功能
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = theme.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isDownloaded) {
                        Spacer(modifier = Modifier.height(8.dp))
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
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Downloading...")
                    }
                } else {
                    Button(
                        onClick = {
                            downloadingTheme = theme
                            viewModel.startDownload(theme)
                            selectedTheme = null
                        }
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
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
        BasicAlertDialog(
            onDismissRequest = { showFilterSheet = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation
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
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text(stringResource(R.string.theme_store_search_hint)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor =  Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(stringResource(R.string.theme_store_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            viewModel.onSearchQueryChange("")
                        } else {
                            navigator.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // "我的主题"按钮
                    IconButton(onClick = { navigator.navigate(MyThemesScreenDestination) }) {
                        Icon(Icons.Filled.ColorLens, contentDescription = "My Themes")
                    }
                    if (isSearchActive) {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (viewModel.isRefreshing) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                AppLoadingIndicator(
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
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = { viewModel.fetchThemes() }) {
                    Text(stringResource(R.string.retry))
                }
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 128.dp),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalItemSpacing = 16.dp,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeDownloadDialog(
    theme: ThemeStoreViewModel.RemoteTheme,
    progress: DownloadProgress,
    onCancel: () -> Unit,
    onPause: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.theme_download_title)) },
        text = {
            Column {
                // 主题信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 预览图（正方形加圆角）
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(theme.previewUrl)
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = theme.name,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    Column {
                        Text(
                            text = theme.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = theme.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 总进度条
                Text(
                    text = "${stringResource(R.string.theme_download_progress)}: ${(progress.overallProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                val animatedProgress by animateFloatAsState(
                    targetValue = progress.overallProgress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "DownloadProgress"
                )
                LinearWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 文件进度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stringResource(R.string.theme_download_file)}: ${(progress.fileProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${stringResource(R.string.theme_download_image)}: ${(progress.imageProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // 错误信息
                if (progress.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = progress.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onCancel) {
                Text(stringResource(R.string.theme_download_cancel))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onPause,
                enabled = false // TODO: 实现暂停功能
            ) {
                Text(stringResource(R.string.theme_download_pause))
            }
        }
    )
}

/**
 * 下载完成对话框
 */
@Composable
fun ThemeDownloadCompleteDialog(
    theme: ThemeStoreViewModel.RemoteTheme,
    onApply: () -> Unit,
    onGoToMyThemes: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.theme_download_completed))
            }
        },
        text = {
            Column {
                Text(
                    text = "${theme.name} ${stringResource(R.string.theme_download_finalizing)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onApply) {
                Text(stringResource(R.string.theme_download_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onGoToMyThemes) {
                Text(stringResource(R.string.theme_download_go_to_my_themes))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeFilterSheetContent(
    currentAuthor: String,
    currentSource: String,
    currentTypePhone: Boolean,
    currentTypeTablet: Boolean,
    onApply: (String, String, Boolean, Boolean) -> Unit,
    onReset: () -> Unit
) {
    var author by remember { mutableStateOf(currentAuthor) }
    var source by remember { mutableStateOf(currentSource) }
    var typePhone by remember { mutableStateOf(currentTypePhone) }
    var typeTablet by remember { mutableStateOf(currentTypeTablet) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.theme_store_filter_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = author,
            onValueChange = { author = it },
            label = { Text(stringResource(R.string.theme_store_filter_author)) },
            placeholder = { Text(stringResource(R.string.theme_store_filter_author_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.theme_store_filter_source),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 1f)
            )
            FilterChip(
                selected = source == "all",
                onClick = { source = "all" },
                label = { Text(stringResource(R.string.theme_store_filter_source_all)) },
                colors = chipColors
            )
            FilterChip(
                selected = source == "official",
                onClick = { source = "official" },
                label = { Text(stringResource(R.string.theme_source_official)) },
                colors = chipColors
            )
            FilterChip(
                selected = source == "third_party",
                onClick = { source = "third_party" },
                label = { Text(stringResource(R.string.theme_source_third_party)) },
                colors = chipColors
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.theme_store_filter_type),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 1f)
            )
            FilterChip(
                selected = typePhone,
                onClick = { typePhone = !typePhone },
                label = { Text(stringResource(R.string.theme_type_phone)) },
                colors = chipColors
            )
            FilterChip(
                selected = typeTablet,
                onClick = { typeTablet = !typeTablet },
                label = { Text(stringResource(R.string.theme_type_tablet)) },
                colors = chipColors
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    onReset()
                    author = ""
                    source = "all"
                    typePhone = true
                    typeTablet = true
                }
            ) {
                Text(stringResource(R.string.theme_store_filter_reset))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onApply(author, source, typePhone, typeTablet) }
            ) {
                Text(stringResource(R.string.theme_store_filter_apply))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ThemeGridItem(
    theme: ThemeStoreViewModel.RemoteTheme,
    localPreviewPath: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val previewFile = localPreviewPath?.let { File(it) }
    val imageModel = if (previewFile != null && previewFile.exists()) {
        Uri.fromFile(previewFile)
    } else {
        theme.previewUrl
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = theme.name,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentScale = ContentScale.FillWidth
        )
    }
}
