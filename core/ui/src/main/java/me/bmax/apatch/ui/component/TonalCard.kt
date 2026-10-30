package me.bmax.apatch.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import me.bmax.apatch.ui.component.folk.FolkWallpaperSurface
import me.bmax.apatch.ui.component.folk.folkPressScale
import me.bmax.apatch.ui.component.folk.navOrigin
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.theme.tokens.FolkShape
import me.bmax.apatch.ui.theme.tokens.FolkTheme
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens
import me.bmax.apatch.ui.theme.tokens.WallpaperSurfaceRole

@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
    shape: Shape = FolkShape.Corner20,
    onClick: (() -> Unit)? = null,
    flat: Boolean = false,
    navOriginRadius: Dp = FolkDimens.Size20,
    content: @Composable () -> Unit,
) {
    val wallpaper = LocalFolkWallpaperTokens.current
    val useWallpaperMaterial = wallpaper != null && containerColor == null
    val fill = if (useWallpaperMaterial) {
        wallpaper.group.fill
    } else {
        containerColor ?: FolkTheme.palette.groupedSurface
    }
    val foreground = contentColor ?: if (useWallpaperMaterial) {
        wallpaper.group.contentColor
    } else {
        contentColorFor(fill)
    }

    if (LocalInsideSplicedGroup.current) {
        if (onClick == null) {
            Box(modifier = modifier.fillMaxWidth()) { content() }
        } else {
            val interactionSource = remember { MutableInteractionSource() }
            val haptics = LocalHapticFeedback.current
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .navOrigin(
                        cornerRadius = navOriginRadius,
                        color = fill,
                        interactionSource = interactionSource,
                    )
                    .folkPressScale(interactionSource)
                    .clickable(
                        role = Role.Button,
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
        }
        return
    }

    if (useWallpaperMaterial) {
        val interactionSource = remember { MutableInteractionSource() }
        val haptics = LocalHapticFeedback.current
        val wallpaperModifier = if (onClick == null) {
            modifier.fillMaxWidth()
        } else {
            modifier
                .fillMaxWidth()
                .navOrigin(
                    cornerRadius = navOriginRadius,
                    color = fill,
                    interactionSource = interactionSource,
                )
                .folkPressScale(interactionSource)
                .clickable(
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    },
                )
        }
        FolkWallpaperSurface(
            role = WallpaperSurfaceRole.Group,
            shape = shape,
            modifier = wallpaperModifier,
            fallbackColor = FolkTheme.palette.groupedSurface,
            fallbackContentColor = foreground,
            content = content,
        )
        return
    }

    val colors = if (flat) {
        CardDefaults.cardColors(containerColor = fill, contentColor = foreground)
    } else {
        CardDefaults.elevatedCardColors(containerColor = fill, contentColor = foreground)
    }
    val cardModifier = modifier.fillMaxWidth()
    val clickableModifier = if (onClick == null) {
        cardModifier
    } else {
        cardModifier.navOrigin(
            cornerRadius = navOriginRadius,
            color = fill,
        )
    }

    if (flat) {
        if (onClick == null) {
            Card(modifier = clickableModifier, colors = colors, shape = shape, content = { content() })
        } else {
            Card(
                modifier = clickableModifier,
                colors = colors,
                shape = shape,
                onClick = onClick,
                content = { content() },
            )
        }
    } else if (onClick == null) {
        ElevatedCard(modifier = clickableModifier, colors = colors, shape = shape, content = { content() })
    } else {
        ElevatedCard(
            modifier = clickableModifier,
            colors = colors,
            shape = shape,
            onClick = onClick,
            content = { content() },
        )
    }
}
