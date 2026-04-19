package me.bmax.apatch.ui.theme

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.util.SafeUriResolver
import java.io.File
import java.io.FileOutputStream

/**
 * 背景配置管理类
 */
object BackgroundConfig {
    // State
    var customBackgroundUri: String? by mutableStateOf(null)
        private set
    var isCustomBackgroundEnabled: Boolean by mutableStateOf(false)
        private set
    var customBackgroundOpacity: Float by mutableStateOf(0.5f)
        private set
    var customBackgroundBlur: Float by mutableStateOf(0.2f)
        private set
    var customBackgroundDim: Float by mutableStateOf(0.0f)
        private set
    var isDualBackgroundDimEnabled: Boolean by mutableStateOf(true)
        private set
    var customBackgroundDayDim: Float by mutableStateOf(0.0f)
        private set
    var customBackgroundNightDim: Float by mutableStateOf(0.5f)
        private set

    // Video Background
    var videoBackgroundUri: String? by mutableStateOf(null)
        private set
    var isVideoBackgroundEnabled: Boolean by mutableStateOf(false)
        private set
    var videoVolume: Float by mutableStateOf(0f)
        private set

    // Grid Layout Working Card Background
    var gridWorkingCardBackgroundUri: String? by mutableStateOf(null)
        private set
    var isGridWorkingCardBackgroundEnabled: Boolean by mutableStateOf(false)
        private set
    var gridWorkingCardBackgroundOpacity: Float by mutableStateOf(1.0f)
        private set
    var gridWorkingCardBackgroundDim: Float by mutableStateOf(0.3f)
        private set
    var isGridDualOpacityEnabled: Boolean by mutableStateOf(false)
        private set
    var gridWorkingCardBackgroundDayOpacity: Float by mutableStateOf(1.0f)
        private set
    var gridWorkingCardBackgroundNightOpacity: Float by mutableStateOf(1.0f)
        private set
    var isGridWorkingCardCheckHidden: Boolean by mutableStateOf(false)
        private set
    var isGridWorkingCardTextHidden: Boolean by mutableStateOf(false)
        private set
    var isGridWorkingCardModeHidden: Boolean by mutableStateOf(false)
        private set

    // List Layout Working Card Config
    var isListWorkingCardModeHidden: Boolean by mutableStateOf(false)
        private set

    var customBadgeTextMode: Int by mutableStateOf(0)
        private set

    // Banner Settings
    var isBannerEnabled: Boolean by mutableStateOf(true)
        private set
    var isFolkBannerEnabled: Boolean by mutableStateOf(true)
        private set
    var isBannerCustomOpacityEnabled: Boolean by mutableStateOf(false)
        private set
    var bannerCustomOpacity: Float by mutableStateOf(0.5f)
        private set

    // Banner API Mode Settings
    var isBannerApiModeEnabled: Boolean by mutableStateOf(false)
        private set
    var bannerApiSource: String by mutableStateOf("")
        private set

    // Multi-Background Mode
    var isMultiBackgroundEnabled: Boolean by mutableStateOf(false)
        private set
    var homeBackgroundUri: String? by mutableStateOf(null)
        private set
    var kernelBackgroundUri: String? by mutableStateOf(null)
        private set
    var superuserBackgroundUri: String? by mutableStateOf(null)
        private set
    var systemModuleBackgroundUri: String? by mutableStateOf(null)
        private set
    var settingsBackgroundUri: String? by mutableStateOf(null)
        private set

    // Advanced Title Style
    var isAdvancedTitleStyleEnabled: Boolean by mutableStateOf(false)
        private set
    var titleImageUri: String? by mutableStateOf(null)
        private set
    var titleImageDayOpacity: Float by mutableStateOf(1.0f)
        private set
    var titleImageNightOpacity: Float by mutableStateOf(1.0f)
        private set
    var titleImageDim: Float by mutableStateOf(0.0f)
        private set
    var titleImageOffsetX: Float by mutableStateOf(0f)
        private set

    // NavBar Glass Effect (Floating mode only)
    var isNavBarGlassEnabled: Boolean by mutableStateOf(true)
        private set
    var navBarGlassBlurStrength: Float by mutableStateOf(0.7f)
        private set
    var navBarGlassTransparency: Float by mutableStateOf(0.3f)
        private set
    var navBarGlassHighlightStrength: Float by mutableStateOf(0.5f)
        private set
    var isNavBarGlassSpecularEnabled: Boolean by mutableStateOf(true)
        private set
    var isNavBarGlassInnerGlowEnabled: Boolean by mutableStateOf(true)
        private set
    var isNavBarGlassBorderEnabled: Boolean by mutableStateOf(true)
        private set
    
    private const val PREFS_NAME = "background_settings"
    private const val KEY_CUSTOM_BACKGROUND_URI = "custom_background_uri"
    private const val KEY_CUSTOM_BACKGROUND_ENABLED = "custom_background_enabled"
    private const val KEY_CUSTOM_BACKGROUND_OPACITY = "custom_background_opacity"
    private const val KEY_CUSTOM_BACKGROUND_BLUR = "custom_background_blur"
    private const val KEY_CUSTOM_BACKGROUND_DIM = "custom_background_dim"
    private const val KEY_CUSTOM_BACKGROUND_DUAL_DIM_ENABLED = "custom_background_dual_dim_enabled"
    private const val KEY_CUSTOM_BACKGROUND_DAY_DIM = "custom_background_day_dim"
    private const val KEY_CUSTOM_BACKGROUND_NIGHT_DIM = "custom_background_night_dim"
    
    private const val KEY_VIDEO_BACKGROUND_URI = "video_background_uri"
    private const val KEY_VIDEO_BACKGROUND_ENABLED = "video_background_enabled"
    private const val KEY_VIDEO_VOLUME = "video_volume"

