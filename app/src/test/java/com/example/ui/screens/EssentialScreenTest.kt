package com.example.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.BudgetUiState
import com.example.data.ExpenseEntity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class EssentialScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun launchEssentialScreen_withItems_noCrash() {
        val stateWithItems = BudgetUiState(
            essentialExpensesInCycle = listOf(
                ExpenseEntity(id = 1, accountType = "ESSENZIALE", amount = 50.0, category = "Spesa", dateMillis = System.currentTimeMillis())
            )
        )
        composeTestRule.setContent {
            EssentialScreen(
                state = stateWithItems,
                onNavigateToAddExpense = {},
                onToggleInvestment = { _, _ -> }
            )
        }
    }
}
