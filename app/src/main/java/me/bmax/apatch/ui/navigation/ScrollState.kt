package me.bmax.apatch.ui.navigation

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

data class ScrollState(
    val isScrollingDown: MutableState<Boolean>,
    val scrollOffset: MutableState<Float>,
    val previousScrollOffset: MutableState<Float>
)

val LocalScrollState = compositionLocalOf<ScrollState?> { null }

val LocalBottomBarVisible = compositionLocalOf { mutableStateOf(true) }
val LocalIsFloatingNavMode = compositionLocalOf { false }

/**
 * Bottom clearance for scrollable list content so the last item can always be
 * scrolled above the FAB and the floating navigation bar overlay, regardless
 * of the current navigation mode (floating / bottom / rail / auto).
 */
@Composable
fun fabNavBottomClearance(): Dp {
    val isFloatingMode = LocalIsFloatingNavMode.current
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    return when {
        // Raised FAB above the floating bar: 16dp margin + 88dp lift + 56dp FAB + 16dp gap
        isFloatingMode && !isLandscape -> 176.dp
        // Floating bar overlay (14dp + 72dp + 14dp) with the FAB at rest beside it
        isFloatingMode -> 104.dp
        // Resting FAB: 16dp margin + 56dp FAB + 16dp gap
        else -> 88.dp
    }
}

@Composable
fun rememberScrollConnection(
    isScrollingDown: MutableState<Boolean>,
    scrollOffset: MutableState<Float>,
    previousScrollOffset: MutableState<Float>,
    threshold: Float = 50f,
    onUserScroll: (() -> Unit)? = null
): NestedScrollConnection {
    return remember(onUserScroll) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                if (delta != 0f) {
                    // Any scroll counts as user interaction, reset auto-hide timer
                    onUserScroll?.invoke()
                }

                // Update scroll offset
                val newOffset = scrollOffset.value + delta
                scrollOffset.value = newOffset

                // Calculate the scroll delta from previous offset
                val scrollDelta = previousScrollOffset.value - newOffset

                // Only update direction if scroll delta exceeds threshold
                if (abs(scrollDelta) > threshold) {
                    isScrollingDown.value = scrollDelta > 0
                    previousScrollOffset.value = newOffset
                }

                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Reset offset tracking after fling
                previousScrollOffset.value = scrollOffset.value
                return super.onPostFling(consumed, available)
            }
        }
    }
}
