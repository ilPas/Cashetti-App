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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseEntity
import com.example.ui.BudgetUiState
import com.example.ui.components.DonutChart
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.AccountBarChart
import com.example.ui.components.BarChartData
import com.example.ui.theme.AppColorPalette
import java.util.Locale

@Composable
fun HistoryScreen(
    state: BudgetUiState,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onNavigateToEditExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSpecialFilter by remember { mutableStateOf("Tutte") }
    var selectedCategoryFilter by remember { mutableStateOf("Tutte le Categorie") }
    var selectedTimeFilter by remember { mutableStateOf("Sempre") }
    var searchQuery by remember { mutableStateOf("") }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntity?>(null) }
    var expenseToView by remember { mutableStateOf<ExpenseEntity?>(null) }
    var showChartDetail by remember { mutableStateOf(false) }
        
    val timeOptions = listOf("Sempre", "Questo Ciclo", "7 Giorni", "30 Giorni", "1 Anno")
    val specialFilters = listOf("Tutte", "⚡ Solo Grief Spending", "👤 Personale", "🏠 Ginevra", "🚫 Spese Eccezionali")
    val categoryOptions = remember(state.categories) {
        listOf("Tutte le Categorie") + state.categories.map { it.name }.distinct()
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
                "🏠 Ginevra" -> exp.accountType == "SERBATOIO_GINEVRA"
                "🚫 Spese Eccezionali" -> exp.excludeFromStats
                else -> exp.accountType != "ESSENZIALE_REALE"
            }

            matchesCategory && matchesSearch && matchesTime && matchesSpecial && exp.accountType != "ESSENZIALE_REALE"
        }.sortedByDescending { it.dateMillis }
    }
    
    // Macro-feature 1: Exclude "excludeFromStats" from regular chart stats unless explicitly filtered!
    val statsExpenses = remember(filteredExpenses, selectedSpecialFilter) {
        if (selectedSpecialFilter == "🚫 Spese Eccezionali") {
            filteredExpenses
        } else {
            filteredExpenses.filter { !it.excludeFromStats }
        }
    }

    val expensesByCategory = remember(statsExpenses) {
        statsExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { kotlin.math.abs(it.amount) } }
            .toList()
            .sortedByDescending { it.second }
    }
    
    val chartData = expensesByCategory.map { it.second.toFloat() }
    val chartColors = listOf(
        Color(0xFF8B5CF6), Color(0xFF6366F1), Color(0xFF3B82F6), 
        Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFFD946EF)
    )

    val accountDiscrezionaleTotal = statsExpenses.filter { it.accountType == "SERBATOIO_PERSONALE" || it.accountType == "DISCREZIONALE_VARIABILE" }.sumOf { kotlin.math.abs(it.amount) }
    val accountGinevraTotal = statsExpenses.filter { it.accountType == "SERBATOIO_GINEVRA" }.sumOf { kotlin.math.abs(it.amount) }
    val accountEventiTotal = statsExpenses.filter { it.accountType == "FONDO_EVENTI_WITHDRAWAL" || it.accountType == "FONDO_EVENTI_DEPOSIT" }.sumOf { kotlin.math.abs(it.amount) }
    
    val barChartData = listOf(
        BarChartData("Discrez.", accountDiscrezionaleTotal.toFloat(), AppColorPalette.Primary),
        BarChartData("Eventi", accountEventiTotal.toFloat(), AppColorPalette.Secondary),
        BarChartData("Sicurezza", accountGinevraTotal.toFloat(), AppColorPalette.StatusSaving)
    )

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

    if (showChartDetail) {
        AlertDialog(
            onDismissRequest = { showChartDetail = false },
            title = { Text("Dettaglio Categorie", color = AppColorPalette.TextPrimary) },
            containerColor = AppColorPalette.Surface,
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val total = expensesByCategory.sumOf { it.second }
                    items(expensesByCategory.size) { index ->
                        val item = expensesByCategory[index]
                        val color = chartColors[index % chartColors.size]
                        val percentage = if (total > 0) (item.second / total) * 100 else 0.0
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.size(16.dp).background(color, RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(item.first, modifier = Modifier.weight(1f), color = AppColorPalette.TextPrimary)
                            Text(String.format(Locale.ITALY, "%.1f%%", percentage), modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodySmall, color = AppColorPalette.TextSecondary)
                            Text(String.format(Locale.ITALY, "€ %.2f", item.second), fontWeight = FontWeight.Bold, color = AppColorPalette.TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChartDetail = false }) { Text("Chiudi", color = AppColorPalette.Primary) }
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
            Text(
                text = "Storico & Statistiche",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColorPalette.TextPrimary
            )
            
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

            // Macro-feature 2: Grief Spending Report Summary Banner if active or present
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
                                text = "Totale: ${String.format(Locale.ITALY, "€ %.2f", griefTotal)} su $griefCount spese contrassegnate come non necessarie.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColorPalette.TextPrimary
                            )
                        }
                    }
                }
            }

            // Bar Chart per Conti
            if (barChartData.any { it.value > 0 }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppColorPalette.SurfaceCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            text = "Spese per Conto",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppColorPalette.Primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        AccountBarChart(
                            data = barChartData,
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Chart Section
            if (chartData.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AppColorPalette.SurfaceCard,
                    modifier = Modifier.fillMaxWidth().clickable { showChartDetail = true }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(
                            data = chartData,
                            colors = chartColors,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Ripartizione Categorie (Regolari)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColorPalette.Primary
                            )
                            expensesByCategory.take(3).forEachIndexed { index, pair ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(chartColors.getOrElse(index) { chartColors.last() }, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${pair.first}: ${String.format(Locale.ITALY, "%.2f €", pair.second)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColorPalette.TextPrimary
                                    )
                                }
                            }
                            if (expensesByCategory.size > 3) {
                                Text(
                                    text = "Tocca per vedere tutte le categorie...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColorPalette.TextSecondary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
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

        // List
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
