package me.bmax.apatch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 计算 Banner 图片底部渐变的 fading 颜色。
 * APM / KPM 模块卡片共用此逻辑，避免硬编码颜色分散在多处。
 */
@Composable
fun bannerFadeColor(): Color {
    val isDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    val isDynamic = colorScheme.primary != colorScheme.secondary
    return when {
        isDynamic -> colorScheme.surface
        isDark -> Color(0xFF222222)
        else -> Color.White
    }
}
