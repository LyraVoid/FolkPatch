package me.bmax.apatch.ui.component.folk

import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.ui.theme.tokens.WallpaperSurfaceRole

val LocalWallpaperMaterialRole = staticCompositionLocalOf<WallpaperSurfaceRole?> { null }

val LocalWallpaperSupportingColor = staticCompositionLocalOf<Color?> { null }

@Composable
fun FolkWallpaperSurface(
    role: WallpaperSurfaceRole,
    shape: Shape,
    modifier: Modifier = Modifier,
    minimumAlpha: Float = 0f,
    fallbackColor: Color = Color.Unspecified,
    fallbackContentColor: Color = Color.Unspecified,
    content: @Composable () -> Unit,
) {
    val tokens = LocalFolkWallpaperTokens.current
    if (tokens == null || LocalWallpaperMaterialRole.current != null) {
        if (tokens == null && fallbackColor != Color.Unspecified) {
            Surface(
                modifier = modifier,
                shape = shape,
                color = fallbackColor,
                contentColor = if (fallbackContentColor != Color.Unspecified) {
                    fallbackContentColor
                } else {
                    contentColorFor(fallbackColor)
                },
                tonalElevation = FolkDimens.Size0,
                shadowElevation = FolkDimens.Size0,
                content = content,
            )
        } else {
            content()
        }
        return
    }

    val material = tokens.material(role, minimumAlpha)
    Surface(
        modifier = modifier,
        shape = shape,
        color = material.fill,
        contentColor = material.contentColor,
        tonalElevation = FolkDimens.Size0,
        shadowElevation = FolkDimens.Size0,
    ) {
        CompositionLocalProvider(
            LocalWallpaperMaterialRole provides role,
            LocalWallpaperSupportingColor provides material.supportingColor,
            content = content,
        )
    }
}
