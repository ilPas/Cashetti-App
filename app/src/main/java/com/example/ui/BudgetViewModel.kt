package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import androidx.lifecycle.viewModelScope
import com.example.data.BillingCycleUtils
import com.example.data.BudgetRepository
import com.example.data.CategoryEntity
import com.example.data.ExpenseEntity
import com.example.data.SubscriptionEntity
import com.example.data.SettingEntity
import com.example.data.EventEntity
import com.example.service.DetectedTransaction
import com.example.service.PaymentNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BudgetUiState(
    val resetDay: Int = 27,
    val monthlyCap: Double = 700.0, // Cassetto 1 (Personale)
    val budgetPersonale: Double = 700.0,
    val budgetGinevra: Double = 180.0, // Cassetto 2 (Fondo Imprevisti / Ginevra)
    val ginevraRollover: Double = 0.0,
    val ginevraTotalAvailable: Double = 180.0,
    val ginevraSpent: Double = 0.0,
    val ginevraRemaining: Double = 180.0,
    val personaleSpent: Double = 0.0,
    val personaleRemaining: Double = 700.0,
    val totalMonthlySpendable: Double = 880.0, // Hero number in Home
    val daysRemainingInCycle: Int = 0,
    val liquidity: Double = 0.0,
    val investments: Double = 0.0,
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val recurringTotal: Double = 0.0,
    val variableBudgetAvailable: Double = 0.0,
    val currentCycleStartMillis: Long = 0L,
    val currentCycleEndMillis: Long = 0L,
    val currentCycleLabel: String = "",
    val discretionaryExpensesInCycle: List<ExpenseEntity> = emptyList(),
    val discretionaryVariableSpent: Double = 0.0,
    val discretionaryVariableRemaining: Double = 0.0,
    val discretionaryTotalRemaining: Double = 0.0,
    val isLowBalanceAlert: Boolean = false,
    val eventFundDeposits: List<ExpenseEntity> = emptyList(),
    val eventFundWithdrawals: List<ExpenseEntity> = emptyList(),
    val eventFundTotalDeposits: Double = 0.0,
    val eventFundTotalWithdrawals: Double = 0.0,
    val eventFundBalance: Double = 0.0,
    val allExpenses: List<ExpenseEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val showMonthlySummary: Boolean = false,
    val summaryPrevCycleLabel: String = "",
    val summaryPrevSpent: Double = 0.0,
    val summaryPrevAvailable: Double = 0.0,
    val summaryPrevDiff: Double = 0.0,
    val summaryPrevPrevSpent: Double = 0.0,
    val isLoading: Boolean = true,
    val geminiApiKey: String = "",
    val essentialBaseline: Double = 1500.0,
    val monthlyInvestmentTarget: Double = 327.0,
    val essentialExpensesInCycle: List<ExpenseEntity> = emptyList(),
    val essentialTotalSpent: Double = 0.0,
    val isInvestmentDone: Boolean = false,
    val avatarUri: String = "",
    val netMonthlyIncome: Double = 2500.0,
    val currentCycleId: String = "",
    // Grief Spending & Exceptional Expenses
    val griefSpentInCycle: Double = 0.0,
    val griefExpensesInCycle: List<ExpenseEntity> = emptyList(),
    val totalGriefSpent: Double = 0.0,
    val allGriefExpenses: List<ExpenseEntity> = emptyList(),
    val exceptionalExpensesInCycle: List<ExpenseEntity> = emptyList(),
    val exceptionalTotalSpentInCycle: Double = 0.0,
    val allowedNotificationApps: String = "PayPal, Google, Apple, Banca, Sella, Intesa, Revolut, N26, Hype, Scalapay",
    // Google Drive Backup & Cloud Recovery
    val googleDriveAccount: String = "",
    val googleDriveLastBackupTime: Long = 0L,
    val isGoogleDriveAutoBackupEnabled: Boolean = true,
    val backupStatusMessage: String? = null,
    val isBackupInProgress: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val driveAuthIntent: android.content.Intent? = null
)

class BudgetViewModel(private val repository: BudgetRepository) : ViewModel() {

    private val referenceTimeState = MutableStateFlow(System.currentTimeMillis())
    private val isBackupInProgressState = MutableStateFlow(false)
    private val isRestoreInProgressState = MutableStateFlow(false)
    private val backupStatusMessageState = MutableStateFlow<String?>(null)
    private val driveAuthIntentState = MutableStateFlow<Intent?>(null)

