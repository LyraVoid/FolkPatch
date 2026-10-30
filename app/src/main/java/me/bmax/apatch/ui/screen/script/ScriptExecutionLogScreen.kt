package me.bmax.apatch.ui.screen.script

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.os.Environment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.apApp
import me.bmax.apatch.data.ScriptInfo
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.util.getSafeDownloadsDir
import me.bmax.apatch.util.ui.AnsiUtils
import me.bmax.apatch.util.ui.LocalSnackbarHost
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import me.bmax.apatch.core.ui.R as CoreR

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
@Composable
fun ScriptExecutionLogScreen(
    navigator: DestinationsNavigator,
    scriptInfo: ScriptInfo
) {
    val logLines = remember { mutableStateListOf<AnnotatedString>() }
    val fullLogBuffer = remember { StringBuffer() }

    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val context = androidx.compose.ui.platform.LocalContext.current
    
    var process by remember { mutableStateOf<Process?>(null) }
    var inputCmd by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            var p: Process? = null
            try {
                val kpatchPath = apApp.applicationInfo.nativeLibraryDir + File.separator + "libkpatch.so"
                val commands = listOf(
                    listOf(
                        kpatchPath,
                        APApplication.superKey,
                        "su",
                        "-Z",
                        APApplication.MAGISK_SCONTEXT
                    ),
                    listOf("su")
                )
                var lastError: Exception? = null
                for (command in commands) {
                    try {
                        val pb = ProcessBuilder(command).redirectErrorStream(true)
                        pb.environment().apply {
                            val basePath = System.getenv("PATH").orEmpty()
                            this["PATH"] = "$basePath:/system_ext/bin:/vendor/bin:${APApplication.APATCH_FOLDER}bin"
                            this["BUSYBOX"] = "${APApplication.APATCH_FOLDER}bin/busybox"
                        }
                        p = pb.start()
                        break
                    } catch (e: Exception) {
                        lastError = e
                    }
                }
                if (p == null) {
                    throw lastError ?: IllegalStateException("Unable to start root shell")
                }

                val runningProcess = p
                process = runningProcess
                run {
                    val os = runningProcess.outputStream
                    // Start the script
                    os.write("sh \"${scriptInfo.path}\"\n".toByteArray())
                    os.flush()

                    val reader = runningProcess.inputStream.bufferedReader()
                    val buffer = CharArray(1024)
                    while (true) {
                        val count = reader.read(buffer)
                        if (count == -1) break
                        if (count > 0) {
                            val chunk = String(buffer, 0, count)
                            fullLogBuffer.append(chunk)
                            
                            val lines = chunk.split("\n")
                            withContext(Dispatchers.Main) {
                                if (lines.isNotEmpty()) {
                                    lines.forEach { line ->
                                        if (line.isNotEmpty()) {
                                             // Handle clear screen
                                            if (line.contains("\u001B[H") || line.contains("\u001B[2J")) {
                                                logLines.clear()
                                            }
                                            logLines.add(AnsiUtils.parseAnsi(line))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    runningProcess.waitFor()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    logLines.add(AnnotatedString("Error: ${e.message}"))
                }
            } finally {
                try {
                    p?.destroy()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                process = null
            }
        }
    }

    FolkScaffold(
        title = stringResource(R.string.script_library_output),
        titleStyle = FolkTitleStyle.Flexible,
        onBack = { navigator.popBackStack() },
        actions = {
            IconButton(
                onClick = {
                    scope.launch {
                        try {
                            val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                            val date = format.format(Date())
                            val file = File(
                                getSafeDownloadsDir(context),
                                "FolkPatch/${scriptInfo.alias}_${date}.log"
                            )
                            file.writeText(fullLogBuffer.toString())
                            snackBarHost.showSnackbar("Log saved to ${file.absolutePath}")
                        } catch (e: Exception) {
                            snackBarHost.showSnackbar("Failed to save log: ${e.message}")
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = stringResource(R.string.script_library_save_log)
                )
            }
        },
        snackbarHostState = snackBarHost,
        addBottomClearance = false,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = FolkDimens.Size8, end = FolkDimens.Size8, top = FolkDimens.Size8, bottom = FolkDimens.Size30),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputCmd,
                    onValueChange = { inputCmd = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type command...") },
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            process?.outputStream?.let { out ->
                                try {
                                    out.write((inputCmd + "\n").toByteArray())
                                    out.flush()
                                    withContext(Dispatchers.Main) {
                                        inputCmd = ""
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = stringResource(CoreR.string.core_action_send))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = listState
        ) {
            itemsIndexed(logLines, key = { index, _ -> index }) { _, line ->
                Text(
                    text = line,
                    modifier = Modifier.padding(horizontal = FolkDimens.Size8, vertical = FolkDimens.Size2),
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                )
            }
        }
        LaunchedEffect(logLines.size) {
            if (logLines.isNotEmpty()) {
                listState.animateScrollToItem(logLines.size - 1)
            }
        }
    }
}
