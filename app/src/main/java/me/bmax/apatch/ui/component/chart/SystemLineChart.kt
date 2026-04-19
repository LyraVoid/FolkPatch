package me.bmax.apatch.ui.component.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SystemLineChart(
    title: String,
    dataPoints: List<Float>,
    unit: String = "%",
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val colors = MaterialTheme.colorScheme

    val lineColor = if (dataPoints.isNotEmpty() && dataPoints.last() > 80f) {
        colors.error
    } else {
        color
    }

    val currentValue = dataPoints.lastOrNull() ?: 0f

    val animatable = remember { Animatable(0f) }
    var prevPoints by remember { mutableStateOf<List<ChartPoint>>(emptyList()) }
    var currentPoints by remember { mutableStateOf<List<ChartPoint>>(emptyList()) }

    LaunchedEffect(dataPoints) {
        val newPoints = normalizePoints(dataPoints)
        prevPoints = currentPoints
        currentPoints = newPoints
        animatable.snapTo(0f)
        animatable.animateTo(
            1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    val progress = animatable.value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                color = colors.surface,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Text(
            text = if (currentValue > 0f && currentValue.isFinite() && currentValue < Int.MAX_VALUE) "${currentValue.toInt()}$unit" else "--$unit",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = lineColor
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 12.dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                color = colors.onSurfaceVariant,
                fontSize = 12.sp
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 12.dp)
        )

        if (dataPoints.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 36.dp)
            ) {
                val interpolated = interpolatePoints(prevPoints, currentPoints, progress)
                val linePath = buildBezierPath(interpolated, size, heightFraction = 0.75f)
                val fillPath = buildFillPath(linePath, size)

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.3f),
                            lineColor.copy(alpha = 0.0f)
                        ),
                        startY = 0f,
                        endY = size.height
                    )
                )

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 2.5f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        } else {
            Text(
                text = "--",
                style = MaterialTheme.typography.displaySmall.copy(
                    color = colors.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.align(Alignment.Center),
                textAlign = TextAlign.Center
            )
        }
    }
}
