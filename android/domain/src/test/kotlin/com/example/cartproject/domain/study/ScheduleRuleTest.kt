package com.example.cartproject.domain.study

import com.example.cartproject.domain.decks.Card
import com.example.cartproject.domain.decks.CardText
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** Проверяет интервалы и сброс расписания. */
class ScheduleRuleTest {
    private val now = Instant.parse("2026-03-28T12:00:00Z")
    private val card = Card(LocalCardId(1), LocalDeckId(1), "Вопрос", "Ответ", revision = 2)

    /** Новая карточка попадает в ближайшее занятие. */
    @Test
    fun newCardIsDueImmediately() {
        assertEquals(ReviewSchedule(0, now, 0), ReviewSchedule.initial(now))
    }

    /** Каждая успешная оценка получает свой интервал в сутках по 24 часа. */
    @Test
    fun rememberedAtEveryStage() {
        val days = listOf(1L, 3L, 7L, 14L, 30L, 30L)
        days.forEachIndexed { stage, interval ->
            val expected =
                ReviewSchedule((stage + 1).coerceAtMost(5), now.plus(Duration.ofDays(interval)), 8)
            val next = ScheduleRule.next(
                ReviewSchedule(stage, now.minusSeconds(60), 7),
                ReviewGrade.Remembered,
                now,
            )
            assertEquals("stage=$stage", expected, next)
        }
    }

    /** Сбой на любой стадии сбрасывает прогресс, следующий успех даёт сутки. */
    @Test
    fun needsReviewAtEveryStageAndThenRemembered() {
        val retryAt = now.plusSeconds(600)
        for (stage in 0..5) {
            val retry = ScheduleRule.next(
                ReviewSchedule(stage, now, 7),
                ReviewGrade.NeedsReview,
                now,
            )
            assertEquals(ReviewSchedule(0, retryAt, 8), retry)
            assertEquals(
                ReviewSchedule(1, retryAt.plusSeconds(86_400), 9),
                ScheduleRule.next(retry, ReviewGrade.Remembered, retryAt),
            )
        }
    }

    /** Правка сбрасывает только расписание изменённой карточки. */
    @Test
    fun editResetsScheduleAndIncrementsRevision() {
        val original = ScheduledCard(card, ReviewSchedule(4, now.plusSeconds(600), 6))
        val updated = original.revise(CardText("Новый вопрос", "Ответ"), now)
        assertEquals(card.copy(question = "Новый вопрос", revision = 3), updated.card)
        assertEquals(ReviewSchedule(0, now, 7), updated.schedule)
    }

    /** Повторное сохранение прежнего текста сохраняет расписание. */
    @Test
    fun unchangedTextKeepsScheduleAndRevision() {
        val original = ScheduledCard(card, ReviewSchedule(3, now.plusSeconds(600), 6))
        assertSame(original, original.revise(CardText(card.question, card.answer), now))
    }

    /** Неверная стадия отвергается на границе модели. */
    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeStage() {
        ReviewSchedule(6, now)
    }
}
