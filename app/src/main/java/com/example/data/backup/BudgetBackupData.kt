package com.example.data.backup

import com.example.data.CategoryEntity
import com.example.data.EventEntity
import com.example.data.ExpenseEntity
import com.example.data.NotificationLogEntity
import com.example.data.SettingEntity
import com.example.data.SubscriptionEntity
import org.json.JSONArray
import org.json.JSONObject

data class BudgetBackupData(
    val version: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val appName: String = "Budget Control",
    val expenses: List<ExpenseEntity> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val settings: List<SettingEntity> = emptyList(),
    val events: List<EventEntity> = emptyList(),
    val notificationLogs: List<NotificationLogEntity> = emptyList()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("exportedAtMillis", exportedAtMillis)
        root.put("appName", appName)

        // Expenses
        val expensesArray = JSONArray()
        for (e in expenses) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("accountType", e.accountType)
            obj.put("amount", e.amount)
            obj.put("category", e.category)
            obj.put("dateMillis", e.dateMillis)
            obj.put("note", e.note)
            obj.put("merchant", e.merchant)
            if (e.latitude != null) obj.put("latitude", e.latitude)
            if (e.longitude != null) obj.put("longitude", e.longitude)
            if (e.eventTargetDateMillis != null) obj.put("eventTargetDateMillis", e.eventTargetDateMillis)
            if (e.eventId != null) obj.put("eventId", e.eventId)
            obj.put("excludeFromStats", e.excludeFromStats)
            obj.put("isNecessary", e.isNecessary)
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        // Subscriptions
        val subsArray = JSONArray()
        for (s in subscriptions) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("amount", s.amount)
            obj.put("dayOfMonth", s.dayOfMonth)
            obj.put("isActive", s.isActive)
            subsArray.put(obj)
        }
        root.put("subscriptions", subsArray)

        // Categories
        val catsArray = JSONArray()
        for (c in categories) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("targetAccount", c.targetAccount)
            catsArray.put(obj)
        }
        root.put("categories", catsArray)

        // Settings
        val settingsArray = JSONArray()
        for (st in settings) {
            val obj = JSONObject()
            obj.put("key", st.key)
            obj.put("value", st.value)
            settingsArray.put(obj)
        }
        root.put("settings", settingsArray)

        // Events
        val eventsArray = JSONArray()
        for (ev in events) {
            val obj = JSONObject()
            obj.put("id", ev.id)
            obj.put("name", ev.name)
            obj.put("estimatedBudget", ev.estimatedBudget)
            if (ev.targetDateMillis != null) obj.put("targetDateMillis", ev.targetDateMillis)
            eventsArray.put(obj)
        }
        root.put("events", eventsArray)

        // Notification Logs
        val logsArray = JSONArray()
        for (log in notificationLogs) {
            val obj = JSONObject()
            obj.put("id", log.id)
            obj.put("timestamp", log.timestamp)
            obj.put("appName", log.appName)
            obj.put("title", log.title)
            obj.put("content", log.content)
            obj.put("isAccepted", log.isAccepted)
            obj.put("reason", log.reason)
            logsArray.put(obj)
        }
        root.put("notificationLogs", logsArray)

        return root.toString(2)
    }

    companion object {
        fun fromJson(jsonString: String): BudgetBackupData {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val exportedAtMillis = root.optLong("exportedAtMillis", System.currentTimeMillis())
            val appName = root.optString("appName", "Budget Control")

            val expenses = mutableListOf<ExpenseEntity>()
            val expensesArray = root.optJSONArray("expenses")
            if (expensesArray != null) {
                for (i in 0 until expensesArray.length()) {
                    val obj = expensesArray.getJSONObject(i)
                    expenses.add(
                        ExpenseEntity(
                            id = obj.optLong("id", 0L),
                            accountType = obj.optString("accountType", "SERBATOIO_PERSONALE"),
                            amount = obj.optDouble("amount", 0.0),
                            category = obj.optString("category", "Altro"),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                            note = obj.optString("note", ""),
                            merchant = obj.optString("merchant", ""),
                            latitude = if (obj.has("latitude") && !obj.isNull("latitude")) obj.getDouble("latitude") else null,
                            longitude = if (obj.has("longitude") && !obj.isNull("longitude")) obj.getDouble("longitude") else null,
                            eventTargetDateMillis = if (obj.has("eventTargetDateMillis") && !obj.isNull("eventTargetDateMillis")) obj.getLong("eventTargetDateMillis") else null,
                            eventId = if (obj.has("eventId") && !obj.isNull("eventId")) obj.getLong("eventId") else null,
                            excludeFromStats = obj.optBoolean("excludeFromStats", false),
                            isNecessary = obj.optBoolean("isNecessary", true)
                        )
                    )
                }
            }

            val subscriptions = mutableListOf<SubscriptionEntity>()
            val subsArray = root.optJSONArray("subscriptions")
            if (subsArray != null) {
                for (i in 0 until subsArray.length()) {
                    val obj = subsArray.getJSONObject(i)
                    subscriptions.add(
                        SubscriptionEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.optString("name", ""),
                            amount = obj.optDouble("amount", 0.0),
                            dayOfMonth = obj.optInt("dayOfMonth", 1),
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                }
            }

            val categories = mutableListOf<CategoryEntity>()
            val catsArray = root.optJSONArray("categories")
            if (catsArray != null) {
                for (i in 0 until catsArray.length()) {
                    val obj = catsArray.getJSONObject(i)
                    categories.add(
                        CategoryEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.optString("name", ""),
                            targetAccount = obj.optString("targetAccount", "BOTH")
                        )
                    )
                }
            }

            val settings = mutableListOf<SettingEntity>()
            val settingsArray = root.optJSONArray("settings")
            if (settingsArray != null) {
                for (i in 0 until settingsArray.length()) {
                    val obj = settingsArray.getJSONObject(i)
                    settings.add(
                        SettingEntity(
                            key = obj.optString("key", ""),
                            value = obj.optString("value", "")
                        )
                    )
                }
            }

            val events = mutableListOf<EventEntity>()
            val eventsArray = root.optJSONArray("events")
            if (eventsArray != null) {
                for (i in 0 until eventsArray.length()) {
                    val obj = eventsArray.getJSONObject(i)
                    events.add(
                        EventEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.optString("name", ""),
                            estimatedBudget = obj.optDouble("estimatedBudget", 0.0),
                            targetDateMillis = if (obj.has("targetDateMillis") && !obj.isNull("targetDateMillis")) obj.getLong("targetDateMillis") else null
                        )
                    )
                }
            }

            val logs = mutableListOf<NotificationLogEntity>()
            val logsArray = root.optJSONArray("notificationLogs")
            if (logsArray != null) {
                for (i in 0 until logsArray.length()) {
                    val obj = logsArray.getJSONObject(i)
                    logs.add(
                        NotificationLogEntity(
                            id = obj.optLong("id", 0L),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            appName = obj.optString("appName", ""),
                            title = obj.optString("title", ""),
                            content = obj.optString("content", ""),
                            isAccepted = obj.optBoolean("isAccepted", true),
                            reason = obj.optString("reason", "")
                        )
                    )
                }
            }

            return BudgetBackupData(
                version = version,
                exportedAtMillis = exportedAtMillis,
                appName = appName,
                expenses = expenses,
                subscriptions = subscriptions,
                categories = categories,
                settings = settings,
                events = events,
                notificationLogs = logs
            )
        }
    }
}
