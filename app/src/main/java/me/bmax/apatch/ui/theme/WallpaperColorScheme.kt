package me.bmax.apatch.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import me.bmax.apatch.ui.component.themeColorOptions
import me.bmax.apatch.ui.theme.tokens.FolkThemeCatalog

// 壁纸有效亮度低于该阈值时，内容采用深色主题的中性色（浅色文字）
private const val WALLPAPER_DARK_THRESHOLD = 0.5f

/**
 * Builds the raw Material3 [ColorScheme] for the requested light/dark mode, following the
 * user's color-generation mode (custom MaterialKolor, system dynamic, or classic catalog).
 */
internal fun generateColorScheme(
    context: Context,
    darkTheme: Boolean,
    colorGenerationMode: String?,
    dynamicColor: Boolean,
    customColorScheme: String?,
    colorStandard: String?,
    colorStyle: String?,
): ColorScheme {
    val scheme = when {
        // Custom dynamic generation (MaterialKolor) with system wallpaper seed
        colorGenerationMode == "custom" && dynamicColor -> {
            val standard = ColorStandard.fromName(colorStandard)
            val style = ColorStyle.fromName(colorStyle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ColorSchemeGenerator.generateFromContext(
                    context, darkTheme, style.paletteStyle, standard.specVersion
                )
            } else {
                // Fallback: system dynamic color not available, use selected color as seed
                val seedColor = catalogSeed(customColorScheme, darkTheme)
                ColorSchemeGenerator.generate(
                    seedColor, darkTheme, style.paletteStyle, standard.specVersion
                )
            }
        }
        // Custom dynamic generation (MaterialKolor) with selected color seed
        colorGenerationMode == "custom" -> {
            val seedColor = catalogSeed(customColorScheme, darkTheme)
            val standard = ColorStandard.fromName(colorStandard)
            val style = ColorStyle.fromName(colorStyle)
            ColorSchemeGenerator.generate(
                seedColor, darkTheme, style.paletteStyle, standard.specVersion
            )
        }
        // System dynamic color (standard Material3 wallpaper extraction)
        dynamicColor -> {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                else -> FolkThemeCatalog.scheme("blue", darkTheme)
            }
        }
        // Classic themes, from the catalog.
        else -> FolkThemeCatalog.scheme(customColorScheme, darkTheme)
    }
    return scheme.withSemanticError(darkTheme)
}

internal fun catalogSeed(key: String?, dark: Boolean): Color {
    val option = themeColorOptions.find { it.key == (key ?: "indigo") }
        ?: themeColorOptions.first { it.key == "indigo" }
    return FolkThemeCatalog.scheme(option.key, dark).primary
}

internal fun ColorScheme.withSemanticError(isDark: Boolean): ColorScheme {
    val semantic = if (isDark) darkColorScheme() else lightColorScheme()
    return copy(
        error = semantic.error,
        onError = semantic.onError,
        errorContainer = semantic.errorContainer,
        onErrorContainer = semantic.onErrorContainer,
    )
}

/**
 * Applies the custom-background adaptation (transparent background + wallpaper-aware neutral
 * content colors) or the AMOLED override on top of [baseColorScheme].
 *
 * 在自定义壁纸模式下，中性色/容器色会跟随壁纸的有效明暗（亮底→深字，暗底→浅字），
 * 以保证半透明容器与其上文字的对比度；强调色及其不透明容器仍沿用 [baseColorScheme]。
 *
 * 返回 [WallpaperThemeResult]：适配后的方案，以及为保证文字对比度而可能需要提高的
 * 壁纸遮罩 dim（仅在选用浅色文字时需要）。
 */
internal fun adaptColorScheme(
    context: Context,
    baseColorScheme: ColorScheme,
    darkTheme: Boolean,
    amoledTheme: Boolean,
    useCustomBackground: Boolean,
    activeBackgroundUri: String?,
    colorGenerationMode: String?,
    dynamicColor: Boolean,
    customColorScheme: String?,
    colorStandard: String?,
    colorStyle: String?,
): WallpaperThemeResult {
    if (!useCustomBackground) {
        val scheme = if (darkTheme && amoledTheme) baseColorScheme.toAmoled() else baseColorScheme
        return WallpaperThemeResult(scheme, null)
    }

    val wallpaperDim = BackgroundConfig.getEffectiveBackgroundDim(darkTheme)
    val effectiveLuminance = BackgroundConfig.wallpaperLuminanceFor(activeBackgroundUri)
    // 夜间模式始终保持深色中性方案：不因壁纸偏亮而把整套 UI 翻成浅色（避免「发白」），
    // 可读性交给下面的对比度保护抬高遮罩。浅色模式仍按壁纸明暗决定，以照顾暗壁纸。
    val useLightContent = wallpaperUsesDarkNeutral(
        darkTheme = darkTheme,
        luminance = effectiveLuminance ?: -1f,
    )

    val neutralScheme = if (useLightContent == darkTheme) {
        baseColorScheme
    } else {
        generateColorScheme(
            context = context,
            darkTheme = useLightContent,
            colorGenerationMode = colorGenerationMode,
            dynamicColor = dynamicColor,
            customColorScheme = customColorScheme,
            colorStandard = colorStandard,
            colorStyle = ColorStyle.NEUTRAL.name,
        )
    }

    val adapted = adaptWallpaperColorScheme(baseColorScheme, neutralScheme, active = true)

    // 浅色模式：浅色中性内容压在暗壁纸上时，用对比度保护兜底（通常已是暗壁纸，基本不改动）。
    // 夜间模式：UI 已固定为深色，遮罩完全交给用户的夜间暗度滑块，不做自动加深。
    val renderDim = if (useLightContent && !darkTheme) {
        guardedDim(
            requested = wallpaperDim,
            scheme = adapted,
            opacity = BackgroundConfig.customBackgroundOpacity,
        ) ?: wallpaperDim
    } else {
        wallpaperDim
    }

    return WallpaperThemeResult(adapted, renderDim)
}

internal fun wallpaperUsesDarkNeutral(darkTheme: Boolean, luminance: Float): Boolean {
    if (darkTheme) return true
    if (luminance < 0f) return false
    return luminance < WALLPAPER_DARK_THRESHOLD
}

internal fun adaptWallpaperColorScheme(
    base: ColorScheme,
    neutral: ColorScheme,
    active: Boolean,
): ColorScheme {
    if (!active) return base
    return base.copy(
        background = Color.Transparent,
        surface = neutral.surface,
        surfaceDim = neutral.surfaceDim,
        surfaceBright = neutral.surfaceBright,
        surfaceContainer = neutral.surfaceContainer,
        surfaceContainerLow = neutral.surfaceContainerLow,
        surfaceContainerLowest = neutral.surfaceContainerLowest,
        surfaceContainerHigh = neutral.surfaceContainerHigh,
        surfaceContainerHighest = neutral.surfaceContainerHighest,
        surfaceVariant = neutral.surfaceVariant,
        onBackground = neutral.onBackground,
        onSurface = neutral.onSurface,
        onSurfaceVariant = neutral.onSurfaceVariant,
        outline = neutral.outline,
        outlineVariant = neutral.outlineVariant,
        inverseSurface = neutral.inverseSurface,
        inverseOnSurface = neutral.inverseOnSurface,
    )
}
