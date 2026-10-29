package me.bmax.apatch.ui.theme.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * Semantic colours for grouped screens, derived from the active [ColorScheme] so every theme
 * source - classic, generated, wallpaper, AMOLED, custom background - works with no branch here.
 * Containers blend away from the page in both directions, so one definition covers a light and a
 * dark appearance and neither can dissolve into the page.
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
    /** Alpha shared by small controls drawn on the translucent wallpaper chrome. */
    val chromeControlAlpha: Float,
) {
    fun chromeControlFill(tint: Color): Color =
        if (onCustomBackground) tint.copy(alpha = chromeControlAlpha) else tint

    companion object {
        private const val CHROME_ALPHA_FLOOR = 0.1f

        /** Dark is read from luminance, which is what the settings pages were calibrated against. */
        fun from(scheme: ColorScheme): FolkPalette {
            val background = scheme.background

            // A custom background image makes the page transparent and puts the user's opacity
            // on a few roles only.
            val onCustomBackground = background.alpha < 0.99f
            val wallpaperOpacity = scheme.surface.alpha.coerceIn(0f, 1f)
            val dark = background.luminance() < 0.5f

            return FolkPalette(
                groupedBackground = background,
                groupedSurface = when {
                    onCustomBackground -> scheme.surfaceContainer
                    dark -> lerp(background, scheme.surfaceContainerHigh, 0.45f)
                    else -> lerp(background, scheme.surfaceContainer, 0.9f)
                },
                groupedInset = when {
                    onCustomBackground ->
                        scheme.surfaceContainer.blendTo(scheme.surfaceContainerHigh, 0.45f)
                    dark -> lerp(background, scheme.surfaceContainerHighest, 0.6f)
                    else -> lerp(background, scheme.surfaceContainerHigh, 0.9f)
                },
                separator = scheme.outlineVariant,
                // The app already means "fine" by the accent and "failed" by error.
                positive = scheme.primary,
                caution = scheme.tertiary,
                critical = scheme.error,
                neutral = scheme.onSurfaceVariant,
                onCustomBackground = onCustomBackground,
                chromeControlAlpha = if (onCustomBackground) {
                    CHROME_ALPHA_FLOOR + (1f - CHROME_ALPHA_FLOOR) * wallpaperOpacity
                } else {
                    1f
                },
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

/** Blend towards [target] keeping this colour's alpha: a fill inside a translucent container must
 *  not turn opaque. */
private fun Color.blendTo(target: Color, fraction: Float): Color =
    lerp(this, target.copy(alpha = alpha), fraction)
