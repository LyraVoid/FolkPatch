package me.bmax.apatch.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.AppLoadingIndicator
import me.bmax.apatch.ui.component.OnlineModuleCard
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.viewmodel.OnlinePluginViewModel
import me.bmax.apatch.util.download
import me.bmax.apatch.util.installPlugin
import me.bmax.apatch.util.ui.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun OnlinePluginScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<OnlinePluginViewModel>()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (viewModel.plugins.isEmpty()) {
            viewModel.fetchPlugins()
        }
    }

    Scaffold(
        topBar = {
            SearchAppBar(
                title = { Text(stringResource(R.string.online_plugin_title)) },
                searchText = viewModel.searchQuery,
                onSearchTextChange = viewModel::onSearchQueryChange,
                onClearClick = { viewModel.onSearchQueryChange("") },
                onBackClick = { navigator.popBackStack() },
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            if (viewModel.isRefreshing) {
                AppLoadingIndicator(
                    text = stringResource(R.string.loading_modules),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (viewModel.errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(onClick = { viewModel.fetchPlugins() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(viewModel.plugins, key = { it.name }) { plugin ->
                        OnlinePluginItem(
                            plugin = plugin,
                            context = context,
                            scope = scope,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlinePluginItem(
    plugin: OnlinePluginViewModel.OnlinePlugin,
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val downloadStartText = stringResource(R.string.online_plugin_download_start, plugin.name)
    val downloadNotificationText = stringResource(R.string.online_plugin_download_notification, plugin.name)

    OnlineModuleCard(
        name = plugin.name,
        version = plugin.version,
        description = plugin.description,
        typeLabel = "Plugin",
        versionLabel = stringResource(R.string.apm_version),
        downloadContentDescription = downloadStartText,
        onDownload = {
            showToast(context, downloadNotificationText)
            download(
                context = context,
                url = plugin.url,
                fileName = "${plugin.name}-${plugin.version}.zip",
                description = downloadStartText,
                onDownloading = {},
                onDownloaded = { uri ->
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            val cached = java.io.File(context.cacheDir, "online_plugin_install.zip")
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                cached.outputStream().use { output -> input.copyTo(output) }
                            }
                            installPlugin(cached.absolutePath).also {
                                cached.delete()
                            }
                        }
                        val msg = if (ok) {
                            context.getString(R.string.plugin_install_success)
                        } else {
                            context.getString(R.string.plugin_install_failed)
                        }
                        showToast(context, msg)
                    }
                },
            )
        },
    )
}
