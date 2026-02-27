package me.bmax.apatch.ui.screen.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import me.bmax.apatch.ui.component.FilePickerDialog
import me.bmax.apatch.ui.component.SettingsCategory
import me.bmax.apatch.ui.component.SwitchItem
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
fun AppearanceSettings(
    searchText: String,
    snackBarHost: SnackbarHostState,
    kPatchReady: Boolean,
    onNavigateToThemeStore: () -> Unit,
    onNavigateToApiMarketplace: () -> Unit
) {
    val prefs = APApplication.sharedPreferences
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()

    LaunchedEffect(Unit) {
        FontConfig.load(context)
        // Sync state after load
        refreshTheme.value = true
    }
    
    // --- Launchers ---
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
    
    // Theme Export/Import Logic
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

    // Appearance Category
    val appearanceTitle = stringResource(R.string.settings_category_appearance)
    val matchAppearance = shouldShow(searchText, appearanceTitle)

    // Night Mode
    val isNightModeSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val nightModeFollowSysTitle = stringResource(id = R.string.settings_night_mode_follow_sys)
    val nightModeFollowSysSummary = stringResource(id = R.string.settings_night_mode_follow_sys_summary)
    val showNightModeFollowSys = isNightModeSupported && (matchAppearance || shouldShow(searchText, nightModeFollowSysTitle, nightModeFollowSysSummary))

    var nightModeFollowSys by remember { mutableStateOf(prefs.getBoolean("night_mode_follow_sys", true)) }
    var nightModeEnabled by remember { mutableStateOf(prefs.getBoolean("night_mode_enabled", true)) }

    val nightModeEnabledTitle = stringResource(id = R.string.settings_night_theme_enabled)
    val showNightModeEnabled = isNightModeSupported && !nightModeFollowSys && (matchAppearance || shouldShow(searchText, nightModeEnabledTitle))

    // Theme Color
    val isDynamicColorSupport = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var useSystemDynamicColor by remember { mutableStateOf(prefs.getBoolean("use_system_color_theme", false)) }
    var customFontEnabled by remember { mutableStateOf(FontConfig.isCustomFontEnabled) }
    
    val refreshThemeObserver by refreshTheme.observeAsState(false)
    if (refreshThemeObserver) {
        nightModeFollowSys = prefs.getBoolean("night_mode_follow_sys", false)
        nightModeEnabled = prefs.getBoolean("night_mode_enabled", true)
        useSystemDynamicColor = prefs.getBoolean("use_system_color_theme", true)
        customFontEnabled = FontConfig.isCustomFontEnabled
    }

    val useSystemColorTitle = stringResource(id = R.string.settings_use_system_color_theme)
    val useSystemColorSummary = stringResource(id = R.string.settings_use_system_color_theme_summary)
    val showUseSystemColor = isDynamicColorSupport && (matchAppearance || shouldShow(searchText, useSystemColorTitle, useSystemColorSummary))

    val customColorTitle = stringResource(id = R.string.settings_custom_color_theme)
    val colorMode = prefs.getString("custom_color", "light_blue")
    val customColorValue = stringResource(colorNameToString(colorMode.toString()))
    val showCustomColor = (!isDynamicColorSupport || !useSystemDynamicColor) && (matchAppearance || shouldShow(searchText, customColorTitle, customColorValue))

    // App Title


    // Home Layout
    val homeLayoutTitle = stringResource(id = R.string.settings_home_layout_style)
    val currentStyle = prefs.getString("home_layout_style", "circle")
    val homeLayoutValue = stringResource(homeLayoutStyleToString(currentStyle.toString()))
    val showHomeLayout = matchAppearance || shouldShow(searchText, homeLayoutTitle, homeLayoutValue)

    // Navigation Layout Settings
    val navLayoutTitle = stringResource(id = R.string.settings_nav_layout_title)
    val navLayoutSummary = stringResource(id = R.string.settings_nav_layout_summary)
    val showNavLayout = kPatchReady && (matchAppearance || shouldShow(searchText, navLayoutTitle, navLayoutSummary))
    
    val showNavApmTitle = stringResource(id = R.string.settings_show_apm)
    val showNavKpmTitle = stringResource(id = R.string.settings_show_kpm)
    val showNavSuperUserTitle = stringResource(id = R.string.settings_show_superuser)

    var showNavApm by remember { mutableStateOf(prefs.getBoolean("show_nav_apm", true)) }
    var showNavKpm by remember { mutableStateOf(prefs.getBoolean("show_nav_kpm", true)) }
    var showNavSuperUser by remember { mutableStateOf(prefs.getBoolean("show_nav_superuser", true)) }

    // Grid Layout Background
    val isKernelSuStyle = currentStyle == "kernelsu"
    val gridBackgroundTitle = stringResource(id = R.string.settings_grid_working_card_background)
    val gridBackgroundSummary = stringResource(id = R.string.settings_grid_working_card_background_summary)
    val gridBackgroundEnabledText = stringResource(id = R.string.settings_grid_working_card_background_enabled)
    val gridSelectImageText = stringResource(id = R.string.settings_select_background_image)
    
    val showGridBackgroundSwitch = isKernelSuStyle && (matchAppearance || shouldShow(searchText, gridBackgroundTitle, gridBackgroundSummary, gridBackgroundEnabledText, gridSelectImageText))
    
    val gridOpacityTitle = stringResource(id = R.string.settings_custom_background_opacity)
    val showGridOpacity = isKernelSuStyle && BackgroundConfig.isGridWorkingCardBackgroundEnabled && (matchAppearance || shouldShow(searchText, gridOpacityTitle))
    val gridDualOpacityTitle = stringResource(id = R.string.settings_grid_working_card_dual_opacity)
    val showGridDualOpacitySwitch = isKernelSuStyle && BackgroundConfig.isGridWorkingCardBackgroundEnabled && (matchAppearance || shouldShow(searchText, gridDualOpacityTitle))
    
    val gridDayOpacityTitle = stringResource(id = R.string.settings_grid_working_card_day_opacity)
    val showGridDayOpacity = isKernelSuStyle && BackgroundConfig.isGridWorkingCardBackgroundEnabled && BackgroundConfig.isGridDualOpacityEnabled && (matchAppearance || shouldShow(searchText, gridDayOpacityTitle))
    
    val gridNightOpacityTitle = stringResource(id = R.string.settings_grid_working_card_night_opacity)
    val showGridNightOpacity = isKernelSuStyle && BackgroundConfig.isGridWorkingCardBackgroundEnabled && BackgroundConfig.isGridDualOpacityEnabled && (matchAppearance || shouldShow(searchText, gridNightOpacityTitle))

    val gridDimTitle = stringResource(id = R.string.settings_custom_background_dim)
    val showGridDim = isKernelSuStyle && BackgroundConfig.isGridWorkingCardBackgroundEnabled && (matchAppearance || shouldShow(searchText, gridDimTitle))

    val gridSelectTitle = stringResource(id = R.string.settings_select_background_image)
    val gridSelectedText = stringResource(id = R.string.settings_grid_working_card_background_selected)
    val showGridPicker = isKernelSuStyle && BackgroundConfig.isGridWorkingCardBackgroundEnabled && (matchAppearance || shouldShow(searchText, gridSelectTitle, gridSelectedText))
    
    val gridClearTitle = stringResource(id = R.string.settings_clear_grid_working_card_background)
    val showGridClear = isKernelSuStyle && BackgroundConfig.isGridWorkingCardBackgroundEnabled && (matchAppearance || shouldShow(searchText, gridClearTitle))

    val gridCheckHiddenTitle = stringResource(id = R.string.settings_grid_working_card_hide_check)
    val gridCheckHiddenSummary = stringResource(id = R.string.settings_grid_working_card_hide_check_summary)
    val showGridCheckHidden = isKernelSuStyle && (matchAppearance || shouldShow(searchText, gridCheckHiddenTitle, gridCheckHiddenSummary))
    
    val gridTextHiddenTitle = stringResource(id = R.string.settings_grid_working_card_hide_text)
    val gridTextHiddenSummary = stringResource(id = R.string.settings_grid_working_card_hide_text_summary)
    val showGridTextHidden = isKernelSuStyle && (matchAppearance || shouldShow(searchText, gridTextHiddenTitle, gridTextHiddenSummary))

    val gridModeHiddenTitle = stringResource(id = R.string.settings_grid_working_card_hide_mode)
    val gridModeHiddenSummary = stringResource(id = R.string.settings_grid_working_card_hide_mode_summary)
    val showGridModeHidden = isKernelSuStyle && (matchAppearance || shouldShow(searchText, gridModeHiddenTitle, gridModeHiddenSummary))

    // List Layout Customization
    val isListStyle = currentStyle != "kernelsu" && currentStyle != "focus"
    val listModeHiddenTitle = stringResource(id = R.string.settings_list_working_card_hide_mode)
    val listModeHiddenSummary = stringResource(id = R.string.settings_list_working_card_hide_mode_summary)
    val showListModeHidden = isListStyle && (matchAppearance || shouldShow(searchText, listModeHiddenTitle, listModeHiddenSummary))

    val listCardHideStatusBadgeTitle = stringResource(id = R.string.settings_list_card_hide_status_badge)
    val listCardHideStatusBadgeSummary = stringResource(id = R.string.settings_list_card_hide_status_badge_summary)
    val showListCardHideStatusBadge = isListStyle && (matchAppearance || shouldShow(searchText, listCardHideStatusBadgeTitle, listCardHideStatusBadgeSummary))

    val customBadgeTextTitle = stringResource(id = R.string.settings_custom_badge_text)
    val customBadgeTextSummary = stringResource(id = R.string.settings_custom_badge_text_summary)
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
    val showCustomBadgeTextList = isListStyle && !BackgroundConfig.isListWorkingCardModeHidden && (matchAppearance || shouldShow(searchText, customBadgeTextTitle, customBadgeTextSummary))
    val showCustomBadgeTextGrid = isKernelSuStyle && !BackgroundConfig.isGridWorkingCardModeHidden && (matchAppearance || shouldShow(searchText, customBadgeTextTitle, customBadgeTextSummary))
    val showCustomBadgeTextDialog = remember { mutableStateOf(false) }

    // Advanced Title Style
    val advancedTitleStyleTitle = stringResource(id = R.string.settings_advanced_title_style)
    val advancedTitleStyleSummary = stringResource(id = R.string.settings_advanced_title_style_summary)
    val advancedTitleStyleEnabledText = stringResource(id = R.string.settings_advanced_title_style_enabled)
    val showAdvancedTitleStyleSwitch = matchAppearance || shouldShow(searchText, advancedTitleStyleTitle, advancedTitleStyleSummary, advancedTitleStyleEnabledText)

    val titleImageDayOpacityTitle = stringResource(id = R.string.settings_title_image_day_opacity)
    val showTitleImageDayOpacity = BackgroundConfig.isAdvancedTitleStyleEnabled && (matchAppearance || shouldShow(searchText, titleImageDayOpacityTitle))

    val titleImageNightOpacityTitle = stringResource(id = R.string.settings_title_image_night_opacity)
    val showTitleImageNightOpacity = BackgroundConfig.isAdvancedTitleStyleEnabled && (matchAppearance || shouldShow(searchText, titleImageNightOpacityTitle))

    val titleImageDimTitle = stringResource(id = R.string.settings_title_image_dim)
    val showTitleImageDim = BackgroundConfig.isAdvancedTitleStyleEnabled && (matchAppearance || shouldShow(searchText, titleImageDimTitle))

    val titleImageSelectTitle = stringResource(id = R.string.settings_select_title_image)
    val titleImageSelectedText = stringResource(id = R.string.settings_title_image_selected)
    val showTitleImagePicker = BackgroundConfig.isAdvancedTitleStyleEnabled && (matchAppearance || shouldShow(searchText, titleImageSelectTitle, titleImageSelectedText))

    val titleImageClearTitle = stringResource(id = R.string.settings_clear_title_image)
    val showTitleImageClear = BackgroundConfig.isAdvancedTitleStyleEnabled && !BackgroundConfig.titleImageUri.isNullOrEmpty() && (matchAppearance || shouldShow(searchText, titleImageClearTitle))

    val titleImageOffsetXTitle = stringResource(id = R.string.settings_title_image_offset_x)
    val showTitleImageOffsetX = BackgroundConfig.isAdvancedTitleStyleEnabled && (matchAppearance || shouldShow(searchText, titleImageOffsetXTitle))

    // Banner Settings
    val bannerEnabledTitle = stringResource(id = R.string.apm_enable_module_banner)
    val bannerEnabledSummary = stringResource(id = R.string.apm_enable_module_banner_summary)
    val showBannerEnabled = matchAppearance || shouldShow(searchText, bannerEnabledTitle, bannerEnabledSummary)

    val folkBannerTitle = stringResource(id = R.string.apm_enable_folk_banner)
    val folkBannerSummary = stringResource(id = R.string.apm_enable_folk_banner_summary)
    val showFolkBanner = BackgroundConfig.isBannerEnabled && (matchAppearance || shouldShow(searchText, folkBannerTitle, folkBannerSummary))

    // Banner API Mode
    val bannerApiModeTitle = stringResource(id = R.string.apm_banner_api_mode)
    val bannerApiModeSummary = stringResource(id = R.string.apm_banner_api_mode_summary)
    val showBannerApiModeSwitch = BackgroundConfig.isBannerEnabled && BackgroundConfig.isFolkBannerEnabled && (matchAppearance || shouldShow(searchText, bannerApiModeTitle, bannerApiModeSummary))

    val bannerApiSourceTitle = stringResource(id = R.string.apm_banner_api_source)
    val bannerApiSourceHint = stringResource(id = R.string.apm_banner_api_source_hint)
    val showBannerApiSource = BackgroundConfig.isBannerEnabled && BackgroundConfig.isFolkBannerEnabled && BackgroundConfig.isBannerApiModeEnabled && (matchAppearance || shouldShow(searchText, bannerApiSourceTitle, bannerApiSourceHint))

    val bannerApiClearCacheTitle = stringResource(id = R.string.apm_banner_clear_cache)

    // API Marketplace
    val apiMarketplaceTitle = stringResource(id = R.string.apm_api_marketplace_title)
    val showApiMarketplace = BackgroundConfig.isBannerEnabled && BackgroundConfig.isFolkBannerEnabled && BackgroundConfig.isBannerApiModeEnabled && (matchAppearance || shouldShow(searchText, apiMarketplaceTitle))

    val bannerCustomOpacityTitle = stringResource(id = R.string.settings_banner_custom_opacity)
    val bannerCustomOpacitySummary = stringResource(id = R.string.settings_banner_custom_opacity_summary)
    val showBannerCustomOpacitySwitch = BackgroundConfig.isBannerEnabled && (matchAppearance || shouldShow(searchText, bannerCustomOpacityTitle, bannerCustomOpacitySummary))

    val bannerOpacityTitle = stringResource(id = R.string.settings_banner_opacity)
    val showBannerOpacity = BackgroundConfig.isBannerEnabled && BackgroundConfig.isBannerCustomOpacityEnabled && (matchAppearance || shouldShow(searchText, bannerOpacityTitle))

    // Custom Background (Single/Multi)
    val customBackgroundTitle = stringResource(id = R.string.settings_custom_background)
    val customBackgroundSummary = stringResource(id = R.string.settings_custom_background_summary)
    val customBackgroundEnabledText = stringResource(id = R.string.settings_custom_background_enabled)
    
    val showCustomBackgroundSwitch = matchAppearance || shouldShow(searchText, customBackgroundTitle, customBackgroundSummary, customBackgroundEnabledText)

    val customDualDimTitle = stringResource(id = R.string.settings_custom_background_dual_dim)
    val showCustomDualDimSwitch = BackgroundConfig.isCustomBackgroundEnabled && (matchAppearance || shouldShow(searchText, customDualDimTitle))

    val customOpacityTitle = stringResource(id = R.string.settings_custom_background_opacity)
    val showCustomOpacity = BackgroundConfig.isCustomBackgroundEnabled && (matchAppearance || shouldShow(searchText, customOpacityTitle))

    val customBlurTitle = stringResource(id = R.string.settings_custom_background_blur)
    val showCustomBlur = BackgroundConfig.isCustomBackgroundEnabled && (matchAppearance || shouldShow(searchText, customBlurTitle))

    val customDimTitle = stringResource(id = R.string.settings_custom_background_dim)
    val showCustomDim = BackgroundConfig.isCustomBackgroundEnabled && (matchAppearance || shouldShow(searchText, customDimTitle))

    val customDayDimTitle = stringResource(id = R.string.settings_custom_background_day_dim)
    val showCustomDayDim = BackgroundConfig.isCustomBackgroundEnabled && BackgroundConfig.isDualBackgroundDimEnabled && (matchAppearance || shouldShow(searchText, customDayDimTitle))

    val customNightDimTitle = stringResource(id = R.string.settings_custom_background_night_dim)
    val showCustomNightDim = BackgroundConfig.isCustomBackgroundEnabled && BackgroundConfig.isDualBackgroundDimEnabled && (matchAppearance || shouldShow(searchText, customNightDimTitle))
    
    val videoVolumeTitle = stringResource(id = R.string.settings_video_volume)
    val showVideoVolume = BackgroundConfig.isCustomBackgroundEnabled && BackgroundConfig.isVideoBackgroundEnabled && (matchAppearance || shouldShow(searchText, videoVolumeTitle))
    
    // Video Background
    val videoBackgroundTitle = stringResource(id = R.string.settings_video_background)
    val videoBackgroundSummary = stringResource(id = R.string.settings_video_background_summary)
    val showVideoBackgroundSwitch = BackgroundConfig.isCustomBackgroundEnabled && (matchAppearance || shouldShow(searchText, videoBackgroundTitle, videoBackgroundSummary))
    
    val videoSelectTitle = stringResource(id = R.string.settings_select_video)
    val videoSelectedText = stringResource(id = R.string.settings_video_selected)
    val showVideoPicker = BackgroundConfig.isCustomBackgroundEnabled && BackgroundConfig.isVideoBackgroundEnabled && (matchAppearance || shouldShow(searchText, videoSelectTitle, videoSelectedText))
    
    val videoClearTitle = stringResource(id = R.string.settings_clear_video_background)
    val showVideoClear = BackgroundConfig.isCustomBackgroundEnabled && BackgroundConfig.isVideoBackgroundEnabled && !BackgroundConfig.videoBackgroundUri.isNullOrEmpty() && (matchAppearance || shouldShow(searchText, videoClearTitle))
    
    val multiBackgroundTitle = stringResource(id = R.string.settings_multi_background_mode)
    val multiBackgroundSummary = stringResource(id = R.string.settings_multi_background_mode_summary)
    val showMultiBackgroundSwitch = BackgroundConfig.isCustomBackgroundEnabled && !BackgroundConfig.isVideoBackgroundEnabled && (matchAppearance || shouldShow(searchText, multiBackgroundTitle, multiBackgroundSummary))
    
    // Single Background Selector
    val singleSelectTitle = stringResource(id = R.string.settings_select_background_image)
    val singleSelectedText = stringResource(id = R.string.settings_background_selected)
    val showSinglePicker = BackgroundConfig.isCustomBackgroundEnabled && !BackgroundConfig.isVideoBackgroundEnabled && !BackgroundConfig.isMultiBackgroundEnabled && (matchAppearance || shouldShow(searchText, singleSelectTitle, singleSelectedText))
    
    val singleClearTitle = stringResource(id = R.string.settings_clear_background)
    val showSingleClear = BackgroundConfig.isCustomBackgroundEnabled && !BackgroundConfig.isVideoBackgroundEnabled && !BackgroundConfig.isMultiBackgroundEnabled && !BackgroundConfig.customBackgroundUri.isNullOrEmpty() && (matchAppearance || shouldShow(searchText, singleClearTitle))

    // Custom Font
    val customFontTitle = stringResource(id = R.string.settings_custom_font)
    val customFontSummary = stringResource(id = R.string.settings_custom_font_summary)
    val customFontEnabledText = stringResource(id = R.string.settings_custom_font_enabled)
    val customFontSelectedText = stringResource(id = R.string.settings_font_selected)
    val showCustomFontSwitch = matchAppearance || shouldShow(searchText, customFontTitle, customFontSummary, customFontEnabledText, customFontSelectedText)
    
    val selectFontTitle = stringResource(id = R.string.settings_select_font_file)
    val showSelectFont = FontConfig.isCustomFontEnabled && (matchAppearance || shouldShow(searchText, selectFontTitle))
    
    val clearFontTitle = stringResource(id = R.string.settings_clear_font)
    val showClearFont = FontConfig.isCustomFontEnabled && FontConfig.customFontFilename != null && (matchAppearance || shouldShow(searchText, clearFontTitle))

    // Theme Store
    val themeStoreTitle = stringResource(id = R.string.theme_store_title)
    val showThemeStore = matchAppearance || shouldShow(searchText, themeStoreTitle)
    
    val saveThemeTitle = stringResource(id = R.string.settings_save_theme)
    val showSaveTheme = matchAppearance || shouldShow(searchText, saveThemeTitle)
    
    val importThemeTitle = stringResource(id = R.string.settings_import_theme)
    val showImportTheme = matchAppearance || shouldShow(searchText, importThemeTitle)

    val resetThemeTitle = stringResource(id = R.string.settings_reset_theme)
    val showResetTheme = matchAppearance || shouldShow(searchText, resetThemeTitle)

    val showAppearanceCategory = showNightModeFollowSys || showNightModeEnabled || showUseSystemColor || showCustomColor || showHomeLayout || showNavLayout || showGridBackgroundSwitch || showGridOpacity || showGridTextHidden || showGridModeHidden || showListModeHidden || showListCardHideStatusBadge || showCustomBackgroundSwitch || showCustomFontSwitch || showThemeStore || showSaveTheme || showImportTheme || showResetTheme || showAdvancedTitleStyleSwitch

    val showThemeChooseDialog = remember { mutableStateOf(false) }
    val showHomeLayoutChooseDialog = remember { mutableStateOf(false) }


    if (showAppearanceCategory) {
        SettingsCategory(icon = Icons.Filled.ColorLens, title = appearanceTitle, isSearching = searchText.isNotEmpty()) {
            
            // Night Mode Follow System
            if (showNightModeFollowSys) {
                SwitchItem(
                    icon = Icons.Filled.DarkMode,
                    title = nightModeFollowSysTitle,
                    summary = nightModeFollowSysSummary,
                    checked = nightModeFollowSys,
                    onCheckedChange = {
                        nightModeFollowSys = it
                        prefs.edit().putBoolean("night_mode_follow_sys", it).apply()
                        refreshTheme.value = true
                    }
                )
            }

            // Night Mode Enabled
            if (showNightModeEnabled) {
                SwitchItem(
                    icon = Icons.Filled.DarkMode,
                    title = nightModeEnabledTitle,
                    summary = null,
                    checked = nightModeEnabled,
                    onCheckedChange = {
                        nightModeEnabled = it
                        prefs.edit().putBoolean("night_mode_enabled", it).apply()
                        refreshTheme.value = true
                    }
                )
            }

            // Use System Color
            if (showUseSystemColor) {
                SwitchItem(
                    icon = Icons.Filled.InvertColors,
                    title = useSystemColorTitle,
                    summary = useSystemColorSummary,
                    checked = useSystemDynamicColor,
                    onCheckedChange = {
                        useSystemDynamicColor = it
                        prefs.edit().putBoolean("use_system_color_theme", it).apply()
                        refreshTheme.value = true
                    }
                )
            }

            // Custom Color
            if (showCustomColor) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(text = customColorTitle) },
                    modifier = Modifier.clickable {
                        showThemeChooseDialog.value = true
                    },
                    supportingContent = {
                        Text(
                            text = customColorValue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.ColorLens, null) }
                )
            }

            // Home Layout
            if (showHomeLayout) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(text = homeLayoutTitle) },
                    modifier = Modifier.clickable {
                        showHomeLayoutChooseDialog.value = true
                    },
                    supportingContent = {
                        Text(
                            text = homeLayoutValue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Dashboard, null) }
                )
            }

            // Nav Layout
            if (showNavLayout) {
                var expanded by remember { mutableStateOf(false) }
                val rotationState by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    label = "ArrowRotation"
                )

                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(text = navLayoutTitle) },
                    modifier = Modifier.clickable { expanded = !expanded },
                    supportingContent = {
                        Text(
                            text = navLayoutSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.ViewQuilt, null) },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.rotate(rotationState)
                        )
                    }
                )

                androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        // APM
                        if (matchAppearance || shouldShow(searchText, showNavApmTitle)) {
                            me.bmax.apatch.ui.component.CheckboxItem(
                                icon = null,
                                title = showNavApmTitle,
                                summary = null,
                                checked = showNavApm,
                                onCheckedChange = {
                                    showNavApm = it
                                    prefs.edit().putBoolean("show_nav_apm", it).apply()
                                }
                            )
                        }
                        // KPM
                        if (matchAppearance || shouldShow(searchText, showNavKpmTitle)) {
                            me.bmax.apatch.ui.component.CheckboxItem(
                                icon = null,
                                title = showNavKpmTitle,
                                summary = null,
                                checked = showNavKpm,
                                onCheckedChange = {
                                    showNavKpm = it
                                    prefs.edit().putBoolean("show_nav_kpm", it).apply()
                                }
                            )
                        }
                        // SuperUser
                        if (matchAppearance || shouldShow(searchText, showNavSuperUserTitle)) {
                            me.bmax.apatch.ui.component.CheckboxItem(
                                icon = null,
                                title = showNavSuperUserTitle,
                                summary = null,
                                checked = showNavSuperUser,
                                onCheckedChange = {
                                    showNavSuperUser = it
                                    prefs.edit().putBoolean("show_nav_superuser", it).apply()
                                }
                            )
                        }
                    }
                }
            }
            
            // Navigation Scheme Settings (独立设置项)
            val navSchemeTitle = stringResource(id = R.string.settings_nav_scheme)
            var currentNavMode by remember { mutableStateOf(prefs.getString("nav_mode", "auto") ?: "auto") }
            val navSchemeLabel = when (currentNavMode) {
                "rail" -> stringResource(R.string.settings_nav_mode_rail)
                "bottom" -> stringResource(R.string.settings_nav_mode_bottom)
                else -> stringResource(R.string.settings_nav_mode_auto)
            }
            var showNavSchemeDialog by remember { mutableStateOf(false) }
            val showNavScheme = matchAppearance || shouldShow(searchText, navSchemeTitle, navSchemeLabel)
            
            if (showNavScheme) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(text = navSchemeTitle) },
                    modifier = Modifier.clickable { showNavSchemeDialog = true },
                    supportingContent = {
                        Text(
                            text = navSchemeLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.SwapHoriz, null) }
                )
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
            
            // ... (Rest of appearance settings, similar pattern)
            // Due to length, I'm abbreviating slightly, but logic is preserved.
            
            // Grid Background
            if (showGridBackgroundSwitch) {
                SwitchItem(
                    icon = Icons.Filled.Image,
                    title = gridBackgroundTitle,
                    summary = if (BackgroundConfig.isGridWorkingCardBackgroundEnabled) gridBackgroundEnabledText else gridBackgroundSummary,
                    checked = BackgroundConfig.isGridWorkingCardBackgroundEnabled
                ) {
                    BackgroundConfig.setGridWorkingCardBackgroundEnabledState(it)
                    BackgroundConfig.save(context)
                }
            }

            if (BackgroundConfig.isGridWorkingCardBackgroundEnabled && isKernelSuStyle) {
                 if (showGridDualOpacitySwitch) {
                    SwitchItem(
                        icon = Icons.Filled.DarkMode,
                        title = gridDualOpacityTitle,
                        summary = null,
                        checked = BackgroundConfig.isGridDualOpacityEnabled,
                        onCheckedChange = {
                            BackgroundConfig.setGridDualOpacityEnabledState(it)
                            BackgroundConfig.save(context)
                        }
                    )
                }

                // Opacity
                if (!BackgroundConfig.isGridDualOpacityEnabled && showGridOpacity) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(gridOpacityTitle) },
                        supportingContent = {
                            androidx.compose.material3.Slider(
                                value = BackgroundConfig.gridWorkingCardBackgroundOpacity,
                                onValueChange = { BackgroundConfig.setGridWorkingCardBackgroundOpacityValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                )
                            )
                        }
                    )
                } else {
                    if (BackgroundConfig.isGridDualOpacityEnabled && showGridDayOpacity) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(gridDayOpacityTitle) },
                            supportingContent = {
                                androidx.compose.material3.Slider(
                                    value = BackgroundConfig.gridWorkingCardBackgroundDayOpacity,
                                    onValueChange = { BackgroundConfig.setGridWorkingCardBackgroundDayOpacityValue(it) },
                                    onValueChangeFinished = { BackgroundConfig.save(context) },
                                    valueRange = 0f..1f,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                        activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                    )
                                )
                            }
                        )
                    }
                    if (BackgroundConfig.isGridDualOpacityEnabled && showGridNightOpacity) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(gridNightOpacityTitle) },
                            supportingContent = {
                                androidx.compose.material3.Slider(
                                    value = BackgroundConfig.gridWorkingCardBackgroundNightOpacity,
                                    onValueChange = { BackgroundConfig.setGridWorkingCardBackgroundNightOpacityValue(it) },
                                    onValueChangeFinished = { BackgroundConfig.save(context) },
                                    valueRange = 0f..1f,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                        activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                    )
                                )
                            }
                        )
                    }
                }

                // Dim
                if (showGridDim) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(gridDimTitle) },
                        supportingContent = {
                            androidx.compose.material3.Slider(
                                value = BackgroundConfig.gridWorkingCardBackgroundDim,
                                onValueChange = { BackgroundConfig.setGridWorkingCardBackgroundDimValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                )
                            )
                        }
                    )
                }

                 // Picker
                 if (showGridPicker) {
                     ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(text = gridSelectTitle) },
                        supportingContent = {
                            if (!BackgroundConfig.gridWorkingCardBackgroundUri.isNullOrEmpty()) {
                                Text(
                                    text = gridSelectedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        leadingContent = { Icon(painterResource(id = R.drawable.ic_custom_background), null) },
                        modifier = Modifier.clickable {
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
                    )
                }

                // Clear
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
                if (showGridClear) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(text = gridClearTitle) },
                        leadingContent = { Icon(painterResource(id = R.drawable.ic_clear_background), null) },
                        modifier = Modifier.clickable {
                            clearGridBackgroundDialog.showConfirm(
                                title = context.getString(R.string.settings_clear_grid_working_card_background),
                                content = context.getString(R.string.settings_clear_grid_working_card_background_confirm),
                                markdown = false
                            )
                        }
                    )
                }
            }

            // Hide Checkmark
             if (showGridCheckHidden) {
                 SwitchItem(
                    icon = Icons.Filled.VisibilityOff,
                    title = gridCheckHiddenTitle,
                    summary = gridCheckHiddenSummary,
                    checked = BackgroundConfig.isGridWorkingCardCheckHidden,
                    onCheckedChange = {
                        BackgroundConfig.setGridWorkingCardCheckHiddenState(it)
                        BackgroundConfig.save(context)
                    }
                )
            }

            // Hide Text
             if (showGridTextHidden) {
                 SwitchItem(
                    icon = Icons.Filled.VisibilityOff,
                    title = gridTextHiddenTitle,
                    summary = gridTextHiddenSummary,
                    checked = BackgroundConfig.isGridWorkingCardTextHidden,
                    onCheckedChange = {
                        BackgroundConfig.setGridWorkingCardTextHiddenState(it)
                        BackgroundConfig.save(context)
                    }
                )
            }

            // Hide Mode
              if (showGridModeHidden) {
                  SwitchItem(
                     icon = Icons.Filled.VisibilityOff,
                     title = gridModeHiddenTitle,
                     summary = gridModeHiddenSummary,
                     checked = BackgroundConfig.isGridWorkingCardModeHidden,
                     onCheckedChange = {
                         BackgroundConfig.setGridWorkingCardModeHiddenState(it)
                         BackgroundConfig.save(context)
                     }
                 )
             }

            // Custom Badge Text (Grid Mode)
            if (showCustomBadgeTextGrid) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(customBadgeTextTitle) },
                    supportingContent = {
                        Text(
                            text = currentBadgeTextMode,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.NewReleases, null) },
                    modifier = Modifier.clickable { showCustomBadgeTextDialog.value = true }
                )
            }

            // Hide List Status Badge
            if (showListCardHideStatusBadge) {
                SwitchItem(
                    icon = Icons.Filled.EmojiEmotions,
                    title = listCardHideStatusBadgeTitle,
                    summary = listCardHideStatusBadgeSummary,
                    checked = BackgroundConfig.isListWorkingCardModeHidden,
                    onCheckedChange = {
                        BackgroundConfig.setListWorkingCardModeHiddenState(it)
                        BackgroundConfig.save(context)
                    }
                )
            }

            // Custom Badge Text (List Mode)
            if (showCustomBadgeTextList) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(customBadgeTextTitle) },
                    supportingContent = {
                        Text(
                            text = currentBadgeTextMode,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.NewReleases, null) },
                    modifier = Modifier.clickable { showCustomBadgeTextDialog.value = true }
                )
            }

            if (showCustomBadgeTextDialog.value) {
                AlertDialog(
                    onDismissRequest = { showCustomBadgeTextDialog.value = false },
                    title = { Text(customBadgeTextTitle) },
                    text = {
                        Column {
                            Text(customBadgeTextSummary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 16.dp))
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

            // Advanced Title Style
            if (showAdvancedTitleStyleSwitch) {
                SwitchItem(
                    icon = Icons.Filled.Brush,
                    title = advancedTitleStyleTitle,
                    summary = if (BackgroundConfig.isAdvancedTitleStyleEnabled) advancedTitleStyleEnabledText else advancedTitleStyleSummary,
                    checked = BackgroundConfig.isAdvancedTitleStyleEnabled
                ) {
                    BackgroundConfig.setAdvancedTitleStyleEnabledState(it)
                    BackgroundConfig.save(context)
                    refreshTheme.value = true
                }
            }

            if (BackgroundConfig.isAdvancedTitleStyleEnabled) {
                if (showTitleImageDayOpacity) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(titleImageDayOpacityTitle) },
                        supportingContent = {
                            androidx.compose.material3.Slider(
                                value = BackgroundConfig.titleImageDayOpacity,
                                onValueChange = { BackgroundConfig.setTitleImageDayOpacityValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                )
                            )
                        }
                    )
                }

                if (showTitleImageNightOpacity) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(titleImageNightOpacityTitle) },
                        supportingContent = {
                            androidx.compose.material3.Slider(
                                value = BackgroundConfig.titleImageNightOpacity,
                                onValueChange = { BackgroundConfig.setTitleImageNightOpacityValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                )
                            )
                        }
                    )
                }

                if (showTitleImageDim) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(titleImageDimTitle) },
                        supportingContent = {
                            androidx.compose.material3.Slider(
                                value = BackgroundConfig.titleImageDim,
                                onValueChange = { BackgroundConfig.setTitleImageDimValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                )
                            )
                        }
                    )
                }

                if (showTitleImageOffsetX) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(titleImageOffsetXTitle) },
                        supportingContent = {
                            androidx.compose.material3.Slider(
                                value = BackgroundConfig.titleImageOffsetX,
                                onValueChange = { BackgroundConfig.setTitleImageOffsetXValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = -1f..1f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                )
                            )
                        }
                    )
                }

                if (showTitleImagePicker) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(text = titleImageSelectTitle) },
                        supportingContent = {
                            if (!BackgroundConfig.titleImageUri.isNullOrEmpty()) {
                                Text(
                                    text = titleImageSelectedText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        leadingContent = { Icon(painterResource(id = R.drawable.ic_custom_background), null) },
                        modifier = Modifier.clickable {
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
                    )
                }

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
                if (showTitleImageClear) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(text = titleImageClearTitle) },
                        leadingContent = { Icon(painterResource(id = R.drawable.ic_clear_background), null) },
                        modifier = Modifier.clickable {
                            clearTitleImageDialog.showConfirm(
                                title = context.getString(R.string.settings_clear_title_image),
                                content = context.getString(R.string.settings_clear_title_image_confirm),
                                markdown = false
                            )
                        }
                    )
                }
            }

            // Banner Settings
            if (showBannerEnabled) {
                SwitchItem(
                    icon = Icons.Filled.ViewCarousel,
                    title = bannerEnabledTitle,
                    summary = bannerEnabledSummary,
                    checked = BackgroundConfig.isBannerEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setBannerEnabledState(it)
                        BackgroundConfig.save(context)
                    }
                )
            }

            if (showFolkBanner) {
                SwitchItem(
                    icon = Icons.Filled.Edit,
                    title = folkBannerTitle,
                    summary = folkBannerSummary,
                    checked = BackgroundConfig.isFolkBannerEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setFolkBannerEnabledState(it)
                        BackgroundConfig.save(context)
                    }
                )
            }

            // Banner API Mode Switch
            if (showBannerApiModeSwitch) {
                SwitchItem(
                    icon = Icons.Filled.CloudDownload,
                    title = bannerApiModeTitle,
                    summary = bannerApiModeSummary,
                    checked = BackgroundConfig.isBannerApiModeEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setBannerApiModeEnabledState(it)
                        BackgroundConfig.save(context)
                    }
                )
            }

            // Banner API Source Config Entry
            if (showBannerApiSource) {
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

                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(bannerApiSourceTitle) },
                    supportingContent = {
                        Text(
                            text = apiSourceSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    leadingContent = { Icon(Icons.Filled.Settings, null) },
                    modifier = Modifier.clickable { showBannerApiConfigDialog.value = true }
                )

                // Banner API Config Dialog
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
            }

            // API Marketplace Entry
            if (showApiMarketplace) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(apiMarketplaceTitle) },
                    leadingContent = { Icon(Icons.Filled.Store, null) },
                    modifier = Modifier.clickable {
                        onNavigateToApiMarketplace()
                    }
                )
            }

            if (showBannerCustomOpacitySwitch) {
                SwitchItem(
                    icon = Icons.Filled.Opacity,
                    title = bannerCustomOpacityTitle,
                    summary = bannerCustomOpacitySummary,
                    checked = BackgroundConfig.isBannerCustomOpacityEnabled,
                    onCheckedChange = {
                        BackgroundConfig.setBannerCustomOpacityEnabledState(it)
                        BackgroundConfig.save(context)
                    }
                )
            }

            if (showBannerOpacity) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(bannerOpacityTitle) },
                    supportingContent = {
                        Slider(
                            value = BackgroundConfig.bannerCustomOpacity,
                            onValueChange = { BackgroundConfig.setBannerCustomOpacityValue(it) },
                            onValueChangeFinished = { BackgroundConfig.save(context) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                            )
                        )
                    }
                )
            }

            // Custom Background
            if (showCustomBackgroundSwitch) {
                 SwitchItem(
                    icon = Icons.Filled.Image,
                    title = customBackgroundTitle,
                    summary = if (BackgroundConfig.isCustomBackgroundEnabled) customBackgroundEnabledText else customBackgroundSummary,
                    checked = BackgroundConfig.isCustomBackgroundEnabled
                ) {
                    BackgroundConfig.setCustomBackgroundEnabledState(it)
                    BackgroundConfig.save(context)
                    refreshTheme.value = true
                }
            }
            
            if (BackgroundConfig.isCustomBackgroundEnabled) {
                 // Single Background Selector (Moved from bottom)
                 if (!BackgroundConfig.isVideoBackgroundEnabled && !BackgroundConfig.isMultiBackgroundEnabled) {
                     if (showSinglePicker) {
                         ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(text = singleSelectTitle) },
                            supportingContent = {
                                if (!BackgroundConfig.customBackgroundUri.isNullOrEmpty()) {
                                    Text(text = singleSelectedText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                }
                            },
                            leadingContent = { Icon(painterResource(id = R.drawable.ic_custom_background), null) },
                            modifier = Modifier.clickable {
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
                        )
                     }
                     
                     // Clear button (Single mode only)
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
                        if (showSingleClear) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(text = singleClearTitle) },
                                leadingContent = { Icon(painterResource(id = R.drawable.ic_clear_background), null) },
                                modifier = Modifier.clickable {
                                    clearBackgroundDialog.showConfirm(title = singleClearTitle, content = context.getString(R.string.settings_clear_background_confirm), markdown = false)
                                }
                            )
                        }
                     }
                 }

                 if (showCustomDualDimSwitch) {
                    SwitchItem(
                        icon = Icons.Filled.DarkMode,
                        title = customDualDimTitle,
                        summary = null,
                        checked = BackgroundConfig.isDualBackgroundDimEnabled
                    ) {
                        BackgroundConfig.setDualBackgroundDimEnabledState(it)
                        BackgroundConfig.save(context)
                        refreshTheme.value = true
                    }
                 }

                 // Sliders
                 if (showCustomOpacity) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(customOpacityTitle) },
                        supportingContent = {
                            androidx.compose.material3.Slider(
                                value = BackgroundConfig.customBackgroundOpacity,
                                onValueChange = { BackgroundConfig.setCustomBackgroundOpacityValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                )
                            )
                        }
                    )
                 }
                 
                 if (showCustomBlur) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(customBlurTitle) },
                        supportingContent = {
                            androidx.compose.material3.Slider(
                                value = BackgroundConfig.customBackgroundBlur,
                                onValueChange = { BackgroundConfig.setCustomBackgroundBlurValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..50f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                )
                            )
                        }
                    )
                 }

                 if (!BackgroundConfig.isDualBackgroundDimEnabled && showCustomDim) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(customDimTitle) },
                        supportingContent = {
                            androidx.compose.material3.Slider(
                                value = BackgroundConfig.customBackgroundDim,
                                onValueChange = { BackgroundConfig.setCustomBackgroundDimValue(it) },
                                onValueChangeFinished = { BackgroundConfig.save(context) },
                                valueRange = 0f..1f,
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                )
                            )
                        }
                    )
                 } else {
                    if (BackgroundConfig.isDualBackgroundDimEnabled && showCustomDayDim) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(customDayDimTitle) },
                            supportingContent = {
                                androidx.compose.material3.Slider(
                                    value = BackgroundConfig.customBackgroundDayDim,
                                    onValueChange = { BackgroundConfig.setCustomBackgroundDayDimValue(it) },
                                    onValueChangeFinished = { BackgroundConfig.save(context) },
                                    valueRange = 0f..1f,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                        activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                    )
                                )
                            }
                        )
                    }
                    if (BackgroundConfig.isDualBackgroundDimEnabled && showCustomNightDim) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(customNightDimTitle) },
                            supportingContent = {
                                androidx.compose.material3.Slider(
                                    value = BackgroundConfig.customBackgroundNightDim,
                                    onValueChange = { BackgroundConfig.setCustomBackgroundNightDimValue(it) },
                                    onValueChangeFinished = { BackgroundConfig.save(context) },
                                    valueRange = 0f..1f,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                        activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                    )
                                )
                            }
                        )
                    }
                 }

                 if (showVideoBackgroundSwitch) {
                     SwitchItem(
                        icon = Icons.Filled.PlayArrow,
                        title = videoBackgroundTitle,
                        summary = videoBackgroundSummary,
                        checked = BackgroundConfig.isVideoBackgroundEnabled
                    ) {
                        BackgroundConfig.setVideoBackgroundEnabledState(it)
                        BackgroundConfig.save(context)
                        refreshTheme.value = true
                    }
                 }
                 
                 if (BackgroundConfig.isVideoBackgroundEnabled) {
                     // Video logic ...
                     if (showVideoPicker) {
                         ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(text = videoSelectTitle) },
                            supportingContent = {
                                if (!BackgroundConfig.videoBackgroundUri.isNullOrEmpty()) {
                                    Text(text = videoSelectedText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                                }
                            },
                            leadingContent = { Icon(Icons.Filled.PlayArrow, null) },
                            modifier = Modifier.clickable {
                                try {
                                    pickVideoLauncher.launch("video/*")
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                     }

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

                    if (showVideoClear) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(text = videoClearTitle) },
                            leadingContent = { Icon(painterResource(id = R.drawable.ic_clear_background), null) },
                            modifier = Modifier.clickable {
                                clearVideoDialog.showConfirm(
                                    title = videoClearTitle,
                                    content = context.getString(R.string.settings_clear_video_background_confirm),
                                    markdown = false
                                )
                            }
                        )
                    }
                    
                    if (showVideoVolume) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text(videoVolumeTitle) },
                            supportingContent = {
                                androidx.compose.material3.Slider(
                                    value = BackgroundConfig.videoVolume,
                                    onValueChange = { BackgroundConfig.setVideoVolumeValue(it) },
                                    onValueChangeFinished = { BackgroundConfig.save(context) },
                                    valueRange = 0f..1f,
                                    colors = androidx.compose.material3.SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f),
                                        activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 1f)
                                    )
                                )
                            },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.VolumeUp, null) }
                        )
                    }
                 } else {
                     // Image logic ...
                     if (showMultiBackgroundSwitch) {
                         SwitchItem(
                            icon = Icons.AutoMirrored.Filled.ViewQuilt,
                            title = multiBackgroundTitle,
                            summary = multiBackgroundSummary,
                            checked = BackgroundConfig.isMultiBackgroundEnabled
                        ) {
                            BackgroundConfig.setMultiBackgroundEnabledState(it)
                            BackgroundConfig.save(context)
                            refreshTheme.value = true
                        }
                     }
                     
                     if (BackgroundConfig.isMultiBackgroundEnabled) {
                        // Multi selectors
                        val items = listOf(
                            Triple(R.string.settings_select_home_background, "home", BackgroundConfig.homeBackgroundUri),
                            Triple(R.string.settings_select_kernel_background, "kernel", BackgroundConfig.kernelBackgroundUri),
                            Triple(R.string.settings_select_superuser_background, "superuser", BackgroundConfig.superuserBackgroundUri),
                            Triple(R.string.settings_select_system_module_background, "system", BackgroundConfig.systemModuleBackgroundUri),
                            Triple(R.string.settings_select_settings_background, "settings", BackgroundConfig.settingsBackgroundUri)
                        )
                        items.forEach { (titleRes, type, uri) ->
                            val title = stringResource(id = titleRes)
                            if (matchAppearance || shouldShow(searchText, title)) {
                                ListItem(
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    headlineContent = { Text(text = title) },
                                    supportingContent = {
                                        if (!uri.isNullOrEmpty()) {
                                            Text(
                                                text = stringResource(id = R.string.settings_background_selected),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    },
                                    leadingContent = { Icon(painterResource(id = R.drawable.ic_custom_background), null) },
                                    modifier = Modifier.clickable {
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
                                )
                            }
                        }
                     }
                 }
            }

            // Custom Font
            if (showCustomFontSwitch) {
                SwitchItem(
                    icon = Icons.Filled.TextFields,
                    title = customFontTitle,
                    summary = if (customFontEnabled) {
                        if (FontConfig.customFontFilename != null) customFontSelectedText else customFontEnabledText
                    } else {
                        customFontSummary
                    },
                    checked = customFontEnabled
                ) {
                    customFontEnabled = it
                    FontConfig.setCustomFontEnabledState(it)
                    FontConfig.save(context)
                    refreshTheme.value = true
                }
            }
            
            if (FontConfig.isCustomFontEnabled) {
                if (showSelectFont) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(text = selectFontTitle) },
                        leadingContent = { Icon(Icons.Filled.Folder, null) },
                        modifier = Modifier.clickable {
                            try {
                                pickFontLauncher.launch("*/*")
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
                
                if (showClearFont) {
                    val clearFontDialog = rememberConfirmDialog(
                        onConfirm = {
                            FontConfig.clearFont(context)
                            refreshTheme.value = true
                            scope.launch {
                                snackBarHost.showSnackbar(message = context.getString(R.string.settings_font_cleared))
                            }
                        }
                    )
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text(text = clearFontTitle) },
                        leadingContent = { Icon(Icons.Filled.RemoveFromQueue, null) },
                        modifier = Modifier.clickable {
                            clearFontDialog.showConfirm(
                                title = clearFontTitle,
                                content = context.getString(R.string.settings_clear_font_confirm)
                            )
                        }
                    )
                }
            }
            
            // Theme Store
            if (showThemeStore) {
                 ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(text = themeStoreTitle) },
                    leadingContent = { Icon(Icons.Filled.ShoppingCart, null) },
                    modifier = Modifier.clickable {
                        onNavigateToThemeStore()
                    }
                )
            }
            
            if (showSaveTheme) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(text = saveThemeTitle) },
                    leadingContent = { Icon(Icons.Filled.Save, null) },
                    modifier = Modifier.clickable {
                        showExportDialog.value = true
                    }
                )
            }
            
            if (showImportTheme) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(text = importThemeTitle) },
                    leadingContent = { Icon(Icons.Filled.Folder, null) },
                    modifier = Modifier.clickable {
                        showFilePicker.value = true
                    }
                )
            }

            if (showResetTheme) {
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
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(text = resetThemeTitle) },
                    leadingContent = { Icon(Icons.Filled.Restore, null) },
                    modifier = Modifier.clickable {
                        resetThemeDialog.showConfirm(
                            title = resetThemeTitle,
                            content = context.getString(R.string.settings_reset_theme_confirm)
                        )
                    }
                )
            }
        }
    }
    
    // Dialogs
    if (showThemeChooseDialog.value) {
        ThemeChooseDialog(showThemeChooseDialog)
    }

    if (showHomeLayoutChooseDialog.value) {
        HomeLayoutChooseDialog(showHomeLayoutChooseDialog)
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

// Dialog definitions from Settings.kt (ThemeChooseDialog, HomeLayoutChooseDialog, ThemeExportDialog, ThemeImportDialog, etc)
// ... (Including these at the end of file, same as GeneralSettings.kt approach)

// Need to define colorNameToString, homeLayoutStyleToString here as well or move to shared
// Duplicating for now to ensure self-contained files.

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
                
                val currentStyle = prefs.getString("home_layout_style", "circle")
                
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
    val modes = listOf(
        "auto" to R.string.settings_nav_mode_auto,
        "bottom" to R.string.settings_nav_mode_bottom,
        "rail" to R.string.settings_nav_mode_rail
    )

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
            Column {
                modes.forEach { (mode, labelRes) ->
                    ListItem(
                        headlineContent = { Text(text = stringResource(labelRes)) },
                        modifier = Modifier.clickable {
                            onModeSelected(mode)
                        },
                        trailingContent = {
                            if (mode == currentMode) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
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

                // 提示信息
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
