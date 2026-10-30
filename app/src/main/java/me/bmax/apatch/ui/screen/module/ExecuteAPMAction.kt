package me.bmax.apatch.ui.screen.module

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.theme.tokens.FolkMotion
import me.bmax.apatch.util.getSafeDownloadsDir
import me.bmax.apatch.util.runAPModuleAction
import me.bmax.apatch.util.ui.LocalSnackbarHost
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
fun ExecuteAPMActionScreen(navigator: DestinationsNavigator, moduleId: String) {
    var text by rememberSaveable { mutableStateOf("") }
    val displayBuffer = remember { StringBuffer() }
    val fullLogBuffer = remember { StringBuffer() }
    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var actionResult: Boolean

    val clearScreenSequence = "\u001B[H\u001B[J"

    fun appendLog(line: String) {
        fullLogBuffer.append(line.removePrefix(clearScreenSequence)).append("\n")
    }

    /**
     * Append a line to the display buffer, truncating to the last 100K chars so the
     * saveable [text] state (mirrored from [displayBuffer] via the polling job) never
     * exceeds the Binder transaction limit and triggers TransactionTooLargeException.
     * The full, untruncated log is kept in [fullLogBuffer] for saving to a file.
     */
    fun appendDisplay(line: String) {
        if (line.startsWith(clearScreenSequence)) { // clear command
            displayBuffer.setLength(0)
            displayBuffer.append(line.removePrefix(clearScreenSequence))
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

    FolkScaffold(
        title = stringResource(R.string.apm_action),
        titleStyle = FolkTitleStyle.Flexible,
        onBack = dropUnlessResumed {
            navigator.popBackStack()
        },
        actions = {
            IconButton(
                onClick = {
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
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = stringResource(R.string.save_log)
                )
            }
        },
        snackbarHostState = snackBarHost,
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
                scrollState.animateScrollTo(scrollState.maxValue, animationSpec = FolkMotion.ScrollIntoView)
            }
            Text(
                modifier = Modifier.padding(FolkDimens.Size8),
                text = text,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                fontFamily = FontFamily.Monospace,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            )
        }
    }
}
