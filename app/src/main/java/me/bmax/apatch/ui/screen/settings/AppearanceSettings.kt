package me.bmax.apatch.ui.screen.settings

import me.bmax.apatch.ui.theme.tokens.FolkDimens
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import me.bmax.apatch.ui.theme.ColorGenerationMode
import me.bmax.apatch.ui.theme.ColorStandard
import me.bmax.apatch.ui.theme.ColorStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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

import me.bmax.apatch.ui.component.ThemeMode
import me.bmax.apatch.ui.component.rememberLoadingDialog
import me.bmax.apatch.ui.component.LoadingDialogHandle
import me.bmax.apatch.ui.theme.BackgroundConfig
import me.bmax.apatch.ui.theme.FontConfig
import me.bmax.apatch.ui.theme.refreshTheme
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceFontSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceThemeSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceBannerSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceBackgroundSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceFocusCardSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceDashboardCardSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceLayoutSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceNightModeSection
import me.bmax.apatch.ui.screen.settings.appearance.AppearanceThemeIoDialogs
import me.bmax.apatch.ui.screen.settings.appearance.HomeLayoutChooseDialog
import me.bmax.apatch.ui.screen.settings.appearance.NavModeChooseDialog
import me.bmax.apatch.ui.screen.settings.appearance.StatsTopLayoutChooseDialog
import me.bmax.apatch.ui.screen.settings.appearance.ThemeExportDialog
import me.bmax.apatch.ui.screen.settings.appearance.ThemeImportDialog
import me.bmax.apatch.util.ui.APDialogBlurBehindUtils
import me.bmax.apatch.util.ui.NavigationBarsSpacer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import me.bmax.apatch.ui.component.folk.folkPressScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsContent(
    snackBarHost: SnackbarHostState,
    kPatchReady: Boolean,
    onNavigateToThemeStore: () -> Unit,
    onNavigateToApiMarketplace: () -> Unit,
    flat: Boolean = false,
    highlightKey: String? = null,
    themeStoreMode: String? = null,
    onThemeStoreModeChanged: ((String) -> Unit)? = null,
) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()


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

    val showExportDialog = remember { mutableStateOf(false) }
    val showFilePicker = remember { mutableStateOf(false) }

    val isNightModeSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var nightModeFollowSys by remember { mutableStateOf(prefs.getBoolean("night_mode_follow_sys", true)) }
    var nightModeEnabled by remember { mutableStateOf(prefs.getBoolean("night_mode_enabled", true)) }
    val isDynamicColorSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var useSystemDynamicColor by remember { mutableStateOf(prefs.getBoolean("use_system_color_theme", false)) }
    var selectedFontMode by remember { mutableStateOf(FontConfig.fontMode) }

    val refreshThemeObserver by refreshTheme.observeAsState(false)

    var customColorScheme by remember { mutableStateOf(prefs.getString("custom_color", "indigo")) }
    var amoledTheme by remember { mutableStateOf(prefs.getBoolean("amoled_theme", false)) }
    var colorGenerationMode by remember { mutableStateOf(ColorGenerationMode.fromKey(prefs.getString("color_generation_mode", "custom"))) }
    var colorStandard by remember { mutableStateOf(ColorStandard.fromName(prefs.getString("color_standard", "M3E_2025"))) }
    var colorStyle by remember { mutableStateOf(ColorStyle.fromName(prefs.getString("color_style", "TONAL_SPOT"))) }

    var currentStyle by remember { mutableStateOf(prefs.getString("home_layout_style", APApplication.HOME_LAYOUT_STYLE_DEFAULT)) }

    if (refreshThemeObserver) {
        nightModeFollowSys = prefs.getBoolean("night_mode_follow_sys", false)
        nightModeEnabled = prefs.getBoolean("night_mode_enabled", true)
        useSystemDynamicColor = prefs.getBoolean("use_system_color_theme", true)
        selectedFontMode = FontConfig.fontMode
        customColorScheme = prefs.getString("custom_color", "indigo")
        amoledTheme = prefs.getBoolean("amoled_theme", false)
        colorGenerationMode = ColorGenerationMode.fromKey(prefs.getString("color_generation_mode", "custom"))
        colorStandard = ColorStandard.fromName(prefs.getString("color_standard", "M3E_2025"))
        colorStyle = ColorStyle.fromName(prefs.getString("color_style", "TONAL_SPOT"))
        currentStyle = prefs.getString("home_layout_style", APApplication.HOME_LAYOUT_STYLE_DEFAULT)
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

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "night_mode_follow_sys" -> nightModeFollowSys = prefs.getBoolean(key, true)
                "night_mode_enabled" -> nightModeEnabled = prefs.getBoolean(key, true)
                "use_system_color_theme" -> useSystemDynamicColor = prefs.getBoolean(key, false)
                "custom_color" -> customColorScheme = prefs.getString(key, "indigo")
                "amoled_theme" -> amoledTheme = prefs.getBoolean(key, false)
                "color_generation_mode" -> colorGenerationMode = ColorGenerationMode.fromKey(prefs.getString(key, "custom"))
                "color_standard" -> colorStandard = ColorStandard.fromName(prefs.getString(key, "M3E_2025"))
                "color_style" -> colorStyle = ColorStyle.fromName(prefs.getString(key, "TONAL_SPOT"))
                "home_layout_style" -> currentStyle = prefs.getString(key, APApplication.HOME_LAYOUT_STYLE_DEFAULT)
                "stats_top_layout" -> statsTopLayout = prefs.getString(key, "list") ?: "list"
                "show_nav_apm" -> showNavApm = prefs.getBoolean(key, true)
                "show_nav_kpm" -> showNavKpm = prefs.getBoolean(key, true)
                "show_nav_superuser" -> showNavSuperUser = prefs.getBoolean(key, true)
                "nav_mode" -> currentNavMode = prefs.getString(key, "floating") ?: "floating"
                "floating_auto_hide" -> floatingAutoHide = prefs.getBoolean(key, true)
                "floating_swipe_hide" -> floatingSwipeHide = prefs.getBoolean(key, true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val isKernelSuStyle = currentStyle == "kernelsu"
    val showGridCardSettings = isKernelSuStyle || (isStatsLayout && statsTopLayout == "grid")
    val isListStyle = currentStyle != "kernelsu" && currentStyle != "focus" && !(isStatsLayout && statsTopLayout == "grid")
    // Focus布局样式（FocusUI），该样式下4个卡片支持独立壁纸
    val isFocusStyle = currentStyle == "focus"
    val isDashboardStyle = currentStyle == "dashboard_ui"
    // 默认ListUI布局（对应HomeScreen的else分支）
    val isDefaultStyle = currentStyle !in listOf("kernelsu", "focus", "circle", "dashboard_ui", "stats")

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

        AppearanceNightModeSection(
            flat = flat,
            highlightKey = highlightKey,
            isNightModeSupported = isNightModeSupported,
            isDynamicColorSupport = isDynamicColorSupport,
            isDarkTheme = isDarkTheme,
            themeMode = themeMode,
            customColorScheme = customColorScheme ?: "indigo",
            useSystemDynamicColor = useSystemDynamicColor,
            colorGenerationMode = colorGenerationMode,
            colorStandard = colorStandard,
            colorStyle = colorStyle,
            amoledTheme = amoledTheme,
            onThemeModeSelected = { mode ->
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
            onColorSelected = { key ->
                prefs.edit().putString("custom_color", key).putBoolean("use_system_color_theme", false).apply()
                customColorScheme = key
                useSystemDynamicColor = false
                refreshTheme.value = true
            },
            onDynamicColorSelected = {
                prefs.edit().putBoolean("use_system_color_theme", true).apply()
                useSystemDynamicColor = true
                refreshTheme.value = true
            },
            onGenerationModeSelected = { mode ->
                colorGenerationMode = mode
                prefs.edit().putString("color_generation_mode", mode.key).apply()
                refreshTheme.value = true
            },
            onStandardSelected = { standard ->
                colorStandard = standard
                prefs.edit().putString("color_standard", standard.name).apply()
                refreshTheme.value = true
            },
            onStyleSelected = { style ->
                colorStyle = style
                prefs.edit().putString("color_style", style.name).apply()
                refreshTheme.value = true
            },
            onAmoledChange = { value ->
                amoledTheme = value
                prefs.edit().putBoolean("amoled_theme", value).apply()
                refreshTheme.value = true
            },
        )

        AppearanceLayoutSection(
            flat = flat,
            highlightKey = highlightKey,
            kPatchReady = kPatchReady,
            snackBarHost = snackBarHost,
            loadingDialog = loadingDialog,
            currentStyle = currentStyle,
            isStatsLayout = isStatsLayout,
            statsTopLayoutValue = statsTopLayoutValue,
            onShowStatsTopLayoutDialog = { showStatsTopLayoutDialog = true },
            navSchemeLabel = navSchemeLabel,
            onShowNavSchemeDialog = { showNavSchemeDialog = true },
            isFloatingNav = isFloatingNav,
            floatingAutoHide = floatingAutoHide,
            onFloatingAutoHideChange = { floatingAutoHide = it },
            floatingSwipeHide = floatingSwipeHide,
            onFloatingSwipeHideChange = { floatingSwipeHide = it },
            showNavApm = showNavApm,
            onShowNavApmChange = { showNavApm = it },
            showNavKpm = showNavKpm,
            onShowNavKpmChange = { showNavKpm = it },
            showNavSuperUser = showNavSuperUser,
            onShowNavSuperUserChange = { showNavSuperUser = it },
            isListStyle = isListStyle,
            isDefaultStyle = isDefaultStyle,
            currentBadgeTextMode = currentBadgeTextMode,
            showCustomBadgeTextDialog = showCustomBadgeTextDialog,
            showHomeLayoutChooseDialog = showHomeLayoutChooseDialog,
        )

        AppearanceBackgroundSection(
            flat = flat,
            highlightKey = highlightKey,
            snackBarHost = snackBarHost,
            loadingDialog = loadingDialog,
            showGridCardSettings = showGridCardSettings,
            currentBadgeTextMode = currentBadgeTextMode,
            showCustomBadgeTextDialog = showCustomBadgeTextDialog,
        )

        // FocusUI card wallpapers are separate from page-level and multi-background settings.
        if (isFocusStyle) {
            AppearanceFocusCardSection(
                flat = flat,
                highlightKey = highlightKey,
                snackBarHost = snackBarHost,
                loadingDialog = loadingDialog,
            )
        }

        if (isDashboardStyle) {
            AppearanceDashboardCardSection(
                flat = flat,
                highlightKey = highlightKey,
                snackBarHost = snackBarHost,
                loadingDialog = loadingDialog,
            )
        }

        if (showCustomBadgeTextDialog.value) {
            AlertDialog(
                onDismissRequest = { showCustomBadgeTextDialog.value = false },
                title = { Text(stringResource(id = R.string.settings_custom_badge_text)) },
                text = {
                    Column {
                        Text(
                            stringResource(id = R.string.settings_custom_badge_text_summary),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = FolkDimens.Size16),
                        )
                        badgeTextModes.forEachIndexed { index, mode ->
                            val badgeInteractionSource = remember { MutableInteractionSource() }
                            val badgeHaptics = LocalHapticFeedback.current
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .folkPressScale(badgeInteractionSource, true)
                                    .selectable(
                                        selected = index == currentBadgeTextModeIndex,
                                        interactionSource = badgeInteractionSource,
                                        indication = null,
                                        role = Role.RadioButton,
                                        onClick = {
                                            badgeHaptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            BackgroundConfig.setCustomBadgeTextModeValue(index)
                                            BackgroundConfig.save(context)
                                            showCustomBadgeTextDialog.value = false
                                        },
                                    )
                                    .padding(vertical = FolkDimens.Size12)
                            ) {
                                Text(text = mode, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(FolkDimens.Size8))
                                RadioButton(selected = index == currentBadgeTextModeIndex, onClick = null)
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

        AppearanceBannerSection(
            flat = flat,
            highlightKey = highlightKey,
            onNavigateToApiMarketplace = onNavigateToApiMarketplace,
            loadingDialog = loadingDialog,
        )

        AppearanceFontSection(
            flat = flat,
            highlightKey = highlightKey,
            fontMode = selectedFontMode,
            onFontModeChange = { mode ->
                selectedFontMode = mode
                FontConfig.setFontMode(context, mode)
                refreshTheme.value = true
            },
            pickFontLauncher = pickFontLauncher,
            snackBarHost = snackBarHost,
        )

        AppearanceThemeSection(
            flat = flat,
            highlightKey = highlightKey,
            onNavigateToThemeStore = onNavigateToThemeStore,
            themeStoreMode = themeStoreMode,
            onThemeStoreModeChanged = onThemeStoreModeChanged,
            showExportDialog = showExportDialog,
            showFilePicker = showFilePicker,
            snackBarHost = snackBarHost,
            loadingDialog = loadingDialog,
        )
        }

    if (showHomeLayoutChooseDialog.value) {
        HomeLayoutChooseDialog(showHomeLayoutChooseDialog) { selectedLayout ->
            currentStyle = selectedLayout
            refreshTheme.value = true
        }
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

    AppearanceThemeIoDialogs(
        showExportDialog = showExportDialog,
        showFilePicker = showFilePicker,
        snackBarHost = snackBarHost,
        loadingDialog = loadingDialog,
    )
}
