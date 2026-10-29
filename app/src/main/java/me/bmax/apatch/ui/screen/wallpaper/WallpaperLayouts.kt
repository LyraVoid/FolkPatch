package me.bmax.apatch.ui.screen.wallpaper

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import kotlinx.coroutines.flow.distinctUntilChanged
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.folk.FolkLoadingIndicator
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens
import me.bmax.apatch.ui.component.folk.folkPressScale
import me.bmax.apatch.ui.theme.tokens.FolkMotion
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.wallpaper.WallpaperItem

/** Real image proportions, retained by the ViewModel, determine each lane's height. */
@Composable
fun WallpaperWaterfall(
    items: List<WallpaperItem>,
    contentPadding: PaddingValues,
    onTap: (WallpaperItem) -> Unit,
    onLoadMore: () -> Unit,
    onImageSize: (WallpaperItem, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyStaggeredGridState()
    LaunchedEffect(gridState, items.size) {
        snapshotFlow {
            val last = gridState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1
            items.isNotEmpty() && last >= items.size - 4
        }.distinctUntilChanged().collect { nearEnd -> if (nearEnd) onLoadMore() }
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(FolkDimens.Size160),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(FolkSettingsDimens.SectionTitleSpacing),
        verticalItemSpacing = FolkSettingsDimens.SectionTitleSpacing,
    ) {
        items(items, key = { it.url }) { item ->
            WallpaperCard(
                item = item,
                onClick = { onTap(item) },
                onImageSize = onImageSize,
                modifier = Modifier.fillMaxWidth().aspectRatio(item.aspectRatio),
            )
        }
    }
}

/** Fit each card into the available viewport, including room for the layers below it. */
@Composable
fun WallpaperStack(
    items: List<WallpaperItem>,
    onTap: (WallpaperItem) -> Unit,
    onLoadMore: () -> Unit,
    onImageSize: (WallpaperItem, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    var current by rememberSaveable(items.first().url) { mutableIntStateOf(0) }
    var drag by remember { mutableFloatStateOf(0f) }
    val index = current.coerceIn(0, items.lastIndex)
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(index, items.size) {
        if (index >= items.size - 4) onLoadMore()
    }
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
        val layerGap = if (maxHeight < FolkDimens.Size320) {
            FolkSettingsDimens.SectionTitleIndent
        } else {
            FolkSettingsDimens.ScreenPadding
        }
        val availableHeight = (maxHeight - layerGap * 6).coerceAtLeast(FolkDimens.Size1)
        val maxDrag = with(density) { (layerGap * 2).toPx() }
        val threshold = with(density) { FolkDimens.Size48.toPx() }
        LaunchedEffect(maxWidth, maxHeight, index) { drag = 0f }
        val animatedDrag by animateFloatAsState(
            targetValue = drag.coerceIn(-maxDrag, maxDrag),
            animationSpec = FolkMotion.PressScale,
            label = "wallpaperDrag",
        )
        for (i in minOf(index + 3, items.lastIndex) downTo index) {
            val item = items[i]
            key(item.url) {
                val depth = i - index
                val scale by animateFloatAsState(
                    targetValue = 1f - depth * 0.07f,
                    animationSpec = FolkMotion.PressScale,
                    label = "wallpaperDepth",
                )
                val offset by animateFloatAsState(
                    targetValue = with(density) { (layerGap * depth).toPx() },
                    animationSpec = FolkMotion.PressScale,
                    label = "wallpaperOffset",
                )
                val width = minOf(maxWidth * 0.84f, availableHeight * item.aspectRatio)
                WallpaperCard(
                    item = item,
                    onClick = { if (depth == 0) onTap(item) else current = i },
                    onImageSize = onImageSize,
                    modifier = Modifier
                        .size(width = width, height = width / item.aspectRatio)
                        .zIndex((10 - depth).toFloat())
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationY = offset + if (depth == 0) animatedDrag else 0f
                            alpha = 1f - depth * 0.12f
                        }
                        .then(
                            if (depth != 0) Modifier else Modifier.pointerInput(index, items.size) {
                                detectVerticalDragGestures(
                                    onDragEnd = {
                                        val next = when {
                                            drag < -threshold -> (index + 1).coerceAtMost(items.lastIndex)
                                            drag > threshold -> (index - 1).coerceAtLeast(0)
                                            else -> index
                                        }
                                        if (next != index) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            current = next
                                        }
                                        drag = 0f
                                    },
                                    onDragCancel = { drag = 0f },
                                    onVerticalDrag = { change, amount ->
                                        change.consume()
                                        drag += amount
                                    },
                                )
                            },
                        ),
                )
            }
        }
    }
}

@Composable
fun WallpaperCard(
    item: WallpaperItem,
    onClick: () -> Unit,
    onImageSize: (WallpaperItem, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val interactionSource = remember(item.url) { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val request = remember(context, item.imageUri) {
        ImageRequest.Builder(context)
            .data(item.imageUri)
            .scale(Scale.FIT)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    val painter = rememberAsyncImagePainter(model = request)
    val state = painter.state
    LaunchedEffect(state) {
        if (state is AsyncImagePainter.State.Success) {
            val size = state.painter.intrinsicSize
            if (size.width.isFinite() && size.height.isFinite()) {
                onImageSize(item, size.width.toInt(), size.height.toInt())
            }
        }
    }
    Card(
        modifier = modifier
            .folkPressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = FolkShape.Corner24,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        ),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painter,
                contentDescription = stringResource(R.string.settings_wallpaper_gallery) + " " + item.id,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            when (state) {
                is AsyncImagePainter.State.Loading -> FolkLoadingIndicator(
                    modifier = Modifier.size(FolkSettingsDimens.IconSize),
                )
                is AsyncImagePainter.State.Error -> Icon(
                    Icons.Outlined.BrokenImage,
                    contentDescription = stringResource(R.string.wallpaper_retry),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Unit
            }
        }
    }
}
