package me.bmax.apatch.ui.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import com.ramcosta.composedestinations.scope.AnimatedDestinationScope
import com.ramcosta.composedestinations.scope.DestinationScope
import com.ramcosta.composedestinations.wrapper.DestinationWrapper
import me.bmax.apatch.ui.component.folk.LocalNavOriginState
import me.bmax.apatch.ui.component.folk.NavOrigin
import me.bmax.apatch.ui.component.folk.navThemeSignature
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import kotlin.math.roundToInt

object NavContainerTransformWrapper : DestinationWrapper {
    @Composable
    override fun <T> DestinationScope<T>.Wrap(screenContent: @Composable () -> Unit) {
        NavContainerTransformEntry(this, screenContent)
    }
}

@Composable
private fun <T> NavContainerTransformEntry(
    scope: DestinationScope<T>,
    content: @Composable () -> Unit,
) {
    val state = LocalNavOriginState.current
    val entryId = scope.navBackStackEntry.id
    val wallpaperActive = LocalFolkWallpaperTokens.current != null
    val transition = (scope as? AnimatedDestinationScope<*>)?.transition

    // Claim the pressed card on the first composition, before the transition spec runs,
    // so the first frame is already the container instead of a bare full page.
    remember(entryId) {
        if (!state.hasOrigin(entryId) && state.hasPendingOrigin()) {
            state.commitPending(entryId)
        }
    }
    val origin = state.origin(entryId)

    DisposableEffect(entryId) {
        onDispose {
            if (scope.navController.currentBackStack.value.none { it.id == entryId }) {
                state.removeOrigin(entryId)
            }
        }
    }

    if (origin == null || wallpaperActive || transition == null) {
        content()
    } else {
        val progress = transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = if (targetState == EnterExitState.PostExit) PopDurationMs else ContainerTransformDurationMs,
                    easing = FastOutSlowInEasing,
                )
            },
            label = "container transform",
        ) { containerFraction(it) }
        ContainerTransformLayout(
            origin = origin,
            progress = progress,
            popping = transition.targetState == EnterExitState.PostExit,
            content = content,
        )
    }
}

internal fun shouldUseContainerTransform(wallpaperActive: Boolean, hasOrigin: Boolean): Boolean =
    !wallpaperActive && hasOrigin

internal fun containerFraction(state: EnterExitState): Float =
    if (state == EnterExitState.Visible) 1f else 0f

@Composable
private fun ContainerTransformLayout(
    origin: NavOrigin,
    progress: State<Float>,
    popping: Boolean,
    content: @Composable () -> Unit,
) {
    var offsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var contentBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val colorScheme = MaterialTheme.colorScheme
    val pageColor = FolkTheme.palette.groupedBackground
    val surfaceColor = if (pageColor.alpha > 0.99f) pageColor else colorScheme.surfaceContainer
    val themeSignature = navThemeSignature(colorScheme)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (popping) 1f else 0f)
            .onGloballyPositioned { coordinates ->
                contentBoundsInRoot = coordinates.boundsInRoot()
                offsetInRoot = coordinates.localToRoot(Offset.Zero)
            }
            .drawWithContent {
                val fraction = progress.value.coerceIn(0f, 1f)
                val localOrigin = origin.rect.translate(-offsetInRoot)
                val localBounds = contentBoundsInRoot
                    .takeIf { it.isFinite && !it.isEmpty }
                    ?.translate(-offsetInRoot)
                    ?: Rect(Offset.Zero, size)
                drawContainerTransform(
                    origin = origin,
                    fraction = fraction,
                    localOrigin = localOrigin,
                    localContentBounds = localBounds,
                    surfaceColor = surfaceColor,
                    themeSignature = themeSignature,
                    popping = popping,
                )
            },
    ) {
        content()
    }
}

