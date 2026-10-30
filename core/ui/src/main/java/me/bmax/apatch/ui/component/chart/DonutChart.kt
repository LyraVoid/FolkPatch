package me.bmax.apatch.ui.component.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.theme.tokens.FolkType

@Immutable
data class PieSlice(
    val value: Int,
    val color: Color,
)

@Composable
fun ModulePieChart(
    slices: List<PieSlice>,
    centerLabel: String,
    modifier: Modifier = Modifier,
) {
    val active = slices.filter { it.value > 0 }
    val total = active.sumOf { it.value }
    val sliceGap = if (active.size > 1) 2f else 0f
    val availableAngle = 360f - sliceGap * active.size
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = FolkDimens.Size20.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            if (total <= 0) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                return@Canvas
            }

            var startAngle = -90f
            active.forEach { slice ->
                val sweep = slice.value.toFloat() / total * availableAngle
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                startAngle += sweep + sliceGap
            }
        }
        Text(text = centerLabel, style = FolkType.Title)
    }
}
