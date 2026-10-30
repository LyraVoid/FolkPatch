package me.bmax.apatch.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens
import me.bmax.apatch.ui.component.folk.folkIconColor
import me.bmax.apatch.ui.component.folk.folkPreferenceSummaryStyle
import me.bmax.apatch.ui.component.folk.folkPreferenceTitleStyle
import me.bmax.apatch.ui.component.folk.folkPreferenceValueStyle
import me.bmax.apatch.ui.component.folk.folkSupportingColor

@Composable
fun SegmentedSliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    valueText: String? = null,
    valueFormat: (Float) -> String = { "${(it * 100).toInt()}%" },
    enabled: Boolean = true,
    onValueChangeFinished: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = FolkSettingsDimens.ItemHorizontalPadding,
                end = FolkSettingsDimens.ItemEndPadding,
                top = FolkSettingsDimens.ItemVerticalPadding,
                bottom = FolkSettingsDimens.ItemVerticalPadding,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = folkIconColor(enabled),
                    modifier = Modifier.size(FolkSettingsDimens.IconSize),
                )
                Spacer(Modifier.width(FolkSettingsDimens.IconSpacing))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = folkPreferenceTitleStyle(),
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
                if (summary != null) {
                    Spacer(Modifier.height(FolkSettingsDimens.TitleSummarySpacing))
                    Text(
                        text = summary,
                        style = folkPreferenceSummaryStyle(),
                        color = folkSupportingColor(),
                    )
                }
            }
            Spacer(Modifier.width(FolkSettingsDimens.TrailingSpacing))
            Text(
                text = valueText ?: valueFormat(value),
                style = folkPreferenceValueStyle(),
                color = folkSupportingColor(),
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