    private const val KEY_GRID_WORKING_CARD_BACKGROUND_URI = "grid_working_card_background_uri"
    private const val KEY_GRID_WORKING_CARD_BACKGROUND_ENABLED = "grid_working_card_background_enabled"
    private const val KEY_GRID_WORKING_CARD_BACKGROUND_OPACITY = "grid_working_card_background_opacity"
    private const val KEY_GRID_WORKING_CARD_BACKGROUND_DIM = "grid_working_card_background_dim"
    private const val KEY_GRID_WORKING_CARD_DUAL_OPACITY_ENABLED = "grid_working_card_dual_opacity_enabled"
    private const val KEY_GRID_WORKING_CARD_BACKGROUND_DAY_OPACITY = "grid_working_card_background_day_opacity"
    private const val KEY_GRID_WORKING_CARD_BACKGROUND_NIGHT_OPACITY = "grid_working_card_background_night_opacity"
    private const val KEY_GRID_WORKING_CARD_CHECK_HIDDEN = "grid_working_card_check_hidden"
    private const val KEY_GRID_WORKING_CARD_TEXT_HIDDEN = "grid_working_card_text_hidden"
    private const val KEY_GRID_WORKING_CARD_MODE_HIDDEN = "grid_working_card_mode_hidden"
    
    private const val KEY_LIST_WORKING_CARD_MODE_HIDDEN = "list_working_card_mode_hidden"
    private const val KEY_CUSTOM_BADGE_TEXT_MODE = "custom_badge_text_mode"

    private const val KEY_BANNER_ENABLED = "banner_enabled"
    private const val KEY_FOLK_BANNER_ENABLED = "folk_banner_enabled"
    private const val KEY_BANNER_CUSTOM_OPACITY_ENABLED = "banner_custom_opacity_enabled"
    private const val KEY_BANNER_CUSTOM_OPACITY = "banner_custom_opacity"

    private const val KEY_BANNER_API_MODE_ENABLED = "banner_api_mode_enabled"
    private const val KEY_BANNER_API_SOURCE = "banner_api_source"
    // Legacy key for migration
    private const val KEY_FOLK_BANNER_API_SOURCE = "folk_banner_api_source"

    private const val KEY_MULTI_BACKGROUND_ENABLED = "multi_background_enabled"
    private const val KEY_HOME_BACKGROUND_URI = "home_background_uri"
    private const val KEY_KERNEL_BACKGROUND_URI = "kernel_background_uri"
    private const val KEY_SUPERUSER_BACKGROUND_URI = "superuser_background_uri"
    private const val KEY_SYSTEM_MODULE_BACKGROUND_URI = "system_module_background_uri"
    private const val KEY_SETTINGS_BACKGROUND_URI = "settings_background_uri"

    private const val KEY_ADVANCED_TITLE_STYLE_ENABLED = "advanced_title_style_enabled"
    private const val KEY_TITLE_IMAGE_URI = "title_image_uri"
    private const val KEY_TITLE_IMAGE_DAY_OPACITY = "title_image_day_opacity"
    private const val KEY_TITLE_IMAGE_NIGHT_OPACITY = "title_image_night_opacity"
    private const val KEY_TITLE_IMAGE_DIM = "title_image_dim"
    private const val KEY_TITLE_IMAGE_OFFSET_X = "title_image_offset_x"

    private const val KEY_NAVBAR_GLASS_ENABLED = "navbar_glass_enabled"
    private const val KEY_NAVBAR_GLASS_BLUR_STRENGTH = "navbar_glass_blur_strength"
    private const val KEY_NAVBAR_GLASS_TRANSPARENCY = "navbar_glass_transparency"
    private const val KEY_NAVBAR_GLASS_HIGHLIGHT_STRENGTH = "navbar_glass_highlight_strength"
    private const val KEY_NAVBAR_GLASS_SPECULAR_ENABLED = "navbar_glass_specular_enabled"
    private const val KEY_NAVBAR_GLASS_INNER_GLOW_ENABLED = "navbar_glass_inner_glow_enabled"
    private const val KEY_NAVBAR_GLASS_BORDER_ENABLED = "navbar_glass_border_enabled"

    private const val TAG = "BackgroundConfig"
    
    /**
     * 更新自定义背景URI
     */
    fun updateCustomBackgroundUri(uri: String?) {
        customBackgroundUri = uri
        isCustomBackgroundEnabled = uri != null
    }

    /**
     * 更新视频背景URI
     */
    fun updateVideoBackgroundUri(uri: String?) {
        videoBackgroundUri = uri
        isVideoBackgroundEnabled = uri != null
    }

    /**
     * 启用/禁用视频背景
     */
    fun setVideoBackgroundEnabledState(enabled: Boolean) {
        isVideoBackgroundEnabled = enabled
    }

    /**
     * 设置视频背景音量
     */
    fun setVideoVolumeValue(volume: Float) {
        videoVolume = volume
    }

    /**
     * 更新Grid布局工作中卡片背景URI
     */
    fun updateGridWorkingCardBackgroundUri(uri: String?) {
        gridWorkingCardBackgroundUri = uri
        isGridWorkingCardBackgroundEnabled = uri != null
    }
    
    /**
     * 启用/禁用自定义背景
     */
    fun setCustomBackgroundEnabledState(enabled: Boolean) {
        isCustomBackgroundEnabled = enabled
    }

    /**
     * 启用/禁用Grid布局工作中卡片背景
     */
    fun setGridWorkingCardBackgroundEnabledState(enabled: Boolean) {
        isGridWorkingCardBackgroundEnabled = enabled
    }

    /**
     * 设置Grid布局工作中卡片背景不透明度
     */
    fun setGridWorkingCardBackgroundOpacityValue(opacity: Float) {
        gridWorkingCardBackgroundOpacity = opacity
    }

    fun setGridDualOpacityEnabledState(enabled: Boolean) {
        isGridDualOpacityEnabled = enabled
    }

    fun setGridWorkingCardBackgroundDayOpacityValue(opacity: Float) {
        gridWorkingCardBackgroundDayOpacity = opacity
    }

    fun setGridWorkingCardBackgroundNightOpacityValue(opacity: Float) {
        gridWorkingCardBackgroundNightOpacity = opacity
    }

