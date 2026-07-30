package me.bmax.apatch.ui.screen

import android.content.Context
import android.net.Uri
import me.bmax.apatch.util.ui.showToast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import me.bmax.apatch.ui.viewmodel.OnlineScriptViewModel
import me.bmax.apatch.util.SafeUriResolver
import me.bmax.apatch.util.download
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun OnlineScriptScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<OnlineScriptViewModel>()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (viewModel.modules.isEmpty()) {
            viewModel.fetchModules()
        }
    }

    Scaffold(
        topBar = {
            SearchAppBar(
                title = { Text(stringResource(R.string.online_script_title)) },
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
                    text = stringResource(R.string.loading_scripts),
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
                    Button(onClick = { viewModel.fetchModules() }) {
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
                    items(viewModel.modules, key = { it.name }) { script ->
                        OnlineScriptItem(script, context)
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineScriptItem(script: OnlineScriptViewModel.OnlineScript, context: Context) {
    val downloadStartText = stringResource(R.string.online_script_download_start, script.name)
    val downloadNotificationText = stringResource(R.string.online_script_download_notification, script.name)
    val scope = rememberCoroutineScope()

    val scriptFileName = "${script.name}-${script.version}.sh"

    fun handleDownloadComplete(uri: Uri) {
        scope.launch {
            try {
                val targetFile = withContext(Dispatchers.IO) {
                    val scriptDir = File("/storage/emulated/0/Download/FolkPatch/script")
                    if (!scriptDir.exists()) {
                        scriptDir.mkdirs()
                    }

                    val outputFile = File(scriptDir, scriptFileName)
                    when (uri.scheme?.lowercase()) {
                        "content" -> {
                            SafeUriResolver.openInputStream(context, uri)?.use { input ->
                                outputFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            } ?: throw IllegalStateException("无法读取下载文件")
                        }
                        "file" -> {
                            val downloadFile = File(uri.path ?: "")
                            if (!downloadFile.exists()) {
                                throw IllegalStateException("Downloaded file not found")
                            }
                            downloadFile.copyTo(outputFile, overwrite = true)
                        }
                        else -> {
                            val downloadPath = uri.path?.replace("file://", "") ?: ""
                            val downloadFile = File(downloadPath)
                            if (!downloadFile.exists()) {
                                throw IllegalStateException("Downloaded file not found")
                            }
                            downloadFile.copyTo(outputFile, overwrite = true)
                        }
                    }
                    outputFile.setExecutable(true)
                    outputFile
                }

                withContext(Dispatchers.Main) {
                    showToast(context, context.getString(R.string.script_library_downloaded_to, targetFile.absolutePath))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast(context, context.getString(R.string.script_library_download_failed, e.message ?: ""))
                }
            }
        }
    }

    OnlineModuleCard(
        name = script.name,
        version = script.version,
        description = script.description,
        typeLabel = "Script",
        versionLabel = stringResource(R.string.apm_version),
        downloadContentDescription = downloadStartText,
        onDownload = {
            showToast(context, downloadNotificationText)
            download(
                context = context,
                url = script.url,
                fileName = scriptFileName,
                description = downloadStartText,
                onDownloaded = { uri ->
                    handleDownloadComplete(uri)
                },
            )
        },
    )
}
