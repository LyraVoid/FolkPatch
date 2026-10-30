package me.bmax.apatch.ui.component

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens
import me.bmax.apatch.ui.component.folk.FolkWallpaperSurface
import me.bmax.apatch.ui.component.folk.folkSectionTitleColor
import me.bmax.apatch.ui.component.folk.folkSectionTitleStyle
import me.bmax.apatch.ui.theme.tokens.ContinuousCornerShape
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.theme.tokens.WallpaperSurfaceRole

val LocalInsideSplicedGroup = compositionLocalOf { false }

private val OuterCorner = 16.dp
private val ConnectionCorner = 5.dp

data class SegmentedItem(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit,
)

class SegmentedColumnScope {
    internal val items = mutableListOf<SegmentedItem>()

    fun item(key: Any? = null, visible: Boolean = true, content: @Composable () -> Unit) {
        items.add(SegmentedItem(key ?: items.size, visible, content))
    }
}

@Composable
fun SegmentedColumn(
    modifier: Modifier = Modifier,
    title: String? = null,
    flat: Boolean = false,
    highlightKey: String? = null,
    shape: Shape? = null,
    content: SegmentedColumnScope.() -> Unit,
) {
    val scope = SegmentedColumnScope().apply(content)
    val visibleItems = scope.items.filter { it.visible }
    if (visibleItems.isEmpty() && title == null) return

    val highlightColor = MaterialTheme.colorScheme.primary
    val atLeastTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val firstVisible = scope.items.indexOfFirst { it.visible }
    val lastVisible = scope.items.indexOfLast { it.visible }

    Column(
        modifier = modifier.padding(horizontal = FolkSettingsDimens.ScreenPadding),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = folkSectionTitleStyle(),
                color = folkSectionTitleColor(),
                modifier = Modifier.padding(
                    start = FolkSettingsDimens.SectionTitleIndent,
                    bottom = FolkSettingsDimens.SectionTitleSpacing,
                ),
            )
        }

        CompositionLocalProvider(LocalInsideSplicedGroup provides true) {
            Column(verticalArrangement = Arrangement.Top) {
                scope.items.forEachIndexed { index, item ->
                    key(item.key) {
                        AnimatedVisibility(
                            visible = item.visible,
                            modifier = Modifier.zIndex(if (item.visible) 0f else 1f),
                            enter = expandVertically(
                                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                expandFrom = Alignment.Top,
                            ) + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                            exit = shrinkVertically(
                                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                                shrinkTowards = Alignment.Top,
                            ) + fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
                        ) {
                            val isFirst = index == firstVisible
                            val isLast = index == lastVisible
                            val topTarget = if (isFirst) OuterCorner else ConnectionCorner
                            val bottomTarget = if (isLast) OuterCorner else ConnectionCorner
                            val top = if (atLeastTiramisu) {
                                animateDpAsState(
                                    targetValue = topTarget,
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                    label = "SegmentedTopCorner",
                                ).value
                            } else {
                                topTarget
                            }
                            val bottom = if (atLeastTiramisu) {
                                animateDpAsState(
                                    targetValue = bottomTarget,
                                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                    label = "SegmentedBottomCorner",
                                ).value
                            } else {
                                bottomTarget
                            }
                            val itemShape = when {
                                visibleItems.size == 1 && shape != null -> shape
                                else -> ContinuousCornerShape(
                                    topStart = top,
                                    topEnd = top,
                                    bottomStart = bottom,
                                    bottomEnd = bottom,
                                )
                            }
                            val isHighlighted = highlightKey != null && item.key?.toString() == highlightKey
                            val focusRequester = remember { FocusRequester() }
                            var highlightAlpha by remember { mutableStateOf(0f) }
                            val highlightIn = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                            val highlightOut = MaterialTheme.motionScheme.slowEffectsSpec<Float>()

                            if (isHighlighted) {
                                LaunchedEffect(Unit) {
                                    runCatching { focusRequester.requestFocus() }
                                    delay(600)
                                    animate(
                                        initialValue = 0f,
                                        targetValue = 0.16f,
                                        animationSpec = highlightIn,
                                    ) { value, _ -> highlightAlpha = value }
                                    delay(2000)
                                    animate(
                                        initialValue = 0.16f,
                                        targetValue = 0f,
                                        animationSpec = highlightOut,
                                    ) { value, _ -> highlightAlpha = value }
                                }
                            }

                            FolkWallpaperSurface(
                                role = WallpaperSurfaceRole.Group,
                                modifier = Modifier
                                    .padding(top = if (isFirst) 0.dp else 2.dp)
                                    .then(
                                        if (isHighlighted) {
                                            Modifier.focusRequester(focusRequester).focusable()
                                        } else {
                                            Modifier
                                        },
                                    ),
                                shape = itemShape,
                                fallbackColor = FolkTheme.palette.groupedSurface,
                                fallbackContentColor = MaterialTheme.colorScheme.onSurface,
                                content = {
                                    Column(
                                        modifier = Modifier.drawBehind {
                                            if (highlightAlpha > 0.01f) {
                                                drawRect(
                                                    highlightColor.copy(alpha = highlightAlpha),
                                                )
                                            }
                                        },
                                    ) {
                                        item.content()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
