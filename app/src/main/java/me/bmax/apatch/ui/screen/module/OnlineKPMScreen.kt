package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkLoadingIndicator
import me.bmax.apatch.ui.component.OnlineModuleCard
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.rememberSearchAppBarScrollBehavior
import me.bmax.apatch.ui.viewmodel.OnlineKPMViewModel
import me.bmax.apatch.util.download
import me.bmax.apatch.util.ui.showToast
import me.bmax.apatch.ui.component.folk.FolkStateView
import me.bmax.apatch.ui.component.folk.FolkStateTone
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.Icons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun OnlineKPMScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<OnlineKPMViewModel>()
    val context = LocalContext.current
    val topAppBarScrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val searchBarScrollBehavior = rememberSearchAppBarScrollBehavior(topAppBarScrollBehavior)
    val moduleGridState = rememberLazyGridState()
    val searchGridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        if (viewModel.modules.isEmpty()) {
            viewModel.fetchModules()
        }
    }

    FolkScaffold(
        topBar = {
            SearchAppBar(
                largeTitle = true,
                collapsibleSearch = true,
                scrollBehavior = searchBarScrollBehavior,
                title = { Text(stringResource(R.string.online_kpm_title)) },
                searchText = viewModel.searchQuery,
                onSearchTextChange = viewModel::onSearchQueryChange,
                onClearClick = { viewModel.onSearchQueryChange("") },
                onBackClick = { navigator.popBackStack() },
                defaultContent = { bottomPadding, _ ->
                    OnlineKPMContent(
                        viewModel = viewModel,
                        context = context,
                        state = searchGridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPadding),
                    )
                },
                searchContent = { bottomPadding, _ ->
                    OnlineKPMContent(
                        viewModel = viewModel,
                        context = context,
                        state = searchGridState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = bottomPadding),
                    )
                },
            )
        }
    ) { innerPadding ->
        OnlineKPMContent(
            viewModel = viewModel,
            context = context,
            state = moduleGridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .nestedScroll(searchBarScrollBehavior.nestedScrollConnection),
        )
    }
}

@Composable
private fun OnlineKPMContent(
    viewModel: OnlineKPMViewModel,
    context: Context,
    state: LazyGridState,
    modifier: Modifier,
) {
    Box(modifier = modifier) {
        if (viewModel.isRefreshing) {
            FolkLoadingIndicator(
                text = stringResource(R.string.loading_modules),
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (viewModel.errorMessage != null) {
            FolkStateView(
                title = viewModel.errorMessage ?: "Unknown error",
                icon = Icons.Outlined.Warning,
                tone = FolkStateTone.Critical,
                action = {
                    Button(onClick = { viewModel.fetchModules() }) {
                        Text(stringResource(R.string.retry))
                    }
                },
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = FolkDimens.Size320),
                modifier = Modifier.fillMaxSize(),
                state = state,
                contentPadding = PaddingValues(FolkDimens.Size16),
                horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
            ) {
                items(viewModel.modules, key = { it.name }) { module ->
                    OnlineKPMItem(module, context)
                }
            }
        }
    }
}

@Composable
fun OnlineKPMItem(module: OnlineKPMViewModel.OnlineKPM, context: Context) {
    val downloadStartText = stringResource(R.string.online_kpm_download_start, module.name)
    val downloadNotificationText = stringResource(R.string.online_kpm_download_notification, module.name)

    OnlineModuleCard(
        name = module.name,
        version = module.version,
        description = module.description,
        typeLabel = "KPM",
        versionLabel = stringResource(R.string.kpm_version),
        capabilityLabel = if (module.needControl) stringResource(R.string.kpm_control) else null,
        downloadContentDescription = downloadStartText,
        onDownload = {
            showToast(context, downloadNotificationText)
            download(
                context = context,
                url = module.url,
                fileName = "${module.name}-${module.version}.kpm",
                description = downloadStartText,
                onDownloading = {},
                onDownloaded = {},
            )
        },
    )
}
