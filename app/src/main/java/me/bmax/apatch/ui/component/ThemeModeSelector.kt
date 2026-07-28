package me.bmax.apatch.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.bmax.apatch.R

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
                icon = Icons.Default.LightMode,
                label = stringResource(R.string.theme_light),
                isSelected = selectedMode == ThemeMode.LIGHT,
                onClick = { onModeSelected(ThemeMode.LIGHT) },
                flat = flat,
                modifier = Modifier.weight(1f),
            )

            ThemeModeOption(
                icon = Icons.Default.DarkMode,
                label = stringResource(R.string.theme_dark),
                isSelected = selectedMode == ThemeMode.DARK,
                onClick = { onModeSelected(ThemeMode.DARK) },
                flat = flat,
                modifier = Modifier.weight(1f),
            )

            ThemeModeOption(
                icon = Icons.Default.AutoAwesome,
                label = stringResource(R.string.theme_system),
                isSelected = selectedMode == ThemeMode.SYSTEM,
                onClick = { onModeSelected(ThemeMode.SYSTEM) },
                flat = flat,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (bare) {
        content()
    } else {
        ExpressiveCard(modifier = modifier, flat = flat) {
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
    flat: Boolean = false,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "themeModeScale",
    )

    val bgColor = when {
        isSelected && flat -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        flat -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
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
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}
