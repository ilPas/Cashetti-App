package com.example

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing

import android.os.Build
import android.os.Bundle

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height

import androidx.compose.ui.unit.dp

import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.data.AppDatabase
import com.example.data.BudgetRepository
import com.example.ui.BudgetViewModel
import com.example.ui.BudgetViewModelFactory
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.screens.AddExpenseScreen
import com.example.ui.screens.AddIncomeScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EventFundScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.SubscriptionsScreen
import com.example.ui.theme.BudgetControlTheme

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Outlined.Dashboard)
    object AddExpense : Screen("add_expense", "Aggiungi Spesa", Icons.Outlined.AddCircle)
    object AddIncome : Screen("add_income", "Aggiungi Entrata", Icons.Outlined.AddCircle)
    object Planning : Screen("planning", "Pianifica", Icons.Outlined.PieChart)
    object Essential : Screen("essential", "Costi fissi", Icons.Outlined.AccountBalanceWallet)
    object Subscriptions : Screen("subscriptions", "Ricorrenti", Icons.Outlined.Autorenew)
    object EventFund : Screen("event_fund", "Risparmi", Icons.Outlined.Celebration)
    object History : Screen("history", "Storico", Icons.Outlined.History)
    object Statistics : Screen("statistics", "Statistiche", Icons.Outlined.Analytics)
    object Settings : Screen("settings", "Impostazioni", Icons.Outlined.Settings)
    object NotificationLogs : Screen("notification_logs", "Log Notifiche", Icons.Outlined.List)
    object Refunds : Screen("refunds", "Rimborsi", Icons.Outlined.MoneyOff)
}

class MainActivity : FragmentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        setContent {
            BudgetControlTheme(darkTheme = true) {
                BudgetControlApp()
            }
        }
    }
}

