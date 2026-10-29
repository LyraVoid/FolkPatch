package me.bmax.apatch.ui.component.folk

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import me.bmax.apatch.ui.theme.tokens.FolkMotion

/**
 * Scale on press instead of a ripple. Pair it with `indication = null` on the
 * clickable and the same [MutableInteractionSource]; a press must still feel
 * like something, so callers should also fire a haptic.
 */
@Composable
fun Modifier.folkPressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) FolkMotion.PressedScale else 1f,
        // Squeeze in fast so even a quick tap reads; release uses the spring.
        animationSpec = if (pressed && enabled) FolkMotion.PressDown else FolkMotion.PressScale,
        label = "folkPressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
