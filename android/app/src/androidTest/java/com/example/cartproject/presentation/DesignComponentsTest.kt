package com.example.cartproject.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cartproject.presentation.components.StudyCard
import com.example.cartproject.presentation.components.ZapomniButton
import com.example.cartproject.presentation.components.ZapomniNavigationBar
import com.example.cartproject.presentation.components.ZapomniSwitch
import com.example.cartproject.presentation.components.ZapomniTab
import com.example.cartproject.presentation.components.ZapomniTextField
import com.example.cartproject.presentation.theme.ZapomniTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Проверяет ввод, блокировку повторного действия и семантику элементов. */
@RunWith(AndroidJUnit4::class)
class DesignComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Кнопка недоступна во время сохранения. */
    @Test
    fun savingDisablesActionUntilCompletion() {
        val saving = mutableStateOf(true)
        var actions = 0
        composeRule.setContent {
            ZapomniTheme {
                ZapomniButton("Сохранить", { actions++ }, loading = saving.value)
            }
        }

        composeRule.onNodeWithText("Сохранить").assertIsNotEnabled().performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(0, actions) }
        composeRule.runOnIdle { saving.value = false }
        composeRule.onNodeWithText("Сохранить").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(1, actions) }
    }

    /** Поле сообщает об ошибке и сохраняет введённое значение. */
    @Test
    fun textFieldExposesErrorAndKeepsText() {
        val value = mutableStateOf("")
        val label = "Название"
        val message = "Слишком длинное название"
        composeRule.setContent {
            ZapomniTheme {
                ZapomniTextField(
                    value = value.value,
                    onValueChange = { value.value = it },
                    label = label,
                    errorMessage = message,
                )
            }
        }

        composeRule.onNodeWithContentDescription(label)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, message))
            .performTextInput("Столицы")
        composeRule.onNodeWithContentDescription(label).assertTextContains("Столицы")
        composeRule.runOnIdle { assertEquals("Столицы", value.value) }
    }

    /** Нажатие меняет состояние переключателя и выбранную вкладку. */
    @Test
    fun switchAndTabExposeSelection() {
        val checked = mutableStateOf(false)
        val tab = mutableStateOf(ZapomniTab.Decks)
        composeRule.setContent {
            ZapomniTheme(darkTheme = true) {
                Column {
                    ZapomniSwitch("Жесты", checked.value, { checked.value = it })
                    ZapomniNavigationBar(tab.value, { tab.value = it }, Modifier.fillMaxWidth())
                }
            }
        }

        composeRule.onNodeWithText("Жесты").assertIsOff().performClick().assertIsOn()
        composeRule.onNodeWithText("Мои наборы").assertIsSelected()
        composeRule.onNodeWithText("Настройки").performClick().assertIsSelected()
    }

    /** Ответ появляется в карточке после передачи значения. */
    @Test
    fun studyCardShowsAnswerOnlyWhenRevealed() {
        val answer = mutableStateOf<String?>(null)
        composeRule.setContent {
            ZapomniTheme {
                StudyCard("Столица Португалии", position = 1, total = 12, answer = answer.value)
            }
        }

        composeRule.onNodeWithText("Лиссабон").assertDoesNotExist()
        composeRule.runOnIdle { answer.value = "Лиссабон" }
        composeRule.onNodeWithText("Лиссабон").assertExists()
    }
}
