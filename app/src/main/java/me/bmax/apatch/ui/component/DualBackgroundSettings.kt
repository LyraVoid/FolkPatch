package me.bmax.apatch.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.bmax.apatch.R

/**
 * Common controls for a wallpaper that can use separate light/dark dim and
 * opacity values.
 */
@Composable
fun DualBackgroundSettings(
    flat: Boolean,
    dualDimEnabled: Boolean,
    onDualDimEnabledChange: (Boolean) -> Unit,
    dim: Float,
    onDimChange: (Float) -> Unit,
    dayDim: Float,
    onDayDimChange: (Float) -> Unit,
    nightDim: Float,
    onNightDimChange: (Float) -> Unit,
    dualOpacityEnabled: Boolean,
    onDualOpacityEnabledChange: (Boolean) -> Unit,
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    dayOpacity: Float,
    onDayOpacityChange: (Float) -> Unit,
    nightOpacity: Float,
    onNightOpacityChange: (Float) -> Unit,
    save: () -> Unit,
    keyPrefix: String,
    dualDimTitle: String = stringResource(R.string.settings_custom_background_dual_dim),
    dualDimDescription: String = stringResource(R.string.settings_custom_background_dual_dim_desc),
    opacityTitle: String = stringResource(R.string.settings_custom_background_opacity),
    dayDimTitle: String = stringResource(R.string.settings_custom_background_day_dim),
    nightDimTitle: String = stringResource(R.string.settings_custom_background_night_dim),
    dualOpacityTitle: String = stringResource(R.string.settings_grid_working_card_dual_opacity),
    dualOpacityDescription: String = stringResource(R.string.settings_grid_working_card_dual_opacity_desc),
    dayOpacityTitle: String = stringResource(R.string.settings_grid_working_card_day_opacity),
    nightOpacityTitle: String = stringResource(R.string.settings_grid_working_card_night_opacity),
    showDualDim: Boolean = true,
) {
    if (showDualDim) {
        ToggleSettingCard(
            flat = flat,
            icon = Icons.Filled.Contrast,
            title = dualDimTitle,
            description = dualDimDescription,
            checked = dualDimEnabled,
            onCheckedChange = { onDualDimEnabledChange(it); save() },
        )
    }
    ToggleSettingCard(
        flat = flat,
        icon = Icons.Filled.Contrast,
        title = dualOpacityTitle,
        description = dualOpacityDescription,
        checked = dualOpacityEnabled,
        onCheckedChange = { onDualOpacityEnabledChange(it); save() },
    )
    if (dualOpacityEnabled) {
        SliderSettingCard(flat = flat, title = dayOpacityTitle, value = dayOpacity, onValueChange = onDayOpacityChange, onValueChangeFinished = save)
        SliderSettingCard(flat = flat, title = nightOpacityTitle, value = nightOpacity, onValueChange = onNightOpacityChange, onValueChangeFinished = save)
    } else {
        SliderSettingCard(flat = flat, title = opacityTitle, value = opacity, onValueChange = onOpacityChange, onValueChangeFinished = save)
    }

    if (showDualDim && dualDimEnabled) {
        SliderSettingCard(flat = flat, title = dayDimTitle, value = dayDim, onValueChange = onDayDimChange, onValueChangeFinished = save)
        SliderSettingCard(flat = flat, title = nightDimTitle, value = nightDim, onValueChange = onNightDimChange, onValueChangeFinished = save)
    } else {
        SliderSettingCard(flat = flat, title = stringResource(R.string.settings_custom_background_dim), value = dim, onValueChange = onDimChange, onValueChangeFinished = save)
    }
}
