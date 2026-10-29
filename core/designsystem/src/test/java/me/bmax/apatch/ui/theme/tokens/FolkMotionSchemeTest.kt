package me.bmax.apatch.ui.theme.tokens

import androidx.compose.animation.core.SpringSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class FolkMotionSchemeTest {

    private fun spring(spec: androidx.compose.animation.core.FiniteAnimationSpec<Float>) =
        spec as SpringSpec<Float>

    @Test
    fun spatialSpecsKeepTheirDistinctCurve() {
        assertEquals(0.55f, spring(FolkExpressiveMotionScheme.defaultSpatialSpec()).dampingRatio)
        assertEquals(250f, spring(FolkExpressiveMotionScheme.defaultSpatialSpec()).stiffness)
        assertEquals(0.55f, spring(FolkExpressiveMotionScheme.fastSpatialSpec()).dampingRatio)
        assertEquals(600f, spring(FolkExpressiveMotionScheme.fastSpatialSpec()).stiffness)
        assertEquals(0.6f, spring(FolkExpressiveMotionScheme.slowSpatialSpec()).dampingRatio)
        assertEquals(150f, spring(FolkExpressiveMotionScheme.slowSpatialSpec()).stiffness)
    }

    @Test
    fun effectsSpecsKeepTheirDistinctCurve() {
        assertEquals(0.8f, spring(FolkExpressiveMotionScheme.defaultEffectsSpec()).dampingRatio)
        assertEquals(1600f, spring(FolkExpressiveMotionScheme.defaultEffectsSpec()).stiffness)
        assertEquals(0.6f, spring(FolkExpressiveMotionScheme.fastEffectsSpec()).dampingRatio)
        assertEquals(3800f, spring(FolkExpressiveMotionScheme.fastEffectsSpec()).stiffness)
        assertEquals(0.8f, spring(FolkExpressiveMotionScheme.slowEffectsSpec()).dampingRatio)
        assertEquals(800f, spring(FolkExpressiveMotionScheme.slowEffectsSpec()).stiffness)
    }
}
