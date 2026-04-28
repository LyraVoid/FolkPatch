package me.bmax.apatch.ui.screen

import android.net.Uri
import android.os.Environment
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.KeyEventBlocker
import me.bmax.apatch.util.getSafeDownloadsDir
import me.bmax.apatch.util.installModule
import me.bmax.apatch.util.BulkInstallManager
import me.bmax.apatch.util.reboot
import me.bmax.apatch.util.ui.LocalSnackbarHost
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MODULE_TYPE {
    KPM, APM
}

@Composable
@Destination<RootGraph>
fun InstallScreen(navigator: DestinationsNavigator, uri: Uri, type: MODULE_TYPE) {
    var text by remember { mutableStateOf("") }
    val displayBuffer = remember { StringBuffer() }
    val fullLogBuffer = remember { StringBuffer() }
    var showFloatAction by rememberSaveable { mutableStateOf(false) }

    val snackBarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val isExternalInstall = remember(activity) {
        activity?.intent?.let { intent ->
            intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND
        } ?: false
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
            installModule(uri, type, onFinish = { success ->
                updaterJob.cancel()
                val finalText = displayBuffer.toString()
                if (text.length != finalText.length) {
                    text = finalText
                }
                if (success) {
                    showFloatAction = true
                }
            }, onStdout = {
                val tempText = "$it\n"
                if (tempText.startsWith("[H[J")) { // clear command
                    displayBuffer.setLength(0)
                    displayBuffer.append(tempText.substring(6))
                } else {
                    displayBuffer.append(tempText)
                }
                fullLogBuffer.append(it).append("\n")
            }, onStderr = {
                val tempText = "$it\n"
                if (tempText.startsWith("[H[J")) { // clear command
                    displayBuffer.setLength(0)
                    displayBuffer.append(tempText.substring(6))
                } else {
                    displayBuffer.append(tempText)
                }
                fullLogBuffer.append(it).append("\n")
            })
        }
    }

    Scaffold(topBar = {
        TopBar(onBack = dropUnlessResumed {
            if (isExternalInstall) {
                activity?.finish()
            } else {
                BulkInstallManager.clear()
                navigator.popBackStack()
            }
        }, onSave = {
            scope.launch {
                val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
                val date = format.format(Date())
                val file = File(
                    getSafeDownloadsDir(context),
                    "APatch_install_${type}_log_${date}.log"
                )
                file.writeText(fullLogBuffer.toString())
                snackBarHost.showSnackbar("Log saved to ${file.absolutePath}")
            }
        })
    }, floatingActionButton = {
        if (showFloatAction) {
            if (BulkInstallManager.hasNext()) {
                val nextText = stringResource(id = R.string.next_module)
                ExtendedFloatingActionButton(
                    onClick = {
                        val nextUri = BulkInstallManager.popNext()
                        if (nextUri != null) {
                            navigator.popBackStack()
                            navigator.navigate(InstallScreenDestination(nextUri, type))
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, nextText) },
                    text = { Text(text = nextText) },
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                    contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1f),
                )
            } else {
                val reboot = stringResource(id = R.string.reboot)
                ExtendedFloatingActionButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                reboot()
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.Refresh, reboot) },
                    text = { Text(text = reboot) },
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                    contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 1f),
                )
            }
        }

    }, snackbarHost = { SnackbarHost(snackBarHost) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(innerPadding)
                .verticalScroll(scrollState),
        ) {
            LaunchedEffect(text) {
                scrollState.animateScrollTo(scrollState.maxValue)
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
    TopAppBar(title = { Text(stringResource(R.string.apm_install)) }, navigationIcon = {
        IconButton(
            onClick = onBack
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
    }, actions = {
        IconButton(onClick = onSave) {
            Icon(
                imageVector = Icons.Filled.Save, contentDescription = "Localized description"
            )
        }
    })
}

@Preview
@Composable
fun InstallPreview() {
//    InstallScreen(DestinationsNavigator(), uri = Uri.EMPTY)
}