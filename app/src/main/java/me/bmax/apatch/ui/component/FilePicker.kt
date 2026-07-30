package me.bmax.apatch.ui.component

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePickerDialog(
    initialPath: String? = null,
    allowedExtensions: List<String> = listOf("fpt"),
    onDismissRequest: () -> Unit,
    onFileSelected: (File) -> Unit
) {
    val context = LocalContext.current
    val prefs = APApplication.sharedPreferences
    val rootPath = "/storage/emulated/0"

    // Permission Check
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
        )
    }

    // Refresh permission status on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    hasPermission = Environment.isExternalStorageManager()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 720.dp)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            // Force opaque color to avoid transparency issues
            color = MaterialTheme.colorScheme.surface.copy(alpha = 1f),
            tonalElevation = 6.dp
        ) {
            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.file_picker_permission_required),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.file_picker_permission_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback if specific intent fails
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            context.startActivity(intent)
                        }
                    }) {
                        Text(stringResource(R.string.file_picker_grant_permission))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.file_picker_cancel))
                    }
                }
            } else {
                // Determine initial path
                val savedPath = remember { prefs.getString("last_file_picker_path", rootPath) ?: rootPath }
                // Use initialPath if provided, otherwise savedPath. Ensure it starts with rootPath.
                val startPath = initialPath ?: savedPath
                val validStartPath = if (startPath.startsWith(rootPath)) startPath else rootPath
                
                var currentPath by remember { mutableStateOf(validStartPath) }
                
                // Save path whenever it changes
                LaunchedEffect(currentPath) {
                    prefs.edit().putString("last_file_picker_path", currentPath).apply()
                }

                val currentFile = File(currentPath)
                // If current path doesn't exist or not directory, fallback to root
                if (!currentFile.exists() || !currentFile.isDirectory) {
                    currentPath = rootPath
                }

                val files = remember(currentPath, allowedExtensions) {
                    try {
                        File(currentPath).listFiles()
                            ?.filter { file ->
                                file.isDirectory || allowedExtensions.isEmpty() || file.extension.lowercase() in allowedExtensions
                            }
                            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
                            ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                val isAtRoot = currentPath == rootPath

                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (isAtRoot) stringResource(R.string.file_picker_internal_storage) else currentFile.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                        },
                        navigationIcon = {
                            if (!isAtRoot) {
                                IconButton(onClick = {
                                    val parent = currentFile.parentFile
                                    if (parent != null && parent.absolutePath.startsWith(rootPath)) {
                                        currentPath = parent.absolutePath
                                    } else {
                                        currentPath = rootPath
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = onDismissRequest) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.file_picker_close)
                                )
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = currentPath.removePrefix(rootPath).ifEmpty { "/" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (allowedExtensions.isNotEmpty()) {
                            Text(
                                text = stringResource(
                                    R.string.file_picker_filtered_files,
                                    allowedExtensions.joinToString(", ") { ".${it.lowercase()}" }
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        if (files.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillParentMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.file_picker_no_files), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(files, key = { it.absolutePath }) { file ->
                                if (file.isDirectory) {
                                    ListItem(
                                        headlineContent = { Text(file.name) },
                                        leadingContent = {
                                            Icon(
                                                Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            currentPath = file.absolutePath
                                        }
                                    )
                                } else {
                                    ListItem(
                                        headlineContent = { Text(file.name, maxLines = 1) },
                                        supportingContent = {
                                            Text(
                                                text = "${file.length() / 1024L} KB",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        },
                                        leadingContent = {
                                            Icon(
                                                Icons.AutoMirrored.Default.InsertDriveFile,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier.clickable { onFileSelected(file) }
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}
