package me.bmax.apatch.ui.component.folk

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens

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
    val wallpaper = LocalFolkWallpaperTokens.current
    val tint = MaterialTheme.colorScheme.surfaceContainerHighest
    IconButton(
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .folkPressScale(interactionSource),
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = wallpaper?.chromeControlFill(tint) ?: tint,
            contentColor = wallpaper?.chrome?.contentColor
                ?: MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        interactionSource = interactionSource,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = contentDescription,
        )
    }
}