    fun getEffectiveGridBackgroundOpacity(isDarkTheme: Boolean): Float {
        return if (isGridDualOpacityEnabled) {
            if (isDarkTheme) gridWorkingCardBackgroundNightOpacity else gridWorkingCardBackgroundDayOpacity
        } else {
            gridWorkingCardBackgroundOpacity
        }
    }

    /**
     * 设置Grid布局工作中卡片背景暗度
     */
    fun setGridWorkingCardBackgroundDimValue(dim: Float) {
        gridWorkingCardBackgroundDim = dim
    }

    /**
     * 设置Grid布局工作中卡片Check隐藏状态
     */
    fun setGridWorkingCardCheckHiddenState(hidden: Boolean) {
        isGridWorkingCardCheckHidden = hidden
    }

    /**
     * 设置Grid布局工作中卡片文字隐藏状态
     */
    fun setGridWorkingCardTextHiddenState(hidden: Boolean) {
        isGridWorkingCardTextHidden = hidden
    }

    /**
     * 设置Grid布局工作中卡片Full/Half隐藏状态
     */
    fun setGridWorkingCardModeHiddenState(hidden: Boolean) {
        isGridWorkingCardModeHidden = hidden
    }

    /**
     * 设置List布局工作中卡片Full/Half隐藏状态
     */
    fun setListWorkingCardModeHiddenState(hidden: Boolean) {
        isListWorkingCardModeHidden = hidden
    }

    fun setCustomBadgeTextModeValue(mode: Int) {
        customBadgeTextMode = mode
    }

    fun getCustomBadgeText(): String? {
        return when (customBadgeTextMode) {
            1 -> "LKM"
            2 -> "GKI"
            3 -> "N-GKI"
            4 -> "OKI"
            5 -> "Built-in"
            else -> null
        }
    }

    /**
     * 设置自定义背景不透明度
     */
    fun setCustomBackgroundOpacityValue(opacity: Float) {
        customBackgroundOpacity = opacity
    }

    /**
     * 启用/禁用横幅
     */
    fun setBannerEnabledState(enabled: Boolean) {
        isBannerEnabled = enabled
    }

    /**
     * 启用/禁用FolkBanner
     */
    fun setFolkBannerEnabledState(enabled: Boolean) {
        isFolkBannerEnabled = enabled
    }

    /**
     * 启用/禁用横幅自定义透明度
     */
    fun setBannerCustomOpacityEnabledState(enabled: Boolean) {
        isBannerCustomOpacityEnabled = enabled
    }

    /**
     * 设置横幅自定义透明度
     */
    fun setBannerCustomOpacityValue(opacity: Float) {
        bannerCustomOpacity = opacity
    }

    /**
     * 启用/禁用横幅API模式
     */
    fun setBannerApiModeEnabledState(enabled: Boolean) {
        isBannerApiModeEnabled = enabled
    }

    /**
     * 设置横幅API源（URL或本地路径）
     */
    fun setBannerApiSourceValue(source: String) {
        bannerApiSource = source
    }

    /**
     * 获取有效的横幅API源
     */
    fun getEffectiveBannerApiSource(): String {
        return bannerApiSource
    }

    /**
     * 设置自定义背景模糊度
     */
    fun setCustomBackgroundBlurValue(blur: Float) {
        customBackgroundBlur = blur
    }

    /**
     * 设置自定义背景暗度
     */
    fun setCustomBackgroundDimValue(dim: Float) {
        customBackgroundDim = dim
    }

    fun setDualBackgroundDimEnabledState(enabled: Boolean) {
        isDualBackgroundDimEnabled = enabled
    }

    fun setCustomBackgroundDayDimValue(dim: Float) {
        customBackgroundDayDim = dim
    }

    fun setCustomBackgroundNightDimValue(dim: Float) {
        customBackgroundNightDim = dim
    }

    fun getEffectiveBackgroundDim(isDarkTheme: Boolean): Float {
        return if (isDualBackgroundDimEnabled) {
            if (isDarkTheme) customBackgroundNightDim else customBackgroundDayDim
        } else {
            customBackgroundDim
        }
    }

    // Multi-Background Setters
    fun setMultiBackgroundEnabledState(enabled: Boolean) {
        isMultiBackgroundEnabled = enabled
    }

    fun updateHomeBackgroundUri(uri: String?) {
        homeBackgroundUri = uri
    }

    fun updateKernelBackgroundUri(uri: String?) {
        kernelBackgroundUri = uri
    }

    fun updateSuperuserBackgroundUri(uri: String?) {
        superuserBackgroundUri = uri
    }

    fun updateSystemModuleBackgroundUri(uri: String?) {
        systemModuleBackgroundUri = uri
    }

    fun updateSettingsBackgroundUri(uri: String?) {
        settingsBackgroundUri = uri
    }

    // Advanced Title Style Setters
    fun setAdvancedTitleStyleEnabledState(enabled: Boolean) {
        isAdvancedTitleStyleEnabled = enabled
    }

    fun updateTitleImageUri(uri: String?) {
        titleImageUri = uri
    }

    fun setTitleImageDayOpacityValue(opacity: Float) {
        titleImageDayOpacity = opacity
    }

    fun setTitleImageNightOpacityValue(opacity: Float) {
        titleImageNightOpacity = opacity
    }

    fun getEffectiveTitleImageOpacity(isDarkTheme: Boolean): Float {
        return if (isDarkTheme) titleImageNightOpacity else titleImageDayOpacity
    }

    fun setTitleImageDimValue(dim: Float) {
        titleImageDim = dim
    }

    fun getEffectiveTitleImageDim(isDarkTheme: Boolean): Float {
        return titleImageDim
    }

    fun setTitleImageOffsetXValue(offset: Float) {
        titleImageOffsetX = offset
    }

    fun setNavBarGlassEnabledState(enabled: Boolean) {
        isNavBarGlassEnabled = enabled
    }

    fun setNavBarGlassBlurStrengthValue(strength: Float) {
        navBarGlassBlurStrength = strength
    }

    fun setNavBarGlassTransparencyValue(transparency: Float) {
        navBarGlassTransparency = transparency
    }

