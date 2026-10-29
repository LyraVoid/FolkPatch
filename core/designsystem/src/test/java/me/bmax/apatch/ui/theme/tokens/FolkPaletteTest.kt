package me.bmax.apatch.ui.theme.tokens

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FolkPaletteTest {

    @Test
    fun semanticRolesMapToTheScheme() {
        val scheme = lightColorScheme()
        val palette = FolkPalette.from(scheme)

        assertEquals(scheme.background, palette.groupedBackground)
        assertEquals(scheme.primary, palette.positive)
        assertEquals(scheme.tertiary, palette.caution)
        assertEquals(scheme.error, palette.critical)
        assertEquals(scheme.onSurfaceVariant, palette.neutral)
        assertEquals(scheme.outlineVariant, palette.separator)
    }

    @Test
    fun lightThemeSeparatesSurfaceAndInsetFromThePage() {
        val scheme = lightColorScheme()
        val palette = FolkPalette.from(scheme)

        assertNotEquals(scheme.background, palette.groupedSurface)
        assertNotEquals(palette.groupedSurface, palette.groupedInset)
    }

    @Test
    fun darkThemeSeparatesSurfaceAndInsetFromThePage() {
        val scheme = darkColorScheme()
        val palette = FolkPalette.from(scheme)

        assertNotEquals(scheme.background, palette.groupedSurface)
        assertNotEquals(palette.groupedSurface, palette.groupedInset)
    }

    @Test
    fun translucentPageUsesTheSchemeContainerDirectly() {
        val base = lightColorScheme()
        val scheme = base.copy(background = base.background.copy(alpha = 0.5f))
        val palette = FolkPalette.from(scheme)

        // With a custom background image the page is translucent, so the container can no longer be
        // derived from it; the scheme's own container is used instead.
        assertEquals(scheme.surfaceContainer, palette.groupedSurface)
        assertNotEquals(scheme.surfaceContainer, palette.groupedInset)
    }

    @Test
    fun wallpaperChromeControlsTrackBackgroundOpacity() {
        val base = lightColorScheme()
        val scheme = base.copy(
            background = base.background.copy(alpha = 0f),
            surface = base.surface.copy(alpha = 0.5f),
        )
        val palette = FolkPalette.from(scheme)
        val expected = 0.1f + 0.9f * scheme.surface.alpha

        assertEquals(expected, palette.chromeControlAlpha, 0.001f)
        assertEquals(
            palette.chromeControlAlpha,
            palette.chromeControlFill(base.surfaceContainerHighest).alpha,
            0.002f,
        )
    }

    @Test
    fun regularChromeControlsStayOpaque() {
        val scheme = lightColorScheme()
        val palette = FolkPalette.from(scheme)

        assertEquals(1f, palette.chromeControlAlpha, 0f)
        assertEquals(
            1f,
            palette.chromeControlFill(scheme.surfaceContainerHighest).alpha,
            0f,
        )
    }
}
