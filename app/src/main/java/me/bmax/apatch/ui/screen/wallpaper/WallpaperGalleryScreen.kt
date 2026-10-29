package me.bmax.apatch.ui.screen.wallpaper

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkLoadingIndicator
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.component.folk.FolkStateView
import me.bmax.apatch.ui.viewmodel.WallpaperGalleryViewModel
import me.bmax.apatch.ui.wallpaper.WallpaperDevice
import me.bmax.apatch.ui.wallpaper.WallpaperItem
import me.bmax.apatch.util.WallpaperDownloader

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperGalleryScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val viewModel = viewModel<WallpaperGalleryViewModel>(factory = WallpaperGalleryViewModel.Factory(app))
    val state by viewModel.state.collectAsState()

    var layoutIndex by rememberSaveable { mutableIntStateOf(0) }
    var preview by remember { mutableStateOf<WallpaperItem?>(null) }
    var showDisplayOptions by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val savedMessage = stringResource(R.string.wallpaper_saved)
    val failedMessage = stringResource(R.string.wallpaper_download_failed)

    if (showDisplayOptions) {
        WallpaperDisplaySheet(
            device = state.device,
            layoutIndex = layoutIndex,
            onDeviceSelected = viewModel::selectDevice,
            onLayoutSelected = { layoutIndex = it },
            onDismiss = { showDisplayOptions = false },
        )
    }

    FolkScaffold(
        title = stringResource(R.string.settings_wallpaper_gallery),
        titleStyle = FolkTitleStyle.Flexible,
        onBack = { navigator.navigateUp() },
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(onClick = { showDisplayOptions = true }) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = stringResource(R.string.wallpaper_display_options),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = FolkSettingsDimens.ScreenPadding),
        ) {
            Spacer(Modifier.height(FolkSettingsDimens.SectionTitleSpacing))
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { preview = null; viewModel.refresh() },
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                when {
                    state.loading || state.refreshing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FolkLoadingIndicator(text = stringResource(R.string.wallpaper_loading))
                    }

                    state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FolkStateView(
                            title = state.error ?: stringResource(R.string.wallpaper_empty),
                            action = {
                                Button(onClick = { viewModel.retry() }) {
                                    Text(stringResource(R.string.wallpaper_retry))
                                }
                            },
                        )
                    }

                    layoutIndex == 0 -> WallpaperWaterfall(
                        items = state.items,
                        contentPadding = PaddingValues(bottom = 24.dp),
                        onTap = { preview = it },
                        onLoadMore = viewModel::loadMore,
                        onImageSize = viewModel::recordImageSize,
                    )

                    else -> WallpaperStack(
                        items = state.items,
                        onTap = { preview = it },
                        onLoadMore = viewModel::loadMore,
                        onImageSize = viewModel::recordImageSize,
                    )
                }
            }
        }
    }

    preview?.let { item ->
        WallpaperPreviewDialog(
            item = item,
            onDismiss = { preview = null },
            onDownload = {
                scope.launch {
                    when (val result = WallpaperDownloader.download(context, item.imageUri, item.fileName)) {
                        is WallpaperDownloader.Result.Success ->
                            snackbarHostState.showSnackbar(savedMessage)
                        is WallpaperDownloader.Result.Failure ->
                            snackbarHostState.showSnackbar("$failedMessage: ${result.message}")
                    }
                }
            },
        )
    }
}

@Composable
private fun WallpaperPreviewDialog(
    item: WallpaperItem,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    val imageMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.65f
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = FolkShape.Dialog,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column {
                AsyncImage(
                    model = item.imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = imageMaxHeight),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.wallpaper_cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onDownload) {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.wallpaper_download))
                    }
                }
            }
        }
    }
}
