package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseEntity
import com.example.ui.BudgetUiState
import com.example.ui.theme.AppColorPalette
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefundsScreen(
    state: BudgetUiState,
    onProcessRefund: (ExpenseEntity, Double) -> Unit,
    onCancelRefund: (ExpenseEntity) -> Unit
) {
    var expenseToProcess by remember { mutableStateOf<ExpenseEntity?>(null) }
    var receivedAmountText by remember { mutableStateOf("") }
    
    val pendingRefunds = state.pendingRefunds
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ITALY)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rimborsi in attesa", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColorPalette.Surface,
                    titleContentColor = AppColorPalette.TextPrimary
                )
            )
        },
        containerColor = AppColorPalette.Surface
    ) { padding ->
        if (pendingRefunds.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = AppColorPalette.TextMuted)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Nessun rimborso in attesa", color = AppColorPalette.TextSecondary, style = MaterialTheme.typography.titleMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingRefunds) { expense ->
                    val daysPassed = ((System.currentTimeMillis() - expense.dateMillis) / (1000 * 60 * 60 * 24)).toInt()
                    val isLate = daysPassed > 30

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AppColorPalette.SurfaceCard),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = expense.category,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColorPalette.TextPrimary
                                    )
                                    if (expense.refundNote.isNotBlank()) {
                                        Text(
                                            text = expense.refundNote,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppColorPalette.TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Registrata il: ${dateFormat.format(Date(expense.dateMillis))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColorPalette.TextMuted
                                    )
                                }
                                Text(
                                    text = String.format(Locale.ITALY, "€ %.2f", expense.expectedRefundAmount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColorPalette.Primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Outlined.AccessTime, 
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isLate) AppColorPalette.StatusExpense else AppColorPalette.TextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "In attesa da $daysPassed giorni",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isLate) AppColorPalette.StatusExpense else AppColorPalette.TextSecondary,
                                    fontWeight = if (isLate) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            
                            if (isLate) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AppColorPalette.StatusExpense.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Warning, contentDescription = null, tint = AppColorPalette.StatusExpense, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Attesa prolungata. Valuta di chiudere la pendenza se il rimborso non arriverà.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppColorPalette.StatusExpense
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isLate) {
                                    OutlinedButton(
                                        onClick = { onCancelRefund(expense) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColorPalette.StatusExpense),
                                    ) {
                                        Text("Non ricevuto", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Button(
                                    onClick = {
                                        receivedAmountText = String.format(Locale.US, "%.2f", expense.expectedRefundAmount)
                                        expenseToProcess = expense
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColorPalette.Primary)
                                ) {
                                    Text("Registra Ricevuto", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
        
        if (expenseToProcess != null) {
            AlertDialog(
                onDismissRequest = { expenseToProcess = null },
                title = { Text("Registra rimborso") },
                text = {
                    Column {
                        Text(
                            text = "Inserisci l'importo che hai effettivamente ricevuto.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColorPalette.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = receivedAmountText,
                            onValueChange = { receivedAmountText = it },
                            label = { Text("Importo ricevuto (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColorPalette.Primary,
                                unfocusedBorderColor = AppColorPalette.SurfaceCardDark,
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsedAmount = receivedAmountText.replace(',', '.').toDoubleOrNull()
                            if (parsedAmount != null && parsedAmount > 0) {
                                onProcessRefund(expenseToProcess!!, parsedAmount)
                                expenseToProcess = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColorPalette.Primary)
                    ) {
                        Text("Conferma")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { expenseToProcess = null }) {
                        Text("Annulla", color = AppColorPalette.TextSecondary)
                    }
                },
                containerColor = AppColorPalette.SurfaceCard,
                titleContentColor = AppColorPalette.TextPrimary,
                textContentColor = AppColorPalette.TextPrimary
            )
        }
    }
}
