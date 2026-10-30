package me.bmax.apatch.ui.component.folk

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.theme.tokens.FolkType
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens

/**
 * Measurements that give the FolkPatch settings its own rhythm.
 *
 * These are deliberately not Material defaults: preferences grow with their
 * content, the trailing slot keeps a wider margin than the leading edge and the
 * section spacing is generous, so the page reads as "breathing" rather than as
 * a dense system settings list.
 */
object FolkSettingsDimens {
    /**
     * Horizontal inset of a settings group from the screen edge.
     * Measured: panel edge sits ~16.2dp from the screen edge.
     */
    val ScreenPadding = 16.dp

    /**
     * Cap for the content column on large screens. The shells centre the
     * content within this width so lines stay readable on tablets and
     * desktop-sized windows.
     */
    val ContentMaxWidth = 840.dp

    /**
     * Vertical gap after a section (before the next section title).
     * Measured: panel bottom -> next panel top is ~51.5dp in total, which is
     * sectionSpacing + one title line box (~18dp) + SectionTitleSpacing.
     */
    val SectionSpacing = 22.dp

    /** Gap between a section title and its group surface. */
    val SectionTitleSpacing = 12.dp

    /** Extra indent of the section title relative to the screen edge. */
    val SectionTitleIndent = 8.dp

    /**
     * Corner radius of a group surface.
     *
     * Measured by fitting a circle to the corner arc of a settings panel:
     * 60-62px @ density 2.975 = 20.2dp; the same fit reproduces our own radii
     * within 0.5dp. The shape is the token layer's continuous corner at the
     * same radius.
     */
    val GroupShape = FolkShape.Corner20

    /**
     * Vertical padding inside a group surface.
     * The reference panel height equals the sum of its row heights exactly, so
     * the group must not add any padding of its own.
     */
    val GroupVerticalPadding = 0.dp

    /** Leading inset of a preference row. */
    val ItemHorizontalPadding = 16.dp

    /** Trailing inset of a preference row. Matches the leading inset so the trailing control lines up with the row's edge. */
    val ItemEndPadding = 16.dp

    /**
     * Vertical padding of a row.
     *
     * Constant, not content-dependent: a single-line row is ~56dp
     * (2x16 + one 20dp line, floored by the row's 56dp minimum) and a two-line
     * row ~74dp (2x16 + 20 + 18),
     * i.e. the height difference comes purely from the extra text line. Our
     * previous 13/17 split made single-line rows too tight and two-line rows
     * too tall, which is what read as "uneven / oddly large".
     */
    val ItemVerticalPadding = 16.dp

    val ItemVerticalPaddingWithSummary = 16.dp

    /** Minimum touch height shared by every segmented settings row. */
    val ItemMinHeight = 56.dp

    /** Leading icon size. The reference uses a standard 24dp icon box. */
    val IconSize = 24.dp

    /**
     * Gap between the leading icon and the text column.
     * Measured: text starts ~56dp from the panel edge = 16 inset + 24 icon + 16.
     */
    val IconSpacing = 16.dp

    /** Tight gap between a title and its summary so they read as one unit. */
    val TitleSummarySpacing = 2.dp

    /** Gap between the text column and the trailing slot. */
    val TrailingSpacing = 14.dp

    /** Trailing chevron size. */
    val ChevronSize = 18.dp
}

/**
 * Group surface colour, taken from the token layer.
 *
 * The reference never turns a setting group into a strong coloured card - the
 * panel is only a touch lighter than the page. That blend now lives once in
 * [FolkTheme.palette] instead of here, so every screen can share it rather than
 * each one deciding how far to lift a container off the page.
 */
@Composable
fun folkGroupColor(): Color =
    LocalFolkWallpaperTokens.current?.group?.fill ?: FolkTheme.palette.groupedSurface

@Composable
fun folkInsetColor(): Color =
    LocalFolkWallpaperTokens.current?.raised?.fill ?: FolkTheme.palette.groupedInset

@Composable
fun folkOverlayColor(): Color =
    LocalFolkWallpaperTokens.current?.overlay?.fill ?: FolkTheme.palette.groupedInset

@Composable
fun folkSectionTitleColor(): Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)

@Composable
fun folkSupportingColor(): Color =
    LocalWallpaperSupportingColor.current ?: MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun folkIconColor(enabled: Boolean = true): Color =
    if (enabled) folkSupportingColor()
    else folkSupportingColor().copy(alpha = 0.38f)

/** Small, letter-spaced section label - brand voice, not a Material label. */
@Composable
fun folkSectionTitleStyle(): TextStyle = MaterialTheme.typography.labelLarge.copy(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.8.sp,
)

/**
 * Preference title: tighter line height and a slightly heavier weight than the
 * Material body style, which is what gives the row its "app" rather than
 * "system settings" feel. The family still comes from the active Typography so
 * user-selected custom fonts keep working.
 */
@Composable
fun folkPreferenceTitleStyle(): TextStyle = FolkType.Title

@Composable
fun folkPreferenceSummaryStyle(): TextStyle = FolkType.Summary

@Composable
fun folkPreferenceValueStyle(): TextStyle = MaterialTheme.typography.bodyMedium.copy(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.2.sp,
)
