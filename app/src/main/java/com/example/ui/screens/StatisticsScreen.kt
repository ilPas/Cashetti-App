package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BillingCycleUtils
import com.example.data.ExpenseEntity
import com.example.ui.BudgetUiState
import com.example.ui.components.CategoryChartSlice
import com.example.ui.components.DaySpendingData
import com.example.ui.components.DotMatrixSpendingChart
import com.example.ui.components.RoundedPillDonutChart
import com.example.ui.components.SpendingTrendLineChart
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.TrendPoint
import com.example.ui.theme.AppColorPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    state: BudgetUiState,
    onNavigateUp: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf("Questo Ciclo") }
    var selectedAccountFilter by remember { mutableStateOf("Tutti i Conti") }
    var isPeriodDropdownOpen by remember { mutableStateOf(false) }
    var selectedDayIndex by remember { mutableIntStateOf(-1) }
    var expenseToView by remember { mutableStateOf<ExpenseEntity?>(null) }

    val periodOptions = listOf("Questo Ciclo", "Ciclo Precedente", "Ultimi 30 Giorni", "Tutto")
    val accountFilterOptions = listOf("Tutti i Conti", "👤 Personale", "🏠 Ginevra", "🏛️ Costi Fissi")

    // Determine Time Range based on Selected Period
    val now = System.currentTimeMillis()
    val (cycleStart, cycleEnd) = remember(selectedPeriod, state.currentCycleStartMillis, state.currentCycleEndMillis, state.resetDay) {
        when (selectedPeriod) {
            "Questo Ciclo" -> Pair(state.currentCycleStartMillis, state.currentCycleEndMillis)
            "Ciclo Precedente" -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = state.currentCycleStartMillis
                    add(Calendar.DAY_OF_MONTH, -5)
                }
                BillingCycleUtils.getCycleRange(cal.timeInMillis, state.resetDay)
            }
            "Ultimi 30 Giorni" -> Pair(now - 30L * 24 * 60 * 60 * 1000, now)
            else -> Pair(0L, Long.MAX_VALUE)
        }
    }

    // Filter Expenses
    val filteredExpenses = remember(state.allExpenses, selectedPeriod, selectedAccountFilter, cycleStart, cycleEnd) {
        state.allExpenses.filter { exp ->
            val matchesTime = exp.dateMillis in cycleStart..cycleEnd
            val matchesAccount = when (selectedAccountFilter) {
                "👤 Personale" -> exp.accountType == "SERBATOIO_PERSONALE" || exp.accountType == "DISCREZIONALE_VARIABILE"
                "🏠 Ginevra" -> exp.accountType == "SERBATOIO_GINEVRA"
                "🏛️ Costi Fissi" -> exp.accountType == "ESSENZIALE_REALE"
                else -> !exp.excludeFromStats
            }
            matchesTime && matchesAccount && !exp.excludeFromStats
        }
    }

    val totalSpent = remember(filteredExpenses) {
        filteredExpenses.sumOf { kotlin.math.abs(it.amount) }
    }

    // Category Palette & Slices
    val categoryColors = listOf(
        Color(0xFFA3E635), // Vibrant Lime
        Color(0xFFA78BFA), // Pastel Violet
        Color(0xFF38BDF8), // Sky Blue
        Color(0xFFF472B6), // Pink
        Color(0xFFFBBF24), // Amber
        Color(0xFF34D399), // Emerald
        Color(0xFFFB7185), // Rose
        Color(0xFF818CF8)  // Indigo
    )

    val categorySlices = remember(filteredExpenses) {
        val grouped = filteredExpenses.groupBy { it.category }
            .mapValues { it.value.sumOf { exp -> kotlin.math.abs(exp.amount) } }
            .toList()
            .sortedByDescending { it.second }

        grouped.mapIndexed { index, (cat, amount) ->
            CategoryChartSlice(
                categoryName = cat,
                amount = amount,
                color = categoryColors[index % categoryColors.size]
            )
        }
    }

    // Map Category to Color for quick lookup
    val categoryColorMap = remember(categorySlices) {
        categorySlices.associate { it.categoryName to it.color }
    }

    // Build Day-by-Day Data for Dot Matrix
    val daysData = remember(cycleStart, cycleEnd, filteredExpenses, categoryColorMap) {
        val cal = Calendar.getInstance()
        val startCal = Calendar.getInstance().apply {
            timeInMillis = if (cycleStart > 0) cycleStart else (filteredExpenses.minOfOrNull { it.dateMillis } ?: now)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance().apply {
            timeInMillis = if (cycleEnd < Long.MAX_VALUE) cycleEnd else now
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val list = mutableListOf<DaySpendingData>()
        val curCal = startCal.clone() as Calendar
        var dayNum = 1
        val dateFormat = SimpleDateFormat("d MMM", Locale.ITALY)

        while (curCal.timeInMillis <= endCal.timeInMillis && dayNum <= 35) {
            val dayStart = curCal.timeInMillis
            val nextCal = (curCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
            val dayEnd = nextCal.timeInMillis - 1

            val dayExpenses = filteredExpenses.filter { it.dateMillis in dayStart..dayEnd }
            val dayTotal = dayExpenses.sumOf { kotlin.math.abs(it.amount) }

            val dominantCat = dayExpenses.groupBy { it.category }
                .maxByOrNull { it.value.sumOf { exp -> kotlin.math.abs(exp.amount) } }?.key

            val color = if (dominantCat != null) {
                categoryColorMap[dominantCat] ?: AppColorPalette.Primary
            } else {
                Color(0xFF2C2C2E)
            }

            list.add(
                DaySpendingData(
                    dayNumber = dayNum,
                    dateMillis = dayStart,
                    formattedDate = dateFormat.format(Date(dayStart)),
                    totalSpent = dayTotal,
                    expenses = dayExpenses,
                    dominantCategory = dominantCat,
                    dominantColor = color
                )
            )

            curCal.add(Calendar.DAY_OF_MONTH, 1)
            dayNum++
        }
        list
    }

    // Set default selected day to current day or latest day with expenses
    val activeDayIndex = remember(daysData, selectedDayIndex) {
        if (selectedDayIndex in daysData.indices) {
            selectedDayIndex
        } else {
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayIdx = daysData.indexOfFirst { it.dateMillis == todayCal.timeInMillis }
            if (todayIdx >= 0) todayIdx else (daysData.size - 1).coerceAtLeast(0)
        }
    }

    // Build Trend Points for Burn-rate Line Chart
    val trendPoints = remember(daysData, state.totalMonthlySpendable) {
        var cum = 0.0
        val totalDays = daysData.size.coerceAtLeast(1)
        val dailyIdeal = state.totalMonthlySpendable / totalDays

        daysData.mapIndexed { idx, day ->
            cum += day.totalSpent
            TrendPoint(
                dayNumber = day.dayNumber,
                dateLabel = day.formattedDate,
                dailySpent = day.totalSpent,
                cumulativeSpent = cum,
                idealCumulative = dailyIdeal * (idx + 1)
            )
        }
    }

    // Key Insights & KPI Calculations
    val peakDay = remember(daysData) {
        daysData.filter { it.totalSpent > 0 }.maxByOrNull { it.totalSpent }
    }

    val noSpendDaysCount = remember(daysData) {
        daysData.count { it.totalSpent == 0.0 }
    }

    val daysElapsed = remember(daysData) {
        val todayMillis = System.currentTimeMillis()
        daysData.count { it.dateMillis <= todayMillis }.coerceAtLeast(1)
    }

    val dailyAverageSpent = remember(totalSpent, daysElapsed) {
        if (daysElapsed > 0) totalSpent / daysElapsed else 0.0
    }

    val projectedCycleSpend = remember(dailyAverageSpent, daysData) {
        dailyAverageSpent * daysData.size.coerceAtLeast(1)
    }

    val necessarySpent = remember(filteredExpenses) {
        filteredExpenses.filter { it.isNecessary }.sumOf { kotlin.math.abs(it.amount) }
    }
    val unnecessarySpent = remember(filteredExpenses) {
        filteredExpenses.filter { !it.isNecessary }.sumOf { kotlin.math.abs(it.amount) }
    }
    val necessaryPercentage = remember(totalSpent, necessarySpent) {
        if (totalSpent > 0) (necessarySpent / totalSpent) * 100 else 100.0
    }

    val selectedDayData = daysData.getOrNull(activeDayIndex)

    // Transaction Details Dialog
    expenseToView?.let { exp ->
        TransactionDetailDialog(
            expense = exp,
            onDismiss = { expenseToView = null },
            onEdit = {},
            onDelete = {}
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColorPalette.Background)
            .testTag("statistics_screen")
    ) {
        // App Bar / Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = AppColorPalette.SurfaceCard,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Indietro",
                            tint = AppColorPalette.TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Statistiche",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColorPalette.TextPrimary
                )
            }

            // Period Selector Dropdown Pill
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AppColorPalette.SurfaceCard,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { isPeriodDropdownOpen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = selectedPeriod,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColorPalette.TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = AppColorPalette.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isPeriodDropdownOpen,
                    onDismissRequest = { isPeriodDropdownOpen = false }
                ) {
                    periodOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                selectedPeriod = opt
                                isPeriodDropdownOpen = false
                            }
                        )
                    }
                }
            }
        }

        // Account Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(accountFilterOptions) { opt ->
                val isSelected = selectedAccountFilter == opt
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AppColorPalette.Primary else AppColorPalette.SurfaceCard,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedAccountFilter = opt }
                ) {
                    Text(
                        text = opt,
                        color = if (isSelected) AppColorPalette.TextPrimary else AppColorPalette.TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Main Scrollable Statistics Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Tendency: Dot Matrix Chart
            item {
                DotMatrixSpendingChart(
                    daysData = daysData,
                    selectedDayIndex = activeDayIndex,
                    onSelectDay = { selectedDayIndex = it }
                )
            }

            // 2. Spending: Rounded Pill Donut Chart (Inspired by Screenshot)
            item {
                RoundedPillDonutChart(
                    slices = categorySlices,
                    totalAmount = totalSpent,
                    containerColor = Color.White
                )
            }

            // 3. Trendline: Cumulative Burn-Rate Chart
            item {
                SpendingTrendLineChart(
                    points = trendPoints,
                    totalBudget = state.totalMonthlySpendable
                )
            }

            // 4. Key Performance Insights & KPI Cards
            item {
                Text(
                    text = "Analisi & Metriche Chiave",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColorPalette.TextPrimary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            // Peak Day & Daily Average Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Peak Day Card
                    StatKpiCard(
                        title = "Giorno Record",
                        value = peakDay?.let { String.format(Locale.ITALY, "€ %.2f", it.totalSpent) } ?: "€ 0.00",
                        subtitle = peakDay?.formattedDate ?: "Nessuna spesa",
                        icon = Icons.Outlined.LocalFireDepartment,
                        iconTint = AppColorPalette.StatusExpense,
                        modifier = Modifier.weight(1f)
                    )

                    // Daily Average Card
                    StatKpiCard(
                        title = "Media Giornaliera",
                        value = String.format(Locale.ITALY, "€ %.2f", dailyAverageSpent),
                        subtitle = "su $daysElapsed giorni",
                        icon = Icons.Outlined.Speed,
                        iconTint = AppColorPalette.Primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // No-Spend Days & Projection Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // No-Spend Days Card
                    StatKpiCard(
                        title = "Giorni No-Spend",
                        value = "$noSpendDaysCount su ${daysData.size}",
                        subtitle = if (noSpendDaysCount > 0) "${((noSpendDaysCount.toDouble() / daysData.size) * 100).toInt()}% senza uscite" else "0 giorni a zero spese",
                        icon = Icons.Outlined.Savings,
                        iconTint = AppColorPalette.StatusSaving,
                        modifier = Modifier.weight(1f)
                    )

                    // Projection Card
                    val isSurplus = projectedCycleSpend <= state.totalMonthlySpendable
                    StatKpiCard(
                        title = "Stima Fine Ciclo",
                        value = String.format(Locale.ITALY, "€ %.0f", projectedCycleSpend),
                        subtitle = if (isSurplus) "In linea col budget" else "Rischio sforamento",
                        icon = if (isSurplus) Icons.Outlined.TrendingDown else Icons.Outlined.TrendingUp,
                        iconTint = if (isSurplus) AppColorPalette.StatusSaving else AppColorPalette.StatusExpense,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quality of Spending (Grief / Necessity analysis)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColorPalette.SurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Psychology,
                                    contentDescription = null,
                                    tint = AppColorPalette.Secondary
                                )
                                Text(
                                    text = "Qualità delle Uscite",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColorPalette.TextPrimary
                                )
                            }
                            Text(
                                text = String.format(Locale.ITALY, "%.0f%% Necessarie", necessaryPercentage),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColorPalette.StatusSaving
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dual Bar Indicator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        ) {
                            val necWeight = (necessaryPercentage / 100.0).toFloat().coerceIn(0f, 1f)
                            val unnecWeight = (1f - necWeight).coerceIn(0f, 1f)

                            if (necWeight > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(necWeight)
                                        .fillMaxSize()
                                        .background(AppColorPalette.StatusSaving)
                                )
                            }
                            if (unnecWeight > 0f) {
                                Box(
                                    modifier = Modifier
                                        .weight(unnecWeight)
                                        .fillMaxSize()
                                        .background(AppColorPalette.StatusExpense)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Spese Essenziali: ${String.format(Locale.ITALY, "€ %.2f", necessarySpent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColorPalette.TextSecondary
                            )
                            Text(
                                text = "Sfizi / Impulso: ${String.format(Locale.ITALY, "€ %.2f", unnecessarySpent)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColorPalette.TextSecondary
                            )
                        }
                    }
                }
            }

            // Selected Day Breakdown (if tapped)
            if (selectedDayData != null && selectedDayData.expenses.isNotEmpty()) {
                item {
                    Text(
                        text = "Movimenti di ${selectedDayData.formattedDate}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColorPalette.TextPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(selectedDayData.expenses, key = { it.id }) { exp ->
                    TransactionItemCard(
                        expense = exp,
                        onClick = { expenseToView = exp },
                        onDelete = {},
                        containerColor = AppColorPalette.SurfaceCard
                    )
                }
            }
        }
    }
}

@Composable
private fun StatKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppColorPalette.SurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColorPalette.TextSecondary
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = AppColorPalette.TextPrimary
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColorPalette.TextMuted
            )
        }
    }
}
