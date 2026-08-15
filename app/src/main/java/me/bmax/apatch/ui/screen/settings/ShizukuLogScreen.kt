package me.bmax.apatch.ui.screen.settings

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.util.ShizukuServiceManager
import me.bmax.apatch.util.ui.showToast
import java.io.File

/** Log source: server persistent log / system logcat. */
private enum class LogSource { SERVER, LOGCAT }

/** One log line and its parsed level. */
private data class LogLine(val level: Char, val text: String)

private val LEVELS = listOf('V', 'D', 'I', 'W', 'E')

/** Parse the log level from a line, e.g. "... I/tag: msg" or logcat's "... I/tag(pid): msg". */
private fun parseLevel(line: String): Char {
    // Find the first " X/" occurrence (X is the level letter)
    var i = 0
    while (i < line.length - 1) {
        val c = line[i]
        if ((c == 'V' || c == 'D' || c == 'I' || c == 'W' || c == 'E' || c == 'A')
            && line[i + 1] == '/'
            && (i == 0 || line[i - 1] == ' ')
        ) {
            return c
        }
        i++
    }
    return '?'
}

private fun parseLines(raw: String): List<LogLine> {
    if (raw.isBlank()) return emptyList()
    return raw.split('\n')
        .filter { it.isNotBlank() }
        .map { LogLine(parseLevel(it), it) }
}

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuLogScreen(navigator: DestinationsNavigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var source by remember { mutableStateOf(LogSource.SERVER) }
    var allLines by remember { mutableStateOf<List<LogLine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    // Active level filter; an empty set means show all.
    var activeLevels by remember { mutableStateOf<Set<Char>>(emptySet()) }

    val listState = rememberLazyListState()

    fun refresh() {
        scope.launch {
            isLoading = true
            val raw = withContext(Dispatchers.IO) {
                when (source) {
                    LogSource.SERVER -> ShizukuServiceManager.getServerLog()
                    LogSource.LOGCAT -> ShizukuServiceManager.getLogcat()
                }
            }
            allLines = parseLines(raw)
            isLoading = false
        }
    }

    LaunchedEffect(source) { refresh() }

    val visibleLines = remember(allLines, query, activeLevels) {
        allLines.filter { line ->
            (activeLevels.isEmpty() || line.level in activeLevels) &&
                (query.isBlank() || line.text.contains(query, ignoreCase = true))
        }
    }

    // Scroll to the bottom (latest logs) when new data arrives
    LaunchedEffect(visibleLines.size, isLoading) {
        if (!isLoading && visibleLines.isNotEmpty()) {
            listState.scrollToItem(visibleLines.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.shizuku_log_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.shizuku_log_refresh))
                    }
                    IconButton(onClick = {
                        val text = visibleLines.joinToString("\n") { it.text }
                        if (text.isBlank()) {
                            showToast(context, context.getString(R.string.shizuku_log_empty))
                        } else {
                            clipboard.setText(AnnotatedString(text))
                            showToast(context, context.getString(R.string.shizuku_log_copied))
                        }
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = stringResource(R.string.shizuku_log_copy))
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val text = withContext(Dispatchers.IO) {
                                buildString {
                                    append("==== Shizuku server log ====\n")
                                    append(ShizukuServiceManager.getServerLog())
                                    append("\n\n==== logcat (shizuku tags) ====\n")
                                    append(ShizukuServiceManager.getLogcat())
                                }
                            }
                            if (text.isBlank()) {
                                showToast(context, context.getString(R.string.shizuku_log_empty))
                                return@launch
                            }
                            val file = File(context.cacheDir, "shizuku_log.txt")
                            file.writeText(text)
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, context.getString(R.string.shizuku_log_export))
                            )
                        }
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.shizuku_log_export))
                    }
                    IconButton(onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { ShizukuServiceManager.clearServerLog() }
                            showToast(context, context.getString(R.string.shizuku_log_cleared))
                            refresh()
                        }
                    }) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = stringResource(R.string.shizuku_log_clear))
                    }
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Source switch
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SegmentedButton(
                    selected = source == LogSource.SERVER,
                    onClick = { source = LogSource.SERVER },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.shizuku_log_source_server)) }
                SegmentedButton(
                    selected = source == LogSource.LOGCAT,
                    onClick = { source = LogSource.LOGCAT },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.shizuku_log_source_logcat)) }
            }

            // Level filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LEVELS.forEach { level ->
                    FilterChip(
                        selected = level in activeLevels,
                        onClick = {
                            activeLevels = if (level in activeLevels) {
                                activeLevels - level
                            } else {
                                activeLevels + level
                            }
                        },
                        label = { Text(level.toString(), color = levelColor(level)) },
                    )
                }
            }

            // Search
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.shizuku_log_search_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                visibleLines.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Article,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.shizuku_log_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    items(visibleLines) { line ->
                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = levelColor(line.level),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun levelColor(level: Char): Color = when (level) {
    'E' -> MaterialTheme.colorScheme.error
    'W' -> Color(0xFFE29A2E)
    'I' -> MaterialTheme.colorScheme.onSurface
    'D' -> MaterialTheme.colorScheme.onSurfaceVariant
    'V' -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