private fun ContentDrawScope.drawContainerTransform(
    origin: NavOrigin,
    fraction: Float,
    localOrigin: Rect,
    localContentBounds: Rect,
    surfaceColor: androidx.compose.ui.graphics.Color,
    themeSignature: Int,
    popping: Boolean,
) {
    val rect = interpolateContainerBounds(localOrigin, localContentBounds, fraction)
    if (rect.width <= 0f || rect.height <= 0f) return
    val corner = CornerRadius(interpolateContainerCorner(origin.cornerRadius, fraction))
    val appearance = resolveContainerAppearance(
        originColor = origin.color,
        originThemeSignature = origin.themeSignature,
        currentThemeSignature = themeSignature,
        surfaceColor = surfaceColor,
        popping = popping,
        hasSnapshot = origin.bitmap != null,
        fraction = fraction,
    )
    val path = Path().apply {
        addRoundRect(RoundRect(rect, corner))
    }

    drawRoundRect(
        color = lerp(appearance.fillColor, surfaceColor, fraction),
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = corner,
    )
    clipPath(path) {
        translate(
            left = rect.left - localContentBounds.left,
            top = rect.top - localContentBounds.top,
        ) {
            this@drawContainerTransform.drawContent()
        }
    }

    val veilAlpha = if (popping) {
        popSurfaceVeilAlpha(fraction) * if (appearance.snapshotEnabled) SnapshotWashAlpha else 1f
    } else {
        pushSurfaceVeilAlpha(fraction)
    }
    if (veilAlpha > 0f) {
        drawRoundRect(
            color = lerp(appearance.fillColor, surfaceColor, fraction).copy(alpha = veilAlpha),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = corner,
        )
    }

    val snapshot = origin.bitmap
    if (snapshot != null && appearance.snapshotAlpha > 0f) {
        clipPath(path) {
            drawImage(
                image = snapshot,
                dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
                dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt()),
                alpha = appearance.snapshotAlpha,
                filterQuality = FilterQuality.Low,
            )
        }
    }
}

internal data class ContainerAppearance(
    val fillColor: androidx.compose.ui.graphics.Color,
    val snapshotEnabled: Boolean,
    val snapshotAlpha: Float,
)

internal fun resolveContainerAppearance(
    originColor: androidx.compose.ui.graphics.Color,
    originThemeSignature: Int,
    currentThemeSignature: Int,
    surfaceColor: androidx.compose.ui.graphics.Color,
    popping: Boolean,
    hasSnapshot: Boolean,
    fraction: Float,
): ContainerAppearance {
    val themeMatches = originThemeSignature == currentThemeSignature
    val snapshotEnabled = themeMatches && popping && hasSnapshot
    return ContainerAppearance(
        fillColor = if (themeMatches) originColor else surfaceColor,
        snapshotEnabled = snapshotEnabled,
        snapshotAlpha = if (snapshotEnabled) popSnapshotAlpha(fraction) else 0f,
    )
}

internal fun interpolateContainerBounds(
    origin: Rect,
    destination: Rect,
    fraction: Float,
): Rect {
    val progress = fraction.coerceIn(0f, 1f)
    return Rect(
        left = origin.left + (destination.left - origin.left) * progress,
        top = origin.top + (destination.top - origin.top) * progress,
        right = origin.right + (destination.right - origin.right) * progress,
        bottom = origin.bottom + (destination.bottom - origin.bottom) * progress,
    )
}

internal fun interpolateContainerCorner(cornerRadius: Float, fraction: Float): Float =
    cornerRadius * (1f - fraction.coerceIn(0f, 1f))

internal fun pushSurfaceVeilAlpha(fraction: Float): Float {
    val reveal = ((fraction - VeilRevealStart) / (VeilRevealEnd - VeilRevealStart))
        .coerceIn(0f, 1f)
    return 1f - FastOutSlowInEasing.transform(reveal)
}

internal fun popSurfaceVeilAlpha(fraction: Float): Float {
    val conceal = ((VeilRevealEnd - fraction) / (VeilRevealEnd - VeilRevealStart))
        .coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(conceal)
}

internal fun popSnapshotAlpha(fraction: Float): Float {
    val snapshot = ((SnapshotRevealStart - fraction) / (SnapshotRevealStart - SnapshotRevealEnd))
        .coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(snapshot)
}

private const val ContainerTransformDurationMs = 300
private const val PopDurationMs = 280
private const val VeilRevealStart = 0.08f
private const val VeilRevealEnd = 0.42f
private const val SnapshotRevealStart = 0.42f
private const val SnapshotRevealEnd = 0.14f
private const val SnapshotWashAlpha = 0.18f
