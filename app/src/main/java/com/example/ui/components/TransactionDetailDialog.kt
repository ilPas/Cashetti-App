package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.theme.AppColorPalette
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri

@Composable
fun TransactionDetailDialog(
    expense: ExpenseEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dettagli Spesa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Chiudi")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow("Importo", String.format(Locale.ITALY, "%.2f €", kotlin.math.abs(expense.amount)))
                
                val accountFriendly = when (expense.accountType) {
                    "SERBATOIO_PERSONALE", "DISCREZIONALE_VARIABILE" -> "👤 Cassetto Personale"
                    "SERBATOIO_GINEVRA" -> "🏠 Cassetto Familiare"
                    "ESSENZIALE_REALE" -> "🏢 Costi Fissi"
                    "FONDO_EVENTI_DEPOSIT" -> "🎁 Fondo Risparmi/Eventi (+)"
                    "FONDO_EVENTI_WITHDRAWAL" -> "🎁 Fondo Risparmi/Eventi (-)"
                    else -> expense.accountType
                }
                DetailRow("Conto / Cassetto", accountFriendly)
                DetailRow("Categoria", expense.category)
                DetailRow("Necessità", if (expense.isNecessary) "✅ Spesa strettamente necessaria" else "⚡ Acquisto d'impulso (Grief spending)")
                DetailRow("Statistiche", if (expense.excludeFromStats) "🚫 Esclusa dalle statistiche (Spesa eccezionale)" else "📊 Inclusa nei report e medie regolari")

                val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.ITALY)
                DetailRow("Data", dateFormat.format(Date(expense.dateMillis)))
                if (expense.merchant.isNotBlank()) {
                    DetailRow("Esercente", expense.merchant)
                }
                if (expense.note.isNotBlank()) {
                    DetailRow("Nota", expense.note)
                }
                if (expense.latitude != null && expense.longitude != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DetailRow("Posizione GPS", "${String.format(Locale.US, "%.4f", expense.latitude)}, ${String.format(Locale.US, "%.4f", expense.longitude)}")
                        IconButton(onClick = {
                            val uri = "geo:${expense.latitude},${expense.longitude}?q=${expense.latitude},${expense.longitude}(${expense.merchant.ifBlank { "Spesa" }})"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Outlined.Map, contentDescription = "Apri su Maps", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (onDelete != null) {
                    TextButton(onClick = {
                        onDismiss()
                        onDelete()
                    }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Elimina")
                    }
                }
                TextButton(onClick = {
                    onDismiss()
                    onEdit()
                }) {
                    Text("Modifica")
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
