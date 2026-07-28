package me.bmax.apatch.ui.screen

import android.os.Environment
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.KeyEventBlocker
import me.bmax.apatch.util.getSafeDownloadsDir
import me.bmax.apatch.util.runAPModuleAction
import me.bmax.apatch.util.ui.LocalSnackbarHost
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@Destination<RootGraph>
fun ExecuteAPMActionScreen(navigator: DestinationsNavigator, moduleId: String) {
    var text by rememberSaveable { mutableStateOf("") }
    val displayBuffer = remember { StringBuffer() }
    val fullLogBuffer = remember { StringBuffer() }
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var actionResult: Boolean

    fun appendLog(line: String) {
        fullLogBuffer.append(line).append("\n")
    }

    /**
     * Append a line to the display buffer, truncating to the last 100K chars so the
     * saveable [text] state (mirrored from [displayBuffer] via the polling job) never
     * exceeds the Binder transaction limit and triggers TransactionTooLargeException.
     * The full, untruncated log is kept in [fullLogBuffer] for saving to a file.
     */
    fun appendDisplay(line: String) {
        if (line.startsWith("\u001B[H\u001BJ")) { // clear command
            displayBuffer.setLength(0)
            displayBuffer.append(line.substring(6))
        } else {
            displayBuffer.append(line)
            val len = displayBuffer.length
            if (len > 100_000) {
                displayBuffer.delete(0, len - 100_000)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (text.isNotEmpty()) {
            return@LaunchedEffect
        }

        val updaterJob = launch {
            while (true) {
                kotlinx.coroutines.delay(100)
                val newText = displayBuffer.toString()
                if (text.length != newText.length) {
                    text = newText
                }
            }
        }

        withContext(Dispatchers.IO) {
            runAPModuleAction(
                moduleId,
                onStdout = {
                    val tempText = "$it\n"
                    appendDisplay(tempText)
                    appendLog(it)
                },
                onStderr = {
                    appendLog(it)
                }
            ).let {
                actionResult = it
            }
        }

        updaterJob.cancel()
        val finalText = displayBuffer.toString()
        if (text.length != finalText.length) {
            text = finalText
        }

        if (actionResult) {
            if (!APApplication.sharedPreferences.getBoolean("apm_action_stay_on_page", true)) {
                navigator.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                onBack = dropUnlessResumed {
                    navigator.popBackStack()
                },
                onSave = {
                    scope.launch {
                        val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                        val date = format.format(Date())
                        val file = File(
                            getSafeDownloadsDir(me.bmax.apatch.apApp),
                            "APatch_apm_action_log_${date}.log"
                        )
                        file.writeText(fullLogBuffer.toString())
                        snackBarHost.showSnackbar("Log saved to ${file.absolutePath}")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost) }
    ) { innerPadding ->
        KeyEventBlocker {
            it.key == Key.VolumeDown || it.key == Key.VolumeUp
        }
        Column(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(innerPadding)
                .verticalScroll(scrollState),
        ) {
            LaunchedEffect(text) {
                scrollState.animateScrollTo(scrollState.maxValue, animationSpec = tween(durationMillis = 80))
            }
            Text(
                modifier = Modifier.padding(8.dp),
                text = text,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                fontFamily = FontFamily.Monospace,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onBack: () -> Unit = {}, onSave: () -> Unit = {}) {
    TopAppBar(
        title = { Text(stringResource(R.string.apm_action)) },
        navigationIcon = {
            IconButton(
                onClick = onBack
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        },
        actions = {
            IconButton(onClick = onSave) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "Save log"
                )
            }
        }
    )
}
