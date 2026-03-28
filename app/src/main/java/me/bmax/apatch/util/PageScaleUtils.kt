package me.bmax.apatch.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import me.bmax.apatch.APApplication

object PageScaleUtils {
    private const val KEY_PAGE_SCALE = "page_scale"
    private const val DEFAULT_SCALE = 1.0f

    var currentScale: Float by mutableFloatStateOf(DEFAULT_SCALE)
        private set

    fun load(context: Context) {
        val prefs = APApplication.sharedPreferences
        currentScale = prefs.getFloat(KEY_PAGE_SCALE, DEFAULT_SCALE)
    }

    fun setScale(scale: Float) {
        val prefs = APApplication.sharedPreferences
        prefs.edit { putFloat(KEY_PAGE_SCALE, scale) }
        currentScale = scale
    }
}
