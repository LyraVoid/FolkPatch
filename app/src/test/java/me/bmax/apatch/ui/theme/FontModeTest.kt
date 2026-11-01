package me.bmax.apatch.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FontModeTest {
    @Test
    fun savedModeNamesRoundTripThroughPreferences() {
        // save() writes FontMode.name and load() reads it back with fromName().
        for (mode in FontMode.entries) {
            assertEquals(mode, FontMode.fromName(mode.name))
        }
    }

    @Test
    fun themeSerializedNamesStayStable() {
        // theme.json carries the serialized name, so these values are a contract.
        assertEquals(FontMode.APP_DEFAULT, FontMode.fromSerializedName("app"))
        assertEquals(FontMode.SYSTEM_DEFAULT, FontMode.fromSerializedName("system"))
        assertEquals(FontMode.CUSTOM, FontMode.fromSerializedName("custom"))
        assertNull(FontMode.fromSerializedName("CUSTOM"))
        for (mode in FontMode.entries) {
            assertEquals(mode, FontMode.fromSerializedName(mode.serializedName))
        }
    }

    @Test
    fun storedCustomChoiceIsNeverDowngradedOnLoad() {
        // Picking "Custom" before importing a font file must stay selected: the
        // mode is the user's choice, not a description of the imported file.
        assertEquals(FontMode.CUSTOM, resolveFontMode(FontMode.CUSTOM, legacyCustomFontEnabled = false))
        assertEquals(FontMode.APP_DEFAULT, resolveFontMode(FontMode.APP_DEFAULT, legacyCustomFontEnabled = false))
        assertEquals(FontMode.SYSTEM_DEFAULT, resolveFontMode(FontMode.SYSTEM_DEFAULT, legacyCustomFontEnabled = true))
    }

    @Test
    fun legacyBooleanOnlyAppliesWhenNoModeWasStored() {
        assertEquals(FontMode.CUSTOM, resolveFontMode(null, legacyCustomFontEnabled = true))
        assertEquals(FontMode.SYSTEM_DEFAULT, resolveFontMode(null, legacyCustomFontEnabled = false))
    }

    @Test
    fun missingFontFileOnlyClearsTheStaleName() {
        assertNull(resolveCustomFontName("custom_font_1.ttf") { false })
        assertEquals("custom_font_1.ttf", resolveCustomFontName("custom_font_1.ttf") { true })
        assertNull(resolveCustomFontName(null) { true })
    }
}