    fun setNavBarGlassHighlightStrengthValue(strength: Float) {
        navBarGlassHighlightStrength = strength
    }

    fun setNavBarGlassSpecularEnabledState(enabled: Boolean) {
        isNavBarGlassSpecularEnabled = enabled
    }

    fun setNavBarGlassInnerGlowEnabledState(enabled: Boolean) {
        isNavBarGlassInnerGlowEnabled = enabled
    }

    fun setNavBarGlassBorderEnabledState(enabled: Boolean) {
        isNavBarGlassBorderEnabled = enabled
    }
    
    /**
     * 保存配置到SharedPreferences
     */
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_CUSTOM_BACKGROUND_URI, customBackgroundUri)
            putBoolean(KEY_CUSTOM_BACKGROUND_ENABLED, isCustomBackgroundEnabled)
            putFloat(KEY_CUSTOM_BACKGROUND_OPACITY, customBackgroundOpacity)
            putFloat(KEY_CUSTOM_BACKGROUND_BLUR, customBackgroundBlur)
            putFloat(KEY_CUSTOM_BACKGROUND_DIM, customBackgroundDim)
            putBoolean(KEY_CUSTOM_BACKGROUND_DUAL_DIM_ENABLED, isDualBackgroundDimEnabled)
            putFloat(KEY_CUSTOM_BACKGROUND_DAY_DIM, customBackgroundDayDim)
            putFloat(KEY_CUSTOM_BACKGROUND_NIGHT_DIM, customBackgroundNightDim)
            
            putString(KEY_VIDEO_BACKGROUND_URI, videoBackgroundUri)
            putBoolean(KEY_VIDEO_BACKGROUND_ENABLED, isVideoBackgroundEnabled)
            putFloat(KEY_VIDEO_VOLUME, videoVolume)

            putString(KEY_GRID_WORKING_CARD_BACKGROUND_URI, gridWorkingCardBackgroundUri)
            putBoolean(KEY_GRID_WORKING_CARD_BACKGROUND_ENABLED, isGridWorkingCardBackgroundEnabled)
            putFloat(KEY_GRID_WORKING_CARD_BACKGROUND_OPACITY, gridWorkingCardBackgroundOpacity)
            putFloat(KEY_GRID_WORKING_CARD_BACKGROUND_DIM, gridWorkingCardBackgroundDim)
            putBoolean(KEY_GRID_WORKING_CARD_DUAL_OPACITY_ENABLED, isGridDualOpacityEnabled)
            putFloat(KEY_GRID_WORKING_CARD_BACKGROUND_DAY_OPACITY, gridWorkingCardBackgroundDayOpacity)
            putFloat(KEY_GRID_WORKING_CARD_BACKGROUND_NIGHT_OPACITY, gridWorkingCardBackgroundNightOpacity)
            putBoolean(KEY_GRID_WORKING_CARD_CHECK_HIDDEN, isGridWorkingCardCheckHidden)
            putBoolean(KEY_GRID_WORKING_CARD_TEXT_HIDDEN, isGridWorkingCardTextHidden)
            putBoolean(KEY_GRID_WORKING_CARD_MODE_HIDDEN, isGridWorkingCardModeHidden)

            putBoolean(KEY_LIST_WORKING_CARD_MODE_HIDDEN, isListWorkingCardModeHidden)
            putInt(KEY_CUSTOM_BADGE_TEXT_MODE, customBadgeTextMode)

            putBoolean(KEY_BANNER_ENABLED, isBannerEnabled)
            putBoolean(KEY_FOLK_BANNER_ENABLED, isFolkBannerEnabled)
            putBoolean(KEY_BANNER_CUSTOM_OPACITY_ENABLED, isBannerCustomOpacityEnabled)
            putFloat(KEY_BANNER_CUSTOM_OPACITY, bannerCustomOpacity)

            putBoolean(KEY_BANNER_API_MODE_ENABLED, isBannerApiModeEnabled)
            putString(KEY_BANNER_API_SOURCE, bannerApiSource)
            // Remove legacy key
            remove(KEY_FOLK_BANNER_API_SOURCE)

            putBoolean(KEY_MULTI_BACKGROUND_ENABLED, isMultiBackgroundEnabled)
            putString(KEY_HOME_BACKGROUND_URI, homeBackgroundUri)
            putString(KEY_KERNEL_BACKGROUND_URI, kernelBackgroundUri)
            putString(KEY_SUPERUSER_BACKGROUND_URI, superuserBackgroundUri)
            putString(KEY_SYSTEM_MODULE_BACKGROUND_URI, systemModuleBackgroundUri)
            putString(KEY_SETTINGS_BACKGROUND_URI, settingsBackgroundUri)

            putBoolean(KEY_ADVANCED_TITLE_STYLE_ENABLED, isAdvancedTitleStyleEnabled)
            putString(KEY_TITLE_IMAGE_URI, titleImageUri)
            putFloat(KEY_TITLE_IMAGE_DAY_OPACITY, titleImageDayOpacity)
            putFloat(KEY_TITLE_IMAGE_NIGHT_OPACITY, titleImageNightOpacity)
            putFloat(KEY_TITLE_IMAGE_DIM, titleImageDim)
            putFloat(KEY_TITLE_IMAGE_OFFSET_X, titleImageOffsetX)

            putBoolean(KEY_NAVBAR_GLASS_ENABLED, isNavBarGlassEnabled)
            putFloat(KEY_NAVBAR_GLASS_BLUR_STRENGTH, navBarGlassBlurStrength)
            putFloat(KEY_NAVBAR_GLASS_TRANSPARENCY, navBarGlassTransparency)
            putFloat(KEY_NAVBAR_GLASS_HIGHLIGHT_STRENGTH, navBarGlassHighlightStrength)
            putBoolean(KEY_NAVBAR_GLASS_SPECULAR_ENABLED, isNavBarGlassSpecularEnabled)
            putBoolean(KEY_NAVBAR_GLASS_INNER_GLOW_ENABLED, isNavBarGlassInnerGlowEnabled)
            putBoolean(KEY_NAVBAR_GLASS_BORDER_ENABLED, isNavBarGlassBorderEnabled)

            apply()
        }
    }
    
    /**
     * 从SharedPreferences加载配置
     */
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uri = prefs.getString(KEY_CUSTOM_BACKGROUND_URI, null)
        val enabled = prefs.getBoolean(KEY_CUSTOM_BACKGROUND_ENABLED, false)
        val opacity = prefs.getFloat(KEY_CUSTOM_BACKGROUND_OPACITY, 0.5f)
        val blur = prefs.getFloat(KEY_CUSTOM_BACKGROUND_BLUR, 0.2f)
        val dim = prefs.getFloat(KEY_CUSTOM_BACKGROUND_DIM, 0.0f)
        val dualDimEnabled = prefs.getBoolean(KEY_CUSTOM_BACKGROUND_DUAL_DIM_ENABLED, true)
        val dayDim = prefs.getFloat(KEY_CUSTOM_BACKGROUND_DAY_DIM, 0.0f)
        val nightDim = prefs.getFloat(KEY_CUSTOM_BACKGROUND_NIGHT_DIM, 0.5f)
        
        val videoUri = prefs.getString(KEY_VIDEO_BACKGROUND_URI, null)
        val videoEnabled = prefs.getBoolean(KEY_VIDEO_BACKGROUND_ENABLED, false)
        val videoVol = prefs.getFloat(KEY_VIDEO_VOLUME, 0f)

        val gridUri = prefs.getString(KEY_GRID_WORKING_CARD_BACKGROUND_URI, null)
        val gridEnabled = prefs.getBoolean(KEY_GRID_WORKING_CARD_BACKGROUND_ENABLED, false)
        val gridOpacity = prefs.getFloat(KEY_GRID_WORKING_CARD_BACKGROUND_OPACITY, 1.0f)
        val gridDim = prefs.getFloat(KEY_GRID_WORKING_CARD_BACKGROUND_DIM, 0.3f)
        val gridDualOpacityEnabled = prefs.getBoolean(KEY_GRID_WORKING_CARD_DUAL_OPACITY_ENABLED, false)
        val gridDayOpacity = prefs.getFloat(KEY_GRID_WORKING_CARD_BACKGROUND_DAY_OPACITY, 1.0f)
        val gridNightOpacity = prefs.getFloat(KEY_GRID_WORKING_CARD_BACKGROUND_NIGHT_OPACITY, 1.0f)
        val gridCheckHidden = prefs.getBoolean(KEY_GRID_WORKING_CARD_CHECK_HIDDEN, false)
        val gridTextHidden = prefs.getBoolean(KEY_GRID_WORKING_CARD_TEXT_HIDDEN, false)
        val gridModeHidden = prefs.getBoolean(KEY_GRID_WORKING_CARD_MODE_HIDDEN, false)

        val listModeHidden = prefs.getBoolean(KEY_LIST_WORKING_CARD_MODE_HIDDEN, false)
        val badgeTextMode = prefs.getInt(KEY_CUSTOM_BADGE_TEXT_MODE, 0)

        val bannerEnabled = prefs.getBoolean(KEY_BANNER_ENABLED, true)
        val folkBannerEnabled = prefs.getBoolean(KEY_FOLK_BANNER_ENABLED, true)
        val bannerCustomOpacityEnabled = prefs.getBoolean(KEY_BANNER_CUSTOM_OPACITY_ENABLED, false)
        val bannerCustomOpacity = prefs.getFloat(KEY_BANNER_CUSTOM_OPACITY, 0.5f)

        val bannerApiModeEnabled = prefs.getBoolean(KEY_BANNER_API_MODE_ENABLED, false)
        val bannerApiSourceValue = prefs.getString(KEY_BANNER_API_SOURCE, "") ?: ""
        // Migration logic: if folk banner source exists, use it and it will be saved as main source later
        val folkBannerApiSourceValue = prefs.getString(KEY_FOLK_BANNER_API_SOURCE, "") ?: ""
        val effectiveApiSource = if (folkBannerApiSourceValue.isNotBlank()) folkBannerApiSourceValue else bannerApiSourceValue

        val multiEnabled = prefs.getBoolean(KEY_MULTI_BACKGROUND_ENABLED, false)
        val homeUri = prefs.getString(KEY_HOME_BACKGROUND_URI, null)
        val kernelUri = prefs.getString(KEY_KERNEL_BACKGROUND_URI, null)
        val superuserUri = prefs.getString(KEY_SUPERUSER_BACKGROUND_URI, null)
        val systemModuleUri = prefs.getString(KEY_SYSTEM_MODULE_BACKGROUND_URI, null)
        val settingsUri = prefs.getString(KEY_SETTINGS_BACKGROUND_URI, null)

        val advancedTitleStyleEnabled = prefs.getBoolean(KEY_ADVANCED_TITLE_STYLE_ENABLED, false)
        val titleImageUriValue = prefs.getString(KEY_TITLE_IMAGE_URI, null)
        val titleDayOpacity = prefs.getFloat(KEY_TITLE_IMAGE_DAY_OPACITY, 1.0f)
        val titleNightOpacity = prefs.getFloat(KEY_TITLE_IMAGE_NIGHT_OPACITY, 1.0f)
        val titleDim = prefs.getFloat(KEY_TITLE_IMAGE_DIM, 0.0f)
        val titleOffsetX = prefs.getFloat(KEY_TITLE_IMAGE_OFFSET_X, 0f)

        val navBarGlassEnabled = prefs.getBoolean(KEY_NAVBAR_GLASS_ENABLED, true)
        val prefsNavBarGlassBlurStrength = prefs.getFloat(KEY_NAVBAR_GLASS_BLUR_STRENGTH, 0.7f)
        val prefsNavBarGlassTransparency = prefs.getFloat(KEY_NAVBAR_GLASS_TRANSPARENCY, 0.3f)
        val prefsNavBarGlassHighlightStrength = prefs.getFloat(KEY_NAVBAR_GLASS_HIGHLIGHT_STRENGTH, 0.5f)
        val navBarGlassSpecularEnabled = prefs.getBoolean(KEY_NAVBAR_GLASS_SPECULAR_ENABLED, true)
        val navBarGlassInnerGlowEnabled = prefs.getBoolean(KEY_NAVBAR_GLASS_INNER_GLOW_ENABLED, true)
        val navBarGlassBorderEnabled = prefs.getBoolean(KEY_NAVBAR_GLASS_BORDER_ENABLED, true)
        
        Log.d(TAG, "加载背景配置: URI=$uri, enabled=$enabled, opacity=$opacity, dim=$dim")
        
        customBackgroundUri = uri
        isCustomBackgroundEnabled = enabled
        customBackgroundOpacity = opacity
        customBackgroundBlur = blur
        customBackgroundDim = dim
        isDualBackgroundDimEnabled = dualDimEnabled
        customBackgroundDayDim = dayDim
        customBackgroundNightDim = nightDim
        
        videoBackgroundUri = videoUri
        isVideoBackgroundEnabled = videoEnabled
        videoVolume = videoVol
        
        gridWorkingCardBackgroundUri = gridUri
        isGridWorkingCardBackgroundEnabled = gridEnabled
        gridWorkingCardBackgroundOpacity = gridOpacity
        gridWorkingCardBackgroundDim = gridDim
        isGridDualOpacityEnabled = gridDualOpacityEnabled
        gridWorkingCardBackgroundDayOpacity = gridDayOpacity
        gridWorkingCardBackgroundNightOpacity = gridNightOpacity
        isGridWorkingCardCheckHidden = gridCheckHidden
        isGridWorkingCardTextHidden = gridTextHidden
        isGridWorkingCardModeHidden = gridModeHidden

        isListWorkingCardModeHidden = listModeHidden
        customBadgeTextMode = badgeTextMode

        isBannerEnabled = bannerEnabled
        isFolkBannerEnabled = folkBannerEnabled
        isBannerCustomOpacityEnabled = bannerCustomOpacityEnabled
        this.bannerCustomOpacity = bannerCustomOpacity

        isBannerApiModeEnabled = bannerApiModeEnabled
        bannerApiSource = effectiveApiSource

        isMultiBackgroundEnabled = multiEnabled
        homeBackgroundUri = homeUri
        kernelBackgroundUri = kernelUri
        superuserBackgroundUri = superuserUri
        systemModuleBackgroundUri = systemModuleUri
        settingsBackgroundUri = settingsUri

        isAdvancedTitleStyleEnabled = advancedTitleStyleEnabled
        titleImageUri = titleImageUriValue
        titleImageDayOpacity = titleDayOpacity
        titleImageNightOpacity = titleNightOpacity
        titleImageDim = titleDim
        titleImageOffsetX = titleOffsetX

        isNavBarGlassEnabled = navBarGlassEnabled
        navBarGlassBlurStrength = prefsNavBarGlassBlurStrength
        navBarGlassTransparency = prefsNavBarGlassTransparency
        navBarGlassHighlightStrength = prefsNavBarGlassHighlightStrength
        isNavBarGlassSpecularEnabled = navBarGlassSpecularEnabled
        isNavBarGlassInnerGlowEnabled = navBarGlassInnerGlowEnabled
        isNavBarGlassBorderEnabled = navBarGlassBorderEnabled
    }
    
    /**
     * 重置配置
     */
    fun reset() {
        // Default to custom background disabled
        customBackgroundUri = null
        isCustomBackgroundEnabled = false
        customBackgroundOpacity = 0.5f
        customBackgroundBlur = 0.2f
        customBackgroundDim = 0.0f
        isDualBackgroundDimEnabled = true
        customBackgroundDayDim = 0.0f
        customBackgroundNightDim = 0.5f
        
        videoBackgroundUri = null
        isVideoBackgroundEnabled = false
        videoVolume = 0f
        
        gridWorkingCardBackgroundUri = null
        isGridWorkingCardBackgroundEnabled = false
        gridWorkingCardBackgroundOpacity = 1.0f
        gridWorkingCardBackgroundDim = 0.3f
        isGridDualOpacityEnabled = false
        gridWorkingCardBackgroundDayOpacity = 1.0f
        gridWorkingCardBackgroundNightOpacity = 1.0f
        isGridWorkingCardCheckHidden = false
        isGridWorkingCardTextHidden = false
        isGridWorkingCardModeHidden = false

        isListWorkingCardModeHidden = false
        customBadgeTextMode = 0

        isBannerEnabled = true
        isFolkBannerEnabled = true
        isBannerCustomOpacityEnabled = false
        bannerCustomOpacity = 0.5f

        isBannerApiModeEnabled = false
        bannerApiSource = ""

        isMultiBackgroundEnabled = false
        homeBackgroundUri = null
        kernelBackgroundUri = null
        superuserBackgroundUri = null
        systemModuleBackgroundUri = null
        settingsBackgroundUri = null

        isAdvancedTitleStyleEnabled = false
        titleImageUri = null
        titleImageDayOpacity = 1.0f
        titleImageNightOpacity = 1.0f
        titleImageDim = 0.0f
        titleImageOffsetX = 0f

        isNavBarGlassEnabled = true
        navBarGlassBlurStrength = 0.7f
        navBarGlassTransparency = 0.3f
        navBarGlassHighlightStrength = 0.5f
        isNavBarGlassSpecularEnabled = true
        isNavBarGlassInnerGlowEnabled = true
        isNavBarGlassBorderEnabled = true
    }
}

