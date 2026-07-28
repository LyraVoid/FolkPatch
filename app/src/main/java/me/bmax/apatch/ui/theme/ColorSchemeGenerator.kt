package me.bmax.apatch.ui.theme

import android.content.Context
import android.os.Build
import android.util.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec

/**
 * Generates ColorSchemes dynamically from seed colors using MaterialKolor.
 * Uses LRU cache to avoid redundant computation.
 */
object ColorSchemeGenerator {

    private val cache = LruCache<String, ColorScheme>(8)

    /**
     * Generate a ColorScheme from a seed color, style, and spec version.
     * Results are cached by combination key.
     */
    fun generate(
        seedColor: Color,
        isDark: Boolean,
        style: PaletteStyle,
        specVersion: ColorSpec.SpecVersion,
    ): ColorScheme {
        val key = "${seedColor.value}_${style.name}_${specVersion.name}_$isDark"
        return cache.get(key) ?: run {
            val scheme = dynamicColorScheme(
                seedColor = seedColor,
                isDark = isDark,
                style = style,
                specVersion = specVersion,
            )
            cache.put(key, scheme)
            scheme
        }
    }

    /**
     * Generate a ColorScheme using system wallpaper seed color (Android 12+).
     * Extracts the seed from system accent color resources.
     */
    fun generateFromContext(
        context: Context,
        isDark: Boolean,
        style: PaletteStyle,
        specVersion: ColorSpec.SpecVersion,
    ): ColorScheme {
        val seedColor = extractSystemSeedColor(context)
        return generate(seedColor, isDark, style, specVersion)
    }

    /**
     * Extract seed color from system wallpaper (Android 12+).
     * Falls back to a default blue if system colors are unavailable.
     */
    private fun extractSystemSeedColor(context: Context): Color {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return try {
                val colorRes = android.R.color.system_accent1_300
                val color = context.getColor(colorRes)
                Color(color)
            } catch (_: Exception) {
                Color(0xFF4285F4) // fallback blue
            }
        }
        return Color(0xFF4285F4)
    }

    fun invalidateCache() {
        cache.evictAll()
    }
}
