package me.bmax.apatch.ui.screen.settings

import me.bmax.apatch.ui.theme.tokens.FolkColors
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkScaffold
import me.bmax.apatch.ui.component.folk.FolkLogCard
import me.bmax.apatch.ui.component.folk.FolkLogEmptyState
import me.bmax.apatch.ui.component.folk.FolkLogLevel
import me.bmax.apatch.ui.component.folk.FolkLogLine
import me.bmax.apatch.ui.component.folk.FolkLogLoading
import me.bmax.apatch.ui.component.folk.FolkTitleStyle
import me.bmax.apatch.ui.component.folk.folkLogLevelColor
import me.bmax.apatch.ui.component.folk.folkLogTextStyle
import me.bmax.apatch.ui.component.folk.parseFolkLogLevel
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenu
import me.bmax.apatch.ui.component.WallpaperAwareDropdownMenuItem
import me.bmax.apatch.util.ShizukuServiceManager
import me.bmax.apatch.util.ui.showToast
import java.io.File
import androidx.compose.material.icons.outlined.*

/** Log source: server persistent log / system logcat. */
private enum class LogSource { SERVER, LOGCAT }

/** One log line, its parsed level and where that level letter sits in the text. */
private data class LogLine(val level: FolkLogLevel, val text: String, val levelIndex: Int)

private val LEVELS = listOf(
    FolkLogLevel.Verbose,
    FolkLogLevel.Debug,
    FolkLogLevel.Info,
    FolkLogLevel.Warn,
    FolkLogLevel.Error,
)

/** Localised name of a level, so the filter chips read as words and not just letters. */
@Composable
private fun logLevelLabel(level: FolkLogLevel): String = when (level) {
    FolkLogLevel.Verbose -> stringResource(R.string.shizuku_log_level_verbose)
    FolkLogLevel.Debug -> stringResource(R.string.shizuku_log_level_debug)
    FolkLogLevel.Info -> stringResource(R.string.shizuku_log_level_info)
    FolkLogLevel.Warn -> stringResource(R.string.shizuku_log_level_warn)
    FolkLogLevel.Error -> stringResource(R.string.shizuku_log_level_error)
    FolkLogLevel.Unknown -> level.letter.toString()
}

private fun parseLines(raw: String): List<LogLine> {
    if (raw.isBlank()) return emptyList()
    return raw.split('\n')
        .filter { it.isNotBlank() }
        .map { line ->
            val (level, index) = parseFolkLogLevel(line)
            LogLine(level, line, index)
        }
}

@Destination<RootGraph>(wrappers = [me.bmax.apatch.ui.navigation.NavContainerTransformWrapper::class])
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
    var activeLevels by remember { mutableStateOf<Set<FolkLogLevel>>(emptySet()) }
    var showMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // One horizontal scroll shared by the whole log stream, so dragging any line
    // shifts every line together instead of each line scrolling on its own.
    val logScrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val logStyle = folkLogTextStyle()
    val density = LocalDensity.current

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

    // Every line is forced to the widest line's width (plus a little slack) so that
    // sharing [logScrollState] cannot clamp against a shorter line's scroll range.
    val logContentWidth = remember(visibleLines, logStyle) {
        val widest = visibleLines.maxByOrNull { it.text.length }?.text
        if (widest == null) {
            Dp.Unspecified
        } else {
            with(density) {
                (textMeasurer.measure(widest, logStyle).size.width + FolkDimens.Size16.roundToPx()).toDp()
            }
        }
    }

    // Scroll to the bottom (latest logs) when new data arrives
    LaunchedEffect(visibleLines.size, isLoading) {
        if (!isLoading && visibleLines.isNotEmpty()) {
            listState.scrollToItem(visibleLines.size - 1)
        }
    }

    FolkScaffold(
        title = stringResource(R.string.shizuku_log_title),
        titleStyle = if (isSearchActive) FolkTitleStyle.Inline else FolkTitleStyle.Flexible,
        onBack = {
            if (isSearchActive) {
                isSearchActive = false
                query = ""
            } else {
                navigator.popBackStack()
            }
        },
        titleContent = if (isSearchActive) {
            {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.shizuku_log_search_hint)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = FolkColors.Transparent,
                        unfocusedContainerColor = FolkColors.Transparent,
                        disabledContainerColor = FolkColors.Transparent,
                        focusedIndicatorColor = FolkColors.Transparent,
                        unfocusedIndicatorColor = FolkColors.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            null
        },
        actions = {
                    if (isSearchActive) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = null)
                            }
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        }
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = null)
                    }
                    WallpaperAwareDropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        WallpaperAwareDropdownMenuItem(
                            text = { Text(stringResource(R.string.shizuku_log_refresh)) },
                            leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                refresh()
                            },
                        )
                        WallpaperAwareDropdownMenuItem(
                            text = { Text(stringResource(R.string.shizuku_log_copy)) },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                val text = visibleLines.joinToString("\n") { it.text }
                                if (text.isBlank()) {
                                    showToast(context, context.getString(R.string.shizuku_log_empty))
                                } else {
                                    clipboard.setText(AnnotatedString(text))
                                    showToast(context, context.getString(R.string.shizuku_log_copied))
                                }
                            },
                        )
                        WallpaperAwareDropdownMenuItem(
                            text = { Text(stringResource(R.string.shizuku_log_export)) },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
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
                            },
                        )
                        WallpaperAwareDropdownMenuItem(
                            text = { Text(stringResource(R.string.shizuku_log_clear)) },
                            leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    withContext(Dispatchers.IO) { ShizukuServiceManager.clearServerLog() }
                                    showToast(context, context.getString(R.string.shizuku_log_cleared))
                                    refresh()
                                }
                            },
                        )
                    }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Compact toolbar on the page itself, not in a card: source on one
            // line, level filters on the next. Two lines keep every chip visible
            // instead of hiding the last one past the screen edge, and still cost
            // far less height than the old filter card.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size8),
                verticalArrangement = Arrangement.spacedBy(FolkDimens.Size8),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size8)) {
                    FilterChip(
                        selected = source == LogSource.SERVER,
                        onClick = { source = LogSource.SERVER },
                        label = { Text(stringResource(R.string.shizuku_log_source_server)) },
                    )
                    FilterChip(
                        selected = source == LogSource.LOGCAT,
                        onClick = { source = LogSource.LOGCAT },
                        label = { Text(stringResource(R.string.shizuku_log_source_logcat)) },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size8),
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
                            label = { Text("${level.letter} ${logLevelLabel(level)}", color = folkLogLevelColor(level)) },
                        )
                    }
                }
            }

            // 2) Log body: a single rounded container in the app's card style,
            // filling the remaining height like a standard manager log page.
            when {
                isLoading -> FolkLogLoading()
                visibleLines.isEmpty() -> FolkLogEmptyState(
                    icon = Icons.AutoMirrored.Outlined.Article,
                    title = stringResource(R.string.shizuku_log_empty),
                )
                else -> Box(modifier = Modifier.fillMaxSize().padding(bottom = FolkDimens.Size8)) {
                    FolkLogCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = FolkDimens.Size16),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = FolkDimens.Size12, vertical = FolkDimens.Size8),
                        ) {
                            items(visibleLines) { line ->
                                FolkLogLine(
                                    level = line.level,
                                    text = line.text,
                                    levelIndex = line.levelIndex,
                                    scrollState = logScrollState,
                                    contentWidth = logContentWidth,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
