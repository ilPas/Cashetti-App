package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseEntity
import com.example.ui.BudgetUiState
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.AppColorPalette
import java.util.Locale

@Composable
fun HistoryScreen(
    state: BudgetUiState,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onNavigateToEditExpense: (Long) -> Unit,
    onNavigateToStatistics: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSpecialFilter by remember { mutableStateOf("Tutte") }
    var selectedCategoryFilter by remember { mutableStateOf("Tutte le Categorie") }
    var selectedTimeFilter by remember { mutableStateOf("Questo Ciclo") }
    var searchQuery by remember { mutableStateOf("") }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseToView by remember { mutableStateOf<ExpenseEntity?>(null) }
        
    val timeOptions = listOf("Questo Ciclo", "Sempre", "7 Giorni", "30 Giorni", "1 Anno")
    val specialFilters = listOf("Tutte", "⚡ Solo Grief Spending", "👤 Personale", "🏠 Familiare", "🏛️ Costi Fissi", "🚫 Spese Eccezionali")
    val categoryOptions = remember(state.allExpenses, state.categories) {
        listOf("Tutte le Categorie") + (state.categories.map { it.name } + state.allExpenses.map { it.category }).distinct().sorted()
    }
        
    val filteredExpenses = remember(
        state.allExpenses,
        selectedSpecialFilter,
        selectedCategoryFilter,
        searchQuery,
        selectedTimeFilter,
        state.currentCycleStartMillis,
        state.currentCycleEndMillis
    ) {
        val now = System.currentTimeMillis()
        val timeLimit = when (selectedTimeFilter) {
            "Questo Ciclo" -> state.currentCycleStartMillis
            "7 Giorni" -> now - 7L * 24 * 60 * 60 * 1000
            "30 Giorni" -> now - 30L * 24 * 60 * 60 * 1000
            "1 Anno" -> now - 365L * 24 * 60 * 60 * 1000
            else -> 0L
        }
        val timeUpper = if (selectedTimeFilter == "Questo Ciclo") state.currentCycleEndMillis else Long.MAX_VALUE
        
        state.allExpenses.filter { exp ->
            val matchesCategory = selectedCategoryFilter == "Tutte le Categorie" || exp.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    exp.category.contains(searchQuery, ignoreCase = true) ||
                    exp.note.contains(searchQuery, ignoreCase = true) ||
                    exp.merchant.contains(searchQuery, ignoreCase = true)
            val matchesTime = exp.dateMillis >= timeLimit && exp.dateMillis <= timeUpper
            
            val matchesSpecial = when (selectedSpecialFilter) {
                "⚡ Solo Grief Spending" -> !exp.isNecessary
                "👤 Personale" -> exp.accountType == "SERBATOIO_PERSONALE" || exp.accountType == "DISCREZIONALE_VARIABILE"
                "🏠 Familiare" -> exp.accountType == "SERBATOIO_GINEVRA"
                "🏛️ Costi Fissi" -> exp.accountType == "ESSENZIALE_REALE"
                "🚫 Spese Eccezionali" -> exp.excludeFromStats
                else -> true
            }

            matchesCategory && matchesSearch && matchesTime && matchesSpecial
        }.sortedByDescending { it.dateMillis }
    }
    
    val totalFilteredSpent = remember(filteredExpenses) {
        filteredExpenses.sumOf { kotlin.math.abs(it.amount) }
    }

    // Grief spending metrics
    val griefCount = filteredExpenses.count { !it.isNecessary }
    val griefTotal = filteredExpenses.filter { !it.isNecessary }.sumOf { kotlin.math.abs(it.amount) }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Elimina Movimento", color = AppColorPalette.TextPrimary) },
            text = { Text("Sei sicuro di voler eliminare questo movimento?", color = AppColorPalette.TextSecondary) },
            containerColor = AppColorPalette.Surface,
            confirmButton = {
                Button(onClick = {
                    onDeleteExpense(expenseToDelete!!)
                    expenseToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
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
        modifier = modifier
            .fillMaxSize()
            .background(AppColorPalette.Background)
            .testTag("history_screen")
    ) {
        // Header & Filters
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Storico Movimenti",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColorPalette.TextPrimary
                    )
                    Text(
                        text = "${filteredExpenses.size} moviment${if (filteredExpenses.size == 1) "o" else "i"} • ${String.format(Locale.ITALY, "€ %.2f", totalFilteredSpent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColorPalette.TextSecondary
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = AppColorPalette.Primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = onNavigateToStatistics) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = "Apri Statistiche",
                            tint = AppColorPalette.Primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))

            // Dedicated Banner Link to Statistics Screen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onNavigateToStatistics() },
                shape = RoundedCornerShape(18.dp),
                color = AppColorPalette.SurfaceCard
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppColorPalette.Primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Insights,
                                contentDescription = null,
                                tint = AppColorPalette.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Analisi & Statistiche Avanzate",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColorPalette.TextPrimary
                            )
                            Text(
                                text = "Matrice a pallini, grafici a ciambella e burn-rate",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColorPalette.TextSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = "Vai a Statistiche",
                        tint = AppColorPalette.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cerca per esercente, nota o categoria...", color = AppColorPalette.TextMuted) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = AppColorPalette.Primary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = AppColorPalette.TextPrimary,
                    unfocusedTextColor = AppColorPalette.TextPrimary,
                    focusedBorderColor = AppColorPalette.Primary,
                    unfocusedBorderColor = AppColorPalette.SurfaceCardDark,
                ),
                shape = RoundedCornerShape(14.dp)
            )
            
            Spacer(modifier = Modifier.height(14.dp))

            // Grief Spending Report Summary Banner if active or present
            if (griefCount > 0 && selectedSpecialFilter == "⚡ Solo Grief Spending") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppColorPalette.StatusExpense.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Psychology,
                            contentDescription = null,
                            tint = AppColorPalette.StatusExpense,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Report Acquisti d'Impulso",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColorPalette.StatusExpense
                            )
                            Text(
                                text = "Spese d'impulso nel periodo selezionato: ${String.format(Locale.ITALY, "€ %.2f", griefTotal)} ($griefCount movimenti)",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColorPalette.TextPrimary
                            )
                        }
                    }
                }
            }
            
            // Special Filters (Macro-features)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(specialFilters) { opt ->
                    val isSelected = selectedSpecialFilter == opt
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) AppColorPalette.Primary else AppColorPalette.SurfaceCard,
                        modifier = Modifier.clickable { selectedSpecialFilter = opt }
                    ) {
                        Text(
                            text = opt,
                            color = if (isSelected) AppColorPalette.TextPrimary else AppColorPalette.TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Category Filters
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FilterList, contentDescription = null, tint = AppColorPalette.TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoryOptions) { catOpt ->
                        val isSelected = selectedCategoryFilter == catOpt
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) AppColorPalette.Secondary else AppColorPalette.SurfaceCard,
                            modifier = Modifier.clickable { selectedCategoryFilter = catOpt }
                        ) {
                            Text(
                                text = catOpt,
                                color = if (isSelected) AppColorPalette.TextPrimary else AppColorPalette.TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Time Filters
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.DateRange, contentDescription = null, tint = AppColorPalette.TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(timeOptions) { timeOpt ->
                        val isSelected = selectedTimeFilter == timeOpt
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) AppColorPalette.StatusSaving else AppColorPalette.SurfaceCard,
                            modifier = Modifier.clickable { selectedTimeFilter = timeOpt }
                        ) {
                            Text(
                                text = timeOpt,
                                color = if (isSelected) AppColorPalette.TextPrimary else AppColorPalette.TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Transactions List
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = AppColorPalette.Surface
        ) {
            if (filteredExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun movimento trovato per questi filtri.", color = AppColorPalette.TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { expense ->
                        TransactionItemCard(
                            expense = expense,
                            onClick = { expenseToView = expense },
                            onDelete = { expenseToDelete = expense },
                            containerColor = AppColorPalette.SurfaceCard
                        )
                    }
                }
            }
        }
    }
}
