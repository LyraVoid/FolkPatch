package me.bmax.apatch.ui.screen.settings.appearance

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.compose.material.icons.automirrored.outlined.Segment
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ColorGenerationModeSelector
import me.bmax.apatch.ui.component.ColorStandardSelector
import me.bmax.apatch.ui.component.ColorStylePicker
import me.bmax.apatch.ui.component.ExpressiveSwitch
import me.bmax.apatch.ui.component.SliderStyleConfig
import me.bmax.apatch.ui.component.SwitchIconState
import me.bmax.apatch.ui.component.ThemeColorPicker
import me.bmax.apatch.ui.component.ThemeMode
import me.bmax.apatch.ui.component.ThemeModeSelector
import me.bmax.apatch.ui.component.SegmentedColumn
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.ColorGenerationMode
import me.bmax.apatch.ui.theme.ColorStandard
import me.bmax.apatch.ui.theme.ColorStyle
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import me.bmax.apatch.ui.component.folk.folkPressScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceNightModeSection(
    flat: Boolean,
    highlightKey: String?,
    isNightModeSupported: Boolean,
    isDynamicColorSupport: Boolean,
    isDarkTheme: Boolean,
    themeMode: ThemeMode,
    customColorScheme: String,
    useSystemDynamicColor: Boolean,
    colorGenerationMode: ColorGenerationMode,
    colorStandard: ColorStandard,
    colorStyle: ColorStyle,
    amoledTheme: Boolean,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onColorSelected: (String) -> Unit,
    onDynamicColorSelected: () -> Unit,
    onGenerationModeSelected: (ColorGenerationMode) -> Unit,
    onStandardSelected: (ColorStandard) -> Unit,
    onStyleSelected: (ColorStyle) -> Unit,
    onAmoledChange: (Boolean) -> Unit,
) {
    val prefs = APApplication.sharedPreferences

    SegmentedColumn(title = stringResource(R.string.settings_appearance_night_mode), flat = flat, highlightKey = highlightKey) {
        if (isNightModeSupported) {
            item(key = "appearance_theme_mode") {
                ThemeModeSelector(
                    selectedMode = themeMode,
                    onModeSelected = onThemeModeSelected,
                    flat = flat,
                    bare = true,
                )
            }
        }

        item(key = "appearance_theme_color") {
            ThemeColorPicker(
                selectedColorKey = customColorScheme,
                onColorSelected = onColorSelected,
                isDarkTheme = isDarkTheme,
                flat = flat,
                isDynamicColorSupported = isDynamicColorSupport,
                isDynamicColorEnabled = useSystemDynamicColor,
                onDynamicColorSelected = onDynamicColorSelected,
                bare = true,
            )
        }

        // Color generation mode & style pickers
        item(key = "appearance_color_generation_mode") {
            ColorGenerationModeSelector(
                selectedMode = colorGenerationMode,
                onModeSelected = onGenerationModeSelected,
                flat = flat,
                bare = true,
            )
        }

        if (colorGenerationMode == ColorGenerationMode.CUSTOM) {
            item(key = "appearance_color_standard") {
                ColorStandardSelector(
                    selectedStandard = colorStandard,
                    onStandardSelected = onStandardSelected,
                    flat = flat,
                    bare = true,
                )
            }

            item(key = "appearance_color_style") {
                ColorStylePicker(
                    selectedStyle = colorStyle,
                    onStyleSelected = onStyleSelected,
                    flat = flat,
                    bare = true,
                )
            }

        }

        if (isDarkTheme) {
            item(key = "appearance_amoled_theme") {
                val isWallpaperEnabled = BackgroundConfig.isCustomBackgroundEnabled
                val amoledInteractionSource = remember { MutableInteractionSource() }
                val amoledHaptics = LocalHapticFeedback.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .folkPressScale(amoledInteractionSource, !isWallpaperEnabled)
                        .toggleable(
                            value = amoledTheme,
                            onValueChange = {
                                if (!isWallpaperEnabled) {
                                    amoledHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAmoledChange(it)
                                }
                            },
                            role = Role.Switch,
                            enabled = !isWallpaperEnabled,
                            interactionSource = amoledInteractionSource,
                            indication = null,
                        )
                        .padding(horizontal = FolkDimens.Size16, vertical = FolkDimens.Size12),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Outlined.DarkMode, contentDescription = null, tint = if (!isWallpaperEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f), modifier = Modifier.size(FolkDimens.Size24))
                    Spacer(Modifier.width(FolkDimens.Size16))
                    Column(modifier = Modifier.weight(1f).padding(end = FolkDimens.Size16)) {
                        Text(
                            text = stringResource(R.string.settings_amoled_theme),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (!isWallpaperEnabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                        Spacer(Modifier.height(FolkDimens.Size4))
                        Text(
                            text = stringResource(R.string.settings_amoled_theme_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!isWallpaperEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        )
                    }
                    ExpressiveSwitch(
                        checked = amoledTheme,
                        onCheckedChange = null,
                        enabled = !isWallpaperEnabled,
                    )
                }
            }
        }

        item(key = "appearance_switch_icon") {
            var showSwitchIcon by remember { mutableStateOf(SwitchIconState.showIcon) }
            SegmentedSwitchItem(
                icon = Icons.Outlined.ToggleOn,
                title = stringResource(R.string.settings_switch_icon),
                summary = stringResource(R.string.settings_switch_icon_desc),
                checked = showSwitchIcon,
                onCheckedChange = {
                    showSwitchIcon = it
                    SwitchIconState.showIcon = it
                    prefs.edit().putBoolean("show_switch_icon", it).apply()
                },
            )
        }

        item(key = "appearance_discrete_slider") {
            var isDiscreteSlider by remember { mutableStateOf(SliderStyleConfig.isDiscrete) }
            SegmentedSwitchItem(
                icon = Icons.AutoMirrored.Outlined.Segment,
                title = stringResource(R.string.settings_discrete_slider),
                summary = stringResource(R.string.settings_discrete_slider_desc),
                checked = isDiscreteSlider,
                onCheckedChange = {
                    isDiscreteSlider = it
                    SliderStyleConfig.isDiscrete = it
                    prefs.edit().putBoolean("discrete_slider", it).apply()
                },
            )
        }
    }
}
