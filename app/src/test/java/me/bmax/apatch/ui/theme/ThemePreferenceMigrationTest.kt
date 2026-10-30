package me.bmax.apatch.ui.theme

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceMigrationTest {
    @Test
    fun removesAllLegacyValuesWithoutTouchingOtherPreferences() {
        for (oldValue in listOf(null, "STANDARD", "MEDIUM", "HIGH", "invalid", 123)) {
            val values = mutableMapOf<String, Any>("color_standard" to "M3E_2025")
            if (oldValue != null) values["color_contrast"] = oldValue
            var editor: SharedPreferences.Editor? = null
            editor = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { _, method, args ->
                when (method.name) {
                    "remove" -> { values.remove(args[0]); editor }
                    "apply" -> null
                    else -> error(method.name)
                }
            } as SharedPreferences.Editor
            val prefs = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
                when (method.name) {
                    "contains" -> values.containsKey(args[0])
                    "edit" -> editor
                    else -> error(method.name)
                }
            } as SharedPreferences

            removeLegacyContrastPreference(prefs)
            removeLegacyContrastPreference(prefs)
            assertEquals(mapOf("color_standard" to "M3E_2025"), values)
        }
    }
}
