package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.QuickRegisterActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import com.example.data.AppDatabase
import java.util.regex.Pattern

data class DetectedTransaction(
    val id: Long = System.currentTimeMillis(),
    val appName: String,
    val rawTitle: String,
    val rawText: String,
    val merchant: String,
    val originalAmount: Double,
    val currencySymbol: String,
    val currencyCode: String,
    val estimatedEurAmount: Double,
    val timestampMillis: Long = System.currentTimeMillis()
)

object PaymentNotificationManager {
    private val _pendingTransaction = MutableStateFlow<DetectedTransaction?>(null)
    val pendingTransaction: StateFlow<DetectedTransaction?> = _pendingTransaction.asStateFlow()

    fun setPendingTransaction(transaction: DetectedTransaction?) {
        _pendingTransaction.value = transaction
    }

    fun clearPendingTransaction() {
        _pendingTransaction.value = null
    }
}

class PaymentNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        if (packageName == applicationContext.packageName) return // Ignore self notifications

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val fullContent = "$title $text"

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val allowedAppsSetting = db.budgetDao().getSettingByKey("allowed_notification_apps")?.value 
                ?: "PayPal, Google, Apple, Banca, Sella, Intesa, Revolut, N26, Hype, Scalapay"
            
            val pm = applicationContext.packageManager
            val appLabel = try {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (e: Exception) {
                packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }

            val isAllowed = allowedAppsSetting.split(",")
                .map { it.trim().lowercase() }
                .any { keyword -> 
                    keyword.isNotBlank() && (
                        appLabel.lowercase().contains(keyword) || 
                        packageName.lowercase().contains(keyword)
                    )
                }

            if (!isAllowed) return@launch
            
            val detected = parsePaymentNotification(appLabel, title, text, fullContent)
            if (detected != null) {
                // Call Gemini to semantically filter out news/fake notifications
                val analysis = com.example.network.isRealTransactionViaGemini(appLabel, title, text)
                
                // Save to log
                val log = com.example.data.NotificationLogEntity(
                    timestamp = System.currentTimeMillis(),
                    appName = appLabel,
                    title = title,
                    content = text,
                    isAccepted = analysis.isTransaction,
                    reason = analysis.reason
                )
                db.budgetDao().insertNotificationLog(log)
                
                if (analysis.isTransaction) {
                    PaymentNotificationManager.setPendingTransaction(detected)
                    sendAppPromptNotification(detected)
                }
            }
        }
    }

    private fun parsePaymentNotification(
        appLabel: String,
        title: String,
        text: String,
        fullContent: String
    ): DetectedTransaction? {
        // Regex pattern for matching various world currencies
        // Matches e.g. "€ 12,50", "12.50 EUR", "$25.00", "25 USD", "£15.99", "CHF 40,00", "150 SEK", "¥5000"
        val patternStr = "(?i)(?:(€|\\$|£|CHF|¥|SEK|NOK|DKK|PLN|EUR|USD|GBP|JPY|CHF)\\s*([0-9]+(?:[.,][0-9]{1,2})?))|(?:([0-9]+(?:[.,][0-9]{1,2})?)\\s*(€|\\$|£|CHF|¥|SEK|NOK|DKK|PLN|EUR|USD|GBP|JPY|euro|dollars?))"
        val pattern = Pattern.compile(patternStr)
        val matcher = pattern.matcher(fullContent)

        if (matcher.find()) {
            var currencyStr = ""
            var amountStr = ""

            if (matcher.group(1) != null) {
                currencyStr = matcher.group(1) ?: "€"
                amountStr = matcher.group(2) ?: "0"
            } else if (matcher.group(3) != null) {
                amountStr = matcher.group(3) ?: "0"
                currencyStr = matcher.group(4) ?: "€"
            }

            val cleanedAmountStr = amountStr.replace(',', '.')
            val amount = cleanedAmountStr.toDoubleOrNull() ?: return null

            if (amount <= 0) return null

            val (code, rate) = normalizeCurrencyAndRate(currencyStr)
            val eurAmount = amount * rate

            val merchantName = extractMerchant(title, text, fullContent)

            return DetectedTransaction(
                appName = appLabel,
                rawTitle = title,
                rawText = text,
                merchant = merchantName,
                originalAmount = amount,
                currencySymbol = getCurrencySymbol(code),
                currencyCode = code,
                estimatedEurAmount = eurAmount
            )
        }
        return null
    }

    private fun normalizeCurrencyAndRate(currencyStr: String): Pair<String, Double> {
        val upper = currencyStr.uppercase().trim()
        return when {
            upper.contains("€") || upper.contains("EUR") || upper.contains("EURO") -> Pair("EUR", 1.0)
            upper.contains("$") || upper.contains("USD") || upper.contains("DOLLAR") -> Pair("USD", 0.92)
            upper.contains("£") || upper.contains("GBP") -> Pair("GBP", 1.17)
            upper.contains("CHF") || upper.contains("FR") -> Pair("CHF", 1.05)
            upper.contains("¥") || upper.contains("JPY") -> Pair("JPY", 0.0062)
            upper.contains("SEK") -> Pair("SEK", 0.088)
            upper.contains("NOK") -> Pair("NOK", 0.086)
            upper.contains("DKK") -> Pair("DKK", 0.13)
            upper.contains("PLN") -> Pair("PLN", 0.23)
            else -> Pair("EUR", 1.0)
        }
    }

    private fun getCurrencySymbol(code: String): String {
        return when (code) {
            "EUR" -> "€"
            "USD" -> "$"
            "GBP" -> "£"
            "CHF" -> "CHF"
            "JPY" -> "¥"
            "SEK" -> "kr"
            else -> code
        }
    }

    private fun extractMerchant(title: String, text: String, fullContent: String): String {
        val clean = if (title.isNotBlank() && !title.contains("pagamento", true)) title else text
        return clean.take(35).ifBlank { "Pagamento Notifica" }
    }

    private fun sendAppPromptNotification(transaction: DetectedTransaction) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "payment_detected_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Rilevamento Spese Automatico",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, QuickRegisterActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Pagamento Rilevato! 🔔")
            .setContentText("${transaction.merchant}: ${transaction.originalAmount} ${transaction.currencyCode} (~${String.format("%.2f €", transaction.estimatedEurAmount)}). Tocca per registrare.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(1001, builder.build())
    }
}
