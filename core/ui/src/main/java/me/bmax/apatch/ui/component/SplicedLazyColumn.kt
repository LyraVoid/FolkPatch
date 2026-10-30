package me.bmax.apatch.ui.component

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.component.folk.FolkWallpaperSurface
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.theme.tokens.WallpaperSurfaceRole

private val CornerRadius = 16.dp
private val ConnectionRadius = 5.dp

/**
 * LazyColumn extension that renders items as a continuous card group with animated corner radii.
 * First and last visible items get 16dp outer corners; middle items get 5dp connection corners.
 * Uses [LocalInsideSplicedGroup] so inner [TonalCard] wrappers are automatically skipped.
 */
fun <T> LazyListScope.splicedLazyColumnGroup(
    items: List<T>,
    key: ((index: Int, item: T) -> Any)? = null,
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
) {
    val isAtLeastTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    itemsIndexed(
        items = items,
        key = key,
        contentType = contentType,
    ) { index, item ->
        val isFirst = index == 0
        val isLast = index == items.size - 1

        val targetTopRadius = if (isFirst) CornerRadius else ConnectionRadius
        val targetBottomRadius = if (isLast) CornerRadius else ConnectionRadius

        val currentTopRadius = if (isAtLeastTiramisu) {
            animateDpAsState(
                targetValue = targetTopRadius,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "TopCornerRadius",
            ).value
        } else {
            targetTopRadius
        }

        val currentBottomRadius = if (isAtLeastTiramisu) {
            animateDpAsState(
                targetValue = targetBottomRadius,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                label = "BottomCornerRadius",
            ).value
        } else {
            targetBottomRadius
        }

        val shape = ContinuousCornerShape(
            topStart = currentTopRadius,
            topEnd = currentTopRadius,
            bottomStart = currentBottomRadius,
            bottomEnd = currentBottomRadius,
        )
        FolkWallpaperSurface(
            role = WallpaperSurfaceRole.Group,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = if (isFirst) 0.dp else 2.dp),
            shape = shape,
            fallbackColor = FolkTheme.palette.groupedSurface,
            fallbackContentColor = MaterialTheme.colorScheme.onSurface,
            content = {
                CompositionLocalProvider(LocalInsideSplicedGroup provides true) {
                    itemContent(index, item)
                }
            }
        )
    }
}
