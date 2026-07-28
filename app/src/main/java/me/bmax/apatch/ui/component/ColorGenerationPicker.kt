package me.bmax.apatch.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Palette
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
import me.bmax.apatch.ui.theme.ColorGenerationMode
import me.bmax.apatch.ui.theme.ColorStandard
import me.bmax.apatch.ui.theme.ColorStyle

// ─── Color Generation Mode (Classic / Custom) ────────────────────────────────

@Composable
fun ColorGenerationModeSelector(
    selectedMode: ColorGenerationMode,
    onModeSelected: (ColorGenerationMode) -> Unit,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    bare: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            SegmentedToggleRow(
                options = ColorGenerationMode.entries.map { mode ->
                    SegmentedOption(
                        label = stringResource(
                            when (mode) {
                                ColorGenerationMode.CLASSIC -> R.string.color_generation_classic
                                ColorGenerationMode.CUSTOM -> R.string.color_generation_custom
                            }
                        ),
                        icon = when (mode) {
                            ColorGenerationMode.CLASSIC -> Icons.Default.Palette
                            ColorGenerationMode.CUSTOM -> Icons.Default.AutoAwesome
                        },
                        key = mode.name,
                    )
                },
                selectedIndex = ColorGenerationMode.entries.indexOf(selectedMode),
                onOptionSelected = { index -> onModeSelected(ColorGenerationMode.entries[index]) },
            )
        }
    }

    if (bare) content() else ExpressiveCard(modifier = modifier, flat = flat) { content() }
}

// ─── Color Standard (MD3 2021 / M3e 2025) ────────────────────────────────────

@Composable
fun ColorStandardSelector(
    selectedStandard: ColorStandard,
    onStandardSelected: (ColorStandard) -> Unit,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    bare: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            SegmentedToggleRow(
                options = ColorStandard.entries.map { standard ->
                    SegmentedOption(
                        label = stringResource(standard.labelRes),
                        icon = when (standard) {
                            ColorStandard.MD3_2021 -> Icons.Default.Build
                            ColorStandard.M3E_2025 -> Icons.Default.AutoAwesome
                        },
                        key = standard.name,
                    )
                },
                selectedIndex = ColorStandard.entries.indexOf(selectedStandard),
                onOptionSelected = { index -> onStandardSelected(ColorStandard.entries[index]) },
            )
        }
    }

    if (bare) content() else ExpressiveCard(modifier = modifier, flat = flat) { content() }
}

// ─── Color Style (9 PaletteStyle options) ────────────────────────────────────

@Composable
fun ColorStylePicker(
    selectedStyle: ColorStyle,
    onStyleSelected: (ColorStyle) -> Unit,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    bare: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            items(ColorStyle.entries, key = { it.name }) { style ->
                StyleSegment(
                    label = stringResource(style.labelRes),
                    isSelected = selectedStyle == style,
                    onClick = { onStyleSelected(style) },
                )
            }
        }
    }

    if (bare) content() else ExpressiveCard(modifier = modifier, flat = flat) { content() }
}

// ─── Segmented Toggle Row (reusable) ─────────────────────────────────────────

data class SegmentedOption(
    val label: String,
    val icon: ImageVector,
    val key: String,
)

@Composable
private fun SegmentedToggleRow(
    options: List<SegmentedOption>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, option ->
            SegmentedItem(
                icon = option.icon,
                label = option.label,
                isSelected = index == selectedIndex,
                onClick = { onOptionSelected(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentedItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "segItem",
    )

    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow

    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(0.dp),
            tint = contentColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun StyleSegment(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "styleSeg",
    )

    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerLow

    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            maxLines = 1,
        )
    }
}
