package me.bmax.apatch.ui.screen.theme

import me.bmax.apatch.ui.theme.tokens.FolkColors
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.component.folk.folkPressScale
import me.bmax.apatch.ui.viewmodel.ThemeStoreViewModel
import java.io.File
import me.bmax.apatch.ui.component.folk.FolkStateView
import androidx.compose.material.icons.outlined.Palette
import me.bmax.apatch.core.ui.R as CoreR

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun MyThemesScreen(
    navigator: DestinationsNavigator
) {
    val viewModel = viewModel<ThemeStoreViewModel>(
        factory = ThemeStoreViewModel.Factory(LocalContext.current)
    )
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var selectedTheme by remember { mutableStateOf<ThemeStoreViewModel.LocalTheme?>(null) }
    var showDeleteDialog by remember { mutableStateOf<ThemeStoreViewModel.LocalTheme?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    // 刷新本地主题列表
    LaunchedEffect(Unit) {
        if (viewModel.localThemes.isEmpty()) {
            viewModel.loadLocalThemes()
        }
    }

    // 主题详情对话框
    if (selectedTheme != null) {
        val theme = selectedTheme!!
        val typeString = if (theme.type == "tablet") stringResource(R.string.theme_type_tablet) else stringResource(R.string.theme_type_phone)
        val sourceString = if (theme.source == "official") stringResource(R.string.theme_source_official) else stringResource(R.string.theme_source_third_party)

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
                        text = theme.description.ifEmpty { "No description" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = viewModel.applyTheme(theme)
                            if (success) {
                                snackbarHostState.showSnackbar(context.getString(R.string.my_themes_applied))
                            } else {
                                snackbarHostState.showSnackbar(context.getString(R.string.my_themes_apply_failed))
                            }
                        }
                        selectedTheme = null
                    }
                ) {
                    Text(stringResource(R.string.my_themes_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTheme = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog != null) {
        val theme = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.my_themes_delete)) },
            text = {
                Column {
                    Text(text = theme.name)
                    Spacer(modifier = Modifier.height(FolkDimens.Size8))
                    Text(
                        text = stringResource(R.string.my_themes_delete_confirm),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = viewModel.deleteTheme(theme)
                            if (success) {
                                snackbarHostState.showSnackbar(context.getString(R.string.my_themes_deleted))
                            } else {
                                snackbarHostState.showSnackbar("Failed to delete theme")
                            }
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    FolkScaffold(
        title = stringResource(R.string.my_themes_title),
        titleStyle = if (isSearchActive) FolkTitleStyle.Inline else FolkTitleStyle.Flexible,
        subtitle = stringResource(R.string.my_themes_subtitle),
        onBack = {
            if (isSearchActive) {
                isSearchActive = false
                viewModel.onLocalSearchQueryChange("")
            } else {
                navigator.popBackStack()
            }
        },
        titleContent = if (isSearchActive) {
            {
                TextField(
                    value = viewModel.localSearchQuery,
                    onValueChange = { viewModel.onLocalSearchQueryChange(it) },
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
            if (isSearchActive) {
                if (viewModel.localSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onLocalSearchQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(CoreR.string.core_action_clear))
                    }
                }
            } else {
                IconButton(onClick = { isSearchActive = true }) {
                    Icon(Icons.Outlined.Search, contentDescription = stringResource(CoreR.string.core_action_search))
                }
                IconButton(onClick = {
                    refreshing = true
                    viewModel.loadLocalThemes()
                    refreshing = false
                }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = stringResource(CoreR.string.core_action_refresh))
                }
            }
        },
        snackbarHostState = snackbarHostState,
    ) { paddingValues ->
        if (viewModel.localThemes.isEmpty()) {
            // 空状态
            FolkStateView(
                title = stringResource(R.string.my_themes_empty),
                modifier = Modifier.padding(paddingValues),
                icon = Icons.Outlined.Palette,
                action = {
                    Button(
                        onClick = { navigator.popBackStack() }
                    ) {
                        Text(stringResource(R.string.my_themes_empty_action))
                    }
                },
            )
        } else {
            // 主题瀑布流（和主题商店一致）
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = FolkDimens.Size128),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(FolkDimens.Size16),
                verticalItemSpacing = FolkDimens.Size16,
                horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size16)
            ) {
                items(
                    items = viewModel.localThemes,
                    key = { it.id }
                ) { theme ->
                    MyThemeGridItem(
                        theme = theme,
                        onClick = { selectedTheme = theme },
                        onLongClick = { showDeleteDialog = theme }
                    )
                }
            }
        }
    }
}

@Composable
fun MyThemeGridItem(
    theme: ThemeStoreViewModel.LocalTheme,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .folkPressScale(interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLongClick()
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = FolkDimens.Size4)
    ) {
        Box {
            // 图片加载：本地优先，回退网络
            val previewImageFile = File(theme.previewImagePath)
            val imageModel = if (previewImageFile.exists()) {
                // 使用本地文件
                Uri.fromFile(previewImageFile)
            } else if (theme.previewUrl.isNotEmpty()) {
                // 回退到网络 URL
                theme.previewUrl
            } else {
                null
            }
            
            // 保持图片原始比例（和主题商店一致）
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageModel)
                    .crossfade(true)
                    .build(),
                contentDescription = theme.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentScale = ContentScale.FillWidth
            )
            
            // 底部信息（渐变背景：深色遮罩确保浅色预览图上白字也可读）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(FolkColors.Transparent, FolkColors.MediaScrim.copy(alpha = 0.6f))
                        )
                    )
                    .padding(FolkDimens.Size8)
            ) {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FolkColors.MediaContent,
                    modifier = Modifier.padding(FolkDimens.Size4),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
