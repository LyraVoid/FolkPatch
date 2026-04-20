package me.bmax.apatch.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.AppLoadingIndicator
import me.bmax.apatch.ui.viewmodel.OnlineKPMViewModel
import me.bmax.apatch.util.download

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun OnlineKPMScreen(navigator: DestinationsNavigator) {
    val viewModel = viewModel<OnlineKPMViewModel>()
    val context = LocalContext.current
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.modules.isEmpty()) {
            viewModel.fetchModules()
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
                            placeholder = { Text(stringResource(R.string.search_modules)) },
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
                        Text(stringResource(R.string.online_kpm_title))
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
                }
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
                    Button(onClick = { viewModel.fetchModules() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            } else {
                val configuration = LocalConfiguration.current
                val isWideScreen = configuration.screenWidthDp >= 600

                if (isWideScreen) {
                    LazyVerticalStaggeredGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = StaggeredGridCells.Fixed(2),
                        verticalItemSpacing = 8.dp,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(viewModel.modules, key = { module -> module.name }) { module ->
                            OnlineKPMItem(module, context)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.modules) { module ->
                            OnlineKPMItem(module, context)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineKPMItem(module: OnlineKPMViewModel.OnlineKPM, context: Context) {
    val downloadStartText = stringResource(R.string.online_kpm_download_start, module.name)
    val downloadNotificationText = stringResource(R.string.online_kpm_download_notification, module.name)

    Surface(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = module.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Version: ${module.version}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Args: ${if (module.needControl) "Control" else "NoControl"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(
                onClick = {
                    // Show toast message with download info
                    Toast.makeText(context, downloadNotificationText, Toast.LENGTH_LONG).show()

                    // Start the download
                    download(
                        context = context,
                        url = module.url,
                        fileName = "${module.name}-${module.version}.kpm",
                        description = downloadStartText,
                        onDownloading = {},
                        onDownloaded = {}
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download"
                )
            }
        }
    }
}