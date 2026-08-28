package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColorPalette
import java.util.Locale

data class CategoryChartSlice(
    val categoryName: String,
    val amount: Double,
    val color: Color
)

@Composable
fun RoundedPillDonutChart(
    slices: List<CategoryChartSlice>,
    totalAmount: Double,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    onCategoryClick: ((String) -> Unit)? = null
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val total = totalAmount.coerceAtLeast(0.0)

    val isLightContainer = containerColor == Color.White || containerColor.red > 0.8f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header inside the card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ripartizione Categorie",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isLightContainer) Color(0xFF1E1E1E) else AppColorPalette.TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLightContainer) Color(0xFFF3F4F6) else AppColorPalette.SurfaceCardDark
                ) {
                    Text(
                        text = "${slices.size} ${if (slices.size == 1) "categoria" else "categorie"}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLightContainer) Color(0xFF6B7280) else AppColorPalette.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Donut Chart Container with Center Text
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(190.dp)) {
                    val strokeWidth = 26.dp.toPx()
                    val gapAngle = if (slices.size > 1) 10f else 0f
                    val totalGap = gapAngle * slices.size
                    val availableAngle = (360f - totalGap).coerceAtLeast(0f)

                    if (slices.isEmpty() || total <= 0.0) {
                        drawArc(
                            color = if (isLightContainer) Color(0xFFE5E7EB) else Color(0xFF2C2C2E),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        return@Canvas
                    }

                    var currentAngle = -90f

                    slices.forEach { slice ->
                        val sweep = if (total > 0) ((slice.amount / total) * availableAngle).toFloat() else 0f
                        if (sweep > 0.5f) {
                            val isSelected = selectedCategory == null || selectedCategory == slice.categoryName
                            val alpha = if (isSelected) 1f else 0.35f

                            drawArc(
                                color = slice.color.copy(alpha = alpha),
                                startAngle = currentAngle + (gapAngle / 2f),
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        currentAngle += sweep + gapAngle
                    }
                }

                // Center Label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val activeSlice = slices.find { it.categoryName == selectedCategory }
                    if (activeSlice != null) {
                        Text(
                            text = activeSlice.categoryName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isLightContainer) Color(0xFF6B7280) else AppColorPalette.TextSecondary
                        )
                        Text(
                            text = String.format(Locale.ITALY, "€ %.2f", activeSlice.amount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (isLightContainer) Color(0xFF111827) else AppColorPalette.TextPrimary
                        )
                        val pct = if (total > 0) (activeSlice.amount / total) * 100 else 0.0
                        Text(
                            text = String.format(Locale.ITALY, "%.1f%% del totale", pct),
                            style = MaterialTheme.typography.labelSmall,
                            color = activeSlice.color,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Totale Speso",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isLightContainer) Color(0xFF6B7280) else AppColorPalette.TextSecondary
                        )
                        Text(
                            text = String.format(Locale.ITALY, "€ %.2f", total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = if (isLightContainer) Color(0xFF111827) else AppColorPalette.TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Legend / Category Breakdown List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                slices.forEach { slice ->
                    val percentage = if (total > 0) (slice.amount / total) * 100 else 0.0
                    val isSelected = selectedCategory == slice.categoryName

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedCategory = if (selectedCategory == slice.categoryName) null else slice.categoryName
                                onCategoryClick?.invoke(slice.categoryName)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            if (isLightContainer) Color(0xFFF3F4F6) else AppColorPalette.SurfaceCardDark
                        } else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(slice.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = slice.categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isLightContainer) Color(0xFF1E1E1E) else AppColorPalette.TextPrimary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(Locale.ITALY, "%.1f%%", percentage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isLightContainer) Color(0xFF6B7280) else AppColorPalette.TextSecondary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = String.format(Locale.ITALY, "€ %.2f", slice.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLightContainer) Color(0xFF111827) else AppColorPalette.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
