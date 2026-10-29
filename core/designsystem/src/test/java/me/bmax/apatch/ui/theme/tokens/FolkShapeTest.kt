package me.bmax.apatch.ui.theme.tokens

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FolkShapeTest {

    private val density = Density(density = 1f, fontScale = 1f)
    private val size = Size(100f, 100f)

    private fun topStartRadius(shape: CornerBasedShape): Float =
        shape.topStart.toPx(size, density)

    @Test
    fun cornerStepsKeepTheirRadius() {
        assertEquals(12f, topStartRadius(FolkShape.Corner12), 0.001f)
        assertEquals(16f, topStartRadius(FolkShape.Corner16), 0.001f)
        assertEquals(20f, topStartRadius(FolkShape.Corner20), 0.001f)
        assertEquals(24f, topStartRadius(FolkShape.Corner24), 0.001f)
        assertEquals(28f, topStartRadius(FolkShape.Corner28), 0.001f)
        assertEquals(30f, topStartRadius(FolkShape.Dialog), 0.001f)
    }

    @Test
    fun materialSlotsReuseTheSameSteps() {
        assertSame(FolkShape.Corner12, FolkShape.materialShapes.extraSmall)
        assertSame(FolkShape.Corner16, FolkShape.materialShapes.small)
        assertSame(FolkShape.Corner20, FolkShape.materialShapes.medium)
        assertSame(FolkShape.Corner24, FolkShape.materialShapes.large)
        assertSame(FolkShape.Corner28, FolkShape.materialShapes.extraLarge)
    }

    @Test
    fun fullIsACapsule() {
        // 50% of the 100px edge, i.e. a capsule.
        assertEquals(50f, FolkShape.CornerFull.topStart.toPx(size, density), 0.001f)
    }
}
