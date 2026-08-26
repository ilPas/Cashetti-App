package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColorPalette

data class BarChartData(
    val label: String,
    val value: Float,
    val color: Color
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun AccountBarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier,
    maxValue: Float = data.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
) {
    val textMeasurer = rememberTextMeasurer()
    val axisColor = AppColorPalette.TextSecondary.copy(alpha = 0.3f)
    val labelColor = AppColorPalette.TextSecondary
    val valueColor = AppColorPalette.TextPrimary

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidth = canvasWidth / (data.size * 2f)
        val spaceBetween = barWidth
        val bottomPadding = 40.dp.toPx()
        val topPadding = 20.dp.toPx()

        // Draw horizontal grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = topPadding + (canvasHeight - bottomPadding - topPadding) * (i.toFloat() / gridLines)
            drawLine(
                color = axisColor,
                start = Offset(0f, y),
                end = Offset(canvasWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw bars and labels
        data.forEachIndexed { index, barData ->
            val xOffset = spaceBetween / 2 + index * (barWidth + spaceBetween)
            
            val normalizedHeight = if (maxValue > 0) (barData.value / maxValue) * (canvasHeight - bottomPadding - topPadding) else 0f
            val barHeight = if (barData.value > 0 && normalizedHeight < 4.dp.toPx()) 4.dp.toPx() else normalizedHeight
            
            val yOffset = canvasHeight - bottomPadding - barHeight

            drawRoundRect(
                color = barData.color,
                topLeft = Offset(xOffset, yOffset),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Draw X-axis label
            val textLayoutResult = textMeasurer.measure(
                text = barData.label,
                style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = labelColor)
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    xOffset + (barWidth - textLayoutResult.size.width) / 2,
                    canvasHeight - bottomPadding + 8.dp.toPx()
                )
            )

            // Draw Value label above bar
            val valueString = String.format("€%.0f", barData.value)
            val valueLayoutResult = textMeasurer.measure(
                text = valueString,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 10.sp, 
                    color = valueColor, 
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            )
            drawText(
                textLayoutResult = valueLayoutResult,
                topLeft = Offset(
                    xOffset + (barWidth - valueLayoutResult.size.width) / 2,
                    yOffset - valueLayoutResult.size.height - 4.dp.toPx()
                )
            )
        }
    }
}
