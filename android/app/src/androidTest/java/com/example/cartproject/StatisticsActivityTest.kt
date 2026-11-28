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
import com.example.cartproject.domain.study.GradeCard
import com.example.cartproject.domain.study.ReviewGrade
import com.example.cartproject.domain.study.StartStudy
import com.example.cartproject.domain.study.StudySessionId
import com.example.cartproject.domain.study.StudyStart
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Проверяет переход из «Моих наборов» к сохранённым оценкам. */
@RunWith(AndroidJUnit4::class)
class StatisticsActivityTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensHistoryAndShowsRussianPercentageAfterRecreation() {
        val services = GlobalContext.get()
        val saveDeck = services.get<SaveOwnDeck>()
        val saveCard = services.get<SaveOwnCard>()
        val startStudy = services.get<StartStudy>()
        val gradeCard = services.get<GradeCard>()
        val delete = services.get<DeleteOwnMaterial>()
        val id = runBlocking { (saveDeck(null, "История занятий", "") as Outcome.Success).value }
        try {
            runBlocking {
                repeat(4) { index ->
                    check(saveCard(id, null, "Вопрос ${index + 1}", "Ответ") is Outcome.Success)
                }
                val start = (startStudy(id, StudySessionId(UUID.randomUUID())) as Outcome.Success)
                    .value as StudyStart.Ready
                start.session.cards.forEachIndexed { index, card ->
                    val grade = if (index < 3) ReviewGrade.Remembered else ReviewGrade.NeedsReview
                    check(gradeCard(start.session, card.id, grade) is Outcome.Success)
                }
            }

            rule.onNode(hasText("Мои наборы") and isSelectable()).performClick()
            expect("Статистика")
            rule.onNodeWithText("Статистика").performClick()
            expect("75,0% оценок «Вспомнил»")
            rule.activityRule.scenario.recreate()
            expect("Всего оценок: 4")
            expect("Вспомнил 3 из 4")
            expect("Нужно повторить 1 из 4")
        } finally {
            runBlocking { delete.deck(id) }
        }
    }

    private fun expect(label: String) {
        rule.waitUntil(5_000) { rule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(label).assertIsDisplayed()
    }
}
