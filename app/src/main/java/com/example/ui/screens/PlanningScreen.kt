package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.ui.BudgetUiState
import com.example.ui.BudgetViewModel

@Composable
fun PlanningScreen(
    state: BudgetUiState,
    navController: NavController,
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ricorrenti", "Risparmi", "Storico")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, color = if (selectedTabIndex == index) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontWeight = if (selectedTabIndex == index) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> SubscriptionsScreen(
                state = state,
                onAddSubscription = { n, a, d -> viewModel.addSubscription(n, a, d) },
                onUpdateSubscription = { s -> viewModel.updateSubscription(s) },
                onToggleActive = { s -> viewModel.toggleSubscriptionActive(s) },
                onDeleteSubscription = { s -> viewModel.deleteSubscription(s) }
            )
            1 -> EventFundScreen(
                state = state,
                onNavigateToAddExpenseWithAccount = { accountType ->
                    navController.navigate("add_expense?accountType=$accountType")
                },
                onAddEvent = { n, b, d -> viewModel.addEvent(n, b, d) }
            )
            2 -> HistoryScreen(
                state = state,
                onDeleteExpense = { exp -> viewModel.deleteExpense(exp) },
                onNavigateToEditExpense = { expenseId -> navController.navigate("add_expense?editId=$expenseId") }
            )
        }
    }
}
