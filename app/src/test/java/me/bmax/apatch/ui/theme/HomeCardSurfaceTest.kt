package me.bmax.apatch.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.bmax.apatch.ui.theme.tokens.FolkPalette
import me.bmax.apatch.ui.theme.tokens.resolveFolkWallpaperTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCardSurfaceTest {
    @Test
    fun groupedCardsRemainDistinctAndReadable() {
        for (mode in listOf("light", "dark", "amoled")) {
            val generated = ColorSchemeGenerator.generate(
                Color(0xFF4355B9), mode != "light", PaletteStyle.TonalSpot, ColorSpec.SpecVersion.SPEC_2025,
            )
            val scheme = if (mode == "amoled") generated.toAmoled() else generated
            val palette = FolkPalette.from(scheme)
            val foreground = scheme.onSurface.luminance()
            val background = palette.groupedSurface.luminance()
            val contrast = (maxOf(foreground, background) + 0.05f) / (minOf(foreground, background) + 0.05f)

            assertTrue(mode, palette.groupedSurface != palette.groupedBackground)
            assertTrue(mode, contrast >= 4.5f)
        }
    }

    @Test
    fun wallpaperGroupUsesItsOwnFillAndContent() {
        val scheme = ColorSchemeGenerator.generate(
            Color(0xFF4355B9), true, PaletteStyle.TonalSpot, ColorSpec.SpecVersion.SPEC_2025,
        )
        val group = resolveFolkWallpaperTokens(scheme, 0.7f).group

        assertEquals(0.7f, group.fill.alpha, 0.005f)
        assertEquals(scheme.surface, group.fill.copy(alpha = 1f))
        assertEquals(scheme.onSurface, group.contentColor)
    }
}
