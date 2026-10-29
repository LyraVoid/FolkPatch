package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.theme.tokens.LocalFolkWallpaperTokens


/**
 * 模块按钮配置数据类
 */
data class ModuleButtonConfig(
    val icon: ImageVector,
    val text: String,
    val contentDescription: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val colors: ButtonColors? = null
)

/**
 * 自适应模块按钮行
 * 当空间不足时，从左边开始将按钮转为纯图标模式
 * 
 * @param buttons 左侧按钮列表
 * @param trailingButton 右侧固定按钮（如删除按钮）
 * @param simpleListBottomBar 是否为简约模式
 * @param spacing 按钮间距
 * @param opacity 背景透明度
 */
@Composable
fun AdaptiveModuleButtonRow(
    buttons: List<ModuleButtonConfig>,
    trailingButton: ModuleButtonConfig? = null,
    simpleListBottomBar: Boolean,
    spacing: Int = 8,
    opacity: Float = 1f
) {
    if (simpleListBottomBar) {
        // 简约模式：所有按钮都显示为纯图标
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            buttons.forEach { config ->
                FilledTonalButton(
                    onClick = config.onClick,
                    enabled = config.enabled,
                    contentPadding = PaddingValues(12.dp),
                    colors = config.colors ?: moduleButtonColors(opacity)
                ) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = config.contentDescription,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            trailingButton?.let { config ->
                FilledTonalButton(
                    onClick = config.onClick,
                    enabled = config.enabled,
                    contentPadding = PaddingValues(12.dp),
                    colors = config.colors ?: moduleButtonColors(opacity)
                ) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = config.contentDescription,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    } else {
        // 非简约模式：自适应显示文字
        AdaptiveButtonRowLayout(
            buttons = buttons,
            trailingButton = trailingButton,
            spacing = spacing,
            opacity = opacity
        )
    }
}

/**
 * 使用 SubcomposeLayout 实现的自适应按钮行布局
 * 自动测量并决定哪些按钮需要切换到纯图标模式
 */
@Composable
private fun AdaptiveButtonRowLayout(
    buttons: List<ModuleButtonConfig>,
    trailingButton: ModuleButtonConfig?,
    spacing: Int,
    opacity: Float
) {
    var iconOnlyCount by remember(buttons.size) { mutableIntStateOf(0) }
    
    SubcomposeLayout(
        modifier = Modifier.fillMaxWidth()
    ) { constraints ->
        val spacingPx = spacing.dp.roundToPx()
        val availableWidth = constraints.maxWidth
        
        // 1. 测量所有按钮在带文字状态下的宽度
        val buttonWidthsWithText = buttons.mapIndexed { index, config ->
            val placeable = subcompose("measure_text_$index") {
                ModuleActionButton(
                    config = config,
                    iconOnly = false,
                    opacity = opacity
                )
            }.first().measure(Constraints())
            placeable.width
        }
        
        // 2. 测量所有按钮在纯图标状态下的宽度
        val buttonWidthsIconOnly = buttons.mapIndexed { index, config ->
            val placeable = subcompose("measure_icon_$index") {
                ModuleActionButton(
                    config = config,
                    iconOnly = true,
                    opacity = opacity
                )
            }.first().measure(Constraints())
            placeable.width
        }
        
        // 3. 测量尾部按钮宽度（如删除按钮，优先保留文字）
        val trailingWidthWithText = trailingButton?.let { config ->
            subcompose("measure_trailing_text") {
                ModuleActionButton(
                    config = config,
                    iconOnly = false,
                    opacity = opacity
                )
            }.first().measure(Constraints()).width
        } ?: 0
        
        val trailingWidthIconOnly = trailingButton?.let { config ->
            subcompose("measure_trailing_icon") {
                ModuleActionButton(
                    config = config,
                    iconOnly = true,
                    opacity = opacity
                )
            }.first().measure(Constraints()).width
        } ?: 0
        
        // 4. 计算需要多少个按钮切换到纯图标模式
        val totalSpacing = if (buttons.isNotEmpty() || trailingButton != null) {
            (buttons.size + (if (trailingButton != null) 1 else 0) - 1).coerceAtLeast(0) * spacingPx
        } else 0
        
        // 计算最小所需宽度（所有按钮都是纯图标 + Spacer最小宽度）
        val minSpacerWidth = 8.dp.roundToPx()
        
        // 从左边开始尝试将按钮转为纯图标，直到总宽度合适
        var currentIconOnlyCount = 0
        for (i in 0..buttons.size) {
            val leftButtonsWidth = buttons.indices.sumOf { index ->
                if (index < i) buttonWidthsIconOnly[index] else buttonWidthsWithText[index]
            }
            
            // 检查尾部按钮是否需要切换为纯图标
            val trailingWidth = if (trailingButton != null) {
                // 先尝试保留尾部按钮的文字
                val totalWithTrailingText = leftButtonsWidth + trailingWidthWithText + totalSpacing + minSpacerWidth
                if (totalWithTrailingText <= availableWidth) {
                    trailingWidthWithText
                } else {
                    trailingWidthIconOnly
                }
            } else 0
            
            val totalWidth = leftButtonsWidth + trailingWidth + totalSpacing + minSpacerWidth
            
            if (totalWidth <= availableWidth) {
                currentIconOnlyCount = i
                break
            }
            currentIconOnlyCount = i + 1
        }
        
        // 限制在按钮数量范围内
        iconOnlyCount = currentIconOnlyCount.coerceAtMost(buttons.size)
        
        // 5. 确定尾部按钮是否需要纯图标
        val leftButtonsWidth = buttons.indices.sumOf { index ->
            if (index < iconOnlyCount) buttonWidthsIconOnly[index] else buttonWidthsWithText[index]
        }
        val trailingIconOnly = if (trailingButton != null) {
            val totalWithText = leftButtonsWidth + trailingWidthWithText + totalSpacing + minSpacerWidth
            totalWithText > availableWidth
        } else false
        
        // 6. 渲染最终的按钮
        val finalPlaceables = buttons.mapIndexed { index, config ->
            subcompose("button_$index") {
                ModuleActionButton(
                    config = config,
                    iconOnly = index < iconOnlyCount,
                    opacity = opacity
                )
            }.first().measure(Constraints())
        }
        
        val trailingPlaceable = trailingButton?.let { config ->
            subcompose("trailing_button") {
                ModuleActionButton(
                    config = config,
                    iconOnly = trailingIconOnly,
                    opacity = opacity
                )
            }.first().measure(Constraints())
        }
        
        // 7. 布局
        val layoutHeight = maxOf(
            finalPlaceables.maxOfOrNull { it.height } ?: 0,
            trailingPlaceable?.height ?: 0
        )

        layout(availableWidth, layoutHeight) {
            var xPosition = 0
            
            finalPlaceables.forEach { placeable ->
                placeable.placeRelative(xPosition, 0)
                xPosition += placeable.width + spacingPx
            }
            
            // 放置尾部按钮（右对齐）
            trailingPlaceable?.let { placeable ->
                placeable.placeRelative(availableWidth - placeable.width, 0)
            }
        }
    }
}

@Composable
private fun moduleButtonColors(opacity: Float): ButtonColors {
    val wallpaper = LocalFolkWallpaperTokens.current
    return if (wallpaper != null) {
        ButtonDefaults.filledTonalButtonColors(
            containerColor = wallpaper.selection.fill,
            contentColor = wallpaper.selection.contentColor,
        )
    } else {
        ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                alpha = (opacity + 0.3f).coerceAtMost(1f),
            ),
        )
    }
}

/** 模块操作按钮 */
@Composable
fun ModuleActionButton(
    config: ModuleButtonConfig,
    iconOnly: Boolean,
    opacity: Float,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = config.onClick,
        enabled = config.enabled,
        contentPadding = if (iconOnly) PaddingValues(12.dp) else ButtonDefaults.TextButtonContentPadding,
        modifier = if (iconOnly) modifier else modifier,
        colors = config.colors ?: moduleButtonColors(opacity)
    ) {
        Icon(
            imageVector = config.icon,
            contentDescription = config.contentDescription,
            modifier = Modifier.size(20.dp)
        )
        if (!iconOnly) {
            Spacer(Modifier.width(8.dp))
            Text(config.text)
        }
    }
}
