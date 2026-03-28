package me.bmax.apatch.util

import androidx.core.content.edit
import me.bmax.apatch.APApplication

object VisualConfig {

    private const val KEY_ENABLE_BLUR = "enable_blur"
    private const val KEY_ENABLE_FLOATING_BOTTOM_BAR = "enable_floating_bottom_bar"
    private const val KEY_FLOATING_BOTTOM_BAR_AUTO_HIDE = "floating_bottom_bar_auto_hide"
    private const val KEY_ENABLE_LIQUID_GLASS = "enable_liquid_glass"
    private const val KEY_KEY_COLOR = "key_color"
    private const val KEY_PREDICTIVE_BACK_GESTURE = "predictive_back_gesture"

    private val prefs get() = APApplication.sharedPreferences

    var keyColor: Int
        get() = prefs.getInt(KEY_KEY_COLOR, 0)
        set(value) = prefs.edit { putInt(KEY_KEY_COLOR, value) }

    var enableBlur: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_BLUR, true)
        set(value) {
            prefs.edit { putBoolean(KEY_ENABLE_BLUR, value) }
            // When blur is disabled, auto-disable liquid glass
            if (!value && enableLiquidGlass) {
                enableLiquidGlass = false
            }
        }

    var enableFloatingBottomBar: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_FLOATING_BOTTOM_BAR, false)
        set(value) {
            prefs.edit { putBoolean(KEY_ENABLE_FLOATING_BOTTOM_BAR, value) }
        }

    var floatingBottomBarAutoHide: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_BOTTOM_BAR_AUTO_HIDE, true)
        set(value) = prefs.edit { putBoolean(KEY_FLOATING_BOTTOM_BAR_AUTO_HIDE, value) }

    var enableLiquidGlass: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_LIQUID_GLASS, false)
        set(value) {
            if (value && !enableBlur) {
                enableBlur = true
            }
            prefs.edit { putBoolean(KEY_ENABLE_LIQUID_GLASS, value) }
        }

    var predictiveBackGesture: Boolean
        get() = prefs.getBoolean(KEY_PREDICTIVE_BACK_GESTURE, true)
        set(value) = prefs.edit { putBoolean(KEY_PREDICTIVE_BACK_GESTURE, value) }
}
