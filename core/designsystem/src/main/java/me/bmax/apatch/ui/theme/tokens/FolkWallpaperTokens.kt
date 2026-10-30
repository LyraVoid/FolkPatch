package me.bmax.apatch.ui.theme.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class WallpaperSurfaceRole { Group, Raised, Overlay }

@Immutable
data class WallpaperMaterial(
    val tint: Color,
    val alpha: Float,
    val contentColor: Color,
    val supportingColor: Color,
) {
    val fill: Color get() = tint.copy(alpha = alpha)
}

@Immutable
data class FolkWallpaperTokens(
    val group: WallpaperMaterial,
    val raised: WallpaperMaterial,
    val overlay: WallpaperMaterial,
    val chrome: WallpaperMaterial,
    val selection: WallpaperMaterial,
) {
    fun material(role: WallpaperSurfaceRole, minimumAlpha: Float = 0f): WallpaperMaterial {
        val material = when (role) {
            WallpaperSurfaceRole.Group -> group
            WallpaperSurfaceRole.Raised -> raised
            WallpaperSurfaceRole.Overlay -> overlay
        }
        return if (material.alpha >= minimumAlpha) material else {
            material.copy(alpha = minimumAlpha.coerceIn(0f, 1f))
        }
    }

    fun raisedOverChrome(): Color {
        val under = chrome.alpha
        val alpha = if (under >= 1f) 0f else (raised.alpha - under) / (1f - under)
        return raised.tint.copy(alpha = alpha.coerceIn(0f, 1f))
    }

    fun chromeControlFill(tint: Color): Color = tint.copy(alpha = chrome.alpha)
}

val LocalFolkWallpaperTokens = compositionLocalOf<FolkWallpaperTokens?> { null }

private const val CHROME_ALPHA_FLOOR = 0.1f
private const val SELECTION_ALPHA_FLOOR = 0.45f
private const val SUPPORTING_CONTENT_ALPHA = 0.8f

fun resolveFolkWallpaperTokens(
    colorScheme: ColorScheme,
    opacity: Float,
): FolkWallpaperTokens {
    val alpha = opacity.coerceIn(0f, 1f)
    val panel = WallpaperMaterial(
        tint = colorScheme.surface,
        alpha = alpha,
        contentColor = colorScheme.onSurface,
        supportingColor = colorScheme.onSurface.copy(alpha = SUPPORTING_CONTENT_ALPHA),
    )
    return FolkWallpaperTokens(
        group = panel,
        raised = panel.copy(alpha = (alpha + 0.12f).coerceAtMost(1f)),
        overlay = WallpaperMaterial(
            tint = colorScheme.surfaceContainerHigh,
            alpha = 0.96f,
            contentColor = colorScheme.onSurface,
            supportingColor = colorScheme.onSurfaceVariant,
        ),
        chrome = panel.copy(
            alpha = CHROME_ALPHA_FLOOR + (1f - CHROME_ALPHA_FLOOR) * alpha,
        ),
        selection = WallpaperMaterial(
            tint = colorScheme.secondaryContainer,
            alpha = SELECTION_ALPHA_FLOOR + (1f - SELECTION_ALPHA_FLOOR) * alpha,
            contentColor = colorScheme.onSurface,
            supportingColor = colorScheme.onSurface.copy(alpha = SUPPORTING_CONTENT_ALPHA),
        ),
    )
}
