package me.bmax.apatch.ui.screen.plugin

import me.bmax.apatch.ui.theme.tokens.FolkDimens
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkLogEmptyState
import me.bmax.apatch.ui.component.folk.FolkLogLoading
import me.bmax.apatch.ui.component.folk.FolkLogText
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.component.splicedLazyColumnGroup
import org.json.JSONArray
import java.io.File

data class PluginLogEntry(
    val id: String,
    val name: String,
    val log: String,
)

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
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

    FolkScaffold(
        title = stringResource(R.string.plugin_log_page_title),
        titleStyle = FolkTitleStyle.Flexible,
        onBack = { navigator.popBackStack() },
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
    ) { paddingValues ->
        if (isLoading) {
            FolkLogLoading(modifier = Modifier.padding(paddingValues))
        } else if (logs.isEmpty()) {
            FolkLogEmptyState(
                icon = Icons.AutoMirrored.Outlined.Article,
                title = stringResource(R.string.plugin_log_all_empty),
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(top = FolkDimens.Size8),
                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
            ) {
                splicedLazyColumnGroup(
                    items = logs,
                    key = { _, entry -> entry.id },
                ) { _, entry ->
                    PluginLogCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun PluginLogCard(entry: PluginLogEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(FolkDimens.Size16),
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
            Spacer(Modifier.width(FolkDimens.Size8))
            Text(
                text = entry.id,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(FolkDimens.Size12))
        FolkLogText(text = entry.log)
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
