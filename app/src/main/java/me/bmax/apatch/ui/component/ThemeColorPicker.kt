package me.bmax.apatch.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import me.bmax.apatch.R
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember

data class ThemeColorOption(
    val key: String,
    @StringRes val labelId: Int,
    val lightPrimary: Color,
    val darkPrimary: Color,
)

val themeColorOptions = listOf(
    ThemeColorOption("indigo", R.string.indigo_theme, Color(0xFF4355B9), Color(0xFFBAC3FF)),
    ThemeColorOption("blue", R.string.blue_theme, Color(0xFF0061A4), Color(0xFF9ECAFF)),
    ThemeColorOption("light_blue", R.string.light_blue_theme, Color(0xFF006493), Color(0xFF95CCFF)),
    ThemeColorOption("cyan", R.string.cyan_theme, Color(0xFF006876), Color(0xFF4FD8EB)),
    ThemeColorOption("teal", R.string.teal_theme, Color(0xFF006A60), Color(0xFF52DEC9)),
    ThemeColorOption("green", R.string.green_theme, Color(0xFF006E1A), Color(0xFF74DD69)),
    ThemeColorOption("light_green", R.string.light_green_theme, Color(0xFF006C48), Color(0xFF6CFAAF)),
    ThemeColorOption("lime", R.string.lime_theme, Color(0xFF5B6300), Color(0xFFC4CA2E)),
    ThemeColorOption("yellow", R.string.yellow_theme, Color(0xFF695F00), Color(0xFFE4C34D)),
    ThemeColorOption("amber", R.string.amber_theme, Color(0xFF785900), Color(0xFFF0C14A)),
    ThemeColorOption("orange", R.string.orange_theme, Color(0xFF8B5000), Color(0xFFFFB870)),
    ThemeColorOption("deep_orange", R.string.deep_orange_theme, Color(0xFFB02F00), Color(0xFFFFB59E)),
    ThemeColorOption("red", R.string.red_theme, Color(0xFFBB1614), Color(0xFFFFB4AA)),
    ThemeColorOption("pink", R.string.pink_theme, Color(0xFFBC004B), Color(0xFFFFB1C3)),
    ThemeColorOption("purple", R.string.purple_theme, Color(0xFF9A25AE), Color(0xFFEFB0FF)),
    ThemeColorOption("deep_purple", R.string.deep_purple_theme, Color(0xFF6F43C0), Color(0xFFD4BBFF)),
    ThemeColorOption("brown", R.string.brown_theme, Color(0xFF9A4522), Color(0xFFFFB594)),
    ThemeColorOption("blue_grey", R.string.blue_grey_theme, Color(0xFF00668A), Color(0xFF5EF0FF)),
    ThemeColorOption("sakura", R.string.sakura_theme, Color(0xFF9B404F), Color(0xFFFFB1BD)),
    ThemeColorOption("ink_wash", R.string.ink_wash_theme, Color(0xFF424242), Color(0xFFC6C6C6)),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeColorPicker(
    selectedColorKey: String,
    onColorSelected: (String) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    isDynamicColorSupported: Boolean = false,
    isDynamicColorEnabled: Boolean = false,
    onDynamicColorSelected: () -> Unit = {},
    bare: Boolean = false,
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.theme_color),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(start = 8.dp),
            ) {
                if (isDynamicColorSupported) {
                    item(key = "system_dynamic") {
                        ThemeColorCircle(
                            label = stringResource(R.string.theme_system),
                            displayColor = MaterialTheme.colorScheme.primary,
                            isSelected = isDynamicColorEnabled,
                            isDynamic = true,
                            onClick = onDynamicColorSelected,
                            isDarkTheme = isDarkTheme,
                        )
                    }
                }

                items(themeColorOptions, key = { it.key }) { theme ->
                    ThemeColorCircle(
                        label = stringResource(theme.labelId),
                        displayColor = if (isDarkTheme) theme.darkPrimary else theme.lightPrimary,
                        isSelected = !isDynamicColorEnabled && selectedColorKey == theme.key,
                        isDynamic = false,
                        onClick = { onColorSelected(theme.key) },
                        isDarkTheme = isDarkTheme,
                    )
                }
            }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeColorCircle(
    label: String,
    displayColor: Color,
    isSelected: Boolean,
    isDynamic: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "colorScale",
    )

    val shape = if (isSelected) MaterialShapes.Cookie9Sided.toShape() else CircleShape

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(scale)
                .clip(shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = true,
                        color = if (isDarkTheme) { 
                            Color.Black.copy(alpha = 0.50f)
                        } else {
                            Color.White.copy(alpha = 0.90f)
                        }
                    )
                ) { onClick() }
                .background(color = displayColor)
                .then(
                    if (isDynamic) {
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.outline,
                            shape,
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
