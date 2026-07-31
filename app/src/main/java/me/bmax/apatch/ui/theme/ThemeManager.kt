package me.bmax.apatch.ui.theme

import android.content.Context
import android.net.Uri

object ThemeManager {

    data class ThemeConfig(
        val isBackgroundEnabled: Boolean,
        val backgroundOpacity: Float,
        val backgroundBlur: Float = 0f,
        val backgroundDim: Float,
        val isDualBackgroundDimEnabled: Boolean = false,
        val backgroundDayDim: Float = 0.0f,
        val backgroundNightDim: Float = 0.0f,
        val isFontEnabled: Boolean,
        val customColor: String,
        val homeLayoutStyle: String,
        val statsTopLayout: String = "list",
        val nightModeEnabled: Boolean,
        val nightModeFollowSys: Boolean,
        val useSystemDynamicColor: Boolean,
        val colorGenerationMode: String = "classic",
        val colorStandard: String = "MD3_2021",
        val colorStyle: String = "TONAL_SPOT",
        val appLanguage: String?,
        // Grid Working Card Background
        val isGridWorkingCardBackgroundEnabled: Boolean = false,
        val gridWorkingCardBackgroundOpacity: Float = 1.0f,
        val isGridDualOpacityEnabled: Boolean = false,
        val gridWorkingCardBackgroundDayOpacity: Float = 1.0f,
        val gridWorkingCardBackgroundNightOpacity: Float = 1.0f,
        val gridWorkingCardBackgroundDim: Float = 0.3f,
        val isGridWorkingCardCheckHidden: Boolean = false,
        val isGridWorkingCardTextHidden: Boolean = false,
        val isGridWorkingCardModeHidden: Boolean = false,
        val isListWorkingCardModeHidden: Boolean = false,
        // Multi-Background Mode
        val isMultiBackgroundEnabled: Boolean = false,
        // Music Config
        val isMusicEnabled: Boolean = false,
        val musicVolume: Float = 1.0f,
        val isAutoPlayEnabled: Boolean = false,
        val isLoopingEnabled: Boolean = false,
        val musicFilename: String? = null,
        // Sound Effect Config
        val isSoundEffectEnabled: Boolean = false,
        val soundEffectFilename: String? = null,
        val soundEffectScope: String = SoundEffectConfig.SCOPE_GLOBAL,
        // Video Background
        val isVideoBackgroundEnabled: Boolean = false,
        val videoVolume: Float = 0f,
        // Advanced Title Style
        val isAdvancedTitleStyleEnabled: Boolean = false,
        val titleImageDayOpacity: Float = 1.0f,
        val titleImageNightOpacity: Float = 1.0f,
        val titleImageDim: Float = 0.0f,
        val titleImageOffsetX: Float = 0f,
         // FocusUI Card Wallpapers
         val isFocusCardBackgroundEnabled: Boolean = false,
         val focusCardBgDim: Float = 0.3f,
         val isFocusCardDualDimEnabled: Boolean = false,
         val focusCardBgDayDim: Float = 0.3f,
         val focusCardBgNightDim: Float = 0.3f,
         val isFocusCardDualOpacityEnabled: Boolean = false,
         val focusCardBgOpacity: Float = 1.0f,
         val focusCardBgDayOpacity: Float = 1.0f,
         val focusCardBgNightOpacity: Float = 1.0f,
        val hasFocusCardKernelBg: Boolean = false,
        val hasFocusCardAppBg: Boolean = false,
        val hasFocusCardDeviceBg: Boolean = false,
        val hasFocusCardStorageBg: Boolean = false,
        // DashboardUI Hero Card Wallpaper
        val isDashboardCardBackgroundEnabled: Boolean = false,
        val dashboardCardBgDim: Float = 0.3f,
        val isDashboardCardDualDimEnabled: Boolean = false,
        val dashboardCardBgDayDim: Float = 0.3f,
        val dashboardCardBgNightDim: Float = 0.3f,
        val dashboardCardBgOpacity: Float = 1.0f,
        val isDashboardCardDualOpacityEnabled: Boolean = false,
        val dashboardCardBgDayOpacity: Float = 1.0f,
        val dashboardCardBgNightOpacity: Float = 1.0f,
        val hasDashboardCardBg: Boolean = false,
    )

    data class ThemeMetadata(
        val name: String,
        val type: String, // "phone" or "tablet"
        val version: String,
        val author: String,
        val description: String
    )


    /**
     * Same LiveData instance as the top-level [refreshTheme] (Theme.kt) that
     * [APatchTheme] and the settings screens observe, so theme import/reset
     * refreshes the UI immediately (e.g. system-following night mode).
     */
    val refreshTheme = me.bmax.apatch.ui.theme.refreshTheme

    suspend fun exportTheme(context: Context, uri: Uri, metadata: ThemeMetadata): Boolean {
        return ThemeIO.exportTheme(context, uri, metadata)
    }

    suspend fun readThemeMetadata(context: Context, uri: Uri): ThemeMetadata? {
        return ThemeIO.readThemeMetadata(context, uri)
    }

    suspend fun importTheme(context: Context, uri: Uri): Boolean {
        return ThemeIO.importTheme(context, uri, refreshTheme)
    }

    suspend fun resetTheme(context: Context): Boolean {
        return ThemeIO.resetTheme(context, refreshTheme)
    }
}
