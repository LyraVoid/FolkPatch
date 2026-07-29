package me.bmax.apatch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs

data class ScrollState(
    val isScrollingDown: MutableState<Boolean>,
    val scrollOffset: MutableState<Float>,
    val previousScrollOffset: MutableState<Float>
)

val LocalScrollState = compositionLocalOf<ScrollState?> { null }

val LocalBottomBarVisible = compositionLocalOf { mutableStateOf(true) }
val LocalIsFloatingNavMode = compositionLocalOf { false }

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
