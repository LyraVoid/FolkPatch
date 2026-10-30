package me.bmax.apatch.ui.component.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import me.bmax.apatch.ui.theme.tokens.FolkDimens
import me.bmax.apatch.ui.theme.tokens.FolkType

private const val WaveHeightFraction = 0.85f
private const val WaveAlpha = 0.35f

@Composable
fun WaveChart(
    label: String,
    value: String,
    samples: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = FolkDimens.Size110,
) {
    val animatable = remember { Animatable(0f) }
    var previous by remember { mutableStateOf<List<ChartPoint>>(emptyList()) }
    var current by remember { mutableStateOf<List<ChartPoint>>(emptyList()) }
    val growthSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()

    LaunchedEffect(samples) {
        val next = normalizePoints(samples)
        previous = current
        current = next
        animatable.snapTo(0f)
        animatable.animateTo(targetValue = 1f, animationSpec = growthSpec)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FolkDimens.Size12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = value, style = FolkType.Title, color = color)
            Text(
                text = label,
                style = FolkType.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(FolkDimens.Size8))
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .drawWithCache {
                    val fill = Path()
                    val stroke = Stroke(
                        width = FolkDimens.Size2.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    )
                    val brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = WaveAlpha), color.copy(alpha = 0f)),
                        startY = 0f,
                        endY = size.height,
                    )
                    onDrawBehind {
                        val points = interpolatePoints(previous, current, animatable.value)
                        if (points.size < 2) {
                            drawLine(
                                color = color.copy(alpha = WaveAlpha),
                                start = Offset(0f, size.height / 2f),
                                end = Offset(size.width, size.height / 2f),
                                strokeWidth = FolkDimens.Size2.toPx(),
                                cap = StrokeCap.Round,
                            )
                            return@onDrawBehind
                        }
                        val line = buildBezierPath(
                            points = points,
                            size = size,
                            heightFraction = WaveHeightFraction,
                        )
                        fill.reset()
                        fill.addPath(line)
                        fill.lineTo(size.width, size.height)
                        fill.lineTo(0f, size.height)
                        fill.close()
                        drawPath(path = fill, brush = brush)
                        drawPath(path = line, color = color, style = stroke)
                    }
                },
        )
    }
}
