package me.bmax.apatch.ui.theme.tokens

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntSize

/**
 * Motion tokens, so a press, an expand and a fade feel the same wherever they happen. Only the
 * duration and the spring constants are decided here; the easing stays Compose's default.
 */
object FolkMotion {
    /** How far a pressed surface shrinks. */
    const val PressedScale = 0.97f

    /** The spring a pressed surface returns with: a small overshoot that reads as a response. */
    val PressScale: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 700f)

    /** The squeeze into the pressed state. Short enough that a quick tap still registers
     *  before the action it triggers takes over the screen. */
    val PressDown: FiniteAnimationSpec<Float> = tween(durationMillis = 80)

    /** Expanding and collapsing: settles without overshooting, so a growing group does not bounce
     *  past its final height. */
    fun <T> smoothSpring(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 250f)

    /** Jumping a list back to its end after new content arrives: quick enough that the list reads
     *  as already settled rather than scrolling. */
    val ScrollIntoView: FiniteAnimationSpec<Float> = tween(durationMillis = 80)

    /** Fading something in or out. */
    val FadeInOut: FiniteAnimationSpec<Float> = tween(durationMillis = 200)

    /** Turning an arrow around to point the other way. */
    val IconRotation: FiniteAnimationSpec<Float> = tween(durationMillis = 300)

    /** A control taking up or giving back room in a row. */
    val SlotSize: FiniteAnimationSpec<IntSize> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 900f,
        visibilityThreshold = IntSize.VisibilityThreshold,
    )

    /** Fading a control while its slot resizes. */
    val SlotFade: FiniteAnimationSpec<Float> = tween(durationMillis = 140)

    /** The pop of a control appearing in or leaving a slot. */
    val SlotScale: FiniteAnimationSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 900f)
}
