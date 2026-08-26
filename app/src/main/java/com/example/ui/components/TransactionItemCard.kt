package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseEntity
import com.example.ui.theme.AppColorPalette

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionItemCard(
    expense: ExpenseEntity,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = AppColorPalette.Surface)
 {
    val isDeposit = expense.accountType == "FONDO_EVENTI_DEPOSIT" || expense.amount < 0
    val amountColor = if (isDeposit) AppColorPalette.StatusPositive else when (expense.accountType) {
        "ESSENZIALE_REALE" -> AppColorPalette.StatusFixedCost
        else -> AppColorPalette.StatusExpense
    }
    val amountSign = if (expense.accountType == "FONDO_EVENTI_DEPOSIT") "+" else "-"
    val absoluteAmount = kotlin.math.abs(expense.amount)

    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.ITALY)
    val formattedDate = dateFormat.format(Date(expense.dateMillis))

    val accountBadgeText = when (expense.accountType) {
        "SERBATOIO_PERSONALE", "DISCREZIONALE_VARIABILE" -> "👤 Personale"
        "SERBATOIO_GINEVRA" -> "🏠 Ginevra"
        "ESSENZIALE_REALE" -> "🏢 Costi fissi"
        "FONDO_EVENTI_DEPOSIT" -> "🎁 Fondo Eventi (+)"
        "FONDO_EVENTI_WITHDRAWAL" -> "🎁 Fondo Eventi (-)"
        else -> expense.accountType
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_item_${expense.id}")
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = amountColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Icon(
                        imageVector = if (isDeposit) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Column {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = accountBadgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (expense.excludeFromStats) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AppColorPalette.StatusFixedCost.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "🚫 Esclusa",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColorPalette.StatusFixedCost,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (!expense.isNecessary) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AppColorPalette.StatusExpense.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "⚡ Impulso",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColorPalette.StatusExpense,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    if (expense.note.isNotBlank()) {
                        Text(
                            text = expense.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = "$amountSign${String.format(Locale.ITALY, "%.2f €", absoluteAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
