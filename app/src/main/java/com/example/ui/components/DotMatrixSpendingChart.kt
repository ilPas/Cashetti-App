package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseEntity
import com.example.ui.theme.AppColorPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DaySpendingData(
    val dayNumber: Int,
    val dateMillis: Long,
    val formattedDate: String,
    val totalSpent: Double,
    val expenses: List<ExpenseEntity>,
    val dominantCategory: String?,
    val dominantColor: Color
)

@Composable
fun DotMatrixSpendingChart(
    daysData: List<DaySpendingData>,
    selectedDayIndex: Int,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (daysData.isEmpty()) return

    val maxDailySpent = remember(daysData) {
        daysData.maxOfOrNull { it.totalSpent }?.takeIf { it > 0.0 } ?: 100.0
    }

    val selectedDay = daysData.getOrNull(selectedDayIndex) ?: daysData.lastOrNull()
    val scrollState = rememberScrollState()

    val maxDots = 6
    val dotDiameter = 12.dp
    val dotSpacing = 5.dp
    val columnSpacing = 7.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(AppColorPalette.SurfaceCard)
            .padding(18.dp)
    ) {
        // Header with Tendency title and Selected Day Amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Tendenza Spesa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColorPalette.TextPrimary
                )
                Text(
                    text = selectedDay?.let { "${it.formattedDate}" } ?: "Seleziona un giorno",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColorPalette.TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = selectedDay?.let { String.format(Locale.ITALY, "€ %.2f", it.totalSpent) } ?: "€ 0.00",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = if ((selectedDay?.totalSpent ?: 0.0) > 0) Color.White else AppColorPalette.TextSecondary
                )
                if (selectedDay != null && selectedDay.expenses.isNotEmpty()) {
                    Text(
                        text = "${selectedDay.expenses.size} moviment${if (selectedDay.expenses.size == 1) "o" else "i"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = selectedDay.dominantColor
                    )
                } else {
                    Text(
                        text = "Nessuna spesa",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColorPalette.StatusSaving
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Horizontal scrollable dot matrix
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(columnSpacing),
            verticalAlignment = Alignment.Bottom
        ) {
            daysData.forEachIndexed { index, day ->
                val isSelected = index == selectedDayIndex
                val ratio = if (maxDailySpent > 0) (day.totalSpent / maxDailySpent).coerceIn(0.0, 1.0) else 0.0
                val filledDotsCount = if (day.totalSpent > 0.0) {
                    (ratio * (maxDots - 1)).toInt() + 1
                } else {
                    0
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { onSelectDay(index) }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    // Vertical dot stack (Top to Bottom: index 5 to 0)
                    for (dotLevel in (maxDots - 1) downTo 0) {
                        val isFilled = dotLevel < filledDotsCount
                        val dotColor = when {
                            isSelected && isFilled -> Color.White
                            isSelected && !isFilled -> Color.White.copy(alpha = 0.25f)
                            isFilled -> day.dominantColor
                            else -> Color(0xFF2C2C2E)
                        }

                        Box(
                            modifier = Modifier
                                .size(dotDiameter)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        if (dotLevel > 0) {
                            Spacer(modifier = Modifier.height(dotSpacing))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day Number Indicator
                    val showLabel = day.dayNumber == 1 || day.dayNumber % 5 == 0 || isSelected || day.dayNumber == daysData.size
                    Text(
                        text = "${day.dayNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = if (isSelected) 11.sp else 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else if (showLabel) AppColorPalette.TextSecondary else Color.Transparent
                    )
                }
            }
        }
    }
}
