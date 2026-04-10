package me.bmax.apatch.ui.screen

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    var isSearchActive by remember { mutableStateOf(false) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

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
                            placeholder = { Text(stringResource(R.string.search_scripts)) },
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
                        Text(stringResource(R.string.online_script_title))
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
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.modules) { script ->
                        OnlineScriptItem(
                            script = script,
                            context = context
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineScriptItem(
    script: OnlineScriptViewModel.OnlineScript,
    context: Context
) {
    val downloadStartText = stringResource(R.string.online_script_download_start, script.name)
    val downloadNotificationText = stringResource(R.string.online_script_download_notification, script.name)
    var fileName by remember { mutableStateOf("") }
    var isDownloading by remember { mutableStateOf(false) }
    var isDownloaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val fileNameWithoutExt = "${script.name}-${script.version}"
    val scriptFileName = "$fileNameWithoutExt.sh"

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
                    Toast.makeText(
                        context,
                        "Downloaded to: ${targetFile.absolutePath}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Download failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                    text = script.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version: ${script.version}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = script.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(
                onClick = {
                    Toast.makeText(context, downloadNotificationText, Toast.LENGTH_LONG).show()
                    
                    download(
                        context = context,
                        url = script.url,
                        fileName = scriptFileName,
                        description = downloadStartText,
                        onDownloaded = { uri ->
                            handleDownloadComplete(uri)
                        }
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
