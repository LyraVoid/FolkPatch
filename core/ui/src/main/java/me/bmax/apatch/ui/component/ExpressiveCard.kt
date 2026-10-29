package me.bmax.apatch.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import me.bmax.apatch.ui.component.LocalInsideSplicedGroup
import me.bmax.apatch.ui.component.folk.folkPressScale
import me.bmax.apatch.ui.theme.tokens.FolkShape
import androidx.compose.ui.semantics.Role

@Composable
fun ExpressiveCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    flat: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (LocalInsideSplicedGroup.current || LocalInsideSplicedGroup.current) {
        // Inside a settings group (SegmentedColumn or SegmentedColumn): skip
        // the card wrapper, the parent already provides the container surface.
        if (onClick != null) {
            val interactionSource = remember { MutableInteractionSource() }
            val haptics = LocalHapticFeedback.current
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .folkPressScale(interactionSource)
                    .clickable(role = Role.Button,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        },
                    ),
            ) {
                content()
            }
        } else {
            Box(modifier = modifier.fillMaxWidth()) {
                content()
            }
        }
        return
    }

    // Standalone: an ElevatedCard whose corners ease out of the edges instead of
    // meeting them in a circular arc.
    val shape = FolkShape.Corner28
    val colors = if (flat) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    } else {
        CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    }

    if (flat) {
        if (onClick != null) {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = colors,
                onClick = onClick,
                shape = shape,
                content = { content() },
            )
        } else {
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = colors,
                shape = shape,
                content = { content() },
            )
        }
    } else {
        if (onClick != null) {
            ElevatedCard(
                modifier = modifier.fillMaxWidth(),
                colors = colors,
                onClick = onClick,
                shape = shape,
                content = { content() },
            )
        } else {
            ElevatedCard(
                modifier = modifier.fillMaxWidth(),
                colors = colors,
                shape = shape,
                content = { content() },
            )
        }
    }
}
