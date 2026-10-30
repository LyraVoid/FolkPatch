package me.bmax.apatch.ui.theme

import android.content.Context
import android.os.Build
import androidx.collection.LruCache
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
        val effectiveSpec = if (specVersion == ColorSpec.SpecVersion.SPEC_2025 &&
            style !in setOf(PaletteStyle.TonalSpot, PaletteStyle.Neutral, PaletteStyle.Vibrant, PaletteStyle.Expressive)
        ) ColorSpec.SpecVersion.SPEC_2021 else specVersion
        val key = "${seedColor.value}_${style.name}_${effectiveSpec.name}_$isDark"
        return cache.get(key) ?: run {
            val scheme = dynamicColorScheme(
                seedColor = seedColor,
                isDark = isDark,
                style = style,
                specVersion = effectiveSpec,
                contrastLevel = 0.0,
            )
            cache.put(key, scheme)
            scheme
        }
    }

    /** Generate from the same platform primary seed FinaSU feeds to MaterialKolor. */
    fun generateFromContext(
        context: Context,
        isDark: Boolean,
        style: PaletteStyle,
        specVersion: ColorSpec.SpecVersion,
    ): ColorScheme {
        val seedColor = extractSystemSeedColor(context, isDark)
        return generate(seedColor, isDark, style, specVersion)
    }

    private fun extractSystemSeedColor(context: Context, isDark: Boolean): Color {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return try {
                val systemScheme =
                    if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                systemScheme.primary
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
