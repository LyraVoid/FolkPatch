package me.bmax.apatch.ui.theme.tokens

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolkPaletteTest {

    private fun slots(palette: FolkPalette) = listOf(
        palette.groupedBackground,
        palette.groupedSurface,
        palette.groupedInset,
        palette.separator,
        palette.positive,
        palette.caution,
        palette.critical,
        palette.neutral,
    )

    @Test
    fun semanticRolesMapToTheScheme() {
        val scheme = lightColorScheme()
        val palette = FolkPalette.from(scheme)

        assertEquals(scheme.surfaceContainer, palette.groupedBackground)
        assertEquals(scheme.surfaceBright, palette.groupedSurface)
        assertEquals(scheme.surfaceContainerHigh, palette.groupedInset)
        assertEquals(scheme.primary, palette.positive)
        assertEquals(scheme.tertiary, palette.caution)
        assertEquals(scheme.error, palette.critical)
        assertEquals(scheme.onSurfaceVariant, palette.neutral)
        assertEquals(scheme.outlineVariant, palette.separator)
    }

    @Test
    fun surfaceRolesRaiseThePageWithoutALuminanceBranch() {
        listOf(lightColorScheme(), darkColorScheme()).forEach { scheme ->
            val palette = FolkPalette.from(scheme)

            assertTrue(palette.groupedSurface.luminance() > palette.groupedBackground.luminance())
            assertTrue(palette.groupedSurface.luminance() > palette.groupedInset.luminance())
        }
    }

    @Test
    fun translucentPageUsesTheSchemeContainerDirectly() {
        val base = lightColorScheme()
        val scheme = base.copy(background = base.background.copy(alpha = 0.5f))
        val palette = FolkPalette.from(scheme)

        // With a custom background image the page is translucent, so the container can no longer be
        // derived from it; the scheme's own container is used instead.
        assertEquals(Color.Transparent, palette.groupedBackground)
        assertEquals(scheme.surfaceBright, palette.groupedSurface)
        assertEquals(scheme.surfaceContainerHigh, palette.groupedInset)
        assertTrue(palette.onCustomBackground)
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

    @Test
    fun eachSemanticSlotTracksItsOwnSchemeRole() {
        val base = lightColorScheme()
        val palette = FolkPalette.from(base)

        assertNotEquals(
            palette.groupedBackground,
            FolkPalette.from(base.copy(surfaceContainer = Color.Black)).groupedBackground,
        )
        assertNotEquals(
            palette.groupedSurface,
            FolkPalette.from(base.copy(surfaceBright = Color.Black)).groupedSurface,
        )
        assertNotEquals(
            palette.groupedInset,
            FolkPalette.from(base.copy(surfaceContainerHigh = Color.Black)).groupedInset,
        )
        assertNotEquals(
            palette.separator,
            FolkPalette.from(base.copy(outlineVariant = Color.Black)).separator,
        )
        assertNotEquals(
            palette.positive,
            FolkPalette.from(base.copy(primary = Color.Black)).positive,
        )
        assertNotEquals(
            palette.caution,
            FolkPalette.from(base.copy(tertiary = Color.Black)).caution,
        )
        assertNotEquals(
            palette.critical,
            FolkPalette.from(base.copy(error = Color.Black)).critical,
        )
        assertNotEquals(
            palette.neutral,
            FolkPalette.from(base.copy(onSurfaceVariant = Color.Black)).neutral,
        )
        assertNotEquals(
            palette.onCustomBackground,
            FolkPalette.from(base.copy(background = Color.Black.copy(alpha = 0.5f)))
                .onCustomBackground,
        )
    }

    @Test
    fun changingOneRoleOnlyMovesItsMappedSlot() {
        val base = lightColorScheme()
        val before = slots(FolkPalette.from(base))
        val mutations = listOf(
            base.copy(surfaceContainer = Color.Black) to 0,
            base.copy(surfaceBright = Color.Black) to 1,
            base.copy(surfaceContainerHigh = Color.Black) to 2,
            base.copy(outlineVariant = Color.Black) to 3,
            base.copy(primary = Color.Black) to 4,
            base.copy(tertiary = Color.Black) to 5,
            base.copy(error = Color.Black) to 6,
            base.copy(onSurfaceVariant = Color.Black) to 7,
        )

        mutations.forEach { (scheme, changedIndex) ->
            val after = slots(FolkPalette.from(scheme))
            after.indices.forEach { index ->
                if (index == changedIndex) {
                    assertNotEquals("slot $index should change", before[index], after[index])
                } else {
                    assertEquals("slot $index should not change", before[index], after[index])
                }
            }
        }
        assertEquals(before, slots(FolkPalette.from(base.copy(background = Color.Black))))
    }

    @Test
    fun amoledKeepsTheGroupAboveItsBlackPage() {
        val light = FolkPalette.from(
            lightColorScheme(
                surfaceContainer = Color(0xFFF3EDF7),
                surfaceBright = Color(0xFFFFFFFF),
                surfaceContainerHigh = Color(0xFFECE6F0),
            ),
        )
        val dark = FolkPalette.from(
            darkColorScheme(
                surfaceContainer = Color(0xFF0A0A0A),
                surfaceBright = Color(0xFF1F1F1F),
                surfaceContainerHigh = Color(0xFF121212),
            ),
        )
        val amoled = FolkPalette.from(
            darkColorScheme(
                surfaceContainer = Color.Black,
                surfaceBright = Color(0xFF1F1F1F),
                surfaceContainerHigh = Color(0xFF121212),
            ),
        )

        listOf(light, dark, amoled).forEach { palette ->
            assertTrue(palette.groupedSurface.luminance() > palette.groupedBackground.luminance())
        }
        assertTrue(amoled.groupedBackground.luminance() < 0.001f)
        assertTrue(amoled.groupedSurface.luminance() > 0.01f)
    }
}
