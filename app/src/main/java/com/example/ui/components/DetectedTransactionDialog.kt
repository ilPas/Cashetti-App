package com.example.ui.components
import androidx.compose.foundation.clickable
import androidx.compose.material3.TextFieldDefaults

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Euro
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.CategoryEntity
import com.example.service.DetectedTransaction
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetectedTransactionDialog(
    transaction: DetectedTransaction,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onConfirmExpense: (
        accountType: String,
        amountEur: Double,
        category: String,
        note: String,
        merchant: String,
        amortizationMonths: Int
    ) -> Unit
) {
    var eurAmountText by remember(transaction) {
        mutableStateOf(String.format(Locale.US, "%.2f", transaction.estimatedEurAmount))
    }
    var selectedAccountIndex by remember { mutableIntStateOf(0) } // 0: Discrezionale, 1: Fissi, 2: Fondo Eventi
    val accountTypes = listOf("DISCREZIONALE_VARIABILE", "ESSENZIALE_REALE", "FONDO_EVENTI_WITHDRAWAL")
    val accountLabels = listOf("Spese", "Fissi", "Risparmi")

    var selectedCategory by remember { mutableStateOf("Shopping") }
    var noteText by remember(transaction) {
        mutableStateOf("${transaction.merchant} (${transaction.originalAmount} ${transaction.currencyCode})")
    }
    var merchantText by remember(transaction) { mutableStateOf(transaction.merchant) }

    // Amortization state
    var isAmortizationEnabled by remember { mutableStateOf(false) }
    var amortizationMonths by remember { mutableIntStateOf(3) }
    val monthOptions = listOf(2, 3, 4, 6, 12, 24)

    val currentAccountType = accountTypes[selectedAccountIndex]
    val availableCategories = remember(categories) {
        categories.map { it.name }.distinct().sorted()
            .ifEmpty { listOf("Shopping", "Cene", "Svago", "Spesa", "Trasporti", "Casa", "Altro") }
    }

    if (!availableCategories.contains(selectedCategory) && availableCategories.isNotEmpty()) {
        selectedCategory = availableCategories.first()
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dialogTextFieldColors = androidx.compose.material3.TextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val configuration = LocalConfiguration.current
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = (configuration.screenHeightDp * 0.6).dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Title Area
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.testTag("detected_transaction_dialog_title")
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "Notifica Spesa",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Spesa Rilevata!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Origine: ${transaction.appName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Detected info card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Dettagli Notifica Originale:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${transaction.originalAmount} ${transaction.currencyCode} (${transaction.merchant})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (transaction.currencyCode != "EUR") {
                                Text(
                                    text = "Convertito stima: ~${String.format(Locale.ITALY, "%.2f €", transaction.estimatedEurAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    // EUR Amount input
                    TextField(
                        value = eurAmountText,
                        onValueChange = {
                            eurAmountText = it.replace(',', '.')
                            errorMessage = null
                        },
                        label = { Text("Importo Finale in Euro (€)") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Euro,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = dialogTextFieldColors,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detected_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Account Choice
                    Text(
                        text = "Conto di Addebito:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        accountLabels.forEachIndexed { idx, label ->
                            SegmentedButton(
                                selected = selectedAccountIndex == idx,
                                onClick = { selectedAccountIndex = idx },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = accountLabels.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primary,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Category Dropdown
                    var expandedCategory by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCategory,
                        onExpandedChange = { expandedCategory = it }
                    ) {
                        TextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoria") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = dialogTextFieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false }
                        ) {
                            availableCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        selectedCategory = cat
                                        expandedCategory = false
                                    }
                                )
                            }
                        }
                    }

                    // Merchant Field
                    TextField(
                        value = merchantText,
                        onValueChange = { merchantText = it },
                        label = { Text("Esercente (Merchant)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Note Field
                    TextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Nota / Descrizione") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = dialogTextFieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Amortization (Ammortamento su più mesi) Section
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Text(
                                        text = "Ammortamento su più mesi",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isAmortizationEnabled,
                                    onCheckedChange = { isAmortizationEnabled = it },
                                    modifier = Modifier.testTag("amortization_switch")
                                )
                            }

                            if (isAmortizationEnabled) {
                                Text(
                                    text = "Suddividi l'impatto di questa spesa in rate mensili uguali sui prossimi cicli:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Months selector
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    monthOptions.forEach { m ->
                                        val isMSelected = amortizationMonths == m
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isMSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.clickable { amortizationMonths = m }
                                        ) {
                                            Text(
                                                text = "$m Mesi",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isMSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isMSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                val parsedTotal = eurAmountText.toDoubleOrNull() ?: 0.0
                                val monthlyQuota = if (amortizationMonths > 0) parsedTotal / amortizationMonths else 0.0

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Quota mensile: ${String.format(Locale.ITALY, "%.2f €", monthlyQuota)} / mese per $amortizationMonths mesi",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ignora",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge.copy(
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        ),
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(8.dp)
                    )
                    Button(
                        onClick = {
                            val parsed = eurAmountText.toDoubleOrNull()
                            if (parsed == null || parsed <= 0) {
                                errorMessage = "Inserisci un importo in Euro valido."
                            } else {
                                val finalAmortization = if (isAmortizationEnabled) amortizationMonths else 1
                                onConfirmExpense(
                                    currentAccountType,
                                    parsed,
                                    selectedCategory,
                                    noteText,
                                    merchantText,
                                    finalAmortization
                                )
                            }
                        },
                        modifier = Modifier.testTag("confirm_detected_expense_button")
                    ) {
                        Text(if (isAmortizationEnabled) "Rateizza ($amortizationMonths mesi)" else "Salva Spesa")
                    }
                }
            }
        }
    }
}
