package com.example.cartproject

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.DeleteOwnMaterial
import com.example.cartproject.domain.decks.SaveOwnCard
import com.example.cartproject.domain.decks.SaveOwnDeck
import com.example.cartproject.domain.settings.ObserveSettings
import com.example.cartproject.domain.settings.SetStudyGestures
import com.example.cartproject.domain.study.ReviewStore
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class OfflineStudyActivityTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun keepsOpenAnswerOnActivityRecreationAndCompletesWithButtons() {
        val services = GlobalContext.get()
        val saveDeck = services.get<SaveOwnDeck>()
        val saveCard = services.get<SaveOwnCard>()
        val delete = services.get<DeleteOwnMaterial>()
        val reviews = services.get<ReviewStore>()
        val setGestures = services.get<SetStudyGestures>()
        val settings = services.get<ObserveSettings>()
        val previous = runBlocking { (settings().first() as Outcome.Success).value.gesturesEnabled }
        val title = "Занятие ${UUID.randomUUID().toString().take(8)}"
        val deck = runBlocking { (saveDeck(null, title, "") as Outcome.Success).value }
        try {
            runBlocking { setGestures(false) }
            val first = runBlocking {
                (saveCard(deck, null, "Вопрос А", "Ответ А") as Outcome.Success).value
            }
            val second = runBlocking {
                (saveCard(deck, null, "Вопрос Б", "Ответ Б") as Outcome.Success).value
            }
            rule.onNode(hasText("Мои наборы") and isSelectable()).performClick()
            tap(title)
            tap("Начать занятие")
            waitFor("Вопрос А")
            rule.onNodeWithText("Ответ А").assertDoesNotExist()
            rule.onNodeWithText("Вспомнил").assertDoesNotExist()
            waitFor("Жесты выключены")
            tap("Показать ответ")
            waitFor("Ответ А")
            rule.activityRule.scenario.recreate()
            waitFor("Ответ А")
            tap("Вспомнил")

            waitFor("Вопрос Б")
            tap("Выйти из занятия")
            waitFor("Выйти из занятия?")
            tap("Отмена")
            tap("Показать ответ")
            tap("Нужно повторить")
            waitFor("Занятие завершено")
            waitFor("Вспомнил 1 из 2")
            waitFor("Нужно повторить 1 из 2")

            val firstSchedule = runBlocking { reviews.readScheduledCard(deck, first) }
            val secondSchedule = runBlocking { reviews.readScheduledCard(deck, second) }
            assertEquals(1, (firstSchedule as Outcome.Success).value.schedule.stage)
            assertEquals(1L, (secondSchedule as Outcome.Success).value.schedule.version)
            tap("Вернуться к набору")
            tap("Начать занятие")
            waitFor("Пока нечего повторять")
        } finally {
            runBlocking {
                delete.deck(deck)
                setGestures(previous)
            }
        }
    }

    private fun tap(text: String) {
        waitFor(text)
        rule.onNodeWithText(text).performClick()
    }

    private fun waitFor(text: String) {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(text).assertIsDisplayed()
    }
}
