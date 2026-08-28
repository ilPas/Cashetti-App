package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppColorPalette
import java.util.Locale

data class TrendPoint(
    val dayNumber: Int,
    val dateLabel: String,
    val dailySpent: Double,
    val cumulativeSpent: Double,
    val idealCumulative: Double
)

@Composable
fun SpendingTrendLineChart(
    points: List<TrendPoint>,
    totalBudget: Double,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxCumulative = points.maxOfOrNull { it.cumulativeSpent }?.coerceAtLeast(totalBudget) ?: totalBudget.coerceAtLeast(100.0)
    val maxVal = (maxCumulative * 1.15).coerceAtLeast(1.0)

    val selectedPoint = selectedIndex?.let { points.getOrNull(it) } ?: points.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColorPalette.SurfaceCard)
            .padding(18.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Andamento Spesa Cumulata",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColorPalette.TextPrimary
                )
                Text(
                    text = selectedPoint?.let { "Giorno ${it.dayNumber} • ${it.dateLabel}" } ?: "Burn-rate vs Budget Ideale",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColorPalette.TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = selectedPoint?.let { String.format(Locale.ITALY, "€ %.2f", it.cumulativeSpent) } ?: "€ 0.00",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = AppColorPalette.Primary
                )
                if (selectedPoint != null) {
                    val diffFromIdeal = selectedPoint.cumulativeSpent - selectedPoint.idealCumulative
                    val isUnderBudget = diffFromIdeal <= 0
                    Text(
                        text = if (isUnderBudget) "Sotto budget di ${String.format(Locale.ITALY, "€ %.2f", kotlin.math.abs(diffFromIdeal))}"
                        else "Sopra ritmo di ${String.format(Locale.ITALY, "€ %.2f", diffFromIdeal)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnderBudget) AppColorPalette.StatusSaving else AppColorPalette.StatusExpense
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legend: Real curve vs Ideal dashed line
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(AppColorPalette.Primary)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Spesa Reale",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColorPalette.TextSecondary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6B7280))
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "Ritmo Ideale",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColorPalette.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Canvas Line Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(points) {
                        detectTapGestures { tapOffset ->
                            val width = size.width
                            val step = width / (points.size - 1).coerceAtLeast(1)
                            val tappedIndex = (tapOffset.x / step).toInt().coerceIn(0, points.size - 1)
                            selectedIndex = tappedIndex
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val stepX = if (points.size > 1) width / (points.size - 1) else width

                // Draw Horizontal Gridlines
                val gridLevels = listOf(0.25f, 0.5f, 0.75f, 1f)
                gridLevels.forEach { lvl ->
                    val y = height * (1f - lvl)
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw Ideal Linear Curve (Dashed)
                val idealPath = Path()
                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = height * (1f - (pt.idealCumulative / maxVal).toFloat().coerceIn(0f, 1f))
                    if (i == 0) idealPath.moveTo(x, y) else idealPath.lineTo(x, y)
                }
                drawPath(
                    path = idealPath,
                    color = Color.Gray.copy(alpha = 0.5f),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )
                )

                // Draw Actual Cumulative Curve with smooth Bezier Spline
                val realPath = Path()
                val fillPath = Path()

                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = height * (1f - (pt.cumulativeSpent / maxVal).toFloat().coerceIn(0f, 1f))
                    if (i == 0) {
                        realPath.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevX = (i - 1) * stepX
                        val prevY = height * (1f - (points[i - 1].cumulativeSpent / maxVal).toFloat().coerceIn(0f, 1f))
                        val cx1 = (prevX + x) / 2f
                        val cy1 = prevY
                        val cx2 = (prevX + x) / 2f
                        val cy2 = y
                        realPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                        fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
                    }
                }

                fillPath.lineTo(width, height)
                fillPath.close()

                // Gradient Fill Under the Curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AppColorPalette.Primary.copy(alpha = 0.35f),
                            AppColorPalette.Primary.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // Stroke Curve
                drawPath(
                    path = realPath,
                    color = AppColorPalette.Primary,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Draw Selected Indicator Point and vertical guide line
                selectedIndex?.let { idx ->
                    if (idx in points.indices) {
                        val pt = points[idx]
                        val x = idx * stepX
                        val y = height * (1f - (pt.cumulativeSpent / maxVal).toFloat().coerceIn(0f, 1f))

                        drawLine(
                            color = Color.White.copy(alpha = 0.3f),
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = AppColorPalette.Primary,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Axis Day markers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val first = points.firstOrNull()?.dayNumber ?: 1
            val mid = points.getOrNull(points.size / 2)?.dayNumber ?: 15
            val last = points.lastOrNull()?.dayNumber ?: 30

            Text("Giorno $first", style = MaterialTheme.typography.labelSmall, color = AppColorPalette.TextMuted)
            Text("Giorno $mid", style = MaterialTheme.typography.labelSmall, color = AppColorPalette.TextMuted)
            Text("Giorno $last", style = MaterialTheme.typography.labelSmall, color = AppColorPalette.TextMuted)
        }
    }
}
