package me.bmax.apatch.ui.screen

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.AppLoadingIndicator
import me.bmax.apatch.ui.component.OnlineModuleCard
import me.bmax.apatch.ui.component.SearchAppBar
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenu
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenuItem
import me.bmax.apatch.ui.viewmodel.OnlineModuleViewModel
import me.bmax.apatch.ui.viewmodel.RepoModuleViewModel
import me.bmax.apatch.util.DownloadListener
import me.bmax.apatch.util.download
import me.bmax.apatch.util.ui.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun OnlineModuleScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<OnlineModuleViewModel>()
    val repoViewModel = viewModel<RepoModuleViewModel>()
    val context = LocalContext.current

    val prefs = remember { APApplication.sharedPreferences }
    var sourceType by remember { mutableStateOf(prefs.getString("online_module_source", "official") ?: "official") }
    var repoUrl by remember { mutableStateOf(prefs.getString("custom_repo_url", "") ?: "") }
    var showSourceMenu by remember { mutableStateOf(false) }
    var showRepoUrlDialog by remember { mutableStateOf(false) }
    var showRepoSelectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.modules.isEmpty() && sourceType == "official") {
            viewModel.fetchModules()
        }
    }

    Scaffold(
        topBar = {
            SearchAppBar(
                title = { Text(stringResource(R.string.online_module_title)) },
                searchText = if (sourceType == "official") viewModel.searchQuery else repoViewModel.searchQuery,
                onSearchTextChange = {
                    if (sourceType == "official") viewModel.onSearchQueryChange(it)
                    else repoViewModel.onSearchQueryChange(it)
                },
                onClearClick = {
                    if (sourceType == "official") viewModel.onSearchQueryChange("")
                    else repoViewModel.onSearchQueryChange("")
                },
                onBackClick = { navigator.popBackStack() },
                trailingActions = {
                    IconButton(onClick = { showSourceMenu = true }) {
                        Icon(Icons.Outlined.SwapHoriz, contentDescription = stringResource(R.string.online_module_source_title))
                    }
                    WallpaperAwareDropdownMenu(
                        expanded = showSourceMenu,
                        onDismissRequest = { showSourceMenu = false }
                    ) {
                        WallpaperAwareDropdownMenuItem(
                            text = { Text(stringResource(R.string.online_module_source_official)) },
                            onClick = {
                                sourceType = "official"
                                prefs.edit().putString("online_module_source", "official").apply()
                                showSourceMenu = false
                            }
                        )
                        WallpaperAwareDropdownMenuItem(
                            text = { Text(stringResource(R.string.online_module_source_cluster)) },
                            onClick = {
                                showSourceMenu = false
                                showRepoSelectDialog = true
                            }
                        )
                        WallpaperAwareDropdownMenuItem(
                            text = { Text(stringResource(R.string.online_module_source_custom)) },
                            onClick = {
                                showSourceMenu = false
                                showRepoUrlDialog = true
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            if (sourceType == "official") {
                OfficialContent(viewModel, context)
            } else {
                RepoContent(repoViewModel, repoUrl, context)
            }
        }

        DownloadListener(context) { uri ->
            navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.APM))
        }
    }

    if (showRepoUrlDialog) {
        RepoUrlDialog(
            initialUrl = repoUrl,
            onDismiss = { showRepoUrlDialog = false },
            onConfirm = { url ->
                repoUrl = url
                prefs.edit().putString("custom_repo_url", url).apply()
                sourceType = "custom"
                prefs.edit().putString("online_module_source", "custom").apply()
                showRepoUrlDialog = false
                repoViewModel.resetModules()
                repoViewModel.fetchModules(url)
            }
        )
    }

    if (showRepoSelectDialog) {
        RepoSelectDialog(
            viewModel = repoViewModel,
            onRepoSelected = { url ->
                repoUrl = url
                prefs.edit().putString("custom_repo_url", url).apply()
                sourceType = "cluster"
                prefs.edit().putString("online_module_source", "cluster").apply()
                showRepoSelectDialog = false
                repoViewModel.resetModules()
                repoViewModel.fetchModules(url)
            },
            onDismiss = { showRepoSelectDialog = false }
        )
    }
}

@Composable
private fun OfficialContent(
    viewModel: OnlineModuleViewModel,
    context: Context
) {
    LaunchedEffect(Unit) {
        if (viewModel.modules.isEmpty() && !viewModel.isRefreshing) {
            viewModel.fetchModules()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (viewModel.isRefreshing) {
            AppLoadingIndicator(
                text = stringResource(R.string.loading_modules),
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (viewModel.errorMessage != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
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
                items(viewModel.modules, key = { it.name }) { module ->
                    OnlineModuleItem(module, context)
                }
            }
        }
    }
}

@Composable
private fun RepoContent(
    viewModel: RepoModuleViewModel,
    repoUrl: String,
    context: Context
) {
    LaunchedEffect(repoUrl) {
        if (viewModel.modules.isEmpty() && !viewModel.isRefreshing) {
            viewModel.fetchModules(repoUrl)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (viewModel.isRefreshing) {
            AppLoadingIndicator(
                text = stringResource(R.string.loading_modules),
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (viewModel.errorMessage != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = viewModel.errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = { viewModel.fetchModules(repoUrl) }) {
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
                items(viewModel.modules, key = { it.id }) { module ->
                    OnlineModuleCard(
                        name = module.name,
                        version = module.version,
                        description = module.description,
                        typeLabel = "APM",
                        versionLabel = stringResource(R.string.apm_version),
                        downloadContentDescription = stringResource(R.string.online_module_download_notification, module.name),
                        onDownload = {
                            val latestVersion = module.versions.firstOrNull()
                            if (latestVersion != null) {
                                download(
                                    context = context,
                                    url = latestVersion.zipUrl,
                                    fileName = "${module.name}-${module.version}.zip",
                                    description = module.description,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RepoSelectDialog(
    viewModel: RepoModuleViewModel,
    onRepoSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showManualInput by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.repositories.isEmpty() && !viewModel.isReposLoading) {
            viewModel.fetchRepositories()
        }
    }

    if (showManualInput) {
        RepoUrlDialog(
            initialUrl = "",
            onDismiss = { showManualInput = false },
            onConfirm = { url ->
                showManualInput = false
                onRepoSelected(url)
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.online_module_select_repo)) },
            text = {
                if (viewModel.isReposLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.repositories, key = { it.url }) { repo ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onRepoSelected(repo.url)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = repo.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (repo.modulesCount > 0) {
                                            Text(
                                                text = stringResource(R.string.online_module_module_count, repo.modulesCount),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    if (repo.description.isNotEmpty()) {
                                        Text(
                                            text = repo.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManualInput = true }) {
                    Text(stringResource(R.string.online_module_enter_url_manually))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun RepoUrlDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputUrl by remember { mutableStateOf(initialUrl) }
    val isValid = inputUrl.isNotBlank() && (inputUrl.startsWith("http://") || inputUrl.startsWith("https://"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.online_module_repo_url_title)) },
        text = {
            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                label = { Text(stringResource(R.string.online_module_repo_url_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(inputUrl) },
                enabled = isValid
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun OnlineModuleItem(module: OnlineModuleViewModel.OnlineModule, context: Context) {
    val downloadStartText = stringResource(R.string.online_module_download_start, module.name)
    val downloadNotificationText = stringResource(R.string.online_module_download_notification, module.name)

    OnlineModuleCard(
        name = module.name,
        version = module.version,
        description = module.description,
        typeLabel = "APM",
        versionLabel = stringResource(R.string.apm_version),
        downloadContentDescription = downloadStartText,
        onDownload = {
            showToast(context, downloadNotificationText)
            download(
                context = context,
                url = module.url,
                fileName = "${module.name}-${module.version}.zip",
                description = downloadStartText,
                onDownloading = {},
                onDownloaded = {},
            )
        },
    )
}
