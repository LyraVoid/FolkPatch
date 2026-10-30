package me.bmax.apatch.ui.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingBarColorTest {

    @Test
    fun floatingBarAlwaysLiftsOffTheCanvas() {
        listOf(Color(0xFFEEEDF6), Color(0xFF181920), Color.Black).forEach { canvas ->
            val bar = resolveFloatingBarContainerColor(canvas)
            assertTrue(bar.luminance() > canvas.luminance())
        }
    }
}
