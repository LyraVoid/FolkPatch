package me.bmax.apatch.ui.theme.tokens

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class FolkDimensTest {

    @Test
    fun valuesStayPinnedToTheirScale() {
        val dimensions = listOf(
            FolkDimens.Size0 to 0f,
            FolkDimens.SizeHalf to 0.5f,
            FolkDimens.Size1 to 1f,
            FolkDimens.Size2 to 2f,
            FolkDimens.Size3 to 3f,
            FolkDimens.Size4 to 4f,
            FolkDimens.Size5 to 5f,
            FolkDimens.Size6 to 6f,
            FolkDimens.Size8 to 8f,
            FolkDimens.Size10 to 10f,
            FolkDimens.Size12 to 12f,
            FolkDimens.Size13 to 13f,
            FolkDimens.Size14 to 14f,
            FolkDimens.Size16 to 16f,
            FolkDimens.Size18 to 18f,
            FolkDimens.Size20 to 20f,
            FolkDimens.Size22 to 22f,
            FolkDimens.Size24 to 24f,
            FolkDimens.Size28 to 28f,
            FolkDimens.Size30 to 30f,
            FolkDimens.Size32 to 32f,
            FolkDimens.Size36 to 36f,
            FolkDimens.Size38 to 38f,
            FolkDimens.Size40 to 40f,
            FolkDimens.Size42 to 42f,
            FolkDimens.Size44 to 44f,
            FolkDimens.Size48 to 48f,
            FolkDimens.Size52 to 52f,
            FolkDimens.Size56 to 56f,
            FolkDimens.Size58 to 58f,
            FolkDimens.Size64 to 64f,
            FolkDimens.Size68 to 68f,
            FolkDimens.Size69 to 69f,
            FolkDimens.Size72 to 72f,
            FolkDimens.Size78 to 78f,
            FolkDimens.Size80 to 80f,
            FolkDimens.Size86 to 86f,
            FolkDimens.Size88 to 88f,
            FolkDimens.Size96 to 96f,
            FolkDimens.Size110 to 110f,
            FolkDimens.Size128 to 128f,
            FolkDimens.Size140 to 140f,
            FolkDimens.Size150 to 150f,
            FolkDimens.Size160 to 160f,
            FolkDimens.Size200 to 200f,
            FolkDimens.Size300 to 300f,
            FolkDimens.Size320 to 320f,
            FolkDimens.Size340 to 340f,
            FolkDimens.Size350 to 350f,
            FolkDimens.Size360 to 360f,
            FolkDimens.Size400 to 400f,
            FolkDimens.Size440 to 440f,
            FolkDimens.Size480 to 480f,
            FolkDimens.Size500 to 500f,
            FolkDimens.Size600 to 600f,
            FolkDimens.Size720 to 720f,
        )

        dimensions.forEach { (token, expected) ->
            assertEquals(expected, token.value, 0f)
        }
        assertEquals(dimensions.map { it.first.value }.sorted(), dimensions.map { it.first.value })
    }

    @Test
    fun mediaColorsStayPinned() {
        assertEquals(Color.Transparent, FolkColors.Transparent)
        assertEquals(Color.Unspecified, FolkColors.Unspecified)
        assertEquals(Color.White, FolkColors.MediaContent)
        assertEquals(Color.Black, FolkColors.MediaScrim)
    }
}
