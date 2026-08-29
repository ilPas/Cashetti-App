package com.example.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.BillingCycleUtils
import com.example.data.BudgetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class BudgetWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private fun formatCurrency(amount: Double): String {
            val sign = if (amount < 0) "-" else ""
            return String.format(Locale.ITALY, "%s€\u00A0%.2f", sign, kotlin.math.abs(amount))
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_budget)
            
            // Intent to launch app when clicked
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            // Calculate data in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val repo = BudgetRepository(db.budgetDao())
                        
                    val settings = repo.allSettings.first()
                    val resetDay = settings.find { it.key == "reset_day" }?.value?.toIntOrNull() ?: 27
                    val budgetPersonale = settings.find { it.key == "budget_personale" }?.value?.toDoubleOrNull() ?: settings.find { it.key == "monthly_cap" }?.value?.toDoubleOrNull() ?: 700.0
                    val budgetGinevra = settings.find { it.key == "budget_ginevra" }?.value?.toDoubleOrNull() ?: settings.find { it.key == "ginevra_monthly_cap" }?.value?.toDoubleOrNull() ?: 180.0

                    val now = System.currentTimeMillis()
                    val (cycleStart, cycleEnd) = BillingCycleUtils.getCycleRange(now, resetDay)
                    val daysRemaining = (((cycleEnd - now).coerceAtLeast(0L)) / (24 * 60 * 60 * 1000L)).toInt() + 1

                    val expenses = repo.allExpenses.first()
                    
                    val personaleExpensesInCycle = expenses.filter { 
                        (it.accountType == "SERBATOIO_PERSONALE" || it.accountType == "DISCREZIONALE_VARIABILE") && 
                        it.dateMillis in cycleStart..cycleEnd 
                    }
                    val personaleSpent = personaleExpensesInCycle.filter { !it.excludeFromStats }.sumOf { if (it.isIncome) -kotlin.math.abs(it.amount) else kotlin.math.abs(it.amount) }
                    
                    val subs = repo.allSubscriptions.first()
                    val recurringTotal = subs.filter { it.isActive }.sumOf { it.amount }
                    val personaleRemaining = budgetPersonale - recurringTotal - personaleSpent

                    // Calcolo Rollover
                    val initialGinevraRollover = settings.find { it.key == "ginevra_initial_rollover" }?.value?.toDoubleOrNull() ?: 0.0
                    val ginevraExpenses = expenses.filter { it.accountType == "SERBATOIO_GINEVRA" }
                    var historicalRollover = 0.0
                    if (ginevraExpenses.isNotEmpty()) {
                        val earliestTime = ginevraExpenses.minOf { it.dateMillis }
                        var testRef = cycleStart - 1000L
                        val visitedCycles = mutableSetOf<Pair<Long, Long>>()
                        while (testRef >= earliestTime) {
                            val (pStart, pEnd) = BillingCycleUtils.getCycleRange(testRef, resetDay)
                            if (visitedCycles.add(Pair(pStart, pEnd))) {
                                val pSpent = expenses.filter {
                                    it.accountType == "SERBATOIO_GINEVRA" && !it.excludeFromStats && it.dateMillis in pStart..pEnd
                                }.sumOf { if (it.isIncome) -kotlin.math.abs(it.amount) else kotlin.math.abs(it.amount) }
                                historicalRollover += (budgetGinevra - pSpent)
                            }
                            testRef = pStart - 1000L
                        }
                    }
                    val ginevraRollover = initialGinevraRollover + historicalRollover
                    val ginevraTotalAvailable = budgetGinevra + ginevraRollover
                    val ginevraExpensesInCycle = expenses.filter { 
                        it.accountType == "SERBATOIO_GINEVRA" && it.dateMillis in cycleStart..cycleEnd 
                    }
                    val ginevraSpent = ginevraExpensesInCycle.filter { !it.excludeFromStats }.sumOf { if (it.isIncome) -kotlin.math.abs(it.amount) else kotlin.math.abs(it.amount) }
                    val ginevraRemaining = ginevraTotalAvailable - ginevraSpent

                    val totalMonthlySpendable = personaleRemaining + ginevraRemaining
                    
                    // Budget Giornaliero Rimanente
                    val calendar = java.util.Calendar.getInstance()
                    calendar.timeInMillis = now
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    val startOfToday = calendar.timeInMillis

                    val allExpensesInCycle = (personaleExpensesInCycle + ginevraExpensesInCycle).distinctBy { it.id }
                    val cycleIncomes = allExpensesInCycle.filter { it.isIncome }.sumOf { kotlin.math.abs(it.amount) }
                    val spentBeforeToday = allExpensesInCycle.filter { it.dateMillis < startOfToday && !it.excludeFromStats && !it.isIncome }.sumOf { kotlin.math.abs(it.amount) }
                    val spentToday = allExpensesInCycle.filter { it.dateMillis >= startOfToday && !it.excludeFromStats && !it.isIncome }.sumOf { kotlin.math.abs(it.amount) }

                    val totalAvailableInCycle = (budgetPersonale - recurringTotal) + ginevraTotalAvailable + cycleIncomes
                    val remainingBeforeToday = totalAvailableInCycle - spentBeforeToday
                    val startOfDayDailyBudget = if (daysRemaining > 0) remainingBeforeToday / daysRemaining else remainingBeforeToday
                    val dailyBudget = startOfDayDailyBudget - spentToday
                    
                    // Progress bar calculation
                    val budgetMax = (budgetPersonale - recurringTotal) + ginevraTotalAvailable
                    val progressMax = 100
                    val spentTotal = personaleSpent + ginevraSpent
                    val progress = if (budgetMax > 0) {
                        ((spentTotal / budgetMax).coerceIn(0.0, 1.0) * progressMax).toInt()
                    } else 0

                    // Update UI on main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        views.setTextViewText(R.id.widget_remaining_text, formatCurrency(totalMonthlySpendable))
                        views.setTextViewText(R.id.widget_daily_text, formatCurrency(dailyBudget))
                        views.setProgressBar(R.id.widget_progress_bar, progressMax, progress, false)
                        
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Initial placeholder while loading
            views.setTextViewText(R.id.widget_remaining_text, "...")
            views.setTextViewText(R.id.widget_daily_text, "...")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BudgetWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
