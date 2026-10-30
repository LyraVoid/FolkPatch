package me.bmax.apatch.ui.component.folk

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Immutable
data class NavOrigin(
    val rect: Rect,
    val cornerRadius: Float,
    val color: Color,
    val themeSignature: Int,
    val bitmap: ImageBitmap? = null,
)

class NavOriginState(
    private val now: () -> Long = SystemClock::uptimeMillis,
) {
    private var pendingOrigin: NavOrigin? = null
    private var pendingAt = 0L
    private val origins = mutableStateMapOf<Any, NavOrigin>()

    fun hasPendingOrigin(): Boolean =
        pendingOrigin != null && now() - pendingAt <= OriginTimeoutMs

    fun commitPending(key: Any): Boolean {
        if (!hasPendingOrigin()) {
            pendingOrigin = null
            return false
        }
        val origin = pendingOrigin ?: return false
        pendingOrigin = null
        origins[key] = origin
        while (origins.size > MaxCachedOrigins) {
            val oldest = origins.keys.firstOrNull { it != key } ?: break
            origins.remove(oldest)
        }
        return true
    }

    fun origin(key: Any?): NavOrigin? = key?.let(origins::get)

    fun hasOrigin(key: Any?): Boolean = key != null && origins.containsKey(key)

    fun removeOrigin(key: Any?) {
        if (key != null) origins.remove(key)
    }

    fun recordOrigin(origin: NavOrigin) {
        pendingOrigin = origin
        pendingAt = now()
    }

    fun clearPending() {
        pendingOrigin = null
    }
}

val LocalNavOriginState = staticCompositionLocalOf { NavOriginState() }

@Composable
fun rememberNavOriginState(): NavOriginState = remember { NavOriginState() }

fun Modifier.trackNavPointer(state: NavOriginState): Modifier = pointerInput(state) {
    awaitPointerEventScope {
        while (true) {
            if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) {
                state.clearPending()
            }
        }
    }
}

@Composable
fun Modifier.navOrigin(
    cornerRadius: Dp = FolkDimens.Size20,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    captureBitmap: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val state = LocalNavOriginState.current
    val density = LocalDensity.current
    val view = LocalView.current
    val colorScheme = MaterialTheme.colorScheme
    val originColor by rememberUpdatedState(color)
    val originRadius by rememberUpdatedState(with(density) { cornerRadius.toPx() })
    val originSignature by rememberUpdatedState(navThemeSignature(colorScheme))
    val shouldCapture by rememberUpdatedState(captureBitmap)
    val geometry = remember { NavOriginGeometry() }

    fun record() {
        val rect = geometry.root
        if (rect == Rect.Zero) return
        state.recordOrigin(
            NavOrigin(
                rect = rect,
                cornerRadius = originRadius,
                color = originColor,
                themeSignature = originSignature,
                bitmap = if (shouldCapture) captureOriginBitmap(view, rect) else null,
            )
        )
    }

    LaunchedEffect(interactionSource) {
        interactionSource?.interactions?.collect { interaction ->
            if (interaction is PressInteraction.Press) record()
        }
    }

    return onGloballyPositioned { geometry.root = it.boundsInRoot() }
        .then(
            if (interactionSource == null) {
                Modifier.pointerInput(state, view) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            if (event.type == PointerEventType.Press) record()
                        }
                    }
                }
            } else {
                Modifier
            }
        )
}

private class NavOriginGeometry {
    var root = Rect.Zero
}

private fun captureOriginBitmap(view: android.view.View, rect: Rect): ImageBitmap? {
    if (!rect.isFinite || rect.width <= 0f || rect.height <= 0f) return null
    val edgeScale = MaxCaptureEdgePx / maxOf(rect.width, rect.height)
    val areaScale = sqrt(MaxCaptureAreaPx / (rect.width * rect.height))
    val scale = min(1f, min(edgeScale, areaScale))
    val width = (rect.width * scale).roundToInt().coerceAtLeast(1)
    val height = (rect.height * scale).roundToInt().coerceAtLeast(1)
    val bitmap = try {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    } catch (_: OutOfMemoryError) {
        return null
    } catch (_: RuntimeException) {
        return null
    }

    return try {
        Canvas(bitmap).apply {
            translate(-rect.left * scale, -rect.top * scale)
            scale(scale, scale)
        }.also(view::draw)
        bitmap.asImageBitmap()
    } catch (_: OutOfMemoryError) {
        bitmap.recycle()
        null
    } catch (_: RuntimeException) {
        bitmap.recycle()
        null
    }
}

fun navThemeSignature(colorScheme: ColorScheme): Int {
    var signature = 1
    signature = 31 * signature + colorScheme.background.hashCode()
    signature = 31 * signature + colorScheme.surface.hashCode()
    signature = 31 * signature + colorScheme.surfaceContainer.hashCode()
    signature = 31 * signature + colorScheme.surfaceBright.hashCode()
    signature = 31 * signature + colorScheme.surfaceDim.hashCode()
    return signature
}

private const val OriginTimeoutMs = 1_200L
private const val MaxCachedOrigins = 4
private const val MaxCaptureEdgePx = 1_280f
private const val MaxCaptureAreaPx = 1_200_000f
