package com.ledgeros.app.ui.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ledgeros.app.ui.theme.chartColors
import java.text.NumberFormat
import java.util.Locale

data class ChartSegment(val label: String, val value: Float)

@Composable
fun SpendingDonutChart(
    segments: List<ChartSegment>,
    total: Double,
    modifier: Modifier = Modifier,
) {
    val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-AU"))
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(segments.size) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        ) { v, _ -> progress = v }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            val strokeWidthDp = 36.dp
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokePx = strokeWidthDp.toPx()
                val inset = strokePx / 2f
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                val arcTopLeft = Offset(inset, inset)

                if (segments.isEmpty() || total <= 0.0) {
                    drawArc(
                        color = surfaceVariant,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx),
                    )
                    return@Canvas
                }

                val totalValue = segments.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.001f)
                var startAngle = -90f

                segments.forEachIndexed { index, segment ->
                    val sweep = (segment.value / totalValue * 360f) * progress
                    drawArc(
                        color = chartColors[index % chartColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep.coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt),
                    )
                    startAngle += segment.value / totalValue * 360f
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currency.format(total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Total spend",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (segments.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ChartLegend(segments = segments)
        }
    }
}

@Composable
private fun ChartLegend(segments: List<ChartSegment>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        segments.chunked(2).forEachIndexed { rowIndex, rowSegments ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowSegments.forEachIndexed { colIndex, segment ->
                    val colorIndex = rowIndex * 2 + colIndex
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    chartColors[colorIndex % chartColors.size],
                                    CircleShape,
                                ),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = segment.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (rowSegments.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun QuarterBarChart(
    labels: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(values.sum()) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        ) { v, _ -> progress = v }
    }

    val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-AU"))
    val maxValue = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        ) {
            val count = labels.size.coerceAtLeast(1)
            val barWidth = size.width / (count * 2.5f)
            val spacing = (size.width - barWidth * count) / (count + 1)
            val chartHeight = size.height - 4.dp.toPx()

            drawLine(
                color = outlineColor,
                start = Offset(0f, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = 1.dp.toPx(),
            )

            values.forEachIndexed { index, value ->
                val x = spacing + index * (barWidth + spacing)
                val barH = (value / maxValue * chartHeight * progress).toFloat().coerceAtLeast(0f)
                val color = if (value >= 0) primaryColor else tertiaryColor

                if (barH > 0f) {
                    drawRect(
                        color = color.copy(alpha = 0.85f),
                        topLeft = Offset(x, chartHeight - barH),
                        size = Size(barWidth, barH),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            labels.forEachIndexed { index, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                    )
                }
            }
        }
    }
}
