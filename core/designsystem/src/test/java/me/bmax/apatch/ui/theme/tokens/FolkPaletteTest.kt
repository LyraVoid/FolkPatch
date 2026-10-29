package me.bmax.apatch.ui.theme.tokens

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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
        assertTrue(palette.onCustomBackground)
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

        assertFalse(palette.onCustomBackground)
        assertEquals(1f, palette.chromeControlAlpha, 0f)
        assertEquals(
            1f,
            palette.chromeControlFill(scheme.surfaceContainerHighest).alpha,
            0f,
        )
    }

    @Test
    fun everyDerivedColorTracksTheActiveScheme() {
        val light = FolkPalette.from(
            lightColorScheme(
                primary = Color.Red,
                tertiary = Color.Green,
                error = Color.Blue,
                background = Color.White,
                onSurfaceVariant = Color.DarkGray,
                outlineVariant = Color.LightGray,
            ),
        )
        val dark = FolkPalette.from(
            darkColorScheme(
                primary = Color.Yellow,
                tertiary = Color.Magenta,
                error = Color.Cyan,
                background = Color.Black,
                onSurfaceVariant = Color.LightGray,
                outlineVariant = Color.DarkGray,
            ),
        )

        assertNotEquals(light.groupedBackground, dark.groupedBackground)
        assertNotEquals(light.groupedSurface, dark.groupedSurface)
        assertNotEquals(light.groupedInset, dark.groupedInset)
        assertNotEquals(light.separator, dark.separator)
        assertNotEquals(light.positive, dark.positive)
        assertNotEquals(light.caution, dark.caution)
        assertNotEquals(light.critical, dark.critical)
        assertNotEquals(light.neutral, dark.neutral)
    }
}
