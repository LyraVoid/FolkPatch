package me.bmax.apatch.ui.component.folk

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.ui.theme.tokens.WallpaperSurfaceRole

/**
 * The pieces shared by every log screen: the neutral monospace line, the rounded
 * container the log lives in, and the loading/empty states. Keeping them here
 * means the Shizuku log, the plugin log and the audit log read as one family
 * instead of three near-copies that drift apart.
 */

/** Severity of a log line, used to tint its level letter. */
enum class FolkLogLevel(val letter: Char) {
    Verbose('V'),
    Debug('D'),
    Info('I'),
    Warn('W'),
    Error('E'),
    Unknown('?'),
}

/**
 * Reads the level out of a log line such as `... I/tag: msg` or logcat's
 * `... I/tag(pid): msg`. Returns the level and the index of its letter so the
 * caller can tint just that character. [FolkLogLevel.Unknown] with index -1
 * means the line carries no level.
 */
fun parseFolkLogLevel(line: String): Pair<FolkLogLevel, Int> {
    var i = 0
    while (i < line.length - 1) {
        val letter = line[i]
        if ((letter == 'V' || letter == 'D' || letter == 'I' || letter == 'W' || letter == 'E')
            && line[i + 1] == '/'
            && (i == 0 || line[i - 1] == ' ')
        ) {
            return FolkLogLevel.entries.first { it.letter == letter } to i
        }
        i++
    }
    return FolkLogLevel.Unknown to -1
}

/**
 * M3 has no "caution" role. Tertiary alone can land on a hue that reads as info
 * or even as success, so warning is blended a third of the way towards error: it
 * stays derived (AMOLED, dynamic colour and every classic theme keep working)
 * while always leaning warm and attention-seeking.
 */
@Composable
fun folkLogLevelColor(level: FolkLogLevel): Color = when (level) {
    FolkLogLevel.Error -> MaterialTheme.colorScheme.error
    FolkLogLevel.Warn -> lerp(
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        0.35f,
    )
    FolkLogLevel.Info -> MaterialTheme.colorScheme.onSurface
    FolkLogLevel.Debug -> folkSupportingColor()
    FolkLogLevel.Verbose -> folkSupportingColor().copy(alpha = 0.7f)
    FolkLogLevel.Unknown -> folkSupportingColor()
}

/** The rounded neutral surface a log stream lives in. */
@Composable
fun FolkLogCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FolkWallpaperSurface(
        role = WallpaperSurfaceRole.Group,
        modifier = modifier,
        shape = FolkShape.Corner16,
        fallbackColor = FolkTheme.palette.groupedSurface,
        fallbackContentColor = MaterialTheme.colorScheme.onSurface,
        content = content,
    )
}

/** Monospace metrics shared by every log line; stays neutral on purpose. */
@Composable
fun folkLogTextStyle() = MaterialTheme.typography.bodySmall.copy(
    fontFamily = FontFamily.Monospace,
    fontSize = 12.sp,
    lineHeight = 17.sp,
)

/**
 * A single log line, with only the level letter tinted and bold.
 *
 * [scrollState] and [contentWidth] are hoisted so a whole stream can share one
 * horizontal scroll: forcing every line to the same content width keeps each
 * line's scroll range identical, which is what makes a shared offset meaningful.
 * Leave them at their defaults for a line that scrolls on its own.
 */
@Composable
fun FolkLogLine(
    level: FolkLogLevel,
    text: String,
    levelIndex: Int,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    contentWidth: Dp = Dp.Unspecified,
) {
    val tint = folkLogLevelColor(level)
    val styled = remember(text, levelIndex, level, tint) {
        buildAnnotatedString {
            if (levelIndex in text.indices && level != FolkLogLevel.Unknown) {
                append(text.substring(0, levelIndex))
                withStyle(SpanStyle(color = tint, fontWeight = FontWeight.Bold)) {
                    append(text[levelIndex])
                }
                append(text.substring(levelIndex + 1))
            } else {
                append(text)
            }
        }
    }
    val widthModifier =
        if (contentWidth != Dp.Unspecified) Modifier.width(contentWidth) else Modifier.fillMaxWidth()
    Text(
        text = styled,
        modifier = modifier
            .horizontalScroll(scrollState)
            .then(widthModifier)
            .padding(vertical = 1.dp),
        style = folkLogTextStyle(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        softWrap = false,
    )
}

/** A raw log blob rendered with the shared monospace metrics. */
@Composable
fun FolkLogText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = folkLogTextStyle(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Centered spinner shown while a log is loading. */
@Composable
fun FolkLogLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/**
 * Centered empty state: an icon, a title and an optional hint line, matching the
 * other log screens.
 */
@Composable
fun FolkLogEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    FolkStateView(title = title, modifier = modifier, icon = icon, hint = hint)
}
