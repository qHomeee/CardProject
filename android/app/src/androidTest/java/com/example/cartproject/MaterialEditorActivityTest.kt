package com.example.cartproject

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.DeleteOwnMaterial
import com.example.cartproject.domain.decks.LocalDeckStore
import com.example.cartproject.domain.decks.SaveOwnCard
import com.example.cartproject.domain.decks.SaveOwnDeck
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class MaterialEditorActivityTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createsDeckAndCardAndKeepsInputAfterValidationAndRecreation() {
        val title = uniqueTitle()
        try {
            openMyDecks()
            tap("Создать набор")
            input("Описание", "Черновик описания")
            tap("Сохранить набор")
            fieldError("Название", "Введите название")
            rule.activityRule.scenario.recreate()
            field("Описание").assertTextContains("Черновик описания")
            input("Название", title)
            tap("Сохранить набор")
            waitForText("Карточек пока нет")

            tap("Добавить карточку")
            input("Вопрос", "Что такое орбита?")
            tap("Сохранить карточку")
            fieldError("Ответ", "Введите ответ")
            field("Вопрос").assertTextContains("Что такое орбита?")
            input("Ответ", "Траектория движения тела.")
            tap("Сохранить карточку")
            waitForText("Траектория движения тела.")
            waitForText("Карточек: 1")
        } finally {
            val store = GlobalContext.get().get<LocalDeckStore>()
            val remove = GlobalContext.get().get<DeleteOwnMaterial>()
            runBlocking {
                val list = (store.observeDecks().first() as Outcome.Success).value
                list.firstOrNull { it.title == title }?.let { remove.deck(it.id) }
            }
        }
    }

    @Test
    fun editsOwnMaterialAndConfirmsDeletionOfCardAndDeck() {
        val services = GlobalContext.get()
        val saveDeck = services.get<SaveOwnDeck>()
        val saveCard = services.get<SaveOwnCard>()
        val remove = services.get<DeleteOwnMaterial>()
        val store = services.get<LocalDeckStore>()
        val title = uniqueTitle()
        val updatedTitle = "$title новая версия"
        val question = "Что такое черновик?"
        val id = runBlocking { (saveDeck(null, title, "") as Outcome.Success).value }
        try {
            runBlocking { saveCard(id, null, question, "Первый вариант.") }
            openMyDecks()
            tap(title)
            tap("Редактировать")
            input("Название", updatedTitle)
            tap(question)
            input("Ответ", "Текст для проверки.")
            tap("Сохранить карточку")
            waitForText("Текст для проверки.")
            field("Название").assertTextContains(updatedTitle)
            tap("Сохранить набор")
            waitForText(updatedTitle)
            waitForText("Текст для проверки.")

            tap("Редактировать")
            tap(question)
            cancelRemoval("Удалить карточку", "Удалить карточку?")
            field("Вопрос").assertTextContains(question)
            tap("Удалить карточку")
            tap("Удалить")
            tap("Сохранить набор")
            waitForText("Карточек пока нет")

            tap("Редактировать")
            cancelRemoval("Удалить набор", "Удалить набор?")
            field("Название").assertTextContains(updatedTitle)
            tap("Удалить набор")
            tap("Удалить")
            rule.onNode(hasText("Мои наборы") and isSelectable()).assertIsDisplayed()
            runBlocking { assertEquals(Outcome.Failure(AppError.DeckNotFound), store.readDeck(id)) }
        } finally {
            runBlocking { remove.deck(id) }
        }
    }

    private fun uniqueTitle(): String = "Набор ${UUID.randomUUID().toString().take(8)}"

    private fun openMyDecks() = rule.onNode(hasText("Мои наборы") and isSelectable()).performClick()

    private fun field(label: String) = rule.onNodeWithContentDescription(label)

    private fun input(label: String, value: String) {
        field(label).performTextReplacement(value)
        closeSoftKeyboard()
    }

    private fun fieldError(label: String, error: String) {
        field(label).assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, error))
    }

    private fun tap(label: String) {
        waitForText(label)
        rule.onNodeWithText(label).performClick()
    }

    private fun waitForText(label: String) {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(label).assertIsDisplayed()
    }

    private fun cancelRemoval(action: String, title: String) {
        tap(action)
        waitForText(title)
        tap("Отмена")
    }
}
