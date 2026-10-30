package me.bmax.apatch.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ColorSchemeGeneratorTest {
    @Test
    fun standardSchemesMatchTheLibraryWithEffectiveSpec() {
        val seed = Color(0xFF4355B9)
        for (dark in listOf(false, true)) {
            for (style in PaletteStyle.entries) {
                for (spec in ColorSpec.SpecVersion.entries) {
                    val effectiveSpec = if (spec == ColorSpec.SpecVersion.SPEC_2025 &&
                        style !in listOf(PaletteStyle.TonalSpot, PaletteStyle.Neutral, PaletteStyle.Vibrant, PaletteStyle.Expressive)
                    ) ColorSpec.SpecVersion.SPEC_2021 else spec
                    val expected = dynamicColorScheme(seedColor = seed, isDark = dark, style = style, specVersion = effectiveSpec, contrastLevel = 0.0)
                    val actual = ColorSchemeGenerator.generate(seed, dark, style, spec)
                    expected.javaClass.methods.filter { it.name.startsWith("get") && it.returnType == Long::class.javaPrimitiveType && it.parameterCount == 0 }
                        .forEach { role -> assertEquals("$dark $style $spec ${role.name}", role.invoke(expected), role.invoke(actual)) }
                    assertSame(actual, ColorSchemeGenerator.generate(seed, dark, style, spec))
                }
            }
        }
    }
}
