package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BudgetUiState
import com.example.ui.theme.AppColorPalette
import com.example.utils.getCurrentLocation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalPermissionsApi::class)
@Composable
fun AddExpenseScreen(
    state: BudgetUiState,
    initialAccountType: String?,
    expenseToEditId: Long? = null,
    onSaveExpense: (String, Double, String, Long, String, String, Double?, Double?, Long?, Long?, Boolean, Boolean, Int) -> Unit,
    onUpdateExpense: ((Long, String, Double, String, Long, String, String, Double?, Double?, Boolean, Boolean) -> Unit)? = null,
    onSavedSuccess: () -> Unit
) {
    val expenseToEdit = remember(expenseToEditId, state.allExpenses) {
        if (expenseToEditId != null) state.allExpenses.find { it.id == expenseToEditId } else null
    }

    var amountText by remember(expenseToEdit) {
        mutableStateOf(if (expenseToEdit != null) kotlin.math.abs(expenseToEdit.amount).toString() else "")
    }
    var noteText by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.note ?: "") }
    var merchantText by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.merchant ?: "") }
    var locationLat by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.latitude) }
    var locationLng by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.longitude) }
    var selectedCategory by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.category ?: "Altro") }
    
    // Amortization state
    var isAmortizationEnabled by remember { mutableStateOf(false) }
    var amortizationMonths by remember { mutableIntStateOf(3) }
    val monthOptions = listOf(2, 3, 4, 6, 12, 24)
    
    var selectedAccountType by remember(expenseToEdit) {
        val initialType = expenseToEdit?.accountType ?: initialAccountType ?: "SERBATOIO_PERSONALE"
        val mapped = if (initialType == "DISCREZIONALE_VARIABILE") "SERBATOIO_PERSONALE" else initialType
        mutableStateOf(mapped)
    }
    
    // Macro-feature 1: Esclusione dalle Statistiche
    var excludeFromStats by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.excludeFromStats ?: false) }

    // Macro-feature 2: Attrito Cognitivo e Tracciamento "Grief Spending"
    // In edit mode it has a value; in new mode it is null to force friction answer!
    var isNecessarySelection by remember(expenseToEdit) {
        mutableStateOf<Boolean?>(expenseToEdit?.isNecessary)
    }
    var showNecessityError by remember { mutableStateOf(false) }
    
    var dateMillis by remember(expenseToEdit) { mutableLongStateOf(expenseToEdit?.dateMillis ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val categories = if (state.categories.isNotEmpty()) {
        state.categories.map { it.name }.distinct().sorted()
    } else {
        listOf("Altro", "Casa & Utenze", "Cene & Ristoranti", "Salute & Benessere", "Shopping", "Spesa & Alimentari", "Svago & Tempo Libero", "Trasporti & Auto")
    }

    val accounts = listOf(
        "SERBATOIO_PERSONALE" to "👤 Serbatoio Personale",
        "SERBATOIO_GINEVRA" to "🏠 Casa / Ginevra (Imprevisti)",
        "ESSENZIALE_REALE" to "🏢 Costi Fissi Essenziali",
        "FONDO_EVENTI_DEPOSIT" to "🎁 Fondo Risparmi (+)",
        "FONDO_EVENTI_WITHDRAWAL" to "🎁 Fondo Risparmi (-)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColorPalette.Background)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            IconButton(
                onClick = onSavedSuccess,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Indietro", tint = AppColorPalette.TextPrimary)
            }
            Text(
                text = if (expenseToEditId != null) "Modifica Spesa" else "Nuovo Movimento",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColorPalette.TextPrimary
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = AppColorPalette.Surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Macro-feature 4: Serbatoio / Conto Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Scegli Serbatoio o Conto:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColorPalette.TextPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.forEach { (key, label) ->
                            val isSelected = selectedAccountType == key
                            val isGinevra = key == "SERBATOIO_GINEVRA"
                            val isPersonale = key == "SERBATOIO_PERSONALE"
                            
                            val activeBg = when {
                                isPersonale -> AppColorPalette.Primary
                                isGinevra -> AppColorPalette.StatusSaving
                                else -> MaterialTheme.colorScheme.secondary
                            }
                            
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) activeBg else AppColorPalette.SurfaceCard,
                                modifier = Modifier
                                    .testTag("account_chip_$key")
                                    .clickable { selectedAccountType = key }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) AppColorPalette.TextPrimary else AppColorPalette.TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Amount Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Importo (€)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColorPalette.TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColorPalette.SurfaceCard, RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "€",
                            style = MaterialTheme.typography.headlineLarge,
                            color = AppColorPalette.Primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        TextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                color = AppColorPalette.TextPrimary,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = AppColorPalette.TextPrimary,
                                unfocusedTextColor = AppColorPalette.TextPrimary,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = AppColorPalette.Primary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = {
                                Text(
                                    "0,00",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = AppColorPalette.TextMuted
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("amount_input")
                        )
                    }
                }

                // Macro-feature 2: Attrito Cognitivo e Tracciamento "Grief Spending"
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (showNecessityError) AppColorPalette.StatusExpense.copy(alpha = 0.12f) else AppColorPalette.SurfaceCard,
                    border = if (showNecessityError) androidx.compose.foundation.BorderStroke(1.5.dp, AppColorPalette.StatusExpense) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                tint = if (showNecessityError) AppColorPalette.StatusExpense else AppColorPalette.Primary
                            )
                            Column {
                                Text(
                                    text = "Riflessione di Spesa (Obbligatoria)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (showNecessityError) AppColorPalette.StatusExpense else AppColorPalette.Primary
                                )
                                Text(
                                    text = "Questa spesa era strettamente necessaria?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColorPalette.TextPrimary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Option 1: Sì, necessaria
                            val isYesSelected = isNecessarySelection == true
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isYesSelected) AppColorPalette.StatusPositive else AppColorPalette.SurfaceCardDark,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        isNecessarySelection = true
                                        showNecessityError = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isYesSelected) AppColorPalette.TextPrimary else AppColorPalette.StatusPositive,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sì, necessaria",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isYesSelected) AppColorPalette.TextPrimary else AppColorPalette.TextPrimary
                                    )
                                }
                            }

                            // Option 2: No, impulso
                            val isNoSelected = isNecessarySelection == false
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isNoSelected) AppColorPalette.StatusExpense else AppColorPalette.SurfaceCardDark,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        isNecessarySelection = false
                                        showNecessityError = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Warning,
                                        contentDescription = null,
                                        tint = if (isNoSelected) AppColorPalette.TextPrimary else AppColorPalette.StatusExpense,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "No, impulso",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNoSelected) AppColorPalette.TextPrimary else AppColorPalette.TextPrimary
                                    )
                                }
                            }
                        }

                        if (showNecessityError) {
                            Text(
                                text = "⚠️ Rispondi alla domanda per completare l'inserimento ed evitare acquisti d'impulso inconsapevoli.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColorPalette.StatusExpense
                            )
                        }
                    }
                }

                // Macro-feature 1: Esclusione dalle Statistiche
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AppColorPalette.SurfaceCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Escludi dalle statistiche",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColorPalette.TextPrimary
                            )
                            Text(
                                text = "Per spese eccezionali / pregresse (es. tasse annuali, debiti vecchi). Viene ignorata nei grafici a torta e dal budget mensile regolare.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColorPalette.TextSecondary
                            )
                        }
                        Switch(
                            checked = excludeFromStats,
                            onCheckedChange = { excludeFromStats = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AppColorPalette.TextPrimary,
                                checkedTrackColor = AppColorPalette.StatusFixedCost,
                                uncheckedThumbColor = AppColorPalette.TextMuted,
                                uncheckedTrackColor = AppColorPalette.SurfaceCardDark
                            )
                        )
                    }
                }

                // Category Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Categoria:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColorPalette.TextPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) AppColorPalette.Primary else AppColorPalette.SurfaceCard,
                                modifier = Modifier
                                    .testTag("category_chip_$cat")
                                    .clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) AppColorPalette.TextPrimary else AppColorPalette.TextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Date
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Data Operazione:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColorPalette.TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AppColorPalette.SurfaceCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.ITALY)
                            Text(
                                text = dateFormat.format(Date(dateMillis)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppColorPalette.TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = "Seleziona Data",
                                tint = AppColorPalette.Primary
                            )
                        }
                    }
                }

                // Merchant
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Esercente / Piattaforma (opzionale)") },
                    placeholder = { Text("es. Amazon, Esselunga") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppColorPalette.TextPrimary,
                        unfocusedTextColor = AppColorPalette.TextPrimary,
                        focusedBorderColor = AppColorPalette.Primary,
                        unfocusedBorderColor = AppColorPalette.SurfaceCardDark,
                        focusedLabelColor = AppColorPalette.Primary,
                        unfocusedLabelColor = AppColorPalette.TextSecondary,
                        cursorColor = AppColorPalette.Primary
                    )
                )

                // Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Nota / Descrizione (opzionale)") },
                    placeholder = { Text("es. Pranzo di lavoro") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppColorPalette.TextPrimary,
                        unfocusedTextColor = AppColorPalette.TextPrimary,
                        focusedBorderColor = AppColorPalette.Primary,
                        unfocusedBorderColor = AppColorPalette.SurfaceCardDark,
                        focusedLabelColor = AppColorPalette.Primary,
                        unfocusedLabelColor = AppColorPalette.TextSecondary,
                        cursorColor = AppColorPalette.Primary
                    )
                )

                if (expenseToEditId == null) {
                    // Amortization (Ammortamento su più mesi) Section
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AppColorPalette.SurfaceCard,
                        modifier = Modifier.fillMaxWidth()
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
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        text = "Ammortamento su più mesi",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColorPalette.TextPrimary
                                    )
                                    Text(
                                        text = "Suddividi l'impatto di questa spesa in rate mensili uguali",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppColorPalette.TextSecondary
                                    )
                                }
                                Switch(
                                    checked = isAmortizationEnabled,
                                    onCheckedChange = { isAmortizationEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = AppColorPalette.TextPrimary,
                                        checkedTrackColor = AppColorPalette.StatusFixedCost,
                                        uncheckedThumbColor = AppColorPalette.TextMuted,
                                        uncheckedTrackColor = AppColorPalette.SurfaceCardDark
                                    )
                                )
                            }

                            if (isAmortizationEnabled) {
                                // Months selector
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    monthOptions.forEach { m ->
                                        val isMSelected = amortizationMonths == m
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isMSelected) AppColorPalette.StatusFixedCost else AppColorPalette.SurfaceCardDark,
                                            modifier = Modifier.clickable { amortizationMonths = m }
                                        ) {
                                            Text(
                                                text = "$m Mesi",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isMSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isMSelected) AppColorPalette.TextPrimary else AppColorPalette.TextSecondary,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                val parsedTotal = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                                val monthlyQuota = if (amortizationMonths > 0) parsedTotal / amortizationMonths else 0.0

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AppColorPalette.SurfaceCardDark
                                ) {
                                    Text(
                                        text = "Quota mensile: ${String.format(Locale.ITALY, "%.2f €", monthlyQuota)} / mese per $amortizationMonths mesi",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AppColorPalette.TextPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Location
                val context = LocalContext.current
                val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
                
                LaunchedEffect(expenseToEditId, locationPermissionState.status.isGranted) {
                    if (expenseToEditId == null && locationLat == null && locationPermissionState.status.isGranted) {
                        getCurrentLocation(context) { loc ->
                            locationLat = loc?.latitude
                            locationLng = loc?.longitude
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (locationLat != null) "GPS: ${String.format(Locale.US, "%.4f, %.4f", locationLat, locationLng)}" else "Nessuna posizione",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColorPalette.TextSecondary
                    )
                    TextButton(onClick = {
                        if (locationPermissionState.status.isGranted) {
                            getCurrentLocation(context) { loc ->
                                locationLat = loc?.latitude
                                locationLng = loc?.longitude
                            }
                        } else {
                            locationPermissionState.launchPermissionRequest()
                        }
                    }) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = AppColorPalette.Primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (locationLat != null) "Aggiorna GPS" else "Rileva GPS",
                            color = AppColorPalette.Primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val parsedAmount = amountText.replace(',', '.').toDoubleOrNull()
                        if (parsedAmount == null || parsedAmount <= 0) {
                            return@Button
                        }
                        if (isNecessarySelection == null) {
                            showNecessityError = true
                            return@Button
                        }

                        val isNecessary = isNecessarySelection ?: true
                        val finalAmount = if (selectedAccountType == "FONDO_EVENTI_WITHDRAWAL") -parsedAmount else parsedAmount
                        
                        if (expenseToEditId != null && onUpdateExpense != null) {
                            onUpdateExpense(
                                expenseToEditId,
                                selectedAccountType,
                                finalAmount,
                                selectedCategory,
                                dateMillis,
                                noteText,
                                merchantText,
                                locationLat,
                                locationLng,
                                excludeFromStats,
                                isNecessary
                            )
                        } else {
                            val finalAmortization = if (isAmortizationEnabled) amortizationMonths else 1
                            onSaveExpense(
                                selectedAccountType,
                                finalAmount,
                                selectedCategory,
                                dateMillis,
                                noteText,
                                merchantText,
                                locationLat,
                                locationLng,
                                null, // targetDateMillis
                                null,  // eventId
                                excludeFromStats,
                                isNecessary,
                                finalAmortization
                            )
                        }
                        onSavedSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("save_expense_button"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColorPalette.Primary,
                        contentColor = AppColorPalette.TextPrimary
                    )
                ) {
                    Text(
                        text = "SALVA MOVIMENTO",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = AppColorPalette.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    showDatePicker = false
                }) {
                    Text("OK", color = AppColorPalette.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annulla", color = AppColorPalette.TextSecondary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }
}
