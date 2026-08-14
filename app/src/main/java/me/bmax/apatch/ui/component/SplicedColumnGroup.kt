package me.bmax.apatch.ui.component

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

/** Whether the current composable is inside a [SplicedColumnGroup]. */
val LocalInsideSplicedGroup = compositionLocalOf { false }

private val CornerRadius = 16.dp
private val ConnectionRadius = 5.dp

data class SplicedItemData(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit,
)

class SplicedGroupScope {
    val items = mutableListOf<SplicedItemData>()

    fun item(key: Any? = null, visible: Boolean = true, content: @Composable () -> Unit) {
        items.add(SplicedItemData(key ?: items.size, visible, content))
    }
}

@Composable
fun SplicedColumnGroup(
    modifier: Modifier = Modifier,
    title: String = "",
    flat: Boolean = false,
    highlightKey: String? = null,
    content: SplicedGroupScope.() -> Unit,
) {
    val scope = SplicedGroupScope().apply(content)
    val allItems = scope.items

    if (allItems.isEmpty()) return

    CompositionLocalProvider(LocalInsideSplicedGroup provides true) {
        Column(modifier = modifier.padding(horizontal = 16.dp).padding(top = 0.dp, bottom = 8.dp)) {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                )
            }

            Column(verticalArrangement = Arrangement.Top) {
                val firstVisibleIndex = allItems.indexOfFirst { it.visible }
                val lastVisibleIndex = allItems.indexOfLast { it.visible }
                val sharedStiffness = Spring.StiffnessMediumLow
                val isAtLeastTiramisu = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

                allItems.forEachIndexed { index, itemData ->
                    key(itemData.key) {
                        val zIndex = if (itemData.visible) 0f else 1f

                        AnimatedVisibility(
                            visible = itemData.visible,
                            modifier = Modifier.zIndex(zIndex),
                            enter = expandVertically(
                                animationSpec = spring(stiffness = sharedStiffness),
                                expandFrom = Alignment.Top,
                            ) + fadeIn(animationSpec = spring(stiffness = sharedStiffness)),
                            exit = shrinkVertically(
                                animationSpec = spring(stiffness = sharedStiffness),
                                shrinkTowards = Alignment.Top,
                            ) + fadeOut(animationSpec = spring(stiffness = sharedStiffness)),
                        ) {
                            val isFirst = index == firstVisibleIndex
                            val isLast = index == lastVisibleIndex

                            val targetTopRadius = if (isFirst) CornerRadius else ConnectionRadius
                            val targetBottomRadius = if (isLast) CornerRadius else ConnectionRadius

                            val currentTopRadius = if (isAtLeastTiramisu) {
                                animateDpAsState(
                                    targetValue = targetTopRadius,
                                    animationSpec = spring(stiffness = sharedStiffness),
                                    label = "TopCornerRadius",
                                ).value
                            } else {
                                targetTopRadius
                            }

                            val currentBottomRadius = if (isAtLeastTiramisu) {
                                animateDpAsState(
                                    targetValue = targetBottomRadius,
                                    animationSpec = spring(stiffness = sharedStiffness),
                                    label = "BottomCornerRadius",
                                ).value
                            } else {
                                targetBottomRadius
                            }

                            val shape = RoundedCornerShape(
                                topStart = currentTopRadius,
                                topEnd = currentTopRadius,
                                bottomStart = currentBottomRadius,
                                bottomEnd = currentBottomRadius,
                            )

                            val targetTopPadding = if (isFirst) 0.dp else 2.dp
                            val currentTopPadding = if (isAtLeastTiramisu) {
                                animateDpAsState(
                                    targetValue = targetTopPadding,
                                    animationSpec = spring(stiffness = sharedStiffness),
                                    label = "TopPadding",
                                ).value
                            } else {
                                targetTopPadding
                            }

                            val containerColor = if (flat) {
                                MaterialTheme.colorScheme.surfaceContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }

                            val isHighlighted = highlightKey != null && itemData.key?.toString() == highlightKey
                            val itemFocusRequester = remember { FocusRequester() }

                            Surface(
                                modifier = Modifier
                                    .padding(top = currentTopPadding)
                                    .then(
                                        if (isHighlighted) Modifier.focusRequester(itemFocusRequester).focusable()
                                        else Modifier
                                    ),
                                shape = shape,
                                color = containerColor,
                                tonalElevation = 0.dp,
                            ) {
                                var highlightAlpha by remember { mutableStateOf(0f) }

                                if (isHighlighted) {
                                    LaunchedEffect(Unit) {
                                        itemFocusRequester.requestFocus()
                                        delay(600)
                                        animate(
                                            initialValue = 0f,
                                            targetValue = 0.18f,
                                            animationSpec = tween(300),
                                        ) { value, _ ->
                                            highlightAlpha = value
                                        }
                                        delay(2000)
                                        animate(
                                            initialValue = 0.18f,
                                            targetValue = 0f,
                                            animationSpec = tween(500),
                                        ) { value, _ ->
                                            highlightAlpha = value
                                        }
                                    }
                                }

                                val highlightColor = MaterialTheme.colorScheme.primary
                                Column(
                                    modifier = Modifier.drawBehind {
                                        if (highlightAlpha > 0.01f) {
                                            drawRect(
                                                color = highlightColor.copy(alpha = highlightAlpha),
                                            )
                                        }
                                    }
                                ) {
                                    itemData.content()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