    val pendingTransaction: StateFlow<DetectedTransaction?> = PaymentNotificationManager.pendingTransaction
    val notificationLogs = repository.allNotificationLogs

    fun clearNotificationLogs() {
        viewModelScope.launch {
            repository.clearNotificationLogs()
        }
    }

    fun clearDriveAuthIntent() {
        driveAuthIntentState.value = null
    }

    fun contestLog(log: com.example.data.NotificationLogEntity) {
        val amountRegex = Regex("([0-9]+[.,][0-9]{2})")
        val match = amountRegex.find(log.content) ?: amountRegex.find(log.title)
        val amountString = match?.value?.replace(',', '.')
        val amount = amountString?.toDoubleOrNull() ?: 0.0
        
        val transaction = DetectedTransaction(
            appName = log.appName,
            rawTitle = log.title,
            rawText = log.content,
            merchant = log.appName,
            originalAmount = amount,
            currencySymbol = "€",
            currencyCode = "EUR",
            estimatedEurAmount = amount,
            timestampMillis = log.timestamp
        )
        PaymentNotificationManager.setPendingTransaction(transaction)
    }

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    val uiState: StateFlow<BudgetUiState> = combine(
        repository.allSettings,
        repository.allSubscriptions,
        repository.allExpenses,
        repository.allCategories,
        repository.allEvents,
        referenceTimeState,
        isBackupInProgressState,
        isRestoreInProgressState,
        backupStatusMessageState,
        driveAuthIntentState
    ) { array ->
        val settingsList = array[0] as List<SettingEntity>
        val subsList = array[1] as List<SubscriptionEntity>
        val expensesList = array[2] as List<ExpenseEntity>
        val catsList = array[3] as List<CategoryEntity>
        val eventsList = array[4] as List<EventEntity>
        val refTime = array[5] as Long
        val isBackupInProgress = array[6] as Boolean
        val isRestoreInProgress = array[7] as Boolean
        val backupStatusMsg = array[8] as String?
        val driveAuthIntent = array[9] as Intent?

        val resetDaySetting = settingsList.find { it.key == "reset_day" }?.value?.toIntOrNull() ?: 27
        val budgetPersonale = settingsList.find { it.key == "budget_personale" }?.value?.toDoubleOrNull()
            ?: settingsList.find { it.key == "monthly_cap" }?.value?.toDoubleOrNull()
            ?: 700.0
        val budgetGinevra = settingsList.find { it.key == "budget_ginevra" }?.value?.toDoubleOrNull() ?: 180.0
        val monthlyCapSetting = budgetPersonale
        val liquiditySetting = settingsList.find { it.key == "liquidity" }?.value?.toDoubleOrNull() ?: 0.0
        val investmentsSetting = settingsList.find { it.key == "investments" }?.value?.toDoubleOrNull() ?: 0.0
        val geminiApiSetting = settingsList.find { it.key == "gemini_api_key" }?.value ?: ""

        val googleDriveAccount = settingsList.find { it.key == "google_drive_account" }?.value ?: ""
        val googleDriveLastBackupTime = settingsList.find { it.key == "google_drive_last_backup_time" }?.value?.toLongOrNull() ?: 0L
        val isGoogleDriveAutoBackupEnabled = settingsList.find { it.key == "google_drive_auto_backup_enabled" }?.value?.toBooleanStrictOrNull() ?: true

        val recurringTotal = subsList.filter { it.isActive }.sumOf { it.amount }
        val variableBudgetAvailable = (budgetPersonale - recurringTotal).coerceAtLeast(0.0)

        val (cycleStart, cycleEnd) = BillingCycleUtils.getCycleRange(refTime, resetDaySetting)
        val cycleLabel = BillingCycleUtils.getCycleLabel(cycleStart, cycleEnd)
        val daysRemainingInCycle = (((cycleEnd - refTime).coerceAtLeast(0L)) / (24 * 60 * 60 * 1000L)).toInt() + 1

        val currentCycleId = cycleStart.toString()
        val netIncome = settingsList.find { it.key == "net_monthly_income" }?.value?.toDoubleOrNull() ?: 2500.0
        val essentialBaseline = settingsList.find { it.key == "essential_baseline" }?.value?.toDoubleOrNull() ?: 1500.0
        val monthlyInvestmentTarget = settingsList.find { it.key == "monthly_investment_target" }?.value?.toDoubleOrNull() ?: 327.0
        val investmentKey = "investment_done_$currentCycleId"
        val isInvestmentDone = settingsList.find { it.key == investmentKey }?.value?.toBoolean() ?: false
        val avatarUri = settingsList.find { it.key == "avatar_uri" }?.value ?: "" 
        val allowedNotificationApps = settingsList.find { it.key == "allowed_notification_apps" }?.value ?: "PayPal, Google, Apple, Banca, Sella, Intesa, Revolut, N26, Hype, Scalapay"
        
        // Essential expenses in cycle (only regular stats)
        val essentialExpensesInCycle = expensesList.filter { expense ->
            expense.accountType == "ESSENZIALE_REALE" && expense.dateMillis in cycleStart..cycleEnd
        }
        val essentialTotalSpent = essentialExpensesInCycle.filter { !it.excludeFromStats }.sumOf { kotlin.math.abs(it.amount) }

        // --- Cassetto 1 (Personale / Svago) ---
        // Il cassetto personale ha un cap mensile fisso (es. 700€), da cui vengono detratti i costi ricorrenti/abbonamenti
        // e le spese personali non eccezionali del ciclo corrente. Si resetta all'inizio di ogni ciclo.
        val personaleExpensesInCycle = expensesList.filter { expense ->
            (expense.accountType == "SERBATOIO_PERSONALE" || expense.accountType == "DISCREZIONALE_VARIABILE") &&
                    expense.dateMillis in cycleStart..cycleEnd
        }
        val personaleSpent = personaleExpensesInCycle.filter { !it.excludeFromStats }.sumOf { kotlin.math.abs(it.amount) }
        val personaleRemaining = budgetPersonale - recurringTotal - personaleSpent

        // --- Cassetto 2 (Fondo Imprevisti / Ginevra con Rollover Dinamico) ---
        // Calcolo del Rollover: si analizzano tutti i cicli di fatturazione pregressi a ritroso a partire dal più vecchio movimento.
        // In ciascun ciclo pregresso, il surplus (budgetGinevra - spese) o deficit viene sommato al rollover cumulativo.
        val initialGinevraRollover = settingsList.find { it.key == "ginevra_initial_rollover" }?.value?.toDoubleOrNull() ?: 0.0
        val ginevraExpenses = expensesList.filter { it.accountType == "SERBATOIO_GINEVRA" }
        var historicalRollover = 0.0

        if (ginevraExpenses.isNotEmpty()) {
            val earliestTime = ginevraExpenses.minOf { it.dateMillis }
            var testRef = cycleStart - 1000L
            val visitedCycles = mutableSetOf<Pair<Long, Long>>()
            
            while (testRef >= earliestTime) {
                val (pStart, pEnd) = BillingCycleUtils.getCycleRange(testRef, resetDaySetting)
                if (visitedCycles.add(Pair(pStart, pEnd))) {
                    val pSpent = expensesList.filter {
                        it.accountType == "SERBATOIO_GINEVRA" && !it.excludeFromStats && it.dateMillis in pStart..pEnd
                    }.sumOf { kotlin.math.abs(it.amount) }
                    
                    val cycleSurplus = budgetGinevra - pSpent
                    historicalRollover += cycleSurplus
                }
                testRef = pStart - 1000L
            }
        }
        
        val ginevraRollover = initialGinevraRollover + historicalRollover
        val ginevraTotalAvailable = budgetGinevra + ginevraRollover
        val ginevraExpensesInCycle = expensesList.filter { expense ->
            expense.accountType == "SERBATOIO_GINEVRA" && expense.dateMillis in cycleStart..cycleEnd
        }
        val ginevraSpent = ginevraExpensesInCycle.filter { !it.excludeFromStats }.sumOf { kotlin.math.abs(it.amount) }
        val ginevraRemaining = ginevraTotalAvailable - ginevraSpent

        // --- Hero Number: Saldo Dinamico del 'Budget Mensile Spendibile' ---
        // Somma dinamica e reattiva in tempo reale dei due cassetti (Personale + Fondo Imprevisti/Ginevra)
        val totalMonthlySpendable = (personaleRemaining + ginevraRemaining)

        // Compatibility fields
        val discExpensesInCycle = (personaleExpensesInCycle + ginevraExpensesInCycle).distinctBy { it.id }
        val discVariableSpent = personaleSpent + ginevraSpent
        val discVariableRemaining = (variableBudgetAvailable + ginevraTotalAvailable) - discVariableSpent
        val discTotalRemaining = totalMonthlySpendable

        // Warning trigger
        val isLowAlert = totalMonthlySpendable <= 0.20 * (variableBudgetAvailable + ginevraTotalAvailable)

        // --- Grief Spending (Acquisti d'Impulso / Non Necessari) ---
        val griefExpensesInCycle = discExpensesInCycle.filter { !it.isNecessary && !it.excludeFromStats }
        val griefSpentInCycle = griefExpensesInCycle.sumOf { kotlin.math.abs(it.amount) }
        val allGriefExpenses = expensesList.filter { !it.isNecessary && !it.excludeFromStats }
        val totalGriefSpent = allGriefExpenses.sumOf { kotlin.math.abs(it.amount) }

        // --- Spese Eccezionali (Escluse dalle Statistiche) ---
        val exceptionalExpensesInCycle = expensesList.filter { it.excludeFromStats && it.dateMillis in cycleStart..cycleEnd }
        val exceptionalTotalSpentInCycle = exceptionalExpensesInCycle.sumOf { kotlin.math.abs(it.amount) }

        // Event Fund movements
        val eventDeposits = expensesList.filter { it.accountType == "FONDO_EVENTI_DEPOSIT" }
        val totalEventDeposits = eventDeposits.sumOf { it.amount }
        val totalEventAllocated = eventsList.sumOf { it.estimatedBudget }
        
        val eventWithdrawals = expensesList.filter { it.eventId != null }
        val totalEventWithdrawals = eventWithdrawals.sumOf { it.amount }
        
        val eventBalance = totalEventDeposits - totalEventAllocated

        // Monthly Summary Logic
        val lastSummaryShownCycle = settingsList.find { it.key == "last_summary_shown_cycle" }?.value ?: ""
        val shouldShowSummary = if (lastSummaryShownCycle.isEmpty()) {
            expensesList.isNotEmpty()
        } else {
            lastSummaryShownCycle != currentCycleId
        }

        // Previous cycle
        val prevRefTime = cycleStart - 1000L
        val (prevCycleStart, prevCycleEnd) = BillingCycleUtils.getCycleRange(prevRefTime, resetDaySetting)
        val prevCycleLabel = BillingCycleUtils.getCycleLabel(prevCycleStart, prevCycleEnd)
        val prevExpenses = expensesList.filter { 
            (it.accountType == "SERBATOIO_PERSONALE" || it.accountType == "DISCREZIONALE_VARIABILE" || it.accountType == "SERBATOIO_GINEVRA") &&
            !it.excludeFromStats && it.dateMillis in prevCycleStart..prevCycleEnd 
        }
        val prevSpent = prevExpenses.sumOf { kotlin.math.abs(it.amount) }
        val prevDiff = (budgetPersonale + budgetGinevra) - prevSpent

        // Previous-previous cycle
        val prevPrevRefTime = prevCycleStart - 1000L
        val (prevPrevCycleStart, prevPrevCycleEnd) = BillingCycleUtils.getCycleRange(prevPrevRefTime, resetDaySetting)
        val prevPrevExpenses = expensesList.filter { 
            (it.accountType == "SERBATOIO_PERSONALE" || it.accountType == "DISCREZIONALE_VARIABILE" || it.accountType == "SERBATOIO_GINEVRA") &&
            !it.excludeFromStats && it.dateMillis in prevPrevCycleStart..prevPrevCycleEnd 
        }
        val prevPrevSpent = prevPrevExpenses.sumOf { kotlin.math.abs(it.amount) }

        BudgetUiState(
            resetDay = resetDaySetting,
            monthlyCap = monthlyCapSetting,
            budgetPersonale = budgetPersonale,
            budgetGinevra = budgetGinevra,
            ginevraRollover = ginevraRollover,
            ginevraTotalAvailable = ginevraTotalAvailable,
            ginevraSpent = ginevraSpent,
            ginevraRemaining = ginevraRemaining,
            personaleSpent = personaleSpent,
            personaleRemaining = personaleRemaining,
            totalMonthlySpendable = totalMonthlySpendable,
            daysRemainingInCycle = daysRemainingInCycle,
            liquidity = liquiditySetting,
            investments = investmentsSetting,
            subscriptions = subsList,
            recurringTotal = recurringTotal,
            variableBudgetAvailable = variableBudgetAvailable,
            currentCycleStartMillis = cycleStart,
            currentCycleEndMillis = cycleEnd,
            currentCycleLabel = cycleLabel,
            discretionaryExpensesInCycle = discExpensesInCycle,
            discretionaryVariableSpent = discVariableSpent,
            discretionaryVariableRemaining = discVariableRemaining,
            discretionaryTotalRemaining = discTotalRemaining,
            isLowBalanceAlert = isLowAlert,
            eventFundDeposits = eventDeposits,
            eventFundWithdrawals = eventWithdrawals,
            eventFundTotalDeposits = totalEventDeposits,
            eventFundTotalWithdrawals = totalEventWithdrawals,
            eventFundBalance = eventBalance,
            allExpenses = expensesList,
            categories = catsList,
            events = eventsList,
            showMonthlySummary = shouldShowSummary,
            summaryPrevCycleLabel = prevCycleLabel,
            summaryPrevSpent = prevSpent,
            summaryPrevAvailable = variableBudgetAvailable,
            summaryPrevDiff = prevDiff,
            summaryPrevPrevSpent = prevPrevSpent,
            isLoading = false,
            geminiApiKey = geminiApiSetting,
            essentialBaseline = essentialBaseline,
            monthlyInvestmentTarget = monthlyInvestmentTarget,
            essentialExpensesInCycle = essentialExpensesInCycle.sortedByDescending { it.dateMillis },
            essentialTotalSpent = essentialTotalSpent,
            isInvestmentDone = isInvestmentDone,
            avatarUri = avatarUri,
            netMonthlyIncome = netIncome,
            currentCycleId = currentCycleId,
            griefSpentInCycle = griefSpentInCycle,
            griefExpensesInCycle = griefExpensesInCycle,
            totalGriefSpent = totalGriefSpent,
            allGriefExpenses = allGriefExpenses,
            exceptionalExpensesInCycle = exceptionalExpensesInCycle,
            exceptionalTotalSpentInCycle = exceptionalTotalSpentInCycle,
            allowedNotificationApps = allowedNotificationApps,
            googleDriveAccount = googleDriveAccount,
            googleDriveLastBackupTime = googleDriveLastBackupTime,
            isGoogleDriveAutoBackupEnabled = isGoogleDriveAutoBackupEnabled,
            backupStatusMessage = backupStatusMsg,
            isBackupInProgress = isBackupInProgress,
            isRestoreInProgress = isRestoreInProgress,
            driveAuthIntent = driveAuthIntent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState()
    )

    fun dismissMonthlySummary(cycleStartMillis: Long) {
        viewModelScope.launch {
            repository.saveSetting("last_summary_shown_cycle", cycleStartMillis.toString())
        }
    }


    fun toggleInvestmentDone(cycleId: String, isDone: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("investment_done_$cycleId", isDone.toString())
        }
    }

