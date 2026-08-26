package com.example.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.BudgetRepository
import com.example.service.PaymentNotificationManager
import com.example.ui.components.DetectedTransactionDialog
import com.example.ui.theme.BudgetControlTheme

import androidx.compose.ui.platform.LocalContext
import com.example.utils.getCurrentLocation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

class QuickRegisterActivity : ComponentActivity() {

    private val viewModel: BudgetViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BudgetRepository(database.budgetDao())
        BudgetViewModelFactory(repository)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BudgetControlTheme {
                val uiState by viewModel.uiState.collectAsState()
                val pendingTransaction by PaymentNotificationManager.pendingTransaction.collectAsState()

                val context = LocalContext.current
                val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
                var locationLat by remember { mutableStateOf<Double?>(null) }
                var locationLng by remember { mutableStateOf<Double?>(null) }

                LaunchedEffect(locationPermissionState.status.isGranted) {
                    if (locationPermissionState.status.isGranted) {
                        getCurrentLocation(context) { loc ->
                            locationLat = loc?.latitude
                            locationLng = loc?.longitude
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val transaction = pendingTransaction
                    if (transaction != null) {
                        DetectedTransactionDialog(
                            transaction = transaction,
                            categories = uiState.categories,
                            onDismiss = {
                                viewModel.clearPendingTransaction()
                                finish()
                            },
                            onConfirmExpense = { accountType, amountEur, category, note, merchant, amortizationMonths ->
                                viewModel.addAmortizedExpense(
                                    accountType = accountType,
                                    totalAmountEur = amountEur,
                                    category = category,
                                    note = note,
                                    merchant = merchant,
                                    latitude = locationLat,
                                    longitude = locationLng,
                                    amortizationMonths = amortizationMonths
                                )
                                finish()
                            }
                        )
                    } else {
                        finish()
                    }
                }
            }
        }
    }
}
