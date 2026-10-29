package me.bmax.apatch.ui.theme.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Semantic colours for grouped screens, derived from the active [ColorScheme] so every theme
 * source - classic, generated, wallpaper, AMOLED, custom background - works with no branch here.
 */
@Immutable
class FolkPalette internal constructor(
    /** The page behind grouped containers. */
    val groupedBackground: Color,
    /** A container on [groupedBackground]: a card, or one group of rows. */
    val groupedSurface: Color,
    /** A fill set inside a [groupedSurface], such as an icon tile or a block of facts. */
    val groupedInset: Color,
    /** Hairlines between the rows of a single container. */
    val separator: Color,
    val positive: Color,
    val caution: Color,
    val critical: Color,
    val neutral: Color,
    /** True when the page shows a custom background image, so chrome should read straight through. */
    val onCustomBackground: Boolean,
) {
    companion object {
        fun from(scheme: ColorScheme): FolkPalette {
            val background = scheme.background

            val onCustomBackground = background.alpha < 0.99f

            return FolkPalette(
                groupedBackground = if (onCustomBackground) Color.Transparent else scheme.surfaceContainer,
                groupedSurface = scheme.surfaceBright,
                groupedInset = scheme.surfaceContainerHigh,
                separator = scheme.outlineVariant,
                // The app already means "fine" by the accent and "failed" by error.
                positive = scheme.primary,
                caution = scheme.tertiary,
                critical = scheme.error,
                neutral = scheme.onSurfaceVariant,
                onCustomBackground = onCustomBackground,
            )
        }
    }
}

/**
 * The token layer. Derived from the ambient theme rather than provided at the root: two dialogs
 * rebuild the scheme inside their own `MaterialTheme`, and a root value would keep describing the
 * outer theme inside them.
 */
object FolkTheme {
    val palette: FolkPalette
        @Composable
        get() {
            val scheme = MaterialTheme.colorScheme
            return remember(scheme) { FolkPalette.from(scheme) }
        }
}
