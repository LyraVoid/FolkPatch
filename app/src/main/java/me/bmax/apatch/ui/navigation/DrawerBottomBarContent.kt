package me.bmax.apatch.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens

/** A floating capsule whose selected destination opens to reveal its label. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DrawerBottomBarContent(
    destinations: List<BottomBarDestination>,
    selectedIndex: Int,
    badgeCount: (BottomBarDestination) -> Int,
    onSelect: (BottomBarDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (destinations.isEmpty()) return
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
    val wallpaper = LocalFolkWallpaperTokens.current
    BoxWithConstraints(modifier = modifier) {
        val barWidth = (56.dp * destinations.size + 88.dp).coerceAtMost(maxWidth)
        // Reserve 48dp for every icon; give the remaining room to the open drawer.
        val extraWidth = (barWidth - 16.dp - 4.dp * (destinations.size - 1) -
            48.dp * destinations.size).coerceAtLeast(0.dp)
        val openWeight = 1f + extraWidth / 48.dp
        Row(
            modifier = Modifier.width(barWidth).height(64.dp)
                .padding(8.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEachIndexed { index, destination ->
                key(destination) {
                    val selected = index == selectedIndex
                    val expansion by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = spatialSpec,
                        label = "drawerExpansion",
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.9f,
                        animationSpec = spatialSpec,
                        label = "drawerIconScale",
                    )
                    val selectedPillColor = wallpaper?.selection?.fill
                        ?: MaterialTheme.colorScheme.secondaryContainer
                    val background by animateColorAsState(
                        targetValue = if (selected) selectedPillColor
                            else Color.Transparent,
                        animationSpec = effectsSpec,
                        label = "drawerSelection",
                    )
                    val label = stringResource(destination.label)
                    val count = badgeCount(destination)
                    val badgeLabel = if (count > 0) ", $count" else ""
                    val tint = if (selected) wallpaper?.selection?.contentColor
                        ?: MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    Row(
                        modifier = Modifier.weight(1f + (openWeight - 1f) * expansion)
                            .height(44.dp).clip(CircleShape).background(background)
                            .selectable(selected = selected, role = Role.Tab,
                                onClick = { onSelect(destination) })
                            .semantics { contentDescription = label + badgeLabel }
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BadgedBox(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                                .clearAndSetSemantics {},
                            badge = {
                                if (count > 0) Badge {
                                    Text(count.toString())
                                }
                            },
                        ) {
                            if (LocalInspectionMode.current) {
                                Icon(if (selected) destination.iconSelected else destination.iconNotSelected,
                                    contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                            } else {
                                NavBarIcon(destination, selected, tint, Modifier.size(24.dp))
                            }
                        }
                        if (expansion > 0f && extraWidth > 0.dp) {
                            Text(
                                text = label,
                                modifier = Modifier
                                    .padding(start = 8.dp * expansion)
                                    .graphicsLayer { alpha = expansion }
                                    .clearAndSetSemantics {},
                                color = tint,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DrawerBottomBarPreview() {
    MaterialTheme {
        DrawerBottomBarContent(BottomBarDestination.entries, 0, { 0 }, {})
    }
}
