package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.theme.AppColorPalette
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.BudgetUiState
import com.example.ui.components.TransactionItemCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import com.example.data.ExpenseEntity
import com.example.ui.components.TransactionDetailDialog

import java.util.Locale

@Composable
fun EssentialScreen(
    state: BudgetUiState,
    onNavigateToAddExpenseWithAccount: (String) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onNavigateToEditExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expenseToView by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Elimina Movimento") },
            text = { Text("Sei sicuro di voler eliminare questo movimento? Questa azione ripristinerà i saldi corrispondenti.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteExpense(expenseToDelete!!)
                    expenseToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    expenseToView?.let { expense ->
        TransactionDetailDialog(
            expense = expense,
            onDismiss = { expenseToView = null },
            onEdit = { onNavigateToEditExpense(expense.id) },
            onDelete = { expenseToDelete = expense }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("essential_screen")
    ) {
        // Header
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Costi Fissi",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Costi fissi e necessità reali tracciate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Totals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Totale Mensile",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                    Text(
                        text = try { String.format(Locale.ITALY, "€ %.2f", state.essentialTotalSpent) } catch(e: Exception) { "€ 0,00" },
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                FloatingActionButton(
                    onClick = { onNavigateToAddExpenseWithAccount("ESSENZIALE_REALE") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Aggiungi Costo fisso")
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            val essentialExpenses = state.essentialExpensesInCycle
            if (essentialExpenses.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nessun costo fisso registrato.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(essentialExpenses, key = { it.id }) { expense ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            TransactionItemCard(
                                expense = expense,
                                onClick = { expenseToView = expense },
                                onDelete = { expenseToDelete = expense },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
