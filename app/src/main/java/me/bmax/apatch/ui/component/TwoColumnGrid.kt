package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A true masonry two-column layout using two independent [Column]s side by side.
 *
 * Items are permanently assigned to columns by index (even → left, odd → right).
 * Because the two columns are completely independent, expanding/collapsing a card
 * in one column never affects the other column — no jumping, no layout jitter.
 *
 * @param items the data list to display
 * @param key stable key provider for each item
 * @param beforeItems composable slot rendered full-width above the two columns
 * @param itemContent composable for each item
 */
@Composable
fun <T> TwoColumnGrid(
    items: List<T>,
    key: (item: T) -> Any,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    verticalSpacing: Dp = 16.dp,
    horizontalSpacing: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(),
    beforeItems: (@Composable () -> Unit)? = null,
    itemContent: @Composable (item: T) -> Unit,
) {
    val leftItems = items.filterIndexed { index, _ -> index % 2 == 0 }
    val rightItems = items.filterIndexed { index, _ -> index % 2 == 1 }
    val layoutDirection = LocalLayoutDirection.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(
                start = contentPadding.calculateLeftPadding(layoutDirection),
                top = contentPadding.calculateTopPadding(),
                end = contentPadding.calculateRightPadding(layoutDirection),
            ),
    ) {
        // Full-width content above the grid (banners, empty states, etc.)
        beforeItems?.invoke()

        if (items.isEmpty()) return@Column

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        ) {
            // Left column (even indices: 0, 2, 4, ...)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            ) {
                leftItems.forEach { item ->
                    key(key(item)) {
                        itemContent(item)
                    }
                }
            }

            // Right column (odd indices: 1, 3, 5, ...)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            ) {
                rightItems.forEach { item ->
                    key(key(item)) {
                        itemContent(item)
                    }
                }
            }
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding()))
    }
}
