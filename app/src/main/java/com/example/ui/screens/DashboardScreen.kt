package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ExpenseEntity
import com.example.ui.BudgetUiState
import com.example.ui.components.CategorySpendingChart
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.AppColorPalette
import java.util.Locale

@Composable
fun DashboardScreen(
    state: BudgetUiState,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEventFund: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onUpdateAvatar: (String) -> Unit,
    onDismissMonthlySummary: (Long) -> Unit,
    onNavigateToEditExpense: (Long) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onToggleInvestment: (String, Boolean) -> Unit
) {
    var expenseToView by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onUpdateAvatar(uri.toString())
            }
        }
    )

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Elimina Movimento", color = AppColorPalette.TextPrimary) },
            text = { Text("Sei sicuro di voler eliminare questo movimento?", color = AppColorPalette.TextSecondary) },
            containerColor = AppColorPalette.Surface,
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(expenseToDelete!!)
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Annulla", color = AppColorPalette.TextSecondary)
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
        modifier = Modifier
            .fillMaxSize()
            .background(AppColorPalette.Background)
    ) {
        // Top Bar & Hero Section
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            color = AppColorPalette.Primary
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Profile & Settings row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(AppColorPalette.SurfaceCard.copy(alpha = 0.5f))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.avatarUri.isNotEmpty()) {
                                AsyncImage(
                                    model = state.avatarUri,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Outlined.Person, contentDescription = "Avatar", tint = AppColorPalette.TextPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ciao Pas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColorPalette.TextPrimary
                            )
                            Text(
                                text = "Mancano ${state.daysRemainingInCycle} giorni al reset",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColorPalette.TextPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(44.dp).testTag("settings_button")
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Impostazioni", tint = AppColorPalette.TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Macro-feature 3: Hero display of "Budget Mensile Spendibile"
                Text(
                    text = "ACTIVE BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColorPalette.TextPrimary.copy(alpha = 0.8f),
                    letterSpacing = 1.0.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = String.format(Locale.ITALY, "€ %.2f", state.totalMonthlySpendable),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = AppColorPalette.TextPrimary,
                    modifier = Modifier.testTag("spendable_budget_value")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AppColorPalette.SurfaceCard.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = "Ciclo: ${state.currentCycleLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColorPalette.TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Main Content Card
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = AppColorPalette.Surface
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Macro-feature 4: Architettura a Doppio Cassetto (Personale vs Ginevra)
                item {
                    Text(
                        text = "I Due Cassetti del Mese",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColorPalette.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cassetto 1: Personale (Fixed limit, resets monthly)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cassetto_personale_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColorPalette.SurfaceCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = AppColorPalette.Primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Personale",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColorPalette.Primary
                                        )
                                    }
                                }

                                Text(
                                    text = String.format(Locale.ITALY, "€ %.2f", state.personaleRemaining),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.personaleRemaining >= 0) AppColorPalette.TextPrimary else AppColorPalette.StatusExpense
                                )

                                val personaleProgress = if (state.budgetPersonale > 0) {
                                    (state.personaleSpent / state.budgetPersonale).toFloat().coerceIn(0f, 1f)
                                } else 0f

                                LinearProgressIndicator(
                                    progress = { personaleProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AppColorPalette.Primary,
                                    trackColor = AppColorPalette.SurfaceCardDark,
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Spesi: ${String.format(Locale.ITALY, "%.0f€", state.personaleSpent)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColorPalette.TextSecondary
                                    )
                                    Text(
                                        text = "Cap: ${String.format(Locale.ITALY, "%.0f€", state.budgetPersonale)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColorPalette.TextMuted
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AppColorPalette.SurfaceCardDark
                                ) {
                                    Text(
                                        text = "Reset a fine ciclo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppColorPalette.TextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Cassetto 2: Casa / Ginevra (Imprevisti with Rollover!)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cassetto_ginevra_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = AppColorPalette.SurfaceCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Home,
                                            contentDescription = null,
                                            tint = AppColorPalette.StatusSaving,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Ginevra/Casa",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColorPalette.StatusSaving
                                        )
                                    }
                                }

                                Text(
                                    text = String.format(Locale.ITALY, "€ %.2f", state.ginevraRemaining),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.ginevraRemaining >= 0) AppColorPalette.TextPrimary else AppColorPalette.StatusExpense
                                )

                                val ginevraProgress = if (state.ginevraTotalAvailable > 0) {
                                    (state.ginevraSpent / state.ginevraTotalAvailable).toFloat().coerceIn(0f, 1f)
                                } else 0f

                                LinearProgressIndicator(
                                    progress = { ginevraProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AppColorPalette.StatusSaving,
                                    trackColor = AppColorPalette.SurfaceCardDark,
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Spesi: ${String.format(Locale.ITALY, "%.0f€", state.ginevraSpent)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColorPalette.TextSecondary
                                    )
                                    Text(
                                        text = "Tot: ${String.format(Locale.ITALY, "%.0f€", state.ginevraTotalAvailable)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColorPalette.TextMuted
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AppColorPalette.StatusSaving.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (state.ginevraRollover != 0.0) {
                                            "🔄 Rollover: ${String.format(Locale.ITALY, "%+.0f€", state.ginevraRollover)}"
                                        } else {
                                            "🔄 Rollover attivo"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = AppColorPalette.StatusSaving,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Macro-feature 2: Grief Spending Awareness Card
                if (state.griefSpentInCycle > 0) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = AppColorPalette.StatusExpense.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToHistory() }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Psychology,
                                        contentDescription = null,
                                        tint = AppColorPalette.StatusExpense,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Grief Spending (Impulsi del Mese)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AppColorPalette.StatusExpense
                                        )
                                        Text(
                                            text = "${String.format(Locale.ITALY, "€ %.2f", state.griefSpentInCycle)} in ${state.griefExpensesInCycle.size} acquisti non necessari",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppColorPalette.TextPrimary
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Outlined.ArrowForward,
                                    contentDescription = "Dettagli",
                                    tint = AppColorPalette.StatusExpense
                                )
                            }
                        }
                    }
                }

                // Quick Action Buttons (History & Add Expense)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = AppColorPalette.SurfaceCard,
                            onClick = onNavigateToHistory
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Outlined.History, contentDescription = null, tint = AppColorPalette.TextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Storico & Report", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AppColorPalette.TextPrimary)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = AppColorPalette.Primary,
                            onClick = onNavigateToAddExpense
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, tint = AppColorPalette.TextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Nuova Spesa", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AppColorPalette.TextPrimary)
                            }
                        }
                    }
                }

                // Recent Transactions Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ultimi Movimenti",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColorPalette.TextPrimary
                        )
                        TextButton(onClick = onNavigateToHistory) {
                            Text("Vedi Tutti", color = AppColorPalette.Primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Recent Transactions list
                val recentTransactions = state.allExpenses.filter { it.accountType != "ESSENZIALE_REALE" }.sortedByDescending { it.dateMillis }.take(5)
                if (recentTransactions.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = AppColorPalette.SurfaceCard,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Nessun movimento registrato finora.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColorPalette.TextSecondary,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(recentTransactions) { tx ->
                        TransactionItemCard(
                            expense = tx,
                            onClick = { expenseToView = tx },
                            containerColor = AppColorPalette.SurfaceCard
                        )
                    }
                }

                // Category Spending Chart (excluding exceptional expenses automatically)
                item {
                    CategorySpendingChart(
                        expensesInCycle = state.discretionaryExpensesInCycle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