/**
 * 背景管理器
 */
object BackgroundManager {
    private const val TAG = "BackgroundManager"
    private const val BACKGROUND_FILENAME = "background.jpg"
    private const val VIDEO_BACKGROUND_FILENAME_BASE = "video_background"
    private const val GRID_WORKING_CARD_BACKGROUND_FILENAME = "grid_working_card_background.jpg"

    // Multi-Background Filenames
    private const val HOME_BACKGROUND_FILENAME = "background_home"
    private const val KERNEL_BACKGROUND_FILENAME = "background_kernel"
    private const val SUPERUSER_BACKGROUND_FILENAME = "background_superuser"
    private const val SYSTEM_MODULE_BACKGROUND_FILENAME = "background_system_module"
    private const val SETTINGS_BACKGROUND_FILENAME = "background_settings"

    private const val TITLE_IMAGE_FILENAME = "title_image"

    /**
     * 获取文件扩展名
     */
    private fun getFileExtension(context: Context, uri: Uri): String {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            when {
                mimeType?.contains("gif", true) == true -> ".gif"
                mimeType?.contains("png", true) == true -> ".png"
                mimeType?.contains("webp", true) == true -> ".webp"
                mimeType?.contains("video/mp4", true) == true -> ".mp4"
                mimeType?.contains("video/webm", true) == true -> ".webm"
                mimeType?.contains("video/x-matroska", true) == true -> ".mkv"
                else -> ".jpg"
            }
        } catch (e: Exception) {
            ".jpg"
        }
    }

    /**
     * 获取背景文件
     */
    private fun getBackgroundFile(context: Context, extension: String = ".jpg"): File {
        return File(context.filesDir, "background$extension")
    }

    private fun getGridWorkingCardBackgroundFile(context: Context, extension: String = ".jpg"): File {
        return File(context.filesDir, "grid_working_card_background$extension")
    }

    /**
     * 清理旧的背景文件
     */
    private fun clearOldFiles(context: Context, baseName: String) {
        val extensions = listOf(".jpg", ".png", ".gif", ".webp", ".mp4", ".webm", ".mkv")
        extensions.forEach { ext ->
            val file = File(context.filesDir, "$baseName$ext")
            if (file.exists()) {
                file.delete()
            }
        }
    }
    
    /**
     * 保存并应用自定义背景
     */
    suspend fun saveAndApplyCustomBackground(context: Context, uri: Uri): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val extension = getFileExtension(context, uri)
                // 清理旧文件
                clearOldFiles(context, "background")
                
                val savedUri = saveImageToInternalStorage(context, uri, getBackgroundFile(context, extension))
                if (savedUri != null) {
                    Log.d(TAG, "图片保存成功，URI: $savedUri")
                    BackgroundConfig.updateCustomBackgroundUri(savedUri.toString())
                    BackgroundConfig.save(context)
                    Log.d(TAG, "背景配置保存成功，URI: ${BackgroundConfig.customBackgroundUri}, 启用状态: ${BackgroundConfig.isCustomBackgroundEnabled}")
                    true
                } else {
                    Log.e(TAG, "图片保存失败")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存自定义背景失败: ${e.message}", e)
            false
        }
    }

    /**
     * 保存并应用视频背景
     */
    suspend fun saveAndApplyVideoBackground(context: Context, uri: Uri): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val extension = getFileExtension(context, uri)
                // 清理旧文件
                clearOldFiles(context, VIDEO_BACKGROUND_FILENAME_BASE)
                
                val targetFile = File(context.filesDir, "$VIDEO_BACKGROUND_FILENAME_BASE$extension")
                val savedUri = saveImageToInternalStorage(context, uri, targetFile)
                if (savedUri != null) {
                    Log.d(TAG, "视频保存成功，URI: $savedUri")
                    BackgroundConfig.updateVideoBackgroundUri(savedUri.toString())
                    BackgroundConfig.save(context)
                    true
                } else {
                    Log.e(TAG, "视频保存失败")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存视频背景失败: ${e.message}", e)
            false
        }
    }

    /**
     * 保存并应用Grid布局工作中卡片背景
     */
    suspend fun saveAndApplyGridWorkingCardBackground(context: Context, uri: Uri): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val extension = getFileExtension(context, uri)
                // 清理旧文件
                clearOldFiles(context, "grid_working_card_background")
                
                val savedUri = saveImageToInternalStorage(context, uri, getGridWorkingCardBackgroundFile(context, extension))
                if (savedUri != null) {
                    Log.d(TAG, "Grid卡片图片保存成功，URI: $savedUri")
                    BackgroundConfig.updateGridWorkingCardBackgroundUri(savedUri.toString())
                    BackgroundConfig.save(context)
                    true
                } else {
                    Log.e(TAG, "Grid卡片图片保存失败")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存Grid卡片自定义背景失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 清除自定义背景
     */
    fun clearCustomBackground(context: Context) {
        try {
            // 删除背景文件
            clearOldFiles(context, "background")
            
            // 重置配置（只重置全局背景相关）
            BackgroundConfig.updateCustomBackgroundUri(null)
            BackgroundConfig.setCustomBackgroundEnabledState(false)
            BackgroundConfig.save(context)
        } catch (e: Exception) {
            Log.e(TAG, "清除自定义背景失败: ${e.message}", e)
        }
    }

    /**
     * 清除视频背景
     */
    fun clearVideoBackground(context: Context) {
        try {
            clearOldFiles(context, VIDEO_BACKGROUND_FILENAME_BASE)
            BackgroundConfig.updateVideoBackgroundUri(null)
            BackgroundConfig.save(context)
        } catch (e: Exception) {
            Log.e(TAG, "清除视频背景失败: ${e.message}", e)
        }
    }

    /**
     * 清除Grid布局工作中卡片背景
     */
    fun clearGridWorkingCardBackground(context: Context) {
        try {
            // 删除背景文件
            clearOldFiles(context, "grid_working_card_background")
            
            // 重置配置
            BackgroundConfig.updateGridWorkingCardBackgroundUri(null)
            BackgroundConfig.setGridWorkingCardBackgroundEnabledState(false)
            BackgroundConfig.setGridWorkingCardBackgroundOpacityValue(1.0f)
            BackgroundConfig.setGridWorkingCardBackgroundDimValue(0.3f)
            BackgroundConfig.setGridDualOpacityEnabledState(false)
            BackgroundConfig.setGridWorkingCardBackgroundDayOpacityValue(1.0f)
            BackgroundConfig.setGridWorkingCardBackgroundNightOpacityValue(1.0f)
            BackgroundConfig.save(context)
        } catch (e: Exception) {
            Log.e(TAG, "清除Grid卡片自定义背景失败: ${e.message}", e)
        }
    }
    
    /**
     * Clear generic background
     */
    private fun clearGenericBackground(
        context: Context,
        filenameBase: String,
        updateConfigAction: (String?) -> Unit
    ) {
        try {
            clearOldFiles(context, filenameBase)
            updateConfigAction(null)
            BackgroundConfig.save(context)
        } catch (e: Exception) {
            Log.e(TAG, "清除 $filenameBase 失败: ${e.message}", e)
        }
    }

    /**
     * Save and apply generic background
     */
    private suspend fun saveAndApplyGenericBackground(
        context: Context,
        uri: Uri,
        filenameBase: String,
        updateConfigAction: (String) -> Unit
    ): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val extension = getFileExtension(context, uri)
                clearOldFiles(context, filenameBase)
                
                val targetFile = File(context.filesDir, "$filenameBase$extension")
                val savedUri = saveImageToInternalStorage(context, uri, targetFile)
                
                if (savedUri != null) {
                    Log.d(TAG, "$filenameBase 图片保存成功，URI: $savedUri")
                    updateConfigAction(savedUri.toString())
                    BackgroundConfig.save(context)
                    true
                } else {
                    Log.e(TAG, "$filenameBase 图片保存失败")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存 $filenameBase 失败: ${e.message}", e)
            false
        }
    }

    // Home Background
    suspend fun saveAndApplyHomeBackground(context: Context, uri: Uri) = 
        saveAndApplyGenericBackground(context, uri, HOME_BACKGROUND_FILENAME) { BackgroundConfig.updateHomeBackgroundUri(it) }
    
    fun clearHomeBackground(context: Context) = 
        clearGenericBackground(context, HOME_BACKGROUND_FILENAME) { BackgroundConfig.updateHomeBackgroundUri(it) }

    // Kernel Background
    suspend fun saveAndApplyKernelBackground(context: Context, uri: Uri) = 
        saveAndApplyGenericBackground(context, uri, KERNEL_BACKGROUND_FILENAME) { BackgroundConfig.updateKernelBackgroundUri(it) }
    
    fun clearKernelBackground(context: Context) = 
        clearGenericBackground(context, KERNEL_BACKGROUND_FILENAME) { BackgroundConfig.updateKernelBackgroundUri(it) }

    // Superuser Background
    suspend fun saveAndApplySuperuserBackground(context: Context, uri: Uri) = 
        saveAndApplyGenericBackground(context, uri, SUPERUSER_BACKGROUND_FILENAME) { BackgroundConfig.updateSuperuserBackgroundUri(it) }
    
    fun clearSuperuserBackground(context: Context) = 
        clearGenericBackground(context, SUPERUSER_BACKGROUND_FILENAME) { BackgroundConfig.updateSuperuserBackgroundUri(it) }

    // System Module Background
    suspend fun saveAndApplySystemModuleBackground(context: Context, uri: Uri) = 
        saveAndApplyGenericBackground(context, uri, SYSTEM_MODULE_BACKGROUND_FILENAME) { BackgroundConfig.updateSystemModuleBackgroundUri(it) }
    
    fun clearSystemModuleBackground(context: Context) = 
        clearGenericBackground(context, SYSTEM_MODULE_BACKGROUND_FILENAME) { BackgroundConfig.updateSystemModuleBackgroundUri(it) }

    // Settings Background
    suspend fun saveAndApplySettingsBackground(context: Context, uri: Uri) = 
        saveAndApplyGenericBackground(context, uri, SETTINGS_BACKGROUND_FILENAME) { BackgroundConfig.updateSettingsBackgroundUri(it) }
    
    fun clearSettingsBackground(context: Context) = 
        clearGenericBackground(context, SETTINGS_BACKGROUND_FILENAME) { BackgroundConfig.updateSettingsBackgroundUri(it) }

    // Title Image
    suspend fun saveAndApplyTitleImage(context: Context, uri: Uri): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val extension = getFileExtension(context, uri)
                clearOldFiles(context, TITLE_IMAGE_FILENAME)
                
                val targetFile = File(context.filesDir, "$TITLE_IMAGE_FILENAME$extension")
                val savedUri = saveImageToInternalStorage(context, uri, targetFile)
                
                if (savedUri != null) {
                    Log.d(TAG, "标题图片保存成功，URI: $savedUri")
                    BackgroundConfig.updateTitleImageUri(savedUri.toString())
                    BackgroundConfig.save(context)
                    true
                } else {
                    Log.e(TAG, "标题图片保存失败")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存标题图片失败: ${e.message}", e)
            false
        }
    }

    fun clearTitleImage(context: Context) {
        try {
            clearOldFiles(context, TITLE_IMAGE_FILENAME)
            BackgroundConfig.updateTitleImageUri(null)
            BackgroundConfig.save(context)
        } catch (e: Exception) {
            Log.e(TAG, "清除标题图片失败: ${e.message}", e)
        }
    }
    
    /**
     * 加载自定义背景
     */
    fun loadCustomBackground(context: Context) {
        BackgroundConfig.load(context)
    }
    
    /**
     * 保存图片到内部存储
     */
    private suspend fun saveImageToInternalStorage(context: Context, uri: Uri, targetFile: File): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = SafeUriResolver.openInputStream(context, uri) ?: return@withContext null
                // val file = getBackgroundFile(context) // Use targetFile instead

                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
                inputStream.close()

                // 返回带时间戳的URI，确保Compose重组
                val fileUri = Uri.fromFile(targetFile).buildUpon()
                    .appendQueryParameter("t", System.currentTimeMillis().toString())
                    .build()
                    
                Log.d(TAG, "图片保存成功，文件URI: $fileUri")
                fileUri
            } catch (e: Exception) {
                Log.e(TAG, "保存图片到内部存储失败: ${e.message}", e)
                null
            }
        }
    }
}
