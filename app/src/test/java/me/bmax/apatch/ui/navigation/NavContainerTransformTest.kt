package me.bmax.apatch.ui.navigation

import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.EnterExitState
import androidx.compose.ui.graphics.Color
import me.bmax.apatch.ui.component.folk.NavOrigin
import me.bmax.apatch.ui.component.folk.NavOriginState
import org.junit.Assert.assertEquals
import org.junit.Test

class NavContainerTransformTest {
    @Test
    fun `wallpaper mode always uses its fallback transition`() {
        assertEquals(false, shouldUseContainerTransform(wallpaperActive = true, hasOrigin = true))
        assertEquals(true, shouldUseContainerTransform(wallpaperActive = false, hasOrigin = true))
        assertEquals(false, shouldUseContainerTransform(wallpaperActive = false, hasOrigin = false))
    }

    @Test
    fun `pending origins are committed once and cached by page`() {
        val state = NavOriginState { 1_000L }
        val origin = origin()

        state.recordOrigin(origin)
        assertEquals(true, state.commitPending("page"))
        assertEquals(origin, state.origin("page"))
        assertEquals(false, state.commitPending("other"))

        state.removeOrigin("page")
        assertEquals(false, state.hasOrigin("page"))
    }

    @Test
    fun `stale origins never attach to a different navigation`() {
        var now = 1_000L
        val state = NavOriginState { now }
        state.recordOrigin(origin())
        now += 1_201L

        assertEquals(false, state.hasPendingOrigin())
        assertEquals(false, state.commitPending("page"))
    }

    @Test
    fun `origin cache stays bounded`() {
        val state = NavOriginState { 1_000L }
        repeat(5) { index ->
            state.recordOrigin(origin())
            state.commitPending(index)
        }

        assertEquals(false, state.hasOrigin(0))
        assertEquals(true, state.hasOrigin(4))
    }

    @Test
    fun `a new touch clears a pending origin`() {
        val state = NavOriginState { 1_000L }
        state.recordOrigin(origin())
        state.clearPending()
        assertEquals(false, state.commitPending("page"))
    }

    @Test
    fun `container follows the navigation transition in both directions`() {
        assertEquals(0f, containerFraction(EnterExitState.PreEnter), 0.001f)
        assertEquals(1f, containerFraction(EnterExitState.Visible), 0.001f)
        assertEquals(0f, containerFraction(EnterExitState.PostExit), 0.001f)
    }

    @Test
    fun `bounds interpolate from the source card to the page`() {
        val origin = Rect(24f, 180f, 264f, 420f)
        val destination = Rect(0f, 0f, 1080f, 2400f)

        assertEquals(origin, interpolateContainerBounds(origin, destination, 0f))
        assertEquals(destination, interpolateContainerBounds(origin, destination, 1f))
        assertEquals(
            Rect(12f, 90f, 672f, 1410f),
            interpolateContainerBounds(origin, destination, 0.5f),
        )
    }

    @Test
    fun `bounds and corners clamp outside the transition`() {
        val origin = Rect(20f, 30f, 120f, 180f)
        val destination = Rect(0f, 0f, 1080f, 2400f)

        assertEquals(origin, interpolateContainerBounds(origin, destination, -1f))
        assertEquals(destination, interpolateContainerBounds(origin, destination, 2f))
        assertEquals(40f, interpolateContainerCorner(40f, 0f), 0.001f)
        assertEquals(0f, interpolateContainerCorner(40f, 1f), 0.001f)
    }

    @Test
    fun `the expanding container uncovers the page`() {
        assertEquals(1f, pushSurfaceVeilAlpha(0f), 0.001f)
        assertEquals(1f, pushSurfaceVeilAlpha(0.08f), 0.001f)
        assertEquals(0f, pushSurfaceVeilAlpha(0.42f), 0.001f)
        assertEquals(0f, pushSurfaceVeilAlpha(1f), 0.001f)
    }

    @Test
    fun `the folding container hides the page before it reaches the card`() {
        assertEquals(0f, popSurfaceVeilAlpha(1f), 0.001f)
        assertEquals(0f, popSurfaceVeilAlpha(0.5f), 0.001f)
        assertEquals(1f, popSurfaceVeilAlpha(0.08f), 0.001f)
        assertEquals(1f, popSurfaceVeilAlpha(0f), 0.001f)
    }

    @Test
    fun `snapshot only returns near the source bounds`() {
        assertEquals(0f, popSnapshotAlpha(0.42f), 0.001f)
        assertEquals(1f, popSnapshotAlpha(0.14f), 0.001f)
        assertEquals(1f, popSnapshotAlpha(0f), 0.001f)
    }

    @Test
    fun `theme changes suppress stale source colors and snapshots`() {
        val originColor = Color(0xFF123456)
        val surfaceColor = Color(0xFF654321)
        val matching = resolveContainerAppearance(
            originColor = originColor,
            originThemeSignature = 7,
            currentThemeSignature = 7,
            surfaceColor = surfaceColor,
            popping = true,
            hasSnapshot = true,
            fraction = 0f,
        )
        val changed = resolveContainerAppearance(
            originColor = originColor,
            originThemeSignature = 7,
            currentThemeSignature = 8,
            surfaceColor = surfaceColor,
            popping = true,
            hasSnapshot = true,
            fraction = 0f,
        )

        assertEquals(originColor, matching.fillColor)
        assertEquals(1f, matching.snapshotAlpha, 0.001f)
        assertEquals(surfaceColor, changed.fillColor)
        assertEquals(0f, changed.snapshotAlpha, 0.001f)
    }

    private fun origin() = NavOrigin(
        rect = Rect(10f, 20f, 110f, 220f),
        cornerRadius = 20f,
        color = Color(0xFF123456),
        themeSignature = 3,
    )
}
