package com.example.cartproject.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.study.StudySummary
import com.example.cartproject.presentation.statistics.StatisticsScreen
import com.example.cartproject.presentation.theme.ZapomniTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Проверяет пустую историю и действие повтора после ошибки. */
@RunWith(AndroidJUnit4::class)
class StatisticsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun retriesAndShowsEmptyHistory() {
        val state = mutableStateOf<Outcome<StudySummary>?>(Outcome.Failure(AppError.LocalRead))
        rule.setContent {
            ZapomniTheme {
                StatisticsScreen(
                    state.value,
                    onRetry = { state.value = Outcome.Success(StudySummary(0, 0)) },
                    onBack = {},
                )
            }
        }

        rule.onNodeWithText("Не удалось прочитать историю занятий").assertIsDisplayed()
        rule.onNodeWithText("Повторить").performClick()
        rule.onNodeWithText("Нет данных").assertIsDisplayed()
        rule.onNodeWithText("0,0%", substring = true).assertDoesNotExist()
    }
}
