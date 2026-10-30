package me.bmax.apatch.ui.theme.tokens

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test

class FolkWallpaperTokensTest {

    private val scheme = lightColorScheme(
        surface = Color(0xFF112233),
        surfaceContainerHigh = Color(0xFF223344),
        secondaryContainer = Color(0xFF334455),
        onSurface = Color(0xFFF5F5F5),
    )

    @Test
    fun eachMaterialUsesItsSemanticSource() {
        val tokens = resolveFolkWallpaperTokens(scheme, opacity = 0.5f)

        assertEquals(scheme.surface, tokens.group.tint)
        assertEquals(scheme.surface, tokens.raised.tint)
        assertEquals(scheme.surfaceContainerHigh, tokens.overlay.tint)
        assertEquals(scheme.secondaryContainer, tokens.selection.tint)
    }

    @Test
    fun opacityChangesOnlyTheResolvedAlpha() {
        val clear = resolveFolkWallpaperTokens(scheme, opacity = 0f)
        val half = resolveFolkWallpaperTokens(scheme, opacity = 0.5f)
        val solid = resolveFolkWallpaperTokens(scheme, opacity = 1f)

        assertEquals(clear.group.tint, solid.group.tint)
        assertEquals(clear.overlay.tint, solid.overlay.tint)
        assertEquals(clear.selection.tint, solid.selection.tint)
        assertEquals(0f, clear.group.alpha, 0.005f)
        assertEquals(0.5f, half.group.alpha, 0.005f)
        assertEquals(1f, solid.group.alpha, 0.005f)
        assertEquals(0.62f, half.raised.alpha, 0.005f)
        assertEquals(0.96f, half.overlay.alpha, 0.005f)
    }

    @Test
    fun onlyTheMaterialResolverAppliesFillAlpha() {
        val tokens = resolveFolkWallpaperTokens(scheme, opacity = 0.4f)

        listOf(tokens.group, tokens.raised, tokens.overlay, tokens.chrome, tokens.selection)
            .forEach { material ->
                assertEquals(material.alpha, material.fill.alpha, 0.005f)
                assertEquals(material.tint.toArgb(), material.fill.copy(alpha = 1f).toArgb())
            }
    }

    @Test
    fun selectionKeepsAClearFloorAndChromeKeepsItsPlatform() {
        val tokens = resolveFolkWallpaperTokens(scheme, opacity = 0f)

        assertEquals(0.45f, tokens.selection.alpha, 0.005f)
        assertEquals(0.1f, tokens.chrome.alpha, 0.005f)
    }

    @Test
    fun raisedOverChromeCompositsBackToRaised() {
        val tokens = resolveFolkWallpaperTokens(scheme, opacity = 0.5f)
        val extra = tokens.raisedOverChrome()

        assertEquals(
            tokens.raised.alpha,
            extra.alpha + tokens.chrome.alpha * (1f - extra.alpha),
            0.005f,
        )
    }
}
