package me.bmax.apatch.ui.screen.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.content.edit
import kotlinx.coroutines.launch
import me.bmax.apatch.APApplication
import me.bmax.apatch.R
import me.bmax.apatch.ui.component.ExpressiveCard
import me.bmax.apatch.ui.component.FilePickerDialog
import me.bmax.apatch.ui.component.SectionHeader
import me.bmax.apatch.ui.component.ToggleSettingCard
import me.bmax.apatch.ui.component.ThemeColorPicker
import me.bmax.apatch.ui.component.ThemeMode
import me.bmax.apatch.ui.component.ThemeModeSelector
import me.bmax.apatch.ui.component.rememberConfirmDialog
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.BackgroundManager
import me.bmax.apatch.ui.theme.FontConfig
import me.bmax.apatch.ui.theme.ThemeManager
import me.bmax.apatch.ui.theme.refreshTheme
import me.bmax.apatch.util.PermissionUtils
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import androidx.compose.ui.draw.rotate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsContent(
    snackBarHost: SnackbarHostState,
    kPatchReady: Boolean,
    onNavigateToThemeStore: () -> Unit,
    onNavigateToApiMarketplace: () -> Unit,
    flat: Boolean = false,
) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()

    var pickingType by remember { mutableStateOf<String?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = when (pickingType) {
                    "home" -> BackgroundManager.saveAndApplyHomeBackground(context, it)
                    "kernel" -> BackgroundManager.saveAndApplyKernelBackground(context, it)
                    "superuser" -> BackgroundManager.saveAndApplySuperuserBackground(context, it)
                    "system" -> BackgroundManager.saveAndApplySystemModuleBackground(context, it)
                    "settings" -> BackgroundManager.saveAndApplySettingsBackground(context, it)
                    else -> BackgroundManager.saveAndApplyCustomBackground(context, it)
                }
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_background_saved))
                    refreshTheme.value = true
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_background_error))
                }
                pickingType = null
            }
        }
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = BackgroundManager.saveAndApplyVideoBackground(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_video_selected))
                    refreshTheme.value = true
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_background_error))
                }
            }
        }
    }

    val pickGridImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = BackgroundManager.saveAndApplyGridWorkingCardBackground(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_grid_working_card_background_saved))
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_grid_working_card_background_error))
                }
            }
        }
    }

    val pickFontLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = FontConfig.saveFontFile(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_font_saved))
                    refreshTheme.value = true
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_custom_font_error))
                }
            }
        }
    }

    val pickTitleImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                loadingDialog.show()
                val success = BackgroundManager.saveAndApplyTitleImage(context, it)
                loadingDialog.hide()
                if (success) {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_title_image_saved))
                    refreshTheme.value = true
                } else {
                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_title_image_error))
                }
            }
        }
    }

    var pendingExportMetadata by remember { mutableStateOf<ThemeManager.ThemeMetadata?>(null) }
    val showExportDialog = remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImportMetadata by remember { mutableStateOf<ThemeManager.ThemeMetadata?>(null) }
    val showImportDialog = remember { mutableStateOf(false) }
    val showFilePicker = remember { mutableStateOf(false) }

    val importThemeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                loadingDialog.show()
                val metadata = ThemeManager.readThemeMetadata(context, uri)
                loadingDialog.hide()
                if (metadata != null) {
                    pendingImportUri = uri
                    pendingImportMetadata = metadata
                    showImportDialog.value = true
                } else {
                    loadingDialog.show()
                    val success = ThemeManager.importTheme(context, uri)
                    loadingDialog.hide()
                    snackBarHost.showSnackbar(
                        message = if (success) context.getString(R.string.settings_theme_imported) else context.getString(R.string.settings_theme_import_failed)
                    )
                }
            }
        }
    }

    val isNightModeSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var nightModeFollowSys by remember { mutableStateOf(prefs.getBoolean("night_mode_follow_sys", true)) }
    var nightModeEnabled by remember { mutableStateOf(prefs.getBoolean("night_mode_enabled", true)) }
    val isDynamicColorSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var useSystemDynamicColor by remember { mutableStateOf(prefs.getBoolean("use_system_color_theme", false)) }
    var customFontEnabled by remember { mutableStateOf(FontConfig.isCustomFontEnabled) }

    val refreshThemeObserver by refreshTheme.observeAsState(false)

    var customColorScheme by remember { mutableStateOf(prefs.getString("custom_color", "indigo")) }

    var currentStyle by remember { mutableStateOf(prefs.getString("home_layout_style", "stats")) }

    if (refreshThemeObserver) {
        nightModeFollowSys = prefs.getBoolean("night_mode_follow_sys", false)
        nightModeEnabled = prefs.getBoolean("night_mode_enabled", true)
        useSystemDynamicColor = prefs.getBoolean("use_system_color_theme", true)
        customFontEnabled = FontConfig.isCustomFontEnabled
        customColorScheme = prefs.getString("custom_color", "indigo")
        currentStyle = prefs.getString("home_layout_style", "stats")
    }

    val isDarkTheme = if (nightModeFollowSys) isSystemInDarkTheme() else nightModeEnabled
    val themeMode = if (nightModeFollowSys) ThemeMode.SYSTEM else if (nightModeEnabled) ThemeMode.DARK else ThemeMode.LIGHT
    val isStatsLayout = currentStyle == "stats"
    var statsTopLayout by remember { mutableStateOf(prefs.getString("stats_top_layout", "list") ?: "list") }
    val statsTopLayoutListLabel = stringResource(id = R.string.settings_stats_top_layout_list)
    val statsTopLayoutGridLabel = stringResource(id = R.string.settings_stats_top_layout_grid)
    val statsTopLayoutValue = if (statsTopLayout == "grid") statsTopLayoutGridLabel else statsTopLayoutListLabel
    var showStatsTopLayoutDialog by remember { mutableStateOf(false) }

    var showNavApm by remember { mutableStateOf(prefs.getBoolean("show_nav_apm", true)) }
    var showNavKpm by remember { mutableStateOf(prefs.getBoolean("show_nav_kpm", true)) }
    var showNavSuperUser by remember { mutableStateOf(prefs.getBoolean("show_nav_superuser", true)) }

    var currentNavMode by remember { mutableStateOf(prefs.getString("nav_mode", "floating") ?: "floating") }
    val navSchemeLabel = when (currentNavMode) {
        "rail" -> stringResource(R.string.settings_nav_mode_rail)
        "bottom" -> stringResource(R.string.settings_nav_mode_bottom)
        "floating" -> stringResource(R.string.settings_nav_mode_floating)
        else -> stringResource(R.string.settings_nav_mode_auto)
    }
    var showNavSchemeDialog by remember { mutableStateOf(false) }

    val isFloatingNav = currentNavMode == "floating"
    var floatingAutoHide by remember { mutableStateOf(prefs.getBoolean("floating_auto_hide", true)) }
    var floatingSwipeHide by remember { mutableStateOf(prefs.getBoolean("floating_swipe_hide", true)) }

    val isKernelSuStyle = currentStyle == "kernelsu"
    val showGridCardSettings = isKernelSuStyle || (isStatsLayout && statsTopLayout == "grid")
    val isListStyle = currentStyle != "kernelsu" && currentStyle != "focus" && !(isStatsLayout && statsTopLayout == "grid")

    val badgeTextModes = listOf(
        stringResource(R.string.settings_custom_badge_text_full_half),
        stringResource(R.string.settings_custom_badge_text_lkm),
        stringResource(R.string.settings_custom_badge_text_gki),
        stringResource(R.string.settings_custom_badge_text_n_gki),
        stringResource(R.string.settings_custom_badge_text_oki),
        stringResource(R.string.settings_custom_badge_text_built_in)
    )
    val currentBadgeTextModeIndex = BackgroundConfig.customBadgeTextMode
    val currentBadgeTextMode = badgeTextModes.getOrElse(currentBadgeTextModeIndex) { badgeTextModes[0] }
    val showCustomBadgeTextDialog = remember { mutableStateOf(false) }

    val showHomeLayoutChooseDialog = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {

        SectionHeader(text = stringResource(R.string.settings_appearance_night_mode))
        Spacer(Modifier.height(8.dp))

        if (isNightModeSupported) {
            ThemeModeSelector(
                selectedMode = themeMode,
                onModeSelected = { mode ->
                    when (mode) {
                        ThemeMode.LIGHT -> {
                            nightModeFollowSys = false
                            nightModeEnabled = false
                            prefs.edit().putBoolean("night_mode_follow_sys", false).putBoolean("night_mode_enabled", false).apply()
                        }
                        ThemeMode.DARK -> {
                            nightModeFollowSys = false
                            nightModeEnabled = true
                            prefs.edit().putBoolean("night_mode_follow_sys", false).putBoolean("night_mode_enabled", true).apply()
                        }
                        ThemeMode.SYSTEM -> {
                            nightModeFollowSys = true
                            prefs.edit().putBoolean("night_mode_follow_sys", true).apply()
                        }
                    }
                    refreshTheme.value = true
                },
                flat = flat,
            )
            Spacer(Modifier.height(8.dp))
        }

        ThemeColorPicker(
            selectedColorKey = customColorScheme ?: "indigo",
            onColorSelected = { key ->
                prefs.edit().putString("custom_color", key).putBoolean("use_system_color_theme", false).apply()
                customColorScheme = key
                useSystemDynamicColor = false
                refreshTheme.value = true
            },
            isDarkTheme = isDarkTheme,
            flat = flat,
            isDynamicColorSupported = isDynamicColorSupport,
            isDynamicColorEnabled = useSystemDynamicColor,
            onDynamicColorSelected = {
                prefs.edit().putBoolean("use_system_color_theme", true).apply()
                useSystemDynamicColor = true
                refreshTheme.value = true
            },
        )
        Spacer(Modifier.height(8.dp))

        if (isDarkTheme) {
            var amoledTheme by remember { mutableStateOf(prefs.getBoolean("amoled_theme", false)) }
            val isWallpaperEnabled = BackgroundConfig.isCustomBackgroundEnabled

            val amoledRefreshObserver by refreshTheme.observeAsState(false)
            if (amoledRefreshObserver) {
                amoledTheme = prefs.getBoolean("amoled_theme", false)
            }

            ToggleSettingCard(
                title = stringResource(R.string.settings_amoled_theme),
                description = stringResource(R.string.settings_amoled_theme_desc),
                checked = amoledTheme,
                enabled = !isWallpaperEnabled,
                flat = flat,
                onCheckedChange = { enabled ->
                    amoledTheme = enabled
                    prefs.edit().putBoolean("amoled_theme", enabled).apply()
                    refreshTheme.value = true
                },
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(text = stringResource(R.string.settings_appearance_layout))
        Spacer(Modifier.height(8.dp))

        ExpressiveCard(flat = flat, onClick = { showHomeLayoutChooseDialog.value = true }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.settings_home_layout_style),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(homeLayoutStyleToString(currentStyle.toString())),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (isStatsLayout) {
            ExpressiveCard(flat = flat, onClick = { showStatsTopLayoutDialog = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_stats_top_layout),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = statsTopLayoutValue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (kPatchReady) {
            var expanded by remember { mutableStateOf(false) }
            val rotationState by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                label = "ArrowRotation",
            )
            ExpressiveCard(flat = flat, onClick = { expanded = !expanded }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_nav_layout_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(id = R.string.settings_nav_layout_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotationState),
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                    me.bmax.apatch.ui.component.CheckboxItem(
                        icon = null,
                        title = stringResource(id = R.string.settings_show_apm),
                        summary = null,
                        checked = showNavApm,
                        onCheckedChange = {
                            showNavApm = it
                            prefs.edit().putBoolean("show_nav_apm", it).apply()
                        },
                    )
                    me.bmax.apatch.ui.component.CheckboxItem(
                        icon = null,
                        title = stringResource(id = R.string.settings_show_kpm),
                        summary = null,
                        checked = showNavKpm,
                        onCheckedChange = {
                            showNavKpm = it
                            prefs.edit().putBoolean("show_nav_kpm", it).apply()
                        },
                    )
                    me.bmax.apatch.ui.component.CheckboxItem(
                        icon = null,
                        title = stringResource(id = R.string.settings_show_superuser),
                        summary = null,
                        checked = showNavSuperUser,
                        onCheckedChange = {
                            showNavSuperUser = it
                            prefs.edit().putBoolean("show_nav_superuser", it).apply()
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        ExpressiveCard(flat = flat, onClick = { showNavSchemeDialog = true }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.settings_nav_scheme),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = navSchemeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (isFloatingNav) {
            ToggleSettingCard(
                flat = flat,
                title = stringResource(id = R.string.settings_navbar_glass_effect),
                description = stringResource(id = R.string.settings_navbar_glass_effect_summary),
                checked = BackgroundConfig.isNavBarGlassEnabled,
                onCheckedChange = {
                    BackgroundConfig.setNavBarGlassEnabledState(it)
                    BackgroundConfig.save(context)
                },
            )
            Spacer(Modifier.height(8.dp))

            if (BackgroundConfig.isNavBarGlassEnabled) {
                ExpressiveCard(flat = flat) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.settings_navbar_glass_blur_strength),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Slider(
                            value = BackgroundConfig.navBarGlassBlurStrength,
                            onValueChange = { BackgroundConfig.setNavBarGlassBlurStrengthValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                ExpressiveCard(flat = flat) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.settings_navbar_glass_transparency),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Slider(
                            value = BackgroundConfig.navBarGlassTransparency,
                            onValueChange = { BackgroundConfig.setNavBarGlassTransparencyValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                ExpressiveCard(flat = flat) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.settings_navbar_glass_highlight_strength),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Slider(
                            value = BackgroundConfig.navBarGlassHighlightStrength,
                            onValueChange = { BackgroundConfig.setNavBarGlassHighlightStrengthValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                ToggleSettingCard(
                    flat = flat,
                    title = stringResource(id = R.string.settings_navbar_glass_specular),
                    description = stringResource(id = R.string.settings_navbar_glass_specular_summary),
                    checked = BackgroundConfig.isNavBarGlassSpecularEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setNavBarGlassSpecularEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
                Spacer(Modifier.height(8.dp))

                ToggleSettingCard(
                    flat = flat,
                    title = stringResource(id = R.string.settings_navbar_glass_inner_glow),
                    description = stringResource(id = R.string.settings_navbar_glass_inner_glow_summary),
                    checked = BackgroundConfig.isNavBarGlassInnerGlowEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setNavBarGlassInnerGlowEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
                Spacer(Modifier.height(8.dp))

                ToggleSettingCard(
                    flat = flat,
                    title = stringResource(id = R.string.settings_navbar_glass_border),
                    description = stringResource(id = R.string.settings_navbar_glass_border_summary),
                    checked = BackgroundConfig.isNavBarGlassBorderEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setNavBarGlassBorderEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
                Spacer(Modifier.height(8.dp))
            }

            ToggleSettingCard(
                flat = flat,
                title = stringResource(id = R.string.settings_floating_auto_hide),
                description = stringResource(id = R.string.settings_floating_auto_hide_summary),
                checked = floatingAutoHide,
                onCheckedChange = {
                    floatingAutoHide = it
                    prefs.edit().putBoolean("floating_auto_hide", it).apply()
                },
            )
            Spacer(Modifier.height(8.dp))
            ToggleSettingCard(
                flat = flat,
                title = stringResource(id = R.string.settings_floating_swipe_hide),
                description = stringResource(id = R.string.settings_floating_swipe_hide_summary),
                checked = floatingSwipeHide,
                onCheckedChange = {
                    floatingSwipeHide = it
                    prefs.edit().putBoolean("floating_swipe_hide", it).apply()
                },
            )
            Spacer(Modifier.height(8.dp))
        }

        if (isListStyle) {
            ToggleSettingCard(
                flat = flat,
                title = stringResource(id = R.string.settings_list_card_hide_status_badge),
                description = stringResource(id = R.string.settings_list_card_hide_status_badge_summary),
                checked = BackgroundConfig.isListWorkingCardModeHidden,
                onCheckedChange = {
                    BackgroundConfig.setListWorkingCardModeHiddenState(it)
                    BackgroundConfig.save(context)
                },
            )
            Spacer(Modifier.height(8.dp))
        }

        if (isListStyle && !BackgroundConfig.isListWorkingCardModeHidden) {
            ExpressiveCard(flat = flat, onClick = { showCustomBadgeTextDialog.value = true }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_custom_badge_text),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = currentBadgeTextMode,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        ToggleSettingCard(
            flat = flat,
            title = stringResource(id = R.string.settings_advanced_title_style),
            description = if (BackgroundConfig.isAdvancedTitleStyleEnabled) stringResource(id = R.string.settings_advanced_title_style_enabled) else stringResource(id = R.string.settings_advanced_title_style_summary),
            checked = BackgroundConfig.isAdvancedTitleStyleEnabled,
            onCheckedChange = {
                BackgroundConfig.setAdvancedTitleStyleEnabledState(it)
                BackgroundConfig.save(context)
                refreshTheme.value = true
            },
        )
        Spacer(Modifier.height(8.dp))

        if (BackgroundConfig.isAdvancedTitleStyleEnabled) {
            ExpressiveCard(flat = flat) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = stringResource(id = R.string.settings_title_image_day_opacity), style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = BackgroundConfig.titleImageDayOpacity,
                        onValueChange = { BackgroundConfig.setTitleImageDayOpacityValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                        ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            ExpressiveCard(flat = flat) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = stringResource(id = R.string.settings_title_image_night_opacity), style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = BackgroundConfig.titleImageNightOpacity,
                        onValueChange = { BackgroundConfig.setTitleImageNightOpacityValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                        ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            ExpressiveCard(flat = flat) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = stringResource(id = R.string.settings_title_image_dim), style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = BackgroundConfig.titleImageDim,
                        onValueChange = { BackgroundConfig.setTitleImageDimValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                        ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            ExpressiveCard(flat = flat) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = stringResource(id = R.string.settings_title_image_offset_x), style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = BackgroundConfig.titleImageOffsetX,
                        onValueChange = { BackgroundConfig.setTitleImageOffsetXValue(it) },
                        onValueChangeFinished = { BackgroundConfig.save(context) },
                        valueRange = -1f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                        ),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            ExpressiveCard(
                flat = flat,
                onClick = {
                    if (PermissionUtils.hasExternalStoragePermission(context)) {
                        try {
                            pickTitleImageLauncher.launch("image/*")
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "请先授予存储权限才能选择标题图片", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.settings_select_title_image), style = MaterialTheme.typography.titleMedium)
                        if (!BackgroundConfig.titleImageUri.isNullOrEmpty()) {
                            Text(text = stringResource(id = R.string.settings_title_image_selected), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            if (!BackgroundConfig.titleImageUri.isNullOrEmpty()) {
                val clearTitleImageDialog = rememberConfirmDialog(
                    onConfirm = {
                        scope.launch {
                            loadingDialog.show()
                            BackgroundManager.clearTitleImage(context)
                            loadingDialog.hide()
                            snackBarHost.showSnackbar(message = context.getString(R.string.settings_title_image_cleared))
                            refreshTheme.value = true
                        }
                    }
                )
                ExpressiveCard(
                    flat = flat,
                    onClick = {
                        clearTitleImageDialog.showConfirm(
                            title = context.getString(R.string.settings_clear_title_image),
                            content = context.getString(R.string.settings_clear_title_image_confirm),
                            markdown = false,
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = stringResource(id = R.string.settings_clear_title_image), style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(text = stringResource(R.string.settings_appearance_background))
        Spacer(Modifier.height(8.dp))

        ToggleSettingCard(
            flat = flat,
            title = stringResource(id = R.string.settings_custom_background),
            description = if (BackgroundConfig.isCustomBackgroundEnabled) stringResource(id = R.string.settings_custom_background_enabled) else stringResource(id = R.string.settings_custom_background_summary),
            checked = BackgroundConfig.isCustomBackgroundEnabled,
            onCheckedChange = {
                BackgroundConfig.setCustomBackgroundEnabledState(it)
                BackgroundConfig.save(context)
                refreshTheme.value = true
            },
        )
        Spacer(Modifier.height(8.dp))

        if (BackgroundConfig.isCustomBackgroundEnabled) {
            if (!BackgroundConfig.isVideoBackgroundEnabled) {
                ToggleSettingCard(
                    flat = flat,
                    title = stringResource(id = R.string.settings_custom_background_dual_dim),
                    description = stringResource(id = R.string.settings_custom_background_dual_dim_desc),
                    checked = BackgroundConfig.isDualBackgroundDimEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setDualBackgroundDimEnabledState(it)
                        BackgroundConfig.save(context)
                        refreshTheme.value = true
                    },
                )
                Spacer(Modifier.height(8.dp))

                ExpressiveCard(flat = flat) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(text = stringResource(id = R.string.settings_custom_background_opacity), style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = BackgroundConfig.customBackgroundOpacity,
                            onValueChange = { BackgroundConfig.setCustomBackgroundOpacityValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                ExpressiveCard(flat = flat) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(text = stringResource(id = R.string.settings_custom_background_blur), style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = BackgroundConfig.customBackgroundBlur,
                            onValueChange = { BackgroundConfig.setCustomBackgroundBlurValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                            valueRange = 0f..50f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (!BackgroundConfig.isDualBackgroundDimEnabled) {
                    ExpressiveCard(flat = flat) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(text = stringResource(id = R.string.settings_custom_background_dim), style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = BackgroundConfig.customBackgroundDim,
                                onValueChange = { BackgroundConfig.setCustomBackgroundDimValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    ExpressiveCard(flat = flat) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(text = stringResource(id = R.string.settings_custom_background_day_dim), style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = BackgroundConfig.customBackgroundDayDim,
                                onValueChange = { BackgroundConfig.setCustomBackgroundDayDimValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    ExpressiveCard(flat = flat) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(text = stringResource(id = R.string.settings_custom_background_night_dim), style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = BackgroundConfig.customBackgroundNightDim,
                                onValueChange = { BackgroundConfig.setCustomBackgroundNightDimValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            ToggleSettingCard(
                flat = flat,
                title = stringResource(id = R.string.settings_video_background),
                description = stringResource(id = R.string.settings_video_background_summary),
                checked = BackgroundConfig.isVideoBackgroundEnabled,
                onCheckedChange = {
                    BackgroundConfig.setVideoBackgroundEnabledState(it)
                    BackgroundConfig.save(context)
                    refreshTheme.value = true
                },
            )
            Spacer(Modifier.height(8.dp))

            if (BackgroundConfig.isVideoBackgroundEnabled) {
                ExpressiveCard(
                    flat = flat,
                    onClick = {
                        try {
                            pickVideoLauncher.launch("video/*")
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.settings_select_video), style = MaterialTheme.typography.titleMedium)
                            if (!BackgroundConfig.videoBackgroundUri.isNullOrEmpty()) {
                                Text(text = stringResource(id = R.string.settings_video_selected), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                val clearVideoDialog = rememberConfirmDialog(
                    onConfirm = {
                        scope.launch {
                            loadingDialog.show()
                            BackgroundManager.clearVideoBackground(context)
                            loadingDialog.hide()
                            snackBarHost.showSnackbar(message = context.getString(R.string.settings_background_image_cleared))
                            refreshTheme.value = true
                        }
                    }
                )

                if (!BackgroundConfig.videoBackgroundUri.isNullOrEmpty()) {
                    val clearVideoTitle = stringResource(id = R.string.settings_clear_video_background)
                    val clearVideoConfirm = context.getString(R.string.settings_clear_video_background_confirm)
                    ExpressiveCard(
                        flat = flat,
                        onClick = {
                            clearVideoDialog.showConfirm(
                                title = clearVideoTitle,
                                content = clearVideoConfirm,
                                markdown = false,
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = clearVideoTitle, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                ExpressiveCard(flat = flat) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(text = stringResource(id = R.string.settings_video_volume), style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = BackgroundConfig.videoVolume,
                            onValueChange = { BackgroundConfig.setVideoVolumeValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            } else {
                ToggleSettingCard(
                    flat = flat,
                    title = stringResource(id = R.string.settings_multi_background_mode),
                    description = stringResource(id = R.string.settings_multi_background_mode_summary),
                    checked = BackgroundConfig.isMultiBackgroundEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setMultiBackgroundEnabledState(it)
                        BackgroundConfig.save(context)
                        refreshTheme.value = true
                    },
                )
                Spacer(Modifier.height(8.dp))

                if (BackgroundConfig.isMultiBackgroundEnabled) {
                    val items = listOf(
                        Triple(R.string.settings_select_home_background, "home", BackgroundConfig.homeBackgroundUri),
                        Triple(R.string.settings_select_kernel_background, "kernel", BackgroundConfig.kernelBackgroundUri),
                        Triple(R.string.settings_select_superuser_background, "superuser", BackgroundConfig.superuserBackgroundUri),
                        Triple(R.string.settings_select_system_module_background, "system", BackgroundConfig.systemModuleBackgroundUri),
                        Triple(R.string.settings_select_settings_background, "settings", BackgroundConfig.settingsBackgroundUri)
                    )
                    items.forEach { (titleRes, type, uri) ->
                        ExpressiveCard(
                            flat = flat,
                            onClick = {
                                if (PermissionUtils.hasExternalStoragePermission(context) &&
                                    PermissionUtils.hasWriteExternalStoragePermission(context)) {
                                    pickingType = type
                                    try {
                                        pickImageLauncher.launch("image/*")
                                    } catch (e: ActivityNotFoundException) {
                                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "请先授予存储权限才能选择背景图片", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = stringResource(id = titleRes), style = MaterialTheme.typography.titleMedium)
                                    if (!uri.isNullOrEmpty()) {
                                        Text(text = stringResource(id = R.string.settings_background_selected), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                } else {
                    ExpressiveCard(
                        flat = flat,
                        onClick = {
                            if (PermissionUtils.hasExternalStoragePermission(context) &&
                                PermissionUtils.hasWriteExternalStoragePermission(context)) {
                                pickingType = "default"
                                try {
                                    pickImageLauncher.launch("image/*")
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "请先授予存储权限才能选择背景图片", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource(id = R.string.settings_select_background_image), style = MaterialTheme.typography.titleMedium)
                                if (!BackgroundConfig.customBackgroundUri.isNullOrEmpty()) {
                                    Text(text = stringResource(id = R.string.settings_background_selected), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (!BackgroundConfig.customBackgroundUri.isNullOrEmpty()) {
                        val clearBackgroundDialog = rememberConfirmDialog(
                            onConfirm = {
                                scope.launch {
                                    loadingDialog.show()
                                    BackgroundManager.clearCustomBackground(context)
                                    loadingDialog.hide()
                                    snackBarHost.showSnackbar(message = context.getString(R.string.settings_background_image_cleared))
                                    refreshTheme.value = true
                                }
                            }
                        )
                        val clearBgTitle = stringResource(id = R.string.settings_clear_background)
                        val clearBgConfirm = context.getString(R.string.settings_clear_background_confirm)
                        ExpressiveCard(
                            flat = flat,
                            onClick = {
                                clearBackgroundDialog.showConfirm(
                                    title = clearBgTitle,
                                    content = clearBgConfirm,
                                    markdown = false,
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(text = clearBgTitle, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        if (showGridCardSettings) {
            ToggleSettingCard(
                flat = flat,
                title = stringResource(id = R.string.settings_grid_working_card_background),
                description = if (BackgroundConfig.isGridWorkingCardBackgroundEnabled) stringResource(id = R.string.settings_grid_working_card_background_enabled) else stringResource(id = R.string.settings_grid_working_card_background_summary),
                checked = BackgroundConfig.isGridWorkingCardBackgroundEnabled,
                onCheckedChange = {
                    BackgroundConfig.setGridWorkingCardBackgroundEnabledState(it)
                    BackgroundConfig.save(context)
                },
            )
            Spacer(Modifier.height(8.dp))

            if (BackgroundConfig.isGridWorkingCardBackgroundEnabled) {
                ToggleSettingCard(
                    flat = flat,
                    title = stringResource(id = R.string.settings_grid_working_card_dual_opacity),
                    description = stringResource(id = R.string.settings_grid_working_card_dual_opacity_desc),
                    checked = BackgroundConfig.isGridDualOpacityEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setGridDualOpacityEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
                Spacer(Modifier.height(8.dp))

                if (!BackgroundConfig.isGridDualOpacityEnabled) {
                    ExpressiveCard(flat = flat) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(text = stringResource(id = R.string.settings_custom_background_opacity), style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = BackgroundConfig.gridWorkingCardBackgroundOpacity,
                                onValueChange = { BackgroundConfig.setGridWorkingCardBackgroundOpacityValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                } else {
                    ExpressiveCard(flat = flat) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(text = stringResource(id = R.string.settings_grid_working_card_day_opacity), style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = BackgroundConfig.gridWorkingCardBackgroundDayOpacity,
                                onValueChange = { BackgroundConfig.setGridWorkingCardBackgroundDayOpacityValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    ExpressiveCard(flat = flat) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(text = stringResource(id = R.string.settings_grid_working_card_night_opacity), style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = BackgroundConfig.gridWorkingCardBackgroundNightOpacity,
                                onValueChange = { BackgroundConfig.setGridWorkingCardBackgroundNightOpacityValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                ExpressiveCard(flat = flat) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(text = stringResource(id = R.string.settings_custom_background_dim), style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = BackgroundConfig.gridWorkingCardBackgroundDim,
                            onValueChange = { BackgroundConfig.setGridWorkingCardBackgroundDimValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                ExpressiveCard(
                    flat = flat,
                    onClick = {
                        if (PermissionUtils.hasExternalStoragePermission(context)) {
                            try {
                                pickGridImageLauncher.launch("image/*")
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "请先授予存储权限才能选择背景图片", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = stringResource(id = R.string.settings_select_background_image), style = MaterialTheme.typography.titleMedium)
                            if (!BackgroundConfig.gridWorkingCardBackgroundUri.isNullOrEmpty()) {
                                Text(text = stringResource(id = R.string.settings_grid_working_card_background_selected), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                val clearGridBackgroundDialog = rememberConfirmDialog(
                    onConfirm = {
                        scope.launch {
                            loadingDialog.show()
                            BackgroundManager.clearGridWorkingCardBackground(context)
                            loadingDialog.hide()
                            snackBarHost.showSnackbar(message = context.getString(R.string.settings_grid_working_card_background_cleared))
                        }
                    }
                )
                ExpressiveCard(
                    flat = flat,
                    onClick = {
                        clearGridBackgroundDialog.showConfirm(
                            title = context.getString(R.string.settings_clear_grid_working_card_background),
                            content = context.getString(R.string.settings_clear_grid_working_card_background_confirm),
                            markdown = false,
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = stringResource(id = R.string.settings_clear_grid_working_card_background), style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            ToggleSettingCard(
                flat = flat,
                title = stringResource(id = R.string.settings_grid_working_card_hide_check),
                description = stringResource(id = R.string.settings_grid_working_card_hide_check_summary),
                checked = BackgroundConfig.isGridWorkingCardCheckHidden,
                onCheckedChange = {
                    BackgroundConfig.setGridWorkingCardCheckHiddenState(it)
                    BackgroundConfig.save(context)
                },
            )
            Spacer(Modifier.height(8.dp))

            ToggleSettingCard(
                flat = flat,
                title = stringResource(id = R.string.settings_grid_working_card_hide_text),
                description = stringResource(id = R.string.settings_grid_working_card_hide_text_summary),
                checked = BackgroundConfig.isGridWorkingCardTextHidden,
                onCheckedChange = {
                    BackgroundConfig.setGridWorkingCardTextHiddenState(it)
                    BackgroundConfig.save(context)
                },
            )
            Spacer(Modifier.height(8.dp))

            ToggleSettingCard(
                flat = flat,
                title = stringResource(id = R.string.settings_grid_working_card_hide_mode),
                description = stringResource(id = R.string.settings_grid_working_card_hide_mode_summary),
                checked = BackgroundConfig.isGridWorkingCardModeHidden,
                onCheckedChange = {
                    BackgroundConfig.setGridWorkingCardModeHiddenState(it)
                    BackgroundConfig.save(context)
                },
            )
            Spacer(Modifier.height(8.dp))

            if (!BackgroundConfig.isGridWorkingCardModeHidden) {
                ExpressiveCard(flat = flat, onClick = { showCustomBadgeTextDialog.value = true }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_custom_badge_text),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = currentBadgeTextMode,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (showCustomBadgeTextDialog.value) {
            AlertDialog(
                onDismissRequest = { showCustomBadgeTextDialog.value = false },
                title = { Text(stringResource(id = R.string.settings_custom_badge_text)) },
                text = {
                    Column {
                        Text(
                            stringResource(id = R.string.settings_custom_badge_text_summary),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                        badgeTextModes.forEachIndexed { index, mode ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        BackgroundConfig.setCustomBadgeTextModeValue(index)
                                        BackgroundConfig.save(context)
                                        showCustomBadgeTextDialog.value = false
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                RadioButton(
                                    selected = index == currentBadgeTextModeIndex,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = mode)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCustomBadgeTextDialog.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(text = stringResource(R.string.settings_appearance_banner))
        Spacer(Modifier.height(8.dp))

        ToggleSettingCard(
            flat = flat,
            title = stringResource(id = R.string.apm_enable_module_banner),
            description = stringResource(id = R.string.apm_enable_module_banner_summary),
            checked = BackgroundConfig.isBannerEnabled,
            onCheckedChange = {
                BackgroundConfig.setBannerEnabledState(it)
                BackgroundConfig.save(context)
            },
        )
        Spacer(Modifier.height(8.dp))

        if (BackgroundConfig.isBannerEnabled) {
            ToggleSettingCard(
                flat = flat,
                title = stringResource(id = R.string.apm_enable_folk_banner),
                description = stringResource(id = R.string.apm_enable_folk_banner_summary),
                checked = BackgroundConfig.isFolkBannerEnabled,
                onCheckedChange = {
                    BackgroundConfig.setFolkBannerEnabledState(it)
                    BackgroundConfig.save(context)
                },
            )
            Spacer(Modifier.height(8.dp))

            if (BackgroundConfig.isFolkBannerEnabled) {
                ToggleSettingCard(
                    flat = flat,
                    title = stringResource(id = R.string.apm_banner_api_mode),
                    description = stringResource(id = R.string.apm_banner_api_mode_summary),
                    checked = BackgroundConfig.isBannerApiModeEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setBannerApiModeEnabledState(it)
                        BackgroundConfig.save(context)
                    },
                )
                Spacer(Modifier.height(8.dp))

                if (BackgroundConfig.isBannerApiModeEnabled) {
                    val showBannerApiConfigDialog = remember { mutableStateOf(false) }
                    val apiSourceSummary = if (BackgroundConfig.bannerApiSource.isNotBlank()) {
                        if (BackgroundConfig.bannerApiSource.startsWith("/")) {
                            context.getString(R.string.apm_banner_local_dir_configured)
                        } else {
                            context.getString(R.string.apm_banner_api_url_configured)
                        }
                    } else {
                        context.getString(R.string.apm_banner_api_source_not_configured)
                    }

                    ExpressiveCard(flat = flat, onClick = { showBannerApiConfigDialog.value = true }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.apm_banner_api_source),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = apiSourceSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (showBannerApiConfigDialog.value) {
                        BannerApiConfigDialog(
                            showDialog = showBannerApiConfigDialog,
                            currentSource = BackgroundConfig.bannerApiSource,
                            onConfirm = { newSource ->
                                BackgroundConfig.setBannerApiSourceValue(newSource)
                                BackgroundConfig.save(context)
                            },
                            onClearCache = {
                                scope.launch {
                                    loadingDialog.show()
                                    me.bmax.apatch.ui.screen.BannerApiService.clearAllCache(context)
                                    loadingDialog.hide()
                                    Toast.makeText(context, context.getString(R.string.apm_banner_cache_cleared), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    ExpressiveCard(flat = flat, onClick = { onNavigateToApiMarketplace() }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(id = R.string.apm_api_marketplace_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            ToggleSettingCard(
                flat = flat,
                title = stringResource(id = R.string.settings_banner_custom_opacity),
                description = stringResource(id = R.string.settings_banner_custom_opacity_summary),
                checked = BackgroundConfig.isBannerCustomOpacityEnabled,
                onCheckedChange = {
                    BackgroundConfig.setBannerCustomOpacityEnabledState(it)
                    BackgroundConfig.save(context)
                },
            )
            Spacer(Modifier.height(8.dp))

            if (BackgroundConfig.isBannerCustomOpacityEnabled) {
                ExpressiveCard(flat = flat) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(text = stringResource(id = R.string.settings_banner_opacity), style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = BackgroundConfig.bannerCustomOpacity,
                            onValueChange = { BackgroundConfig.setBannerCustomOpacityValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(text = stringResource(R.string.settings_appearance_font))
        Spacer(Modifier.height(8.dp))

        ToggleSettingCard(
            flat = flat,
            title = stringResource(id = R.string.settings_custom_font),
            description = if (customFontEnabled) {
                if (FontConfig.customFontFilename != null) stringResource(id = R.string.settings_font_selected) else stringResource(id = R.string.settings_custom_font_enabled)
            } else {
                stringResource(id = R.string.settings_custom_font_summary)
            },
            checked = customFontEnabled,
            onCheckedChange = {
                customFontEnabled = it
                FontConfig.setCustomFontEnabledState(it)
                FontConfig.save(context)
                refreshTheme.value = true
            },
        )
        Spacer(Modifier.height(8.dp))

        if (FontConfig.isCustomFontEnabled) {
            ExpressiveCard(
                flat = flat,
                onClick = {
                    try {
                        pickFontLauncher.launch("*/*")
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(id = R.string.settings_select_font_file), style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(8.dp))

            if (FontConfig.customFontFilename != null) {
                val clearFontDialog = rememberConfirmDialog(
                    onConfirm = {
                        FontConfig.clearFont(context)
                        refreshTheme.value = true
                        scope.launch {
                            snackBarHost.showSnackbar(message = context.getString(R.string.settings_font_cleared))
                        }
                    }
                )
                val clearFontTitle = stringResource(id = R.string.settings_clear_font)
                val clearFontConfirm = context.getString(R.string.settings_clear_font_confirm)
                ExpressiveCard(
                    flat = flat,
                    onClick = {
                        clearFontDialog.showConfirm(
                            title = clearFontTitle,
                            content = clearFontConfirm,
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = clearFontTitle, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionHeader(text = stringResource(R.string.settings_appearance_theme))
        Spacer(Modifier.height(8.dp))

        ExpressiveCard(flat = flat, onClick = { onNavigateToThemeStore() }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(id = R.string.theme_store_title), style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(8.dp))

        ExpressiveCard(flat = flat, onClick = { showExportDialog.value = true }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(id = R.string.settings_save_theme), style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(8.dp))

        ExpressiveCard(flat = flat, onClick = { showFilePicker.value = true }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = stringResource(id = R.string.settings_import_theme), style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(8.dp))

        val resetThemeDialog = rememberConfirmDialog(
            onConfirm = {
                scope.launch {
                    loadingDialog.show()
                    val success = ThemeManager.resetTheme(context)
                    loadingDialog.hide()
                    snackBarHost.showSnackbar(
                        message = if (success) context.getString(R.string.settings_theme_reset) else context.getString(R.string.settings_theme_reset_failed)
                    )
                }
            }
        )
        val resetThemeTitle = stringResource(id = R.string.settings_reset_theme)
        val resetThemeConfirm = context.getString(R.string.settings_reset_theme_confirm)
        ExpressiveCard(
            flat = flat,
            onClick = {
                resetThemeDialog.showConfirm(
                    title = resetThemeTitle,
                    content = resetThemeConfirm,
                )
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = resetThemeTitle, style = MaterialTheme.typography.titleMedium)
            }
        }
        }

    if (showHomeLayoutChooseDialog.value) {
        HomeLayoutChooseDialog(showHomeLayoutChooseDialog)
    }

    if (showNavSchemeDialog) {
        NavModeChooseDialog(
            showDialog = remember { mutableStateOf(true) }.apply { value = showNavSchemeDialog },
            currentMode = currentNavMode,
            onModeSelected = { mode ->
                currentNavMode = mode
                prefs.edit().putString("nav_mode", mode).apply()
                showNavSchemeDialog = false
            },
            onDismiss = { showNavSchemeDialog = false }
        )
    }

    if (showStatsTopLayoutDialog) {
        StatsTopLayoutChooseDialog(
            showDialog = remember { mutableStateOf(true) }.apply { value = showStatsTopLayoutDialog },
            currentMode = statsTopLayout,
            onModeSelected = { mode ->
                statsTopLayout = mode
                prefs.edit().putString("stats_top_layout", mode).apply()
                showStatsTopLayoutDialog = false
            },
            onDismiss = { showStatsTopLayoutDialog = false }
        )
    }

    if (showExportDialog.value) {
        ThemeExportDialog(
            showDialog = showExportDialog,
            onConfirm = { metadata ->
                pendingExportMetadata = metadata
                scope.launch {
                    loadingDialog.show()
                    try {
                        val exportDir = java.io.File("/storage/emulated/0/Download/FolkPatch/Themes/")
                        if (!exportDir.exists()) {
                            exportDir.mkdirs()
                        }
                        val safeName = metadata.name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                        val fileName = "$safeName.fpt"
                        val file = java.io.File(exportDir, fileName)
                        val uri = Uri.fromFile(file)
                        val success = ThemeManager.exportTheme(context, uri, metadata)
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(
                            message = if (success) context.getString(R.string.settings_theme_saved) + ": ${file.absolutePath}" else context.getString(R.string.settings_theme_save_failed)
                        )
                    } catch (e: Exception) {
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(message = context.getString(R.string.settings_theme_save_failed) + ": ${e.message}")
                    }
                    pendingExportMetadata = null
                }
            }
        )
    }

    if (showImportDialog.value && pendingImportMetadata != null) {
        ThemeImportDialog(
            showDialog = showImportDialog,
            metadata = pendingImportMetadata!!,
            onConfirm = {
                pendingImportUri?.let { uri ->
                    scope.launch {
                        loadingDialog.show()
                        val success = ThemeManager.importTheme(context, uri)
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(
                            message = if (success) context.getString(R.string.settings_theme_imported) else context.getString(R.string.settings_theme_import_failed)
                        )
                        pendingImportUri = null
                        pendingImportMetadata = null
                    }
                }
            }
        )
    }

    if (showFilePicker.value) {
        FilePickerDialog(
            onDismissRequest = { showFilePicker.value = false },
            onFileSelected = { file ->
                showFilePicker.value = false
                val uri = Uri.fromFile(file)
                scope.launch {
                    loadingDialog.show()
                    val metadata = ThemeManager.readThemeMetadata(context, uri)
                    loadingDialog.hide()
                    if (metadata != null) {
                        pendingImportUri = uri
                        pendingImportMetadata = metadata
                        showImportDialog.value = true
                    } else {
                        loadingDialog.show()
                        val success = ThemeManager.importTheme(context, uri)
                        loadingDialog.hide()
                        snackBarHost.showSnackbar(
                            message = if (success) context.getString(R.string.settings_theme_imported) else context.getString(R.string.settings_theme_import_failed)
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun colorNameToString(colorName: String): Int {
    return colorsList().find { it.name == colorName }?.nameId ?: R.string.blue_theme
}

private data class APColor(
    val name: String, @param:StringRes val nameId: Int
)

private fun colorsList(): List<APColor> {
    return listOf(
        APColor("amber", R.string.amber_theme),
        APColor("blue_grey", R.string.blue_grey_theme),
        APColor("blue", R.string.blue_theme),
        APColor("brown", R.string.brown_theme),
        APColor("cyan", R.string.cyan_theme),
        APColor("deep_orange", R.string.deep_orange_theme),
        APColor("deep_purple", R.string.deep_purple_theme),
        APColor("green", R.string.green_theme),
        APColor("indigo", R.string.indigo_theme),
        APColor("light_blue", R.string.light_blue_theme),
        APColor("light_green", R.string.light_green_theme),
        APColor("lime", R.string.lime_theme),
        APColor("orange", R.string.orange_theme),
        APColor("pink", R.string.pink_theme),
        APColor("purple", R.string.purple_theme),
        APColor("red", R.string.red_theme),
        APColor("sakura", R.string.sakura_theme),
        APColor("teal", R.string.teal_theme),
        APColor("yellow", R.string.yellow_theme),
        APColor("ink_wash", R.string.ink_wash_theme),
    )
}

@Composable
private fun homeLayoutStyleToString(style: String): Int {
    return when (style) {
        "kernelsu" -> R.string.settings_home_layout_grid
        "focus" -> R.string.settings_home_layout_focus
        "sign" -> R.string.settings_home_layout_sign
        "circle" -> R.string.settings_home_layout_circle
        "dashboard_ui" -> R.string.settings_home_layout_dashboard_pro
        "stats" -> R.string.settings_home_layout_stats
        else -> R.string.settings_home_layout_default
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeChooseDialog(showDialog: MutableState<Boolean>) {
    val prefs = APApplication.sharedPreferences
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            LazyColumn {
                items(colorsList()) {
                    ListItem(
                        headlineContent = { Text(text = stringResource(it.nameId)) },
                        modifier = Modifier.clickable {
                            showDialog.value = false
                            prefs.edit { putString("custom_color", it.name) }
                            refreshTheme.value = true
                        })
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeLayoutChooseDialog(showDialog: MutableState<Boolean>) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_home_layout_style),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val currentStyle = prefs.getString("home_layout_style", "stats")
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_default)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "default",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "default").apply()
                                showDialog.value = false
                            }
                        )
                        
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_grid)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "kernelsu",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "kernelsu").apply()
                                showDialog.value = false
                            }
                        )
                        
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_focus)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "focus",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "focus").apply()
                                showDialog.value = false
                            }
                        )

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_sign)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "sign",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "sign").apply()
                                showDialog.value = false
                            }
                        )

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_circle)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "circle",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "circle").apply()
                                showDialog.value = false
                            }
                        )

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_dashboard_pro)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "dashboard_ui",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "dashboard_ui").apply()
                                showDialog.value = false
                            }
                        )

                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_home_layout_stats)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentStyle == "stats",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                prefs.edit().putString("home_layout_style", "stats").apply()
                                showDialog.value = false
                            }
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeExportDialog(
    showDialog: MutableState<Boolean>,
    onConfirm: (ThemeManager.ThemeMetadata) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("phone") }
    var version by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false },
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.theme_export_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.theme_name)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.theme_type),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { type = "phone" }
                    ) {
                        RadioButton(
                            selected = type == "phone",
                            onClick = { type = "phone" }
                        )
                        Text(
                            text = stringResource(R.string.theme_type_phone),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { type = "tablet" }
                            .padding(start = 16.dp)
                    ) {
                        RadioButton(
                            selected = type == "tablet",
                            onClick = { type = "tablet" }
                        )
                        Text(
                            text = stringResource(R.string.theme_type_tablet),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text(stringResource(R.string.theme_version)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(R.string.theme_author)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.theme_description)) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    minLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Button(
                        onClick = {
                            if (name.isNotEmpty()) {
                                showDialog.value = false
                                onConfirm(
                                    ThemeManager.ThemeMetadata(
                                        name = name,
                                        type = type,
                                        version = version,
                                        author = author,
                                        description = description
                                    )
                                )
                            }
                        },
                        enabled = name.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.theme_export_action))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeImportDialog(
    showDialog: MutableState<Boolean>,
    metadata: ThemeManager.ThemeMetadata,
    onConfirm: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false },
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.theme_import_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = stringResource(R.string.theme_import_confirm),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.theme_info),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(text = "${stringResource(R.string.theme_name)}: ${metadata.name}")
                        Text(text = "${stringResource(R.string.theme_type)}: ${if (metadata.type == "tablet") stringResource(R.string.theme_type_tablet) else stringResource(R.string.theme_type_phone)}")
                        if (metadata.version.isNotEmpty()) {
                            Text(text = "${stringResource(R.string.theme_version)}: ${metadata.version}")
                        }
                        if (metadata.author.isNotEmpty()) {
                            Text(text = "${stringResource(R.string.theme_author)}: ${metadata.author}")
                        }
                        if (metadata.description.isNotEmpty()) {
                            Text(
                                text = "${stringResource(R.string.theme_description)}: ${metadata.description}",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Button(onClick = {
                        showDialog.value = false
                        onConfirm()
                    }) {
                        Text(stringResource(R.string.theme_import_action))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavModeChooseDialog(
    showDialog: MutableState<Boolean>,
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_nav_scheme),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_nav_mode_floating)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentMode == "floating",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onModeSelected("floating")
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_nav_mode_auto)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentMode == "auto",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onModeSelected("auto")
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_nav_mode_bottom)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentMode == "bottom",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onModeSelected("bottom")
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_nav_mode_rail)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentMode == "rail",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onModeSelected("rail")
                            }
                        )
                    }
                }
            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsTopLayoutChooseDialog(
    showDialog: MutableState<Boolean>,
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val prefs = APApplication.sharedPreferences

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.settings_stats_top_layout),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AlertDialogDefaults.containerColor,
                    tonalElevation = 2.dp
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_stats_top_layout_list)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentMode == "list",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onModeSelected("list")
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_stats_top_layout_grid)) },
                            leadingContent = {
                                RadioButton(
                                    selected = currentMode == "grid",
                                    onClick = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onModeSelected("grid")
                            }
                        )
                    }
                }
            }

            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerApiConfigDialog(
    showDialog: MutableState<Boolean>,
    currentSource: String,
    onConfirm: (String) -> Unit,
    onClearCache: () -> Unit
) {
    val context = LocalContext.current
    var sourceText by remember { mutableStateOf(currentSource) }

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false },
        properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(340.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.apm_banner_api_config_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = stringResource(R.string.apm_banner_api_config_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = sourceText,
                    onValueChange = { sourceText = it },
                    label = { Text(stringResource(R.string.apm_banner_api_source)) },
                    placeholder = { Text(stringResource(R.string.apm_banner_api_source_hint), style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (sourceText.isNotEmpty()) {
                            IconButton(onClick = { sourceText = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.apm_banner_api_examples_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.apm_banner_api_examples),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onClearCache()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.apm_banner_clear_cache))
                    }
                    Button(
                        onClick = {
                            onConfirm(sourceText)
                            showDialog.value = false
                            Toast.makeText(context, context.getString(R.string.apm_banner_api_source_saved), Toast.LENGTH_SHORT).show()
                        },
                        enabled = sourceText.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
            val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
            APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
        }
    }
}
