package me.bmax.apatch.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import me.bmax.apatch.APApplication

object SwitchIconState {
    var showIcon by mutableStateOf(
        APApplication.sharedPreferences.getBoolean("show_switch_icon", true)
    )
}

@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        thumbContent = if (SwitchIconState.showIcon) {
            {
                if (checked) {
                    ThumbIcon(Icons.Filled.Check, MaterialTheme.colorScheme.primary)
                } else {
                    ThumbIcon(Icons.Filled.Close, MaterialTheme.colorScheme.surfaceContainerHighest)
                }
            }
        } else null,
    )
}

@Composable
private fun ThumbIcon(icon: ImageVector, tint: androidx.compose.ui.graphics.Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(SwitchDefaults.IconSize),
    )
}
