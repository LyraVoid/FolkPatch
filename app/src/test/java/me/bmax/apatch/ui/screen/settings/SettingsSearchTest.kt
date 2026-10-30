package me.bmax.apatch.ui.screen.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {
    private fun entry(
        key: String,
        title: String,
        summary: String = "",
        category: SettingCategory = SettingCategory.GENERAL,
        categoryName: String = "General",
    ) = ResolvedEntry(
        entry = SettingEntry(key, titleResId = 0, category = category),
        title = title,
        summary = summary,
        categoryName = categoryName,
    )

    @Test
    fun everyTokenMustMatch() {
        val entries = listOf(
            entry("theme", "Theme color", "Change the accent"),
            entry("update", "Check update", "Download the latest release"),
        )

        assertEquals(
            listOf("theme"),
            settingsSearchResults("accent theme", entries).map { it.entry.key },
        )
        assertTrue(settingsSearchResults("accent update", entries).isEmpty())
    }

    @Test
    fun categoryNameIsSearchable() {
        val entries = listOf(
            entry(
                key = "backup",
                title = "Local backup",
                category = SettingCategory.BACKUP,
                categoryName = "Backup",
            ),
        )

        assertEquals(entries, settingsSearchResults("backup", entries))
    }

    @Test
    fun matchRangesAreCaseInsensitiveAndMerged() {
        assertEquals(
            listOf(0..4),
            settingsMatchRanges("Theme", listOf("theme", "he")),
        )
    }

    @Test
    fun queryWhitespaceIsIgnored() {
        assertEquals(listOf("search", "theme"), settingsSearchTokens("  search   theme "))
    }

    @Test
    fun contrastSelectorIsNotIndexed() {
        assertTrue(SettingsRegistry.allSettings.none { it.key == "appearance_color_contrast" })
    }
}
