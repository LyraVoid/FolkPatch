package me.bmax.apatch.util.ui

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape

/**
 * 悬浮底栏紧凑圆角风格配置 - 独立于主题管理器
 */
object FloatingBarConfig {

    /** 是否启用紧凑圆角风格 */
    var isCompactRoundedStyle by mutableStateOf(true)

    /** 获取紧凑胶囊形圆角形状 */
    fun getCompactRoundedShape(): Shape = RoundedCornerShape(percent = 50)

    // ---- Persistence ----

    private const val PREFS_NAME = "floating_bar_settings"
    private const val KEY_COMPACT_ROUNDED = "compact_rounded_style"

    fun save(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putBoolean(KEY_COMPACT_ROUNDED, isCompactRoundedStyle)
            apply()
        }
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isCompactRoundedStyle = prefs.getBoolean(KEY_COMPACT_ROUNDED, true)
    }
}