@Composable
fun BudgetControlApp() {
    val context = LocalContext.current
    val database = remember(context) { AppDatabase.getDatabase(context) }
    val repository = remember(database) { BudgetRepository(database.budgetDao()) }
    val viewModel: BudgetViewModel = viewModel(factory = BudgetViewModelFactory(repository, context.applicationContext as android.app.Application))

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingTransaction by viewModel.pendingTransaction.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    com.example.ui.OnLifecycleEvent { _, event ->
        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
            viewModel.refreshTime()
        }
    }

    // Weekly Google Drive Auto-Backup check on app launch
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.checkAndPerformWeeklyAutoBackup(context)
    }

    // Threshold Alert Logic
    val previousRemaining = remember { androidx.compose.runtime.mutableStateOf<Double?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(uiState.totalMonthlySpendable, uiState.dailyBudget) {
        val currentRemaining = uiState.discretionaryVariableRemaining
        val totalAvailable = uiState.variableBudgetAvailable
        val prev = previousRemaining.value
        
        com.example.ui.widget.BudgetWidgetProvider.updateAllWidgets(context)
        com.example.ui.widget.BudgetVerticalWidgetProvider.updateAllWidgets(context)
        
        if (prev != null && totalAvailable > 0) {
            val threshold20 = totalAvailable * 0.20
            
            // crossed 20% downwards
            if (prev > threshold20 && currentRemaining <= threshold20 && currentRemaining > 0) {
                com.example.utils.NotificationHelper.sendLowBalanceNotification(
                    context, 
                    "Attenzione: Saldo Sotto il 20%", 
                    "Ti resta solo il 20% del budget discrezionale (${String.format(java.util.Locale.ITALY, "%.2f", currentRemaining)} €)."
                )
            }
            
            // crossed 0 downwards
            if (prev > 0 && currentRemaining <= 0) {
                com.example.utils.NotificationHelper.sendLowBalanceNotification(
                    context, 
                    "Budget Esaurito!", 
                    "Hai esaurito il budget discrezionale per questo ciclo."
                )
            }
        }
        previousRemaining.value = currentRemaining
    }

    LaunchedEffect(pendingTransaction) {
        if (pendingTransaction != null) {
            val destination = "${Screen.AddExpense.route}?accountType=DISCREZIONALE_VARIABILE"
            navController.navigate(destination) {
                launchSingleTop = true
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = listOf(
        Screen.Dashboard,
        Screen.Essential,
        Screen.Planning,
        Screen.AddExpense
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {}
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {

                val emphasizedEasing = FastOutSlowInEasing
        val defaultDuration = 500
                val bottomSheetDuration = 600

        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(defaultDuration, easing = emphasizedEasing)
                ) + fadeIn(animationSpec = tween(defaultDuration, easing = emphasizedEasing))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(defaultDuration, easing = emphasizedEasing)
                ) + fadeOut(animationSpec = tween(defaultDuration, easing = emphasizedEasing))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(defaultDuration, easing = emphasizedEasing)
                ) + fadeIn(animationSpec = tween(defaultDuration, easing = emphasizedEasing))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(defaultDuration, easing = emphasizedEasing)
                ) + fadeOut(animationSpec = tween(defaultDuration, easing = emphasizedEasing))
            }
        ) {
            composable(Screen.Dashboard.route) {
                com.example.ui.screens.DashboardScreen(
                    state = uiState,
                    onNavigateToAddExpense = { navController.navigate(Screen.AddExpense.route) },
                    onNavigateToAddIncome = { navController.navigate(Screen.AddIncome.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToEventFund = { navController.navigate(Screen.EventFund.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToRefunds = { navController.navigate(Screen.Refunds.route) },
                    onUpdateAvatar = { uri -> viewModel.updateAvatarUri(uri) },
                    onDismissMonthlySummary = { cycleStart -> viewModel.dismissMonthlySummary(cycleStart) },
                    onNavigateToEditExpense = { expenseId -> navController.navigate("${Screen.AddExpense.route}?editId=$expenseId") },
                    onDeleteExpense = { exp -> viewModel.deleteExpense(exp) },
                    onToggleInvestment = { cycleId, isDone -> viewModel.toggleInvestmentDone(cycleId, isDone) }
                )
            }

            composable(
                route = Screen.AddIncome.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing)
                    ) + fadeIn(animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing)
                    ) + fadeOut(animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing))
                }
            ) {
                AddIncomeScreen(
                    state = uiState,
                    onSaveIncome = { accountType, amount, note, merchant ->
                        viewModel.addExpense(
                            accountType = accountType,
                            amount = amount,
                            category = "ENTRATA",
                            note = note,
                            merchant = merchant,
                            isIncome = true
                        )
                    },
                    onSavedSuccess = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "${Screen.AddExpense.route}?accountType={accountType}&editId={editId}",
                arguments = listOf(
                    navArgument("accountType") { type = NavType.StringType; nullable = true },
                    navArgument("editId") { type = NavType.StringType; nullable = true }
                ),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing)
                    ) + fadeIn(animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing)
                    ) + fadeOut(animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing)
                    ) + fadeIn(animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing)
                    ) + fadeOut(animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing))
                }
            ) { backStackEntry ->
                val accountType = backStackEntry.arguments?.getString("accountType")
                val editIdStr = backStackEntry.arguments?.getString("editId")
                val editId = editIdStr?.toLongOrNull()
                
                AddExpenseScreen(
                    state = uiState,
                    initialAccountType = accountType,
                    expenseToEditId = editId,
                    pendingTransaction = pendingTransaction,
                    onClearPendingTransaction = { viewModel.clearPendingTransaction() },
                    onSaveExpense = { accType, amount, category, dateMillis, note, merchant, lat, lng, targetDateMillis, eventId, excludeFromStats, isNecessary, amortizationMonths, isRefundExpected, expectedRefundAmount, refundNote ->
                        if (amortizationMonths > 1) {
                            viewModel.addAmortizedExpense(
                                accountType = accType,
                                totalAmountEur = amount,
                                category = category,
                                note = note,
                                merchant = merchant,
                                latitude = lat,
                                longitude = lng,
                                amortizationMonths = amortizationMonths,
                                excludeFromStats = excludeFromStats,
                                isNecessary = isNecessary,
                                dateMillis = dateMillis
                            )
                        } else {
                            viewModel.addExpense(
                                accountType = accType,
                                amount = amount,
                                category = category,
                                dateMillis = dateMillis,
                                note = note,
                                merchant = merchant,
                                latitude = lat,
                                longitude = lng,
                                eventTargetDateMillis = targetDateMillis,
                                eventId = eventId,
                                excludeFromStats = excludeFromStats,
                                isNecessary = isNecessary,
                                isRefundExpected = isRefundExpected,
                                expectedRefundAmount = expectedRefundAmount,
                                refundNote = refundNote
                            )
                        }
                    },
                    onUpdateExpense = { id, accType, amount, category, dateMillis, note, merchant, lat, lng, excludeFromStats, isNecessary, isRefundExpected, expectedRefundAmount, refundNote ->
                        val existing = uiState.allExpenses.find { it.id == id }
                        if (existing != null) {
                            val updated = existing.copy(
                                accountType = accType,
                                amount = amount,
                                category = category,
                                dateMillis = dateMillis,
                                note = note,
                                merchant = merchant,
                                latitude = lat,
                                longitude = lng,
                                excludeFromStats = excludeFromStats,
                                isNecessary = isNecessary,
                                isRefundExpected = isRefundExpected,
                                expectedRefundAmount = expectedRefundAmount,
                                refundNote = refundNote
                            )
                            viewModel.updateExpense(updated)
                        }
                    },
                    onSavedSuccess = {
                        navController.navigateUp()
                    }
                )
            }

            composable(Screen.Subscriptions.route) {
                SubscriptionsScreen(
                    state = uiState,
                    onAddSubscription = { name, amount, dayOfMonth ->
                        viewModel.addSubscription(name, amount, dayOfMonth)
                    },
                    onUpdateSubscription = { sub ->
                        viewModel.updateSubscription(sub)
                    },
                    onToggleActive = { sub ->
                        viewModel.toggleSubscriptionActive(sub)
                    },
                    onDeleteSubscription = { sub ->
                        viewModel.deleteSubscription(sub)
                    }
                )
            }

            composable(Screen.Planning.route) {
                com.example.ui.screens.PlanningScreen(
                    state = uiState,
                    navController = navController,
                    viewModel = viewModel
                )
            }
            composable(Screen.EventFund.route) {
                EventFundScreen(
                    state = uiState,
                    onNavigateToAddExpenseWithAccount = { accountType ->
                        navController.navigate("${Screen.AddExpense.route}?accountType=$accountType")
                    },
                    onAddEvent = { name, budget, date ->
                        viewModel.addEvent(name, budget, date)
                    }
                )
            }

            composable(Screen.Essential.route) {
                com.example.ui.screens.EssentialScreen(
                    state = uiState,
                    onNavigateToAddExpenseWithAccount = { accountType ->
                        navController.navigate("${Screen.AddExpense.route}?accountType=$accountType")
                    },
                    onDeleteExpense = { exp ->
                        viewModel.deleteExpense(exp)
                    },
                    onNavigateToEditExpense = { expenseId ->
                        navController.navigate("${Screen.AddExpense.route}?editId=$expenseId")
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    state = uiState,
                    onDeleteExpense = { exp ->
                        viewModel.deleteExpense(exp)
                    },
                    onNavigateToEditExpense = { expenseId -> navController.navigate("${Screen.AddExpense.route}?editId=$expenseId") },
                    onNavigateToStatistics = { navController.navigate(Screen.Statistics.route) }
                )
            }

            composable(
                route = Screen.Statistics.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(defaultDuration, easing = emphasizedEasing)
                    ) + fadeIn(animationSpec = tween(defaultDuration, easing = emphasizedEasing))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(defaultDuration, easing = emphasizedEasing)
                    ) + fadeOut(animationSpec = tween(defaultDuration, easing = emphasizedEasing))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(defaultDuration, easing = emphasizedEasing)
                    ) + fadeIn(animationSpec = tween(defaultDuration, easing = emphasizedEasing))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(defaultDuration, easing = emphasizedEasing)
                    ) + fadeOut(animationSpec = tween(defaultDuration, easing = emphasizedEasing))
                }
            ) {
                StatisticsScreen(
                    state = uiState,
                    onNavigateUp = { navController.navigateUp() },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) }
                )
            }

            composable(Screen.Refunds.route) {
                com.example.ui.screens.RefundsScreen(
                    state = uiState,
                    onProcessRefund = { expense, amount ->
                        viewModel.processRefund(expense, amount)
                    },
                    onCancelRefund = { expense ->
                        viewModel.cancelRefund(expense)
                    }
                )
            }

            composable(
                route = Screen.Settings.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing)
                    ) + fadeIn(animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing)
                    ) + fadeOut(animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing)
                    ) + fadeIn(animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing)
                    ) + fadeOut(animationSpec = tween(bottomSheetDuration, easing = emphasizedEasing))
                }
            ) {
                SettingsScreen(
                    state = uiState,
                    onUpdateSettings = { resetDay, monthlyCap, liquidity, investments, geminiApiKey, netMonthlyIncome, essentialBaseline, monthlyInvestmentTarget, budgetPersonale, budgetGinevra, allowedNotificationApps ->
                        viewModel.updateSettings(resetDay, monthlyCap, liquidity, investments, geminiApiKey, netMonthlyIncome, essentialBaseline, monthlyInvestmentTarget, budgetPersonale, budgetGinevra, allowedNotificationApps)
                    },
                    onAddCategory = { name, targetAccount ->
                        viewModel.addCategory(name, targetAccount)
                    },
                    onDeleteCategory = { cat ->
                        viewModel.deleteCategory(cat)
                    },
                    onPerformReallocation = { fromAcc, toAcc, amount, reason ->
                        viewModel.performExplicitReallocation(fromAcc, toAcc, amount, reason)
                    },
                    onExportCsv = { ctx ->
                        viewModel.exportCsvData(ctx)
                    },
                    onSimulateNotification = { merchant, amount, currency, appName ->
                        viewModel.simulateNotificationDetection(merchant, amount, currency, appName)
                    },
                    onTriggerGoogleDriveBackup = { ctx ->
                        viewModel.performGoogleDriveBackup(ctx)
                    },
                    onRestoreFromGoogleDrive = { ctx, onDone ->
                        viewModel.restoreFromGoogleDrive(ctx, onDone)
                    },
                    onToggleAutoBackup = { enabled ->
                        viewModel.setGoogleDriveAutoBackupEnabled(enabled)
                    },
                    onDisconnectGoogleDrive = { ctx ->
                        viewModel.disconnectGoogleDrive(ctx)
                    },
                    onUpdateGoogleAccount = { email ->
                        viewModel.setGoogleDriveAccount(email)
                    },
                    onExportJsonBackup = { ctx ->
                        viewModel.shareBackupJson(ctx)
                    },
                    onImportJsonBackup = { json, onDone ->
                        viewModel.importBackupFromJsonString(json, onDone)
                    },
                    onClearBackupMessage = {
                        viewModel.clearBackupStatusMessage()
                    },
                    onClearDriveAuthIntent = {
                        viewModel.clearDriveAuthIntent()
                    },
                    onNavigateToLogs = { navController.navigate(Screen.NotificationLogs.route) },
                    onNavigateUp = { navController.navigateUp() }
                )
            }
            
            composable(
                route = Screen.NotificationLogs.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    ) + fadeIn()
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    ) + fadeOut()
                }
            ) {
                com.example.ui.screens.NotificationLogsScreen(
                    viewModel = viewModel,
                    onNavigateUp = { navController.navigateUp() }
                )
            }
        }
            
// Custom Floating Bottom Bar
            // Let's implement the floating bar
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 24.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        val selectedColor = MaterialTheme.colorScheme.onPrimary
                        val unselectedColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        IconButton(
                            onClick = {
                                val destination = if (screen == Screen.AddExpense) "${Screen.AddExpense.route}?accountType=DISCREZIONALE_VARIABILE" else screen.route
                                if (currentRoute != destination && currentRoute != screen.route) {
                                    val popped = navController.popBackStack(destination, false)
                                    if (!popped) {
                                        navController.navigate(destination) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) selectedColor else unselectedColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        } // Close Box
    } // Close Scaffold innerPadding
}
