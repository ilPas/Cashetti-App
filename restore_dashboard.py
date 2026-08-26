with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

dashboard_composable = """        ) {
            composable(Screen.Dashboard.route) {
                com.example.ui.screens.DashboardScreen(
                    state = uiState,
                    onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToEventFund = { navController.navigate(Screen.EventFund.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onUpdateAvatar = { uri -> viewModel.updateAvatarUri(uri) },
                    onDismissMonthlySummary = { cycleStart -> viewModel.dismissMonthlySummary(cycleStart) },
                    onNavigateToEditExpense = { expenseId -> navController.navigate("${Screen.AddExpense.route}?editId=$expenseId") },
                    onDeleteExpense = { exp -> viewModel.deleteExpense(exp) }
                )
            }
"""

content = content.replace('        ) {\n            composable(\n                route = "${Screen.AddExpense.route}', dashboard_composable + '            composable(\n                route = "${Screen.AddExpense.route}')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
