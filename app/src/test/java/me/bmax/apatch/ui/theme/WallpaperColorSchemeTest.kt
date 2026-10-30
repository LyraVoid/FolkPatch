package me.bmax.apatch.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.bmax.apatch.ui.component.themeColorOptions
import me.bmax.apatch.ui.theme.tokens.FolkThemeCatalog
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

    @Test
    fun amoledBlackPageDoesNotEraseRaisedContainers() {
        val amoled = lightColorScheme(surface = Color(0xFF1A1C1E)).toAmoled()

        assertEquals(Color.Black, amoled.background)
        assertEquals(Color.Black, amoled.surface)
        assertTrue(amoled.surfaceContainerHigh.luminance() > amoled.surfaceContainer.luminance())
        assertTrue(amoled.surfaceBright.luminance() > amoled.surfaceContainer.luminance())
    }

    @Test
    fun contrastGuardMeasuresTheRenderedMaterialFill() {
        val guarded = guardedDim(
            requested = 0f,
            scheme = darkColorScheme(),
            opacity = 0f,
        )

        assertTrue(guarded != null && guarded > 0f)
    }

    @Test
    fun semanticErrorMatchesTheMaterialBaseline() {
        val light = lightColorScheme(primary = Color(0xFF123456)).withSemanticError(false)
        val dark = darkColorScheme(primary = Color(0xFF123456)).withSemanticError(true)

        assertEquals(lightColorScheme().errorContainer, light.errorContainer)
        assertEquals(lightColorScheme().onErrorContainer, light.onErrorContainer)
        assertEquals(darkColorScheme().errorContainer, dark.errorContainer)
        assertEquals(darkColorScheme().onErrorContainer, dark.onErrorContainer)
    }

    @Test
    fun generationDefaultsMatchFinaSU() {
        assertEquals(ColorGenerationMode.CUSTOM, ColorGenerationMode.fromKey(null))
        assertEquals(ColorStandard.M3E_2025, ColorStandard.fromName(null))
    }

    @Test
    fun catalogSeedsFollowClassicPrimaryForBothModes() {
        for (option in themeColorOptions) {
            for (dark in listOf(false, true)) {
                assertEquals(FolkThemeCatalog.scheme(option.key, dark).primary, catalogSeed(option.key, dark))
            }
        }
        assertEquals(Color(0xFF44D8F1), catalogSeed("cyan", true))
        assertEquals(catalogSeed("indigo", true), catalogSeed("invalid", true))
    }

    @Test
    fun generatedStandardSchemesKeepAccessibleTextAndControls() {
        for (mode in listOf("light", "dark", "amoled")) {
            val dark = mode != "light"
            val generated = ColorSchemeGenerator.generate(
                Color(0xFF4355B9), dark, PaletteStyle.TonalSpot, ColorSpec.SpecVersion.SPEC_2025,
            ).withSemanticError(dark)
            val scheme = if (mode == "amoled") generated.toAmoled() else generated
            fun ratio(a: Color, b: Color): Float {
                val x = a.luminance()
                val y = b.luminance()
                return (maxOf(x, y) + 0.05f) / (minOf(x, y) + 0.05f)
            }
            val pairs = mapOf(
                "onBackground/background" to (scheme.onBackground to scheme.background),
                "onSurface/surface" to (scheme.onSurface to scheme.surface),
                "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
                "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
                "onSecondaryContainer/secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
                "onTertiaryContainer/tertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
                "onErrorContainer/errorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
            )
            pairs.forEach { (name, colors) -> assertTrue("$mode $name", ratio(colors.first, colors.second) >= 4.5f) }
            assertTrue("$mode outline/surface", ratio(scheme.outline, scheme.surface) >= 3f)
        }
    }
}
