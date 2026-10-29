package me.bmax.apatch.ui.screen.settings.appearance

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.TonalCard
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.util.BottomBarIconConfig
import me.bmax.apatch.util.ui.FloatingBarConfig
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import me.bmax.apatch.ui.component.folk.FolkSettingsDimens
import me.bmax.apatch.ui.component.SegmentedSliderItem
import me.bmax.apatch.ui.component.SegmentedSwitchItem
import me.bmax.apatch.ui.component.SegmentedValueItem
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import me.bmax.apatch.ui.component.SegmentedColumnScope
import me.bmax.apatch.ui.component.folk.ExpressiveDialog
import me.bmax.apatch.ui.component.SegmentedSelectableItem

fun SegmentedColumnScope.appearanceNavItems(
    kPatchReady: Boolean,
    isFloatingNav: Boolean,
    flat: Boolean,
    context: Context,
    prefs: SharedPreferences,
    showNavApm: Boolean,
    onShowNavApmChange: (Boolean) -> Unit,
    showNavKpm: Boolean,
    onShowNavKpmChange: (Boolean) -> Unit,
    showNavSuperUser: Boolean,
    onShowNavSuperUserChange: (Boolean) -> Unit,
    navSchemeLabel: String,
    onShowNavSchemeDialog: () -> Unit,
    floatingAutoHide: Boolean,
    onFloatingAutoHideChange: (Boolean) -> Unit,
    floatingSwipeHide: Boolean,
    onFloatingSwipeHideChange: (Boolean) -> Unit,
) {
    if (kPatchReady) {
        item(key = "appearance_nav_layout") {
            var expanded by remember { mutableStateOf(false) }
            val rotationState by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                label = "ArrowRotation",
            )
            TonalCard(flat = flat, onClick = { expanded = !expanded }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = FolkSettingsDimens.ItemHorizontalPadding,
                            end = FolkSettingsDimens.ItemEndPadding,
                            top = FolkDimens.Size16,
                            bottom = FolkDimens.Size16,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Outlined.Navigation, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(FolkDimens.Size24))
                    Spacer(Modifier.width(FolkDimens.Size16))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_nav_layout_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(id = R.string.settings_nav_layout_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(FolkSettingsDimens.ChevronSize)
                            .rotate(rotationState),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()) + expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()),
                exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
            ) {
                Column(modifier = Modifier.padding(start = FolkDimens.Size16, top = FolkDimens.Size8)) {
                    me.bmax.apatch.ui.component.SegmentedCheckboxItem(
                        icon = null,
                        title = stringResource(id = R.string.settings_show_apm),
                        summary = null,
                        checked = showNavApm,
                        onCheckedChange = {
                            onShowNavApmChange(it)
                            prefs.edit().putBoolean("show_nav_apm", it).apply()
                        },
                    )
                    me.bmax.apatch.ui.component.SegmentedCheckboxItem(
                        icon = null,
                        title = stringResource(id = R.string.settings_show_kpm),
                        summary = null,
                        checked = showNavKpm,
                        onCheckedChange = {
                            onShowNavKpmChange(it)
                            prefs.edit().putBoolean("show_nav_kpm", it).apply()
                        },
                    )
                    me.bmax.apatch.ui.component.SegmentedCheckboxItem(
                        icon = null,
                        title = stringResource(id = R.string.settings_show_superuser),
                        summary = null,
                        checked = showNavSuperUser,
                        onCheckedChange = {
                            onShowNavSuperUserChange(it)
                            prefs.edit().putBoolean("show_nav_superuser", it).apply()
                        },
                    )
                }
            }
        }
    }

    item(key = "appearance_nav_scheme") {
        SegmentedValueItem(
        icon = Icons.Outlined.Menu,
        title = stringResource(id = R.string.settings_nav_scheme),
        summary = navSchemeLabel,
        onClick = { onShowNavSchemeDialog() },
    )
    }

    if (isFloatingNav) {
        item(key = "appearance_floating_bar_style") {
            var style by remember { mutableStateOf(prefs.getString("floating_bar_style", FloatingBarConfig.DEFAULT_STYLE)) }
            var showStyleDialog by remember { mutableStateOf(false) }
            DisposableEffect(prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
                    if (key == "floating_bar_style") style = preferences.getString(key, FloatingBarConfig.DEFAULT_STYLE)
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            SegmentedValueItem(
                icon = Icons.Outlined.ViewStream,
                title = stringResource(R.string.settings_floating_bar_style),
                summary = stringResource(if (style == FloatingBarConfig.STYLE_DRAWER) R.string.settings_bottom_bar_drawer
                    else R.string.settings_bottom_bar_standard),
                onClick = { showStyleDialog = true },
            )
            if (showStyleDialog) {
                ExpressiveDialog(onDismissRequest = { showStyleDialog = false }) {
                    Column(Modifier.padding(FolkDimens.Size24)) {
                        Text(stringResource(R.string.settings_floating_bar_style),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = FolkDimens.Size16))
                        listOf(FloatingBarConfig.STYLE_STANDARD to R.string.settings_bottom_bar_standard,
                            FloatingBarConfig.STYLE_DRAWER to R.string.settings_bottom_bar_drawer).forEach { (value, title) ->
                            SegmentedSelectableItem(
                                title = stringResource(title),
                                selected = style == value,
                                onClick = {
                                    prefs.edit { putString("floating_bar_style", value) }
                                    showStyleDialog = false
                                },
                            )
                        }
                    }
                }
            }
        }
        item(key = "appearance_navbar_glass") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.AutoAwesome,
                title = stringResource(id = R.string.settings_navbar_glass_effect),
                summary = stringResource(id = R.string.settings_navbar_glass_effect_summary),
                checked = BackgroundConfig.isNavBarGlassEnabled,
                onCheckedChange = {
                    BackgroundConfig.setNavBarGlassEnabledState(it)
                    BackgroundConfig.save(context)
                },
            )
        }

        if (BackgroundConfig.isNavBarGlassEnabled) {
            item(key = "appearance_navbar_glass_blur") {
                SegmentedSliderItem(
                    title = stringResource(id = R.string.settings_navbar_glass_blur_strength),
                    value = BackgroundConfig.navBarGlassBlurStrength,
                    onValueChange = { BackgroundConfig.setNavBarGlassBlurStrengthValue(it) },
                    onValueChangeFinished = { BackgroundConfig.save(context) },
                )
            }

            item(key = "appearance_navbar_glass_transparency") {
                SegmentedSliderItem(
                    title = stringResource(id = R.string.settings_navbar_glass_transparency),
                    value = BackgroundConfig.navBarGlassTransparency,
                    onValueChange = { BackgroundConfig.setNavBarGlassTransparencyValue(it) },
                    onValueChangeFinished = { BackgroundConfig.save(context) },
                )
            }

            item(key = "appearance_navbar_glass_highlight") {
                SegmentedSliderItem(
                    title = stringResource(id = R.string.settings_navbar_glass_highlight_strength),
                    value = BackgroundConfig.navBarGlassHighlightStrength,
                    onValueChange = { BackgroundConfig.setNavBarGlassHighlightStrengthValue(it) },
                    onValueChangeFinished = { BackgroundConfig.save(context) },
                )
            }

            item(key = "appearance_navbar_glass_specular") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.LensBlur,
                    title = stringResource(id = R.string.settings_navbar_glass_specular),
                    summary = stringResource(id = R.string.settings_navbar_glass_specular_summary),
                    checked = BackgroundConfig.isNavBarGlassSpecularEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setNavBarGlassSpecularEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
            }

            item(key = "appearance_navbar_glass_glow") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.Grain,
                    title = stringResource(id = R.string.settings_navbar_glass_inner_glow),
                    summary = stringResource(id = R.string.settings_navbar_glass_inner_glow_summary),
                    checked = BackgroundConfig.isNavBarGlassInnerGlowEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setNavBarGlassInnerGlowEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
            }

            item(key = "appearance_navbar_glass_border") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.BorderStyle,
                    title = stringResource(id = R.string.settings_navbar_glass_border),
                    summary = stringResource(id = R.string.settings_navbar_glass_border_summary),
                    checked = BackgroundConfig.isNavBarGlassBorderEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setNavBarGlassBorderEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
            }
        }

        // ---- 紧凑圆角风格 ----
        // 仅非毛玻璃模式显示（毛玻璃已有独立的外观控制）
        if (!BackgroundConfig.isNavBarGlassEnabled) {
            item(key = "appearance_compact_rounded_bar") {
                SegmentedSwitchItem(
                    icon = Icons.Outlined.RoundedCorner,
                    title = stringResource(id = R.string.settings_compact_rounded_bar),
                    summary = stringResource(id = R.string.settings_compact_rounded_bar_summary),
                    checked = FloatingBarConfig.isCompactRoundedStyle,
                    onCheckedChange = { enabled ->
                        FloatingBarConfig.isCompactRoundedStyle = enabled
                        FloatingBarConfig.save(context)
                    },
                )
            }
        }

        item(key = "appearance_floating_auto_hide") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.VisibilityOff,
                title = stringResource(id = R.string.settings_floating_auto_hide),
                summary = stringResource(id = R.string.settings_floating_auto_hide_summary),
                checked = floatingAutoHide,
                onCheckedChange = {
                    onFloatingAutoHideChange(it)
                    prefs.edit().putBoolean("floating_auto_hide", it).apply()
                },
            )
        }

        item(key = "appearance_floating_swipe_hide") {
            SegmentedSwitchItem(
                icon = Icons.Outlined.Swipe,
                title = stringResource(id = R.string.settings_floating_swipe_hide),
                summary = stringResource(id = R.string.settings_floating_swipe_hide_summary),
                checked = floatingSwipeHide,
                onCheckedChange = {
                    onFloatingSwipeHideChange(it)
                    prefs.edit().putBoolean("floating_swipe_hide", it).apply()
                },
            )
        }
    }
}
