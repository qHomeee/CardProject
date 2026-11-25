package com.example.cartproject.data.local

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cartproject.MainActivity
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.DeleteOwnMaterial
import com.example.cartproject.domain.decks.SaveOwnDeck
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Проверяет связь экрана с настоящим Room и графом приложения. */
@RunWith(AndroidJUnit4::class)
class SavedDeckActivityTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    /** Экран видит запись Room и поднимает её после пересоздания Activity. */
    @Test
    fun displaysSavedDeckAfterActivityRecreation() {
        val koin = GlobalContext.get()
        val save = koin.get<SaveOwnDeck>()
        val delete = koin.get<DeleteOwnMaterial>()
        val title = "Офлайн набор ${UUID.randomUUID().toString().take(6)}"
        val id = runBlocking { (save(null, title, "Проверка Room") as Outcome.Success).value }
        try {
            rule.onNode(hasText("Мои наборы") and isSelectable()).performClick()
            waitForTitle(title)
            rule.activityRule.scenario.recreate()
            waitForTitle(title)
        } finally {
            runBlocking { assertEquals(Outcome.Success(Unit), delete.deck(id)) }
        }
    }

    private fun waitForTitle(title: String) {
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(title).assertIsDisplayed()
    }
}
