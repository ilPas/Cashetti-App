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
                    val monthlyCap = settings.find { it.key == "monthly_cap" }?.value?.toDoubleOrNull() ?: 673.0

                    val (cycleStart, cycleEnd) = BillingCycleUtils.getCycleRange(System.currentTimeMillis(), resetDay)

                    val expenses = repo.allExpenses.first()
                    val discretionaryExpenses = expenses.filter { 
                        it.accountType == "DISCREZIONALE_VARIABILE" && it.dateMillis in cycleStart..cycleEnd 
                    }
                    val totalSpent = discretionaryExpenses.sumOf { it.amount }

                    val subs = repo.allSubscriptions.first()
                    val activeSubs = subs.filter { it.isActive }.sumOf { it.amount }

                    val variableBudgetAvailable = monthlyCap - activeSubs
                    val remainingDiscretionary = variableBudgetAvailable - totalSpent

                    val progressMax = 100
                    val progress = if (variableBudgetAvailable > 0) {
                        ((totalSpent / variableBudgetAvailable).coerceIn(0.0, 1.0) * progressMax).toInt()
                    } else 0

                    // Update UI on main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        views.setTextViewText(R.id.widget_remaining_text, String.format(Locale.ITALY, "€%.2f", remainingDiscretionary))
                        views.setTextViewText(R.id.widget_spent_text, String.format(Locale.ITALY, "€%.2f", totalSpent))
                        views.setTextViewText(R.id.widget_cap_text, String.format(Locale.ITALY, "€%.0f", variableBudgetAvailable))
                        views.setProgressBar(R.id.widget_progress_bar, progressMax, progress, false)
                        
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Initial placeholder while loading
            views.setTextViewText(R.id.widget_remaining_text, "...")
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
