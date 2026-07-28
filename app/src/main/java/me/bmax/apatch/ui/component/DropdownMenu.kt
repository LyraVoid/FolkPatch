package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.bmax.apatch.ui.theme.BackgroundConfig

@Composable
fun ProvideMenuShape(
    value: CornerBasedShape = RoundedCornerShape(8.dp), content: @Composable () -> Unit
) = MaterialTheme(
    shapes = MaterialTheme.shapes.copy(extraSmall = value), content = content
)

@Composable
fun WallpaperAwareDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(28.dp),
    containerColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    // 在壁纸模式下移除阴影效果
    if (BackgroundConfig.isCustomBackgroundEnabled) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier.shadow(
                elevation = 0.dp,
                shape = shape,
                ambientColor = Color.Transparent,
                spotColor = Color.Transparent
            ),
            shape = shape,
            tonalElevation = 0.dp, // 在壁纸模式下不使用阴影
            shadowElevation = 0.dp, // 确保没有阴影
            containerColor = containerColor.copy(alpha = 0.95f), // 略微透明，与壁纸更好地融合
            content = content
        )
    } else {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            shape = shape,
            tonalElevation = 3.dp, // 默认阴影
            shadowElevation = 3.dp, // 默认阴影
            containerColor = containerColor,
            content = content
        )
    }
}

@Composable
fun WallpaperAwareDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    if (BackgroundConfig.isCustomBackgroundEnabled) {
        DropdownMenuItem(
            text = text,
            onClick = onClick,
            modifier = modifier.shadow(
                elevation = 0.dp,
                ambientColor = Color.Transparent,
                spotColor = Color.Transparent
            ),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            enabled = enabled
        )
    } else {
        DropdownMenuItem(
            text = text,
            onClick = onClick,
            modifier = modifier,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            enabled = enabled
        )
    }
}