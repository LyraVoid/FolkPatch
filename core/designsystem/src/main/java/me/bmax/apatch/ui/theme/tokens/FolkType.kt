package me.bmax.apatch.ui.theme.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp

/**
 * The type ladder a page's own text is written against. The family is never set here - it comes
 * from the active [MaterialTheme.typography], where the font the user picked is applied - and how
 * a line breaks is part of a token, so a long word hyphenates instead of overflowing its box.
 */
object FolkType {

    /** The name of a thing: 17sp over a 22sp line box. */
    val Title: TextStyle
        @Composable get() = MaterialTheme.typography.bodyLarge.merge(TitleMetrics)

    /** What that thing is or does: 15sp over 20sp. */
    val Summary: TextStyle
        @Composable get() = MaterialTheme.typography.bodyLarge.merge(SummaryMetrics)

    /** The small print: 14sp over 18sp. */
    val Caption: TextStyle
        @Composable get() = MaterialTheme.typography.bodyLarge.merge(CaptionMetrics)

    /** [Summary] with tabular figures, so a changing number does not push its neighbours around. */
    val Numeral: TextStyle
        @Composable get() = MaterialTheme.typography.bodyLarge.merge(NumeralMetrics)

    /** [Caption] for machine strings that should keep whole tokens together. */
    val Machine: TextStyle
        @Composable get() = MaterialTheme.typography.bodyLarge.merge(MachineMetrics)
}

internal val TitleMetrics = TextStyle(
    fontSize = 17.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.sp,
    lineBreak = LineBreak.Heading,
    hyphens = Hyphens.Auto,
)

internal val SummaryMetrics = TextStyle(
    fontSize = 15.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
    lineBreak = LineBreak.Paragraph,
    hyphens = Hyphens.Auto,
)

internal val CaptionMetrics = TextStyle(
    fontSize = 14.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 0.sp,
    lineBreak = LineBreak.Paragraph,
    hyphens = Hyphens.Auto,
)

internal val NumeralMetrics = SummaryMetrics.copy(fontFeatureSettings = "tnum")

internal val MachineMetrics = CaptionMetrics.copy(
    fontFeatureSettings = "tnum",
    hyphens = Hyphens.None,
    lineBreak = LineBreak.Heading,
)

/**
 * How every style breaks a line: a heading breaks where a phrase ends, everything else fills its
 * line, and both hyphenate. Public so the Material typography takes the same two settings.
 */
fun TextStyle.wrapAware(lineBreak: LineBreak = LineBreak.Paragraph): TextStyle = copy(
    lineBreak = lineBreak,
    hyphens = Hyphens.Auto,
)
