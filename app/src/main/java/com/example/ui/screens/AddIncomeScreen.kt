package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.BudgetUiState
import com.example.ui.theme.AppColorPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeScreen(
    state: BudgetUiState,
    onSaveIncome: (String, Double, String, String) -> Unit, // accountType, amount, dateMillis (handled inside), note, merchant
    onSavedSuccess: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedAccountType by remember { mutableStateOf("SERBATOIO_PERSONALE") }

    val accounts = listOf(
        "SERBATOIO_PERSONALE" to "Cassetto Personale",
        "SERBATOIO_GINEVRA" to "Cassetto Ginevra (Imprevisti)",
        "ESSENZIALE_REALE" to "Costi Fissi",
        "FONDO_EVENTI_DEPOSIT" to "Fondo Risparmi/Eventi"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "Registra un'Entrata",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Aggiungi fondi a un tuo cassetto",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Importo (€)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColorPalette.Primary,
                    focusedLabelColor = AppColorPalette.Primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Cassetto di destinazione", style = MaterialTheme.typography.labelLarge, color = AppColorPalette.TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            accounts.forEach { (type, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedAccountType == type,
                        onClick = { selectedAccountType = type },
                        colors = RadioButtonDefaults.colors(selectedColor = AppColorPalette.Primary)
                    )
                    Text(
                        text = label,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColorPalette.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Nota (es. rimborso assicurazione)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColorPalette.Primary,
                    focusedLabelColor = AppColorPalette.Primary
                )
            )

            Spacer(modifier = Modifier.height(160.dp))
        }

        val isButtonEnabled = amountText.isNotBlank() && amountText.replace(',', '.').toDoubleOrNull() != null
        val buttonColor by animateColorAsState(
            targetValue = if (isButtonEnabled) Color(0xFF10B981) else AppColorPalette.SurfaceCardDark,
            animationSpec = tween(durationMillis = 300),
            label = "buttonColor"
        )
        val textColor by animateColorAsState(
            targetValue = if (isButtonEnabled) Color.White else AppColorPalette.TextMuted,
            animationSpec = tween(durationMillis = 300),
            label = "textColor"
        )

        Button(
            onClick = {
                val parsedAmount = amountText.replace(',', '.').toDoubleOrNull()
                if (parsedAmount != null && parsedAmount > 0) {
                    onSaveIncome(selectedAccountType, parsedAmount, noteText, "Entrata Manuale")
                    onSavedSuccess()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 124.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = textColor
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "REGISTRA ENTRATA",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
