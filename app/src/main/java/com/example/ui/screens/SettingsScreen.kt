package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.example.service.GoogleDriveBackupService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.theme.AppColorPalette
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.CategoryEntity
import com.example.ui.BudgetUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: BudgetUiState,
    onUpdateSettings: (resetDay: Int, monthlyCap: Double, liquidity: Double, investments: Double, geminiApiKey: String, netMonthlyIncome: Double, essentialBaseline: Double, monthlyInvestmentTarget: Double, budgetPersonale: Double, budgetGinevra: Double, allowedNotificationApps: String) -> Unit,
    onAddCategory: (name: String, targetAccount: String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onPerformReallocation: (fromAccount: String, toAccount: String, amount: Double, reason: String) -> Unit,
    onExportCsv: (Context) -> Unit,
    onSimulateNotification: (merchant: String, amount: Double, currency: String, appName: String) -> Unit = { _, _, _, _ -> },
    onTriggerGoogleDriveBackup: (Context) -> Unit = {},
    onRestoreFromGoogleDrive: (Context, onDone: (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onToggleAutoBackup: (Boolean) -> Unit = {},
    onDisconnectGoogleDrive: (Context) -> Unit = {},
    onUpdateGoogleAccount: (String) -> Unit = {},
    onExportJsonBackup: (Context) -> Unit = {},
    onImportJsonBackup: (String, onDone: (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onClearBackupMessage: () -> Unit = {},
    onClearDriveAuthIntent: () -> Unit = {},
    onNavigateToLogs: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var pendingDriveAction by remember { mutableStateOf<String?>(null) }
    var showRestoreDriveConfirmDialog by remember { mutableStateOf(false) }
    var restoreDriveDialogMessage by remember { mutableStateOf<String?>(null) }
    var driveAuthErrorMessage by remember { mutableStateOf<String?>(null) }

    // Google Sign-In Activity Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val email = account?.email ?: ""
            if (email.isNotEmpty()) {
                driveAuthErrorMessage = null
                onUpdateGoogleAccount(email)
                if (pendingDriveAction == "BACKUP") {
                    onTriggerGoogleDriveBackup(context)
                } else if (pendingDriveAction == "RESTORE") {
                    showRestoreDriveConfirmDialog = true
                }
                pendingDriveAction = null
            } else {
                driveAuthErrorMessage = "Nessun indirizzo email restituito dall'account."
                pendingDriveAction = null
            }
        } catch (e: ApiException) {
            e.printStackTrace()
            val msg = when (e.statusCode) {
                10 -> "Errore di configurazione Google Play (Codice 10: DEVELOPER_ERROR). L'app richiede la registrazione del fingerprint SHA-1 su Google Cloud Console. Nel frattempo puoi usare il 'Salva Backup Locale (JSON)' sottostante."
                12500 -> "Accesso Google non riuscito (Codice 12500). Verifica la connessione o usa 'Salva Backup Locale (JSON)'."
                7 -> "Errore di connessione di rete con i server Google."
                else -> "Errore accesso Google (${e.statusCode}: ${e.localizedMessage ?: "Non autorizzato"}). Usa 'Salva Backup Locale (JSON)' per esportare i tuoi dati."
            }
            driveAuthErrorMessage = msg
            pendingDriveAction = null
        } catch (e: Exception) {
            e.printStackTrace()
            driveAuthErrorMessage = "Errore durante l'accesso: ${e.localizedMessage ?: "Operazione annullata"}"
            pendingDriveAction = null
        }
    }

    val authPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Re-trigger the pending action if we just granted permissions
            if (pendingDriveAction == "BACKUP") {
                onTriggerGoogleDriveBackup(context)
            } else if (pendingDriveAction == "RESTORE") {
                showRestoreDriveConfirmDialog = true
            }
            pendingDriveAction = null
        } else {
            pendingDriveAction = null
        }
    }

    LaunchedEffect(state.driveAuthIntent) {
        state.driveAuthIntent?.let { intent ->
            authPermissionLauncher.launch(intent)
            onClearDriveAuthIntent()
        }
    }

    // JSON file picker launcher
    var jsonImportError by remember { mutableStateOf<String?>(null) }
    var jsonImportSuccess by remember { mutableStateOf<String?>(null) }
    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val jsonString = inputStream?.bufferedReader()?.use { reader -> reader.readText() }
                if (jsonString != null) {
                    onImportJsonBackup(jsonString) { success, msg ->
                        if (success) {
                            jsonImportSuccess = msg
                            jsonImportError = null
                        } else {
                            jsonImportError = msg
                            jsonImportSuccess = null
                        }
                    }
                }
            } catch (e: Exception) {
                jsonImportError = "Errore lettura file: ${e.localizedMessage}"
            }
        }
    }

    var resetDayText by remember(state.resetDay) { mutableStateOf(state.resetDay.toString()) }
    var netMonthlyIncomeText by remember(state.netMonthlyIncome) { mutableStateOf(state.netMonthlyIncome.toString().replace('.', ',')) }
    var essentialBaselineText by remember(state.essentialBaseline) { mutableStateOf(state.essentialBaseline.toString().replace('.', ',')) }
    var monthlyInvestmentTargetText by remember(state.monthlyInvestmentTarget) { mutableStateOf(state.monthlyInvestmentTarget.toString().replace('.', ',')) }
    var budgetPersonaleText by remember(state.budgetPersonale) { mutableStateOf(state.budgetPersonale.toString().replace('.', ',')) }
    var budgetGinevraText by remember(state.budgetGinevra) { mutableStateOf(state.budgetGinevra.toString().replace('.', ',')) }
    var liquidityText by remember(state.liquidity) { mutableStateOf(state.liquidity.toString().replace('.', ',')) }
    var investmentsText by remember(state.investments) { mutableStateOf(state.investments.toString().replace('.', ',')) }
    var geminiApiText by remember(state.geminiApiKey) { mutableStateOf(state.geminiApiKey) }
    var allowedNotificationAppsText by remember(state.allowedNotificationApps) { mutableStateOf(state.allowedNotificationApps) }

    var isSettingsSavedMessage by remember { mutableStateOf(false) }

    val netIncomeNum = netMonthlyIncomeText.replace(',', '.').toDoubleOrNull() ?: state.netMonthlyIncome
    val essentialNum = essentialBaselineText.replace(',', '.').toDoubleOrNull() ?: state.essentialBaseline
    val pacNum = monthlyInvestmentTargetText.replace(',', '.').toDoubleOrNull() ?: state.monthlyInvestmentTarget
    val bPersNum = budgetPersonaleText.replace(',', '.').toDoubleOrNull() ?: state.budgetPersonale
    val bGinNum = budgetGinevraText.replace(',', '.').toDoubleOrNull() ?: state.budgetGinevra
    
    val totalAllocated = essentialNum + bPersNum + bGinNum + pacNum
    val balance = netIncomeNum - totalAllocated
    
    val balanceColor = if (balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary



    // Category Add State
    var newCategoryName by remember { mutableStateOf("") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var selectedTargetAccount by remember { mutableStateOf("DISCREZIONALE_VARIABILE") }
    
    // Reallocation State
    var showReallocationDialog by remember { mutableStateOf(false) }

    val accountTypes = listOf("DISCREZIONALE_VARIABILE", "ESSENZIALE_REALE", "FONDO_EVENTI_DEPOSIT")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_screen")
    ) {
        // Header
        Column(modifier = Modifier.padding(24.dp)) {
            IconButton(
                onClick = onNavigateUp,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = "Impostazioni",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Configurazione del budget",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Config Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Parametri Mensili",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = resetDayText,
                            onValueChange = { resetDayText = it },
                            label = { Text("Giorno di ripristino mensile (1-31)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        OutlinedTextField(
                            value = netMonthlyIncomeText,
                            onValueChange = { netMonthlyIncomeText = it },
                            label = { Text("Entrate Mensili Nette (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        OutlinedTextField(
                            value = essentialBaselineText,
                            onValueChange = { essentialBaselineText = it },
                            label = { Text("Costi Fissi (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        Text(
                            text = "Spese fisse e prevedibili, stesso importo ogni mese (mutuo, utenze, abbonamenti)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = budgetPersonaleText,
                            onValueChange = { budgetPersonaleText = it },
                            label = { Text("Budget Cassetto Personale (€)") },
                            placeholder = { Text("es. 700") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        OutlinedTextField(
                            value = budgetGinevraText,
                            onValueChange = { budgetGinevraText = it },
                            label = { Text("Budget Cassetto Familiare (€) [con Rollover]") },
                            placeholder = { Text("es. 180") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        Text(
                            text = "Spese necessarie per la famiglia/casa ma irregolari — manutenzioni, riparazioni, imprevisti",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = monthlyInvestmentTargetText,
                            onValueChange = { monthlyInvestmentTargetText = it },
                            label = { Text("Obiettivo Investimenti Mensile (PAC) (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        
                        // Budget Balance Banner
                        Surface(
                            color = if (balance < 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Bilancio Mensile Stimato",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (balance < 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (balance == 0.0) "Budget perfettamente bilanciato a 0!" 
                                           else if (balance > 0) String.format("Avanzano %.2f € non allocati", balance) 
                                           else String.format("Sei in negativo di %.2f €", -balance),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (balance < 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        OutlinedTextField(
                            value = liquidityText,
                            onValueChange = { liquidityText = it },
                            label = { Text("Liquidità Intoccabile (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        OutlinedTextField(
                            value = investmentsText,
                            onValueChange = { investmentsText = it },
                            label = { Text("Capitale Investito Totale (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        OutlinedTextField(
                            value = geminiApiText,
                            onValueChange = { geminiApiText = it },
                            label = { Text("Gemini API Key (per AI Insights)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )

                        OutlinedTextField(
                            value = allowedNotificationAppsText,
                            onValueChange = { allowedNotificationAppsText = it },
                            label = { Text("App abilitate notifiche (comma separated)") },
                            placeholder = { Text("es. PayPal, Google, Banca") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )

                        Button(
                            onClick = {
                                val resetDay = resetDayText.toIntOrNull() ?: state.resetDay
                                val netIncome = netMonthlyIncomeText.replace(',', '.').toDoubleOrNull() ?: state.netMonthlyIncome
                                val essential = essentialBaselineText.replace(',', '.').toDoubleOrNull() ?: state.essentialBaseline
                                val targetPAC = monthlyInvestmentTargetText.replace(',', '.').toDoubleOrNull() ?: state.monthlyInvestmentTarget
                                val bPers = budgetPersonaleText.replace(',', '.').toDoubleOrNull() ?: state.budgetPersonale
                                val bGin = budgetGinevraText.replace(',', '.').toDoubleOrNull() ?: state.budgetGinevra
                                val liq = liquidityText.replace(',', '.').toDoubleOrNull() ?: state.liquidity
                                val inv = investmentsText.replace(',', '.').toDoubleOrNull() ?: state.investments
                                onUpdateSettings(resetDay, bPers, liq, inv, geminiApiText, netIncome, essential, targetPAC, bPers, bGin, allowedNotificationAppsText)
                                isSettingsSavedMessage = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Salva Impostazioni")
                        }
                        if (isSettingsSavedMessage) {
                            Text(
                                text = "Impostazioni salvate con successo!",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Categories Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Categorie di Spesa",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = { showAddCategoryDialog = true }) {
                                Icon(Icons.Outlined.Add, contentDescription = "Aggiungi Categoria", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            state.categories.forEach { category ->
                                val accountLabel = when (category.targetAccount) {
                                    "DISCREZIONALE", "DISCREZIONALE_VARIABILE", "SERBATOIO_PERSONALE" -> "Personale"
                                    "FONDO_EVENTI", "FONDO_EVENTI_DEPOSIT", "SERBATOIO_GINEVRA" -> "Risparmi/Familiare"
                                    "ESSENZIALE_REALE" -> "Costi Fissi"
                                    else -> "Generale"
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { onDeleteCategory(category) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = category.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = accountLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = "Elimina",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Notifications Card
                val isNotificationListenerEnabled = remember(context) {
                    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                }

                // Diagnostic: Excluded from stats
                val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                val excludedExpenses = remember(state.allExpenses) {
                    state.allExpenses.filter { it.excludeFromStats && it.dateMillis >= thirtyDaysAgo }
                }
                val excludedCount = excludedExpenses.size
                val excludedTotal = excludedExpenses.sumOf { kotlin.math.abs(it.amount) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Diagnostica Esclusioni",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Negli ultimi 30 giorni hai escluso dalle statistiche $excludedCount ${if (excludedCount == 1) "spesa" else "spese"} per un totale di ${String.format(Locale.ITALY, "€ %.2f", excludedTotal)}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (excludedCount > 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Notifiche Bancarie",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!isNotificationListenerEnabled) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Abilita Lettura Notifiche", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                onSimulateNotification("TEST MERCHANT", 42.0, "EUR", "Test App")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Simula Notifica Spesa (Test)", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Google Drive Backup & Recovery Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = "Google Drive",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Backup Google Drive (Cloud)",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Backup settimanale incrementale per evitare la perdita dei dati storici",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        // Account status
                        val signedInAccount = remember(state.googleDriveAccount) {
                            GoogleDriveBackupService.getSignedInAccount(context)
                        }
                        val currentEmail = signedInAccount?.email ?: state.googleDriveAccount

                        if (currentEmail.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = "Connesso",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Account Google collegato",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = currentEmail,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    TextButton(
                                        onClick = { onDisconnectGoogleDrive(context) }
                                    ) {
                                        Text("Disconnetti", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    val client = GoogleDriveBackupService.getGoogleSignInClient(context)
                                    googleSignInLauncher.launch(client.signInIntent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connetti Account Google per il Backup")
                            }
                        }

                        if (driveAuthErrorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = driveAuthErrorMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        // Auto-backup toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Backup automatico settimanale",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Esegue l'aggiornamento incrementale ogni 7 giorni all'avvio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = state.isGoogleDriveAutoBackupEnabled,
                                onCheckedChange = { onToggleAutoBackup(it) }
                            )
                        }

                        // Last backup info
                        val lastBackupLabel = if (state.googleDriveLastBackupTime > 0L) {
                            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            formatter.format(Date(state.googleDriveLastBackupTime))
                        } else {
                            "Nessun backup recente"
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Ultimo backup su Drive: $lastBackupLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Status banner if present
                        state.backupStatusMessage?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = onClearBackupMessage,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("✕", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                            }
                        }

                        // WhatsApp style progress bar
                        if (state.isBackupInProgress || state.isRestoreInProgress) {
                            var simulatedProgress by remember { mutableFloatStateOf(0f) }
                            LaunchedEffect(state.isBackupInProgress, state.isRestoreInProgress) {
                                simulatedProgress = 0f
                                while (simulatedProgress < 0.9f) {
                                    kotlinx.coroutines.delay(100)
                                    simulatedProgress += 0.05f
                                }
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(
                                    progress = { simulatedProgress },
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                )
                            }
                        }

                        // Backup and restore buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    pendingDriveAction = "BACKUP"
                                    if (currentEmail.isBlank()) {
                                        val client = GoogleDriveBackupService.getGoogleSignInClient(context)
                                        googleSignInLauncher.launch(client.signInIntent)
                                    } else {
                                        onTriggerGoogleDriveBackup(context)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isBackupInProgress && !state.isRestoreInProgress
                            ) {
                                if (state.isBackupInProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Backup...", style = MaterialTheme.typography.labelMedium)
                                } else {
                                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Backup Ora", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    pendingDriveAction = "RESTORE"
                                    if (currentEmail.isBlank()) {
                                        val client = GoogleDriveBackupService.getGoogleSignInClient(context)
                                        googleSignInLauncher.launch(client.signInIntent)
                                    } else {
                                        showRestoreDriveConfirmDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isBackupInProgress && !state.isRestoreInProgress
                            ) {
                                if (state.isRestoreInProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ripristino...", style = MaterialTheme.typography.labelMedium)
                                } else {
                                    Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ripristina Drive", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        // Local Offline JSON Backup / Restore
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onExportJsonBackup(context) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Esporta JSON", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    jsonPickerLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Importa JSON", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (jsonImportSuccess != null) {
                            Text(
                                text = jsonImportSuccess ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (jsonImportError != null) {
                            Text(
                                text = jsonImportError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Data Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Gestione Dati",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { onExportCsv(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Esporta Dati in CSV", color = MaterialTheme.colorScheme.onSurface)
                        }
                        OutlinedButton(
                            onClick = { showReallocationDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Storno/Riallocazione Manuale", color = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        OutlinedButton(
                            onClick = onNavigateToLogs,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Log Analisi Notifiche Gemini", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }

    if (showRestoreDriveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDriveConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Ripristinare da Google Drive?") },
            text = {
                Text(
                    "Questa operazione scaricherà l'ultimo backup da Google Drive e aggiornerà il database locale con tutte le spese, abbonamenti, categorie ed eventi salvati."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDriveConfirmDialog = false
                        onRestoreFromGoogleDrive(context) { _, _ -> }
                    }
                ) {
                    Text("Conferma Ripristino")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDriveConfirmDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Nuova Categoria") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nome Categoria") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )
                    Text("Conto di Default:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accountTypes.forEach { acc ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedTargetAccount == acc) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { selectedTargetAccount = acc }.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = acc,
                                    color = if (selectedTargetAccount == acc) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        onAddCategory(newCategoryName, selectedTargetAccount)
                        newCategoryName = ""
                        showAddCategoryDialog = false
                    }
                }) {
                    Text("Aggiungi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Annulla") }
            }
        )
    }

    if (showReallocationDialog) {
        var fromAccount by remember { mutableStateOf("DISCREZIONALE_VARIABILE") }
        var toAccount by remember { mutableStateOf("FONDO_EVENTI_DEPOSIT") }
        var reallocAmountText by remember { mutableStateOf("") }
        var reallocReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showReallocationDialog = false },
            title = { Text("Riallocazione Fondi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Da:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accountTypes.forEach { acc ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (fromAccount == acc) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { fromAccount = acc }.padding(bottom = 8.dp)
                            ) {
                                Text(acc, color = if (fromAccount == acc) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                    Text("A:", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        accountTypes.forEach { acc ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (toAccount == acc) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { toAccount = acc }.padding(bottom = 8.dp)
                            ) {
                                Text(acc, color = if (toAccount == acc) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                    OutlinedTextField(
                        value = reallocAmountText,
                        onValueChange = { reallocAmountText = it },
                        label = { Text("Importo (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )
                    OutlinedTextField(
                        value = reallocReason,
                        onValueChange = { reallocReason = it },
                        label = { Text("Motivo") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = reallocAmountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onPerformReallocation(fromAccount, toAccount, amount, reallocReason)
                        showReallocationDialog = false
                    }
                }) {
                    Text("Rialloca")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReallocationDialog = false }) { Text("Annulla") }
            }
        )
    }
}
