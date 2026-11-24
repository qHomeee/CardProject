package com.example.cartproject

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Проверяет открытие стартового экрана на Android. */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Дожидается, когда приложение покажет пустой локальный список. */
    @Test
    fun opensMyDecksInEmptyState() {
        val title = composeRule.activity.getString(R.string.my_decks_title)
        val emptyMessage = composeRule.activity.getString(R.string.my_decks_empty)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(emptyMessage).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithText(emptyMessage).assertIsDisplayed()
    }
}
