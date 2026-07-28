package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.bmax.apatch.APApplication

object SliderStyleConfig {
    var isDiscrete by mutableStateOf(
        APApplication.sharedPreferences.getBoolean("discrete_slider", true)
    )
}

@Composable
fun SliderSettingCard(
    flat: Boolean = false,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 19,
    valueFormat: (Float) -> String = { "${(it * 100).toInt()}%" },
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
) {
    ExpressiveCard(flat = flat) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = valueFormat(value),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = if (SliderStyleConfig.isDiscrete) steps else 0,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
