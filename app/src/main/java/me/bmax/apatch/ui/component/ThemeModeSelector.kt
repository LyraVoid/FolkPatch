package me.bmax.apatch.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.bmax.apatch.R
import androidx.compose.ui.semantics.Role
import me.bmax.apatch.ui.theme.LocalThemeRevealState
import me.bmax.apatch.ui.theme.tokens.FolkShape

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@Composable
fun ThemeModeSelector(
    selectedMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    bare: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeModeOption(
                icon = Icons.Outlined.LightMode,
                label = stringResource(R.string.theme_light),
                isSelected = selectedMode == ThemeMode.LIGHT,
                onClick = { onModeSelected(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f),
            )

            ThemeModeOption(
                icon = Icons.Outlined.DarkMode,
                label = stringResource(R.string.theme_dark),
                isSelected = selectedMode == ThemeMode.DARK,
                onClick = { onModeSelected(ThemeMode.DARK) },
                modifier = Modifier.weight(1f),
            )

            ThemeModeOption(
                icon = Icons.Outlined.AutoAwesome,
                label = stringResource(R.string.theme_system),
                isSelected = selectedMode == ThemeMode.SYSTEM,
                onClick = { onModeSelected(ThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (bare) {
        content()
    } else {
        TonalCard(modifier = modifier, flat = flat) {
            content()
        }
    }
}

@Composable
private fun ThemeModeOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "themeModeScale",
    )

    val revealState = LocalThemeRevealState.current
    val scope = rememberCoroutineScope()
    var centerInRoot by remember { mutableStateOf(Offset.Zero) }

    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow

    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .onGloballyPositioned { centerInRoot = it.boundsInRoot().center }
            .scale(scale)
            .clip(FolkShape.Corner24)
            .background(bgColor)
            .clickable(role = Role.RadioButton, onClick = {
                // Snapshot the current frame first so the ripple overlay can peel
                // the previous theme away from this control's centre; only then
                // apply the change (which triggers recomposition with the new theme).
                if (revealState != null) {
                    scope.launch {
                        revealState.capture(centerInRoot)
                        onClick()
                    }
                } else {
                    onClick()
                }
            })
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = contentColor,
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
        )
    }
}
