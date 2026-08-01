package me.bmax.apatch.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.util.getPluginLog
import me.bmax.apatch.util.listPlugins
import me.bmax.apatch.util.rootShellForResult
import me.bmax.apatch.util.ui.showToast
import org.json.JSONArray
import java.io.File

data class PluginLogEntry(
    val id: String,
    val name: String,
    val log: String,
)

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginLogScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var logs by remember { mutableStateOf<List<PluginLogEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshLogs() {
        scope.launch {
            isLoading = true
            logs = withContext(Dispatchers.IO) { loadAllPluginLogs() }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refreshLogs() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.plugin_log_page_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Export button
                    IconButton(onClick = {
                        scope.launch {
                            val content = withContext(Dispatchers.IO) { buildExportText(logs) }
                            if (content.isBlank()) {
                                showToast(context, context.getString(R.string.plugin_log_all_empty))
                                return@launch
                            }
                            val file = File(context.cacheDir, "plugin_logs.txt")
                            file.writeText(content)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.plugin_log_export)))
                        }
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.plugin_log_export))
                    }
                    // Clear button
                    IconButton(onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { clearAllPluginLogs() }
                            showToast(context, context.getString(R.string.plugin_log_cleared))
                            refreshLogs()
                        }
                    }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.plugin_log_clear))
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            )
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else if (logs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Article,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.plugin_log_all_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(logs, key = { it.id }) { entry ->
                    PluginLogCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun PluginLogCard(entry: PluginLogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = entry.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = entry.log,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Load logs from all installed plugins. */
private fun loadAllPluginLogs(): List<PluginLogEntry> {
    val raw = listPlugins().trim()
    if (!raw.startsWith("[")) return emptyList()

    val plugins = runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val id = obj.optString("id").trim()
            val name = obj.optString("name").ifBlank { id }
            if (id.isEmpty()) null else (id to name)
        }
    }.getOrDefault(emptyList())

    return plugins.mapNotNull { (id, name) ->
        val log = getPluginLog(id)
        if (log.isBlank()) null else PluginLogEntry(id = id, name = name, log = log)
    }
}

/** Build a single text blob for export. */
private fun buildExportText(logs: List<PluginLogEntry>): String {
    if (logs.isEmpty()) return ""
    return logs.joinToString("\n\n") { entry ->
        "=== ${entry.name} (${entry.id}) ===\n${entry.log}"
    }
}

/** Clear all plugin logs via apd CLI. */
private fun clearAllPluginLogs() {
    runCatching {
        rootShellForResult("${APApplication.APD_PATH} plugin clear-log all")
    }
}
