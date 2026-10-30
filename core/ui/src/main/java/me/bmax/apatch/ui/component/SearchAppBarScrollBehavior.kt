package me.bmax.apatch.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Stable
class SearchAppBarScrollBehavior internal constructor(
    private val topAppBarScrollBehavior: TopAppBarScrollBehavior,
    private val searchBarHeight: Float,
) : TopAppBarScrollBehavior {
    private var searchBarHeightOffset by mutableFloatStateOf(0f)
    private var lastSearchBarScrollDelta = 0f
    private var isUserScrollInProgress = false
    private var ignoreCurrentScroll = false

    internal val searchBarExpandedFraction: Float
        get() = (1f + searchBarHeightOffset / searchBarHeight).coerceIn(0f, 1f)

    override val state: TopAppBarState
        get() = topAppBarScrollBehavior.state

    override val isPinned: Boolean
        get() = topAppBarScrollBehavior.isPinned

    override val snapAnimationSpec: AnimationSpec<Float>?
        get() = topAppBarScrollBehavior.snapAnimationSpec

    override val flingAnimationSpec: DecayAnimationSpec<Float>?
        get() = topAppBarScrollBehavior.flingAnimationSpec

    internal suspend fun expandSearchBar() {
        ignoreCurrentScroll = isUserScrollInProgress
        lastSearchBarScrollDelta = 0f
        animateSearchBarTo(0f)
    }

    internal fun reset() {
        searchBarHeightOffset = 0f
        lastSearchBarScrollDelta = 0f
        isUserScrollInProgress = false
        ignoreCurrentScroll = false
    }

    private fun consumeSearchBarScroll(delta: Float): Float {
        val previousOffset = searchBarHeightOffset
        searchBarHeightOffset = (previousOffset + delta).coerceIn(-searchBarHeight, 0f)
        val consumed = searchBarHeightOffset - previousOffset
        if (consumed != 0f) lastSearchBarScrollDelta = consumed
        return consumed
    }

    private suspend fun animateSearchBarTo(targetOffset: Float, initialVelocity: Float = 0f) {
        if (searchBarHeightOffset == targetOffset) return
        Animatable(searchBarHeightOffset).animateTo(
            targetValue = targetOffset,
            animationSpec = snapAnimationSpec ?: spring(),
            initialVelocity = initialVelocity,
        ) {
            searchBarHeightOffset = value.coerceIn(-searchBarHeight, 0f)
        }
        lastSearchBarScrollDelta = 0f
    }

    override val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.UserInput) isUserScrollInProgress = true
            if (ignoreCurrentScroll || available.y >= 0f) {
                return topAppBarScrollBehavior.nestedScrollConnection.onPreScroll(available, source)
            }
            val previousOffset = searchBarHeightOffset
            consumeSearchBarScroll(available.y)
            if (previousOffset != searchBarHeightOffset) return available.copy(x = 0f)
            return topAppBarScrollBehavior.nestedScrollConnection.onPreScroll(available, source)
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            val topAppBarConsumed =
                topAppBarScrollBehavior.nestedScrollConnection.onPostScroll(consumed, available, source)
            if (ignoreCurrentScroll || available.y <= 0f) return topAppBarConsumed
            val remaining = available.y - topAppBarConsumed.y
            return topAppBarConsumed + Offset(0f, consumeSearchBarScroll(remaining))
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (ignoreCurrentScroll) {
                return topAppBarScrollBehavior.nestedScrollConnection.onPreFling(available)
            }
            if (available.y < 0f && searchBarHeightOffset > -searchBarHeight) {
                animateSearchBarTo(-searchBarHeight, available.y)
                return available.copy(x = 0f)
            }
            return topAppBarScrollBehavior.nestedScrollConnection.onPreFling(available)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            isUserScrollInProgress = false
            if (ignoreCurrentScroll) {
                ignoreCurrentScroll = false
                return topAppBarScrollBehavior.nestedScrollConnection.onPostFling(consumed, available)
            }
            if (searchBarHeightOffset > -searchBarHeight && searchBarHeightOffset < 0f) {
                val shouldExpand =
                    available.y > 0f || (available.y == 0f && lastSearchBarScrollDelta > 0f)
                animateSearchBarTo(if (shouldExpand) 0f else -searchBarHeight, available.y)
                return available.copy(x = 0f)
            }
            val topAppBarVelocity =
                topAppBarScrollBehavior.nestedScrollConnection.onPostFling(consumed, available)
            val remaining = available.y - topAppBarVelocity.y
            if (remaining > 0f &&
                topAppBarScrollBehavior.state.collapsedFraction < 0.01f &&
                searchBarHeightOffset < 0f
            ) {
                animateSearchBarTo(0f, remaining)
                return topAppBarVelocity
            }
            return topAppBarVelocity
        }
    }
}

@Composable
fun rememberSearchAppBarScrollBehavior(
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
): SearchAppBarScrollBehavior {
    val density = LocalDensity.current
    return remember(topAppBarScrollBehavior, density) {
        SearchAppBarScrollBehavior(
            topAppBarScrollBehavior = topAppBarScrollBehavior,
            searchBarHeight = with(density) { FolkDimens.Size69.toPx() },
        )
    }
}

internal fun Modifier.collapseWithTopAppBar(expandedFraction: Float): Modifier =
    this
        .clipToBounds()
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minHeight = 0))
            val visibleHeight = (placeable.height * expandedFraction.coerceIn(0f, 1f)).roundToInt()
            layout(placeable.width, visibleHeight) {
                placeable.placeRelative(x = 0, y = visibleHeight - placeable.height)
            }
        }
