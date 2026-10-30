package me.bmax.apatch.ui.theme

import android.content.SharedPreferences

internal fun removeLegacyContrastPreference(prefs: SharedPreferences) {
    if (prefs.contains("color_contrast")) prefs.edit().remove("color_contrast").apply()
}
