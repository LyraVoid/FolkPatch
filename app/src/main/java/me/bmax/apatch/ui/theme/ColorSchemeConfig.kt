package me.bmax.apatch.ui.theme

import androidx.annotation.StringRes
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.bmax.apatch.R

/**
 * Color generation mode - Classic uses hardcoded themes, Custom uses MaterialKolor dynamic generation
 */
enum class ColorGenerationMode(val key: String) {
    CLASSIC("classic"),
    CUSTOM("custom");

    companion object {
        fun fromKey(key: String?): ColorGenerationMode =
            entries.find { it.key == key } ?: CLASSIC
    }
}

/**
 * Color standard - MD3 2021 (original) vs M3e 2025 (extended color roles)
 */
enum class ColorStandard(val specVersion: ColorSpec.SpecVersion, @StringRes val labelRes: Int) {
    MD3_2021(ColorSpec.SpecVersion.SPEC_2021, R.string.color_standard_md3_2021),
    M3E_2025(ColorSpec.SpecVersion.SPEC_2025, R.string.color_standard_m3e_2025);

    companion object {
        fun fromName(name: String?): ColorStandard =
            entries.find { it.name == name } ?: MD3_2021
    }
}

/**
 * Color style - wraps MaterialKolor's PaletteStyle
 */
enum class ColorStyle(val paletteStyle: PaletteStyle, @StringRes val labelRes: Int) {
    TONAL_SPOT(PaletteStyle.TonalSpot, R.string.color_style_tonal_spot),
    VIBRANT(PaletteStyle.Vibrant, R.string.color_style_vibrant),
    CONTENT(PaletteStyle.Content, R.string.color_style_content),
    EXPRESSIVE(PaletteStyle.Expressive, R.string.color_style_expressive),
    RAINBOW(PaletteStyle.Rainbow, R.string.color_style_rainbow),
    FRUIT_SALAD(PaletteStyle.FruitSalad, R.string.color_style_fruit_salad),
    MONOCHROME(PaletteStyle.Monochrome, R.string.color_style_monochrome),
    NEUTRAL(PaletteStyle.Neutral, R.string.color_style_neutral),
    FIDELITY(PaletteStyle.Fidelity, R.string.color_style_fidelity);

    companion object {
        fun fromName(name: String?): ColorStyle =
            entries.find { it.name == name } ?: TONAL_SPOT
    }
}
