package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AppColorPalette
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun AccountAllocationChart(
    discrezionale: Double,
    fondoEventi: Double,
    modifier: Modifier = Modifier
) {
    val total = (discrezionale + fondoEventi).coerceAtLeast(1.0).toFloat()
    val pDiscrezionale = (discrezionale / total).toFloat()
    val pFondoEventi = (fondoEventi / total).toFloat()

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(1f, animationSpec = tween(1000))
    }

    val colorDiscrezionale = AppColorPalette.StatusExpense
    val colorFondoEventi = AppColorPalette.StatusSaving

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ripartizione Mensile Conti",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Stacked Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width * animProgress.value
                    val h = size.height
                    
                    val w1 = width * pDiscrezionale
                    val w2 = width * pFondoEventi
                    
                    val cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())

                    // Draw Fondo Eventi (Amber, goes to the end)
                    drawRoundRect(
                        color = colorFondoEventi,
                        size = Size(width, h),
                        cornerRadius = cornerRadius
                    )
                    
                    // Draw Discrezionale (Green)
                    if (w1 > 0) {
                        drawRoundRect(
                            color = colorDiscrezionale,
                            size = Size(w1, h),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }

            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendItem(color = colorDiscrezionale, label = "Spese", amount = discrezionale)
                LegendItem(color = colorFondoEventi, label = "Risparmi", amount = fondoEventi)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = String.format(Locale.ITALY, "€ %.2f", amount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
