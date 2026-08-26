package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BudgetRepository(private val dao: BudgetDao) {

    val allExpenses: Flow<List<ExpenseEntity>> = dao.getAllExpenses()
    val allSubscriptions: Flow<List<SubscriptionEntity>> = dao.getAllSubscriptions()
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    val allSettings: Flow<List<SettingEntity>> = dao.getAllSettings()
    val allEvents: Flow<List<EventEntity>> = dao.getAllEvents()
    val allNotificationLogs: Flow<List<NotificationLogEntity>> = dao.getAllNotificationLogs()

    suspend fun seedInitialDataIfEmpty() {
        // Check subscriptions
        val existingSubs = dao.getAllSubscriptions().first()
        if (existingSubs.isEmpty()) {
            val defaultSubscriptions = listOf(
                SubscriptionEntity(name = "Sky", amount = 38.0, dayOfMonth = 1, isActive = true),
                SubscriptionEntity(name = "HBO", amount = 6.0, dayOfMonth = 5, isActive = true),
                SubscriptionEntity(name = "Spotify", amount = 17.0, dayOfMonth = 10, isActive = true),
                SubscriptionEntity(name = "Netflix", amount = 14.0, dayOfMonth = 15, isActive = true),
                SubscriptionEntity(name = "Claude", amount = 22.0, dayOfMonth = 18, isActive = true),
                SubscriptionEntity(name = "Google", amount = 10.0, dayOfMonth = 20, isActive = true),
                SubscriptionEntity(name = "Amazon Prime", amount = 4.17, dayOfMonth = 25, isActive = true)
            )
            defaultSubscriptions.forEach { dao.insertSubscription(it) }
        }

        // Check categories
        val existingCats = dao.getAllCategories().first()
        if (existingCats.isEmpty()) {
            val defaultCategories = listOf(
                CategoryEntity(name = "Shopping", targetAccount = "DISCREZIONALE"),
                CategoryEntity(name = "Cene & Ristoranti", targetAccount = "DISCREZIONALE"),
                CategoryEntity(name = "Svago & Tempo Libero", targetAccount = "DISCREZIONALE"),
                CategoryEntity(name = "Spesa & Alimentari", targetAccount = "DISCREZIONALE"),
                CategoryEntity(name = "Trasporti & Auto", targetAccount = "DISCREZIONALE"),
                CategoryEntity(name = "Casa & Utenze", targetAccount = "DISCREZIONALE"),
                CategoryEntity(name = "Salute & Benessere", targetAccount = "DISCREZIONALE"),
                CategoryEntity(name = "Tredicesima", targetAccount = "FONDO_EVENTI"),
                CategoryEntity(name = "Rimborso 730", targetAccount = "FONDO_EVENTI"),
                CategoryEntity(name = "Welfare Aziendale", targetAccount = "FONDO_EVENTI"),
                CategoryEntity(name = "Matrimoni & Feste", targetAccount = "FONDO_EVENTI"),
                CategoryEntity(name = "Viaggi & Vacanze", targetAccount = "FONDO_EVENTI"),
                CategoryEntity(name = "Regali Importanti", targetAccount = "FONDO_EVENTI"),
                CategoryEntity(name = "Altro", targetAccount = "BOTH")
            )
            defaultCategories.forEach { dao.insertCategory(it) }
        }

        // Check settings
        if (dao.getSettingByKey("reset_day") == null) {
            dao.insertSetting(SettingEntity("reset_day", "27"))
        }
        if (dao.getSettingByKey("monthly_cap") == null) {
            dao.insertSetting(SettingEntity("monthly_cap", "700.0"))
        }
        if (dao.getSettingByKey("budget_personale") == null) {
            dao.insertSetting(SettingEntity("budget_personale", "700.0"))
        }
        if (dao.getSettingByKey("budget_ginevra") == null) {
            dao.insertSetting(SettingEntity("budget_ginevra", "180.0"))
        }
        if (dao.getSettingByKey("safety_capital") == null) {
            dao.insertSetting(SettingEntity("safety_capital", "113000.0"))
        }
    }

    suspend fun insertExpense(expense: ExpenseEntity): Long = dao.insertExpense(expense)
    suspend fun updateExpense(expense: ExpenseEntity) = dao.updateExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = dao.deleteExpense(expense)
    suspend fun deleteExpenseById(id: Long) = dao.deleteExpenseById(id)

    suspend fun insertSubscription(subscription: SubscriptionEntity): Long = dao.insertSubscription(subscription)
    suspend fun updateSubscription(subscription: SubscriptionEntity) = dao.updateSubscription(subscription)
    suspend fun deleteSubscription(subscription: SubscriptionEntity) = dao.deleteSubscription(subscription)

    suspend fun insertCategory(category: CategoryEntity): Long = dao.insertCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = dao.deleteCategory(category)

    suspend fun saveSetting(key: String, value: String) {
        dao.insertSetting(SettingEntity(key, value))
    }

    suspend fun allSettingsList(): List<SettingEntity> = dao.getAllSettingsList()

    suspend fun insertEvent(event: EventEntity): Long = dao.insertEvent(event)
    suspend fun updateEvent(event: EventEntity) = dao.updateEvent(event)
    suspend fun deleteEvent(event: EventEntity) = dao.deleteEvent(event)
    suspend fun clearNotificationLogs() = dao.clearAllNotificationLogs()

    suspend fun getAllDataForBackup(): com.example.data.backup.BudgetBackupData {
        return com.example.data.backup.BudgetBackupData(
            version = 1,
            exportedAtMillis = System.currentTimeMillis(),
            expenses = dao.getAllExpensesList(),
            subscriptions = dao.getAllSubscriptionsList(),
            categories = dao.getAllCategoriesList(),
            settings = dao.getAllSettingsList(),
            events = dao.getAllEventsList(),
            notificationLogs = dao.getAllNotificationLogsList()
        )
    }

    suspend fun restoreDataFromBackup(backupData: com.example.data.backup.BudgetBackupData, clearExisting: Boolean = true) {
        if (clearExisting) {
            dao.clearAllExpenses()
            dao.clearAllSubscriptions()
            dao.clearAllCategories()
            dao.clearAllSettings()
            dao.clearAllEvents()
            dao.clearAllNotificationLogs()
        }

        if (backupData.expenses.isNotEmpty()) {
            dao.insertExpenses(backupData.expenses)
        }
        if (backupData.subscriptions.isNotEmpty()) {
            dao.insertSubscriptions(backupData.subscriptions)
        }
        if (backupData.categories.isNotEmpty()) {
            dao.insertCategories(backupData.categories)
        }
        if (backupData.settings.isNotEmpty()) {
            dao.insertSettings(backupData.settings)
        }
        if (backupData.events.isNotEmpty()) {
            dao.insertEvents(backupData.events)
        }
        if (backupData.notificationLogs.isNotEmpty()) {
            dao.insertNotificationLogs(backupData.notificationLogs)
        }
    }
}
