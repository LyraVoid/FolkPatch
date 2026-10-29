package me.bmax.apatch.ui.theme.tokens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FolkMotionTest {

    @Test
    fun pressDepthIsPinned() {
        assertEquals(0.97f, FolkMotion.PressedScale, 0.0001f)
    }

    @Test
    fun animationSpecsAreConfigured() {
        assertNotNull(FolkMotion.PressScale)
        assertNotNull(FolkMotion.PressDown)
        assertNotNull(FolkMotion.ScrollIntoView)
        assertNotNull(FolkMotion.FadeInOut)
        assertNotNull(FolkMotion.IconRotation)
        assertNotNull(FolkMotion.smoothSpring<Float>())
    }

    @Test
    fun springsHaveTheirPinnedValues() {
        val press = FolkMotion.PressScale as SpringSpec<Float>
        val smooth = FolkMotion.smoothSpring<Float>() as SpringSpec<Float>
        val slotSize = FolkMotion.SlotSize as SpringSpec<IntSize>
        val slotScale = FolkMotion.SlotScale as SpringSpec<Float>

        assertEquals(0.7f, press.dampingRatio, 0.0001f)
        assertEquals(700f, press.stiffness, 0.0001f)
        assertEquals(Spring.DampingRatioNoBouncy, smooth.dampingRatio, 0.0001f)
        assertEquals(250f, smooth.stiffness, 0.0001f)
        assertEquals(Spring.DampingRatioNoBouncy, slotSize.dampingRatio, 0.0001f)
        assertEquals(900f, slotSize.stiffness, 0.0001f)
        assertEquals(IntSize.VisibilityThreshold, slotSize.visibilityThreshold)
        assertEquals(0.8f, slotScale.dampingRatio, 0.0001f)
        assertEquals(900f, slotScale.stiffness, 0.0001f)
    }

    @Test
    fun tweensHaveTheirPinnedDurations() {
        assertEquals(80, (FolkMotion.PressDown as TweenSpec<Float>).durationMillis)
        assertEquals(80, (FolkMotion.ScrollIntoView as TweenSpec<Float>).durationMillis)
        assertEquals(200, (FolkMotion.FadeInOut as TweenSpec<Float>).durationMillis)
        assertEquals(300, (FolkMotion.IconRotation as TweenSpec<Float>).durationMillis)
        assertEquals(140, (FolkMotion.SlotFade as TweenSpec<Float>).durationMillis)
    }
}
