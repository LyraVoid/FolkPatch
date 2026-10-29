package me.bmax.apatch.ui.component.folk

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.theme.tokens.FolkTheme

/** The filled, circular back control used by the settings app bars. */
@Composable
fun TopBarBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    horizontalPadding: Dp = 10.dp,
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .folkPressScale(interactionSource),
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = FolkTheme.palette.chromeControlFill(
                MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            contentColor = if (FolkTheme.palette.onCustomBackground) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
        interactionSource = interactionSource,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = contentDescription,
        )
    }
}
