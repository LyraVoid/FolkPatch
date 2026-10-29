package me.bmax.apatch.ui.theme.tokens

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import org.junit.Assert.assertEquals
import org.junit.Test

class FolkTypeTest {

    private fun assertText(
        style: TextStyle,
        size: Float,
        lineHeight: Float,
        weight: FontWeight = FontWeight.Normal,
        lineBreak: LineBreak = LineBreak.Paragraph,
        hyphens: Hyphens = Hyphens.Auto,
        features: String? = null,
    ) {
        assertEquals(size, style.fontSize.value, 0.001f)
        assertEquals(lineHeight, style.lineHeight.value, 0.001f)
        assertEquals(weight, style.fontWeight)
        assertEquals(lineBreak, style.lineBreak)
        assertEquals(hyphens, style.hyphens)
        assertEquals(features, style.fontFeatureSettings)
        assertEquals(0f, style.letterSpacing.value, 0.001f)
    }

    @Test
    fun levelsUseThePinnedLadder() {
        assertText(TitleMetrics, 17f, 22f, FontWeight.Medium, LineBreak.Heading)
        assertText(SummaryMetrics, 15f, 20f)
        assertText(CaptionMetrics, 14f, 18f)
    }

    @Test
    fun ladderDescendsInSizeAndLineHeight() {
        val sizes = listOf(TitleMetrics, SummaryMetrics, CaptionMetrics).map { it.fontSize.value }
        val lines = listOf(TitleMetrics, SummaryMetrics, CaptionMetrics).map { it.lineHeight.value }

        assertEquals(sizes.sortedDescending(), sizes)
        assertEquals(lines.sortedDescending(), lines)
    }

    @Test
    fun numeralUsesSummaryMetricsWithTabularFigures() {
        assertText(NumeralMetrics, 15f, 20f, features = "tnum")
    }

    @Test
    fun machineKeepsWholeTokensAndTabularFigures() {
        assertText(
            MachineMetrics,
            14f,
            18f,
            lineBreak = LineBreak.Heading,
            hyphens = Hyphens.None,
            features = "tnum",
        )
    }
}
