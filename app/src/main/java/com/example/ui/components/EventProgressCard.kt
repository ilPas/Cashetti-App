package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.EventEntity
import com.example.data.ExpenseEntity
import java.util.Locale

@Composable
fun EventProgressCard(
    event: EventEntity,
    expenses: List<ExpenseEntity>,
    onAddExpense: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val totalSpent = expenses.sumOf { it.amount }
    val progress = if (event.estimatedBudget > 0) (totalSpent / event.estimatedBudget).toFloat().coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(event.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Speso: ${String.format(Locale.ITALY, "%.2f €", totalSpent)}", style = MaterialTheme.typography.bodySmall)
                Text("Budget: ${String.format(Locale.ITALY, "%.2f €", event.estimatedBudget)}", style = MaterialTheme.typography.bodySmall)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp),
                color = if (progress >= 1f) com.example.ui.theme.AppColorPalette.StatusExpense else androidx.compose.ui.graphics.Color.White,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    if (expenses.isEmpty()) {
                        Text("Nessuna spesa inserita per questo evento.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        expenses.forEach { expense ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(expense.note.ifBlank { "Spesa" }, style = MaterialTheme.typography.bodySmall)
                                Text(String.format(Locale.ITALY, "%.2f €", expense.amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onAddExpense,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text("Aggiungi Spesa")
                    }
                }
            }
        }
    }
}