    fun addExpense(
        accountType: String,
        amount: Double,
        category: String,
        dateMillis: Long = System.currentTimeMillis(),
        note: String = "",
        merchant: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        eventTargetDateMillis: Long? = null,
        eventId: Long? = null,
        excludeFromStats: Boolean = false,
        isNecessary: Boolean = true
    ) {
        viewModelScope.launch {
            repository.insertExpense(
                ExpenseEntity(
                    accountType = accountType,
                    amount = amount,
                    category = category,
                    dateMillis = dateMillis,
                    note = note,
                    merchant = merchant,
                    latitude = latitude,
                    longitude = longitude,
                    eventTargetDateMillis = eventTargetDateMillis,
                    eventId = eventId,
                    excludeFromStats = excludeFromStats,
                    isNecessary = isNecessary
                )
            )
        }
    }

    fun addEvent(name: String, estimatedBudget: Double, targetDateMillis: Long? = null) {
        viewModelScope.launch {
            repository.insertEvent(EventEntity(name = name, estimatedBudget = estimatedBudget, targetDateMillis = targetDateMillis))
        }
    }

    fun updateEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.updateEvent(event)
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    fun addAmortizedExpense(
        accountType: String,
        totalAmountEur: Double,
        category: String,
        note: String,
        merchant: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        amortizationMonths: Int,
        excludeFromStats: Boolean = false,
        isNecessary: Boolean = true,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            if (amortizationMonths <= 1) {
                addExpense(
                    accountType = accountType,
                    amount = totalAmountEur,
                    category = category,
                    note = note,
                    merchant = merchant,
                    latitude = latitude,
                    longitude = longitude,
                    excludeFromStats = excludeFromStats,
                    isNecessary = isNecessary,
                    dateMillis = dateMillis
                )
            } else {
                val monthlyQuota = totalAmountEur / amortizationMonths
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = dateMillis
                for (i in 0 until amortizationMonths) {
                    val dateForInstallment = calendar.timeInMillis
                    val installmentNote = if (note.isBlank()) {
                        "Rata ${i + 1}/$amortizationMonths (Ammortamento ${String.format(Locale.ITALY, "%.2f €", totalAmountEur)})"
                    } else {
                        "$note (Rata ${i + 1}/$amortizationMonths)"
                    }
                    repository.insertExpense(
                        ExpenseEntity(
                            accountType = accountType,
                            amount = monthlyQuota,
                            category = category,
                            dateMillis = dateForInstallment,
                            note = installmentNote,
                            merchant = merchant,
                            latitude = latitude,
                            longitude = longitude,
                            excludeFromStats = excludeFromStats,
                            isNecessary = isNecessary
                        )
                    )
                    calendar.add(Calendar.MONTH, 1)
                }
            }
            clearPendingTransaction()
        }
    }

    fun clearPendingTransaction() {
        PaymentNotificationManager.clearPendingTransaction()
    }

    fun simulateNotificationDetection(
        merchant: String = "Amazon Store",
        originalAmount: Double = 120.0,
        currencyCode: String = "USD",
        appName: String = "PayPal"
    ) {
        val rate = when (currencyCode.uppercase()) {
            "USD" -> 0.92
            "GBP" -> 1.17
            "CHF" -> 1.05
            "JPY" -> 0.0062
            "SEK" -> 0.088
            else -> 1.0
        }
        val symbol = when (currencyCode.uppercase()) {
            "USD" -> "$"
            "GBP" -> "£"
            "CHF" -> "CHF"
            "JPY" -> "¥"
            "SEK" -> "kr"
            else -> "€"
        }
        val estimatedEur = originalAmount * rate
        val transaction = DetectedTransaction(
            appName = appName,
            rawTitle = "Pagamento con $appName",
            rawText = "Addebito di $symbol$originalAmount presso $merchant",
            merchant = merchant,
            originalAmount = originalAmount,
            currencySymbol = symbol,
            currencyCode = currencyCode,
            estimatedEurAmount = estimatedEur
        )
        PaymentNotificationManager.setPendingTransaction(transaction)
    }

    fun updateExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun addSubscription(name: String, amount: Double, dayOfMonth: Int) {
        viewModelScope.launch {
            repository.insertSubscription(
                SubscriptionEntity(name = name, amount = amount, dayOfMonth = dayOfMonth, isActive = true)
            )
        }
    }

    fun updateSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            repository.updateSubscription(subscription)
        }
    }

    fun toggleSubscriptionActive(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            repository.updateSubscription(subscription.copy(isActive = !subscription.isActive))
        }
    }

    fun deleteSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            repository.deleteSubscription(subscription)
        }
    }

    fun updateSettings(
        resetDay: Int,
        monthlyCap: Double,
        liquidity: Double,
        investments: Double,
        geminiApiKey: String,
        netMonthlyIncome: Double,
        essentialBaseline: Double,
        monthlyInvestmentTarget: Double,
        budgetPersonale: Double = monthlyCap,
        budgetGinevra: Double = 180.0,
        allowedNotificationApps: String = "PayPal, Google, Apple, Banca, Sella, Intesa, Revolut, N26, Hype, Scalapay"
    ) {
        viewModelScope.launch {
            repository.saveSetting("reset_day", resetDay.toString())
            repository.saveSetting("monthly_cap", monthlyCap.toString())
            repository.saveSetting("budget_personale", budgetPersonale.toString())
            repository.saveSetting("budget_ginevra", budgetGinevra.toString())
            repository.saveSetting("liquidity", liquidity.toString())
            repository.saveSetting("investments", investments.toString())
            repository.saveSetting("gemini_api_key", geminiApiKey)
            repository.saveSetting("net_monthly_income", netMonthlyIncome.toString())
            repository.saveSetting("essential_baseline", essentialBaseline.toString())
            repository.saveSetting("monthly_investment_target", monthlyInvestmentTarget.toString())
            repository.saveSetting("allowed_notification_apps", allowedNotificationApps)
        }
    }

    fun addCategory(name: String, targetAccount: String = "BOTH") {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertCategory(CategoryEntity(name = name.trim(), targetAccount = targetAccount))
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun performExplicitReallocation(
        fromAccount: String,
        toAccount: String,
        amount: Double,
        reason: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // Record withdrawal from source account if Event Fund
            if (fromAccount == "FONDO_EVENTI") {
                repository.insertExpense(
                    ExpenseEntity(
                        accountType = "FONDO_EVENTI_WITHDRAWAL",
                        amount = amount,
                        category = "Riallocazione Straordinaria",
                        dateMillis = now,
                        note = "Spostamento verso $toAccount: $reason"
                    )
                )
            }
            // Record deposit into destination account if Event Fund or Discretionary
            if (toAccount == "FONDO_EVENTI") {
                repository.insertExpense(
                    ExpenseEntity(
                        accountType = "FONDO_EVENTI_DEPOSIT",
                        amount = amount,
                        category = "Riallocazione Straordinaria",
                        dateMillis = now,
                        note = "Ricevuto da $fromAccount: $reason"
                    )
                )
            } else if (toAccount == "DISCREZIONALE") {
                // If added as discretionary adjustment, we record as negative variable expense (credit)
                repository.insertExpense(
                    ExpenseEntity(
                        accountType = "DISCREZIONALE_VARIABILE",
                        amount = -amount, // Negative expense equals income adjustment
                        category = "Accredito Straordinario",
                        dateMillis = now,
                        note = "Riallocazione da $fromAccount: $reason"
                    )
                )
            }
        }
    }

    fun exportCsvData(context: Context) {
        val currentState = uiState.value
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)

        val csvBuilder = StringBuilder()
        csvBuilder.append("Data,Importo,Categoria,Conto,Nota\n")

        currentState.allExpenses.forEach { exp ->
            val dateStr = dateFormat.format(Date(exp.dateMillis))
            val cleanNote = exp.note.replace(",", " ").replace("\n", " ").trim()
            val cleanCat = exp.category.replace(",", " ").trim()
            val cleanAccount = exp.accountType.replace(",", " ")
            csvBuilder.append("$dateStr,${exp.amount},$cleanCat,$cleanAccount,$cleanNote\n")
        }

        try {
            val file = File(context.cacheDir, "spese.csv")
            val writer = FileWriter(file)
            writer.write(csvBuilder.toString())
            writer.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Esportazione Spese CSV")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // In some context, FLAG_ACTIVITY_NEW_TASK might be needed, but since it's an intent chooser 
                // we'll add it to the chooser
            }
            
            val chooser = Intent.createChooser(sendIntent, "Esporta CSV")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateAvatarUri(uri: String) {
        viewModelScope.launch {
            repository.saveSetting("avatar_uri", uri)
        }
    }

    // --- GOOGLE DRIVE BACKUP & RESTORE METHODS ---

    fun checkAndPerformWeeklyAutoBackup(context: Context) {
        viewModelScope.launch {
            val settings = repository.allSettingsList()
            val autoBackupEnabled = settings.find { it.key == "google_drive_auto_backup_enabled" }?.value?.toBooleanStrictOrNull() ?: true
            val lastBackupTime = settings.find { it.key == "google_drive_last_backup_time" }?.value?.toLongOrNull() ?: 0L
            val account = com.example.service.GoogleDriveBackupService.getSignedInAccount(context)

            if (autoBackupEnabled && account != null) {
                val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L
                val now = System.currentTimeMillis()
                if (now - lastBackupTime >= sevenDaysMillis) {
                    performGoogleDriveBackup(context, isAutomated = true)
                }
            }
        }
    }

    fun performGoogleDriveBackup(context: Context, isAutomated: Boolean = false) {
        viewModelScope.launch {
            isBackupInProgressState.value = true
            backupStatusMessageState.value = if (isAutomated) "Backup automatico settimanale in corso..." else "Salvataggio backup su Google Drive..."
            try {
                val backupData = repository.getAllDataForBackup()
                val result = com.example.service.GoogleDriveBackupService.uploadBackup(context, backupData)
                isBackupInProgressState.value = false
                when (result) {
                    is com.example.service.DriveBackupResult.Success -> {
                        repository.saveSetting("google_drive_last_backup_time", result.timestampMillis.toString())
                        com.example.service.GoogleDriveBackupService.getSignedInAccount(context)?.email?.let { email ->
                            repository.saveSetting("google_drive_account", email)
                        }
                        backupStatusMessageState.value = result.message
                    }
                    is com.example.service.DriveBackupResult.Error -> {
                        backupStatusMessageState.value = result.message
                        if (result.recoveryIntent != null) {
                            driveAuthIntentState.value = result.recoveryIntent
                        }
                    }
                }
            } catch (e: Exception) {
                isBackupInProgressState.value = false
                backupStatusMessageState.value = "Errore durante il backup: ${e.localizedMessage}"
            }
        }
    }

    fun restoreFromGoogleDrive(context: Context, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            isRestoreInProgressState.value = true
            backupStatusMessageState.value = "Download del backup da Google Drive in corso..."
            try {
                val result = com.example.service.GoogleDriveBackupService.downloadBackup(context)
                isRestoreInProgressState.value = false
                when (result) {
                    is com.example.service.DriveRestoreResult.Success -> {
                        repository.restoreDataFromBackup(result.backupData, clearExisting = true)
                        backupStatusMessageState.value = "Dati ripristinati con successo da Google Drive!"
                        onComplete(true, result.infoMessage)
                    }
                    is com.example.service.DriveRestoreResult.Error -> {
                        backupStatusMessageState.value = result.message
                        if (result.recoveryIntent != null) {
                            driveAuthIntentState.value = result.recoveryIntent
                        }
                        onComplete(false, result.message)
                    }
                }
            } catch (e: Exception) {
                isRestoreInProgressState.value = false
                backupStatusMessageState.value = "Errore durante il ripristino: ${e.localizedMessage}"
                onComplete(false, e.localizedMessage ?: "Errore sconosciuto")
            }
        }
    }

    fun setGoogleDriveAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("google_drive_auto_backup_enabled", enabled.toString())
        }
    }

    fun setGoogleDriveAccount(email: String) {
        viewModelScope.launch {
            repository.saveSetting("google_drive_account", email)
        }
    }

    fun disconnectGoogleDrive(context: Context) {
        viewModelScope.launch {
            try {
                val client = com.example.service.GoogleDriveBackupService.getGoogleSignInClient(context)
                client.signOut()
            } catch (_: Exception) {}
            repository.saveSetting("google_drive_account", "")
            backupStatusMessageState.value = "Account Google disconnesso."
        }
    }

    fun clearBackupStatusMessage() {
        backupStatusMessageState.value = null
    }

    fun shareBackupJson(context: Context) {
        viewModelScope.launch {
            try {
                val backupData = repository.getAllDataForBackup()
                val json = backupData.toJson()
                val file = File(context.cacheDir, "budget_control_backup.json")
                val writer = FileWriter(file)
                writer.write(json)
                writer.flush()
                writer.close()

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "Backup Budget Control (JSON)")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(sendIntent, "Esporta Backup JSON")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                backupStatusMessageState.value = "Errore esportazione JSON: ${e.localizedMessage}"
            }
        }
    }

    fun importBackupFromJsonString(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val backupData = com.example.data.backup.BudgetBackupData.fromJson(jsonString)
                repository.restoreDataFromBackup(backupData, clearExisting = true)
                backupStatusMessageState.value = "Backup ripristinato con successo!"
                onResult(true, "Ripristinate ${backupData.expenses.size} spese, ${backupData.subscriptions.size} abbonamenti e ${backupData.categories.size} categorie.")
            } catch (e: Exception) {
                backupStatusMessageState.value = "Errore importazione JSON: ${e.localizedMessage}"
                onResult(false, "File JSON non valido: ${e.localizedMessage}")
            }
        }
    }
}

class BudgetViewModelFactory(private val repository: BudgetRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}