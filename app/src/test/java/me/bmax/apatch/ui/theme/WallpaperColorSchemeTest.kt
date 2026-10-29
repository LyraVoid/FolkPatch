package me.bmax.apatch.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperColorSchemeTest {

    @Test
    fun wallpaperSourceSurfacesStayOpaque() {
        val base = lightColorScheme(surface = Color(0xFF112233))
        val neutral = darkColorScheme(
            surface = Color(0xFF223344),
            surfaceDim = Color(0xFF111111),
            surfaceBright = Color(0xFF333333),
            surfaceContainerLow = Color(0xFF222222),
            surfaceContainer = Color(0xFF242424),
            surfaceContainerHigh = Color(0xFF282828),
            surfaceContainerHighest = Color(0xFF2C2C2C),
        )
        val adapted = adaptWallpaperColorScheme(base, neutral, active = true)

        listOf(
            adapted.surface,
            adapted.surfaceDim,
            adapted.surfaceBright,
            adapted.surfaceContainerLowest,
            adapted.surfaceContainerLow,
            adapted.surfaceContainer,
            adapted.surfaceContainerHigh,
            adapted.surfaceContainerHighest,
            adapted.surfaceVariant,
        ).forEach { color -> assertEquals(1f, color.alpha, 0f) }
        assertEquals(Color.Transparent, adapted.background)
        assertEquals(neutral.surface, adapted.surface)
        assertEquals(base.secondaryContainer, adapted.secondaryContainer)
    }

    @Test
    fun opacityCannotChangeWallpaperPolarity() {
        assertFalse(wallpaperUsesDarkNeutral(darkTheme = false, luminance = 1f))
        assertFalse(wallpaperUsesDarkNeutral(darkTheme = false, luminance = 0.5f))
        assertTrue(wallpaperUsesDarkNeutral(darkTheme = false, luminance = 0.49f))
        assertTrue(wallpaperUsesDarkNeutral(darkTheme = true, luminance = 1f))
    }
}
