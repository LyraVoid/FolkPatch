package me.bmax.apatch.ui.component.folk

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.ui.theme.tokens.WallpaperMaterial

enum class FolkChromeEdge { Top, Bottom, Start }

private const val CollapsedChromeFloor = 0.85f

@Composable
fun FolkWallpaperChromeZone(
    edge: FolkChromeEdge,
    modifier: Modifier = Modifier,
    progress: () -> Float = { 0f },
    tailHeight: Dp = FolkDimens.Size48,
    material: WallpaperMaterial? = null,
    content: @Composable () -> Unit,
) {
    val tokens = LocalFolkWallpaperTokens.current
    if (tokens == null) {
        content()
        return
    }

    val chrome = material ?: tokens.chrome
    val tailPx = with(LocalDensity.current) { tailHeight.toPx() }
    val layoutDirection = LocalLayoutDirection.current
    Box(
        modifier = modifier.drawBehind {
            val collapsed = progress().coerceIn(0f, 1f)
            val floor = if (edge == FolkChromeEdge.Top) CollapsedChromeFloor else chrome.alpha
            val alpha = chrome.alpha + (floor - chrome.alpha).coerceAtLeast(0f) * collapsed
            val solid = chrome.tint.copy(alpha = alpha.coerceAtMost(1f))
            val clear = solid.copy(alpha = 0f)
            when (edge) {
                FolkChromeEdge.Top -> {
                    drawRect(solid, topLeft = Offset.Zero, size = Size(size.width, size.height))
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(solid, clear),
                            startY = size.height,
                            endY = size.height + tailPx,
                        ),
                        topLeft = Offset(0f, size.height),
                        size = Size(size.width, tailPx),
                    )
                }

                FolkChromeEdge.Bottom -> {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(clear, solid),
                            startY = -tailPx,
                            endY = 0f,
                        ),
                        topLeft = Offset(0f, -tailPx),
                        size = Size(size.width, tailPx),
                    )
                    drawRect(solid, topLeft = Offset.Zero, size = Size(size.width, size.height))
                }

                FolkChromeEdge.Start -> {
                    val rtl = layoutDirection == LayoutDirection.Rtl
                    drawRect(solid, topLeft = Offset.Zero, size = Size(size.width, size.height))
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = if (rtl) listOf(clear, solid) else listOf(solid, clear),
                            startX = if (rtl) -tailPx else size.width,
                            endX = if (rtl) 0f else size.width + tailPx,
                        ),
                        topLeft = Offset(if (rtl) -tailPx else size.width, 0f),
                        size = Size(tailPx, size.height),
                    )
                }
            }
        },
        content = { content() },
    )
}
