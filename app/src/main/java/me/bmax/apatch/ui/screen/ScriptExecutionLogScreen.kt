package me.bmax.apatch.ui.screen

import android.os.Environment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import me.bmax.apatch.util.getSafeDownloadsDir
import me.bmax.apatch.util.ui.AnsiUtils
import me.bmax.apatch.util.ui.LocalSnackbarHost
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
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
                // Try to start root shell with multiple strategies
                
                if (p == null) {
                    try {
                        val pb = ProcessBuilder(
                            APApplication.SUPERCMD,
                            APApplication.superKey,
                            "-Z",
                            APApplication.MAGISK_SCONTEXT
                        )
                        pb.redirectErrorStream(true)
                        pb.environment()?.apply {
                            set("PATH", System.getenv("PATH") ?: "" + ":/system_ext/bin:/vendor/bin:${APApplication.APATCH_FOLDER}bin")
                            set("BUSYBOX", "${APApplication.APATCH_FOLDER}bin/busybox")
                        }
                        p = pb.start()
                    } catch (e: Exception) {
                        // Continue
                    }
                }

                if (p == null) {
                    try {
                        val pb = ProcessBuilder(
                            APApplication.SUPERCMD,
                            APApplication.superKey,
                            "su",
                            "-Z",
                            APApplication.MAGISK_SCONTEXT
                        )
                        pb.redirectErrorStream(true)
                        pb.environment()?.apply {
                            set("PATH", System.getenv("PATH") ?: "" + ":/system_ext/bin:/vendor/bin:${APApplication.APATCH_FOLDER}bin")
                            set("BUSYBOX", "${APApplication.APATCH_FOLDER}bin/busybox")
                        }
                        p = pb.start()
                    } catch (e: Exception) {
                        // Continue
                    }
                }

              
                if (p == null) {
                    try {
                        val pb = ProcessBuilder("su")
                        pb.redirectErrorStream(true)
                        pb.environment()?.apply {
                            set("PATH", System.getenv("PATH") ?: "" + ":/system_ext/bin:/vendor/bin:${APApplication.APATCH_FOLDER}bin")
                            set("BUSYBOX", "${APApplication.APATCH_FOLDER}bin/busybox")
                        }
                        p = pb.start()
                    } catch (e: Exception) {
                        // Continue
                    }
                }

              
                if (p == null) {
                    try {
                        val kpatchPath = apApp.applicationInfo.nativeLibraryDir + File.separator + "libkpatch.so"
                        val pb = ProcessBuilder(
                            kpatchPath,
                            APApplication.superKey,
                            "su",
                            "-Z",
                            APApplication.MAGISK_SCONTEXT
                        )
                        pb.redirectErrorStream(true)
                        val env = pb.environment()
                        env?.set("PATH", System.getenv("PATH") ?: "" + ":/system_ext/bin:/vendor/bin:${APApplication.APATCH_FOLDER}bin")
                        env?.set("BUSYBOX", "${APApplication.APATCH_FOLDER}bin/busybox")
                        p = pb.start()
                    } catch (e: Exception) {
                        // Continue
                    }
                }

               
                if (p == null) {
                    try {
                        val pb = ProcessBuilder("su")
                        pb.redirectErrorStream(true)
                        val env = pb.environment()
                        env?.set("PATH", System.getenv("PATH") ?: "" + ":/system_ext/bin:/vendor/bin:${APApplication.APATCH_FOLDER}bin")
                        env?.set("BUSYBOX", "${APApplication.APATCH_FOLDER}bin/busybox")
                        p = pb.start()
                    } catch (e: Exception) {
                        throw e // Rethrow if all failed
                    }
                }

                process = p
                
                if (p != null) {
                    val os = p!!.outputStream
                    // Start the script
                    os.write("sh \"${scriptInfo.path}\"\n".toByteArray())
                    os.flush()

                    val reader = p!!.inputStream.bufferedReader()
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
                    p!!.waitFor()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.script_library_output)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.popBackStack() }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
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
                            imageVector = Icons.Default.Save,
                            contentDescription = stringResource(R.string.script_library_save_log)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost) },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 30.dp),
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
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
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
            items(logLines) { line ->
                Text(
                    text = line,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
