package com.example.cartproject

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Проверяет стартовый маршрут и восстановление вкладки через MainActivity. */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startsAtCatalogAndRestoresSettingsTabAfterRecreation() {
        tab("Каталог").assertIsSelected()
        composeRule.onNodeWithContentDescription("Поиск наборов").assertIsDisplayed()
        tab("Настройки").performClick()
        tab("Настройки").assertIsSelected()
        composeRule.activityRule.scenario.recreate()
        tab("Настройки").assertIsSelected()
        pressBack()
        tab("Каталог").assertIsSelected()
        composeRule.onNodeWithContentDescription("Поиск наборов").assertIsDisplayed()
    }

    private fun tab(name: String) = composeRule.onNode(hasText(name) and isSelectable())
}
