package com.example.cartproject.presentation.catalog

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.cartproject.domain.AppError
import com.example.cartproject.presentation.theme.ZapomniTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CatalogScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun offlineLetsUserOpenLocalDecksAndRetry() {
        var attempts = 0
        var opened = false
        rule.setContent {
            ZapomniTheme {
                CatalogScreen(
                    state = CatalogUiState("", CatalogResult.Offline),
                    onQueryChange = {},
                    onOpenDeck = {},
                    onRetry = { attempts++ },
                    onOpenDecks = { opened = true },
                )
            }
        }
        rule.onNodeWithText("Нет интернета").assertIsDisplayed()
        rule.onNodeWithText("Повторить").performClick()
        rule.onNodeWithText("Открыть мои наборы").performClick()
        rule.runOnIdle {
            assertEquals(1, attempts)
            assertEquals(true, opened)
        }
    }

    @Test
    fun failedRequestCanBeRetriedAndEmptySearchShowsItsOwnMessage() {
        val result = mutableStateOf<CatalogResult>(CatalogResult.Error(AppError.CatalogUnavailable))
        rule.setContent {
            ZapomniTheme(darkTheme = true) {
                CatalogScreen(
                    state = CatalogUiState("Планеты", result.value),
                    onQueryChange = {},
                    onOpenDeck = {},
                    onRetry = { result.value = CatalogResult.Content(emptyList()) },
                    onOpenDecks = {},
                )
            }
        }
        rule.onNodeWithText("Не удалось загрузить каталог").assertIsDisplayed()
        rule.onNodeWithText("Повторить").performClick()
        rule.onNodeWithText("Ничего не найдено").assertIsDisplayed()
        rule.onNodeWithText("Планеты").assertIsDisplayed()
    }
}
