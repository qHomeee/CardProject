package com.example.cartproject.domain.study

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.LocalCardId
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверяет подготовку занятия, сохранение оценки и подсчёт результатов. */
class StudyUseCasesTest {
    private val store = ReviewStoreFake(listOf(sampleCard(1)))
    private val remembered = ReviewGrade.Remembered
    private val retry = ReviewGrade.NeedsReview

    private suspend fun session(): StudySession = (
        (
            StartStudy(store) {
                startTime
            }(studyDeck, firstSession) as Outcome.Success
            ).value as StudyStart.Ready
        ).session

    /** Равный срок включается, очередь сортируется и остаётся неизменной. */
    @Test
    fun queueIsSortedInclusiveAndFrozen() = runTest {
        store.cards += sampleCard(4, startTime.plusMillis(1))
        store.cards += sampleCard(3, startTime.minusSeconds(20))
        store.cards += sampleCard(2)
        store.cards += sampleCard(1)
        var reads = 0
        val useCase = StartStudy(store) {
            reads++
            startTime
        }
        val result = useCase(studyDeck, firstSession) as Outcome.Success
        val study = (result.value as StudyStart.Ready).session
        store.cards.clear()

        assertEquals(1, reads)
        assertEquals(startTime, study.startedAt)
        assertEquals(listOf(3L, 1L, 2L), study.cards.map { it.id.value })
        assertEquals("Ответ 3", study.cards.first().answer)
    }

    /** При отсутствии готовых карточек возвращается следующий срок либо пустой набор. */
    @Test
    fun emptyQueueReportsClosestFutureCard() = runTest {
        val next = startTime.plusSeconds(5)
        store.cards.clear()
        val useCase = StartStudy(store) { startTime }
        assertEquals(Outcome.Success(StudyStart.Empty(null)), useCase(studyDeck, firstSession))
        store.cards += sampleCard(2, startTime.plusSeconds(10))
        store.cards += sampleCard(3, next)
        assertEquals(Outcome.Success(StudyStart.Empty(next)), useCase(studyDeck, firstSession))
    }

    /** Ошибка чтения отличается от пустого набора. */
    @Test
    fun queuePropagatesReadFailure() = runTest {
        store.readsError = AppError.LocalRead
        assertEquals(
            Outcome.Failure(AppError.LocalRead),
            StartStudy(store) {
                startTime
            }(studyDeck, firstSession),
        )
    }

    /** Повтор команды возвращает первоначальное время и сохранённый срок. */
    @Test
    fun identicalReviewReturnsOriginalAndDifferentGradeConflicts() = runTest {
        var now = startTime
        val useCase = GradeCard(store) { now }
        val study = session()
        val cardId = study.cards.single().id
        val first = useCase(study, cardId, remembered) as Outcome.Success
        now = now.plusSeconds(500)
        assertEquals(first, useCase(study, cardId, remembered))
        assertEquals(Outcome.Failure(AppError.ReviewConflict), useCase(study, cardId, retry))
        assertEquals(1, store.writes)
        assertEquals(1, store.saved.size)
        assertEquals(startTime.plusSeconds(86_400), first.value.schedule.dueAt)
    }

    /** После ошибки без записи команда допускает повтор. */
    @Test
    fun failedWriteLeavesScheduleAndAllowsRetry() = runTest {
        val useCase = GradeCard(store) { startTime }
        val study = session()
        val cardId = study.cards.single().id
        store.rejectWrite = true
        assertEquals(Outcome.Failure(AppError.LocalWrite), useCase(study, cardId, retry))
        assertEquals(ReviewSchedule.initial(startTime), store.cards.single().schedule)
        assertTrue(store.saved.isEmpty())
        store.rejectWrite = false
        assertTrue(useCase(study, cardId, retry) is Outcome.Success)
        assertEquals(1, store.saved.size)
    }

    /** При потерянном подтверждении повтор не создаёт вторую запись. */
    @Test
    fun retryAfterLostConfirmationReturnsPersistedReview() = runTest {
        val useCase = GradeCard(store) { startTime }
        val study = session()
        val cardId = study.cards.single().id
        store.loseConfirmation = true
        assertEquals(Outcome.Failure(AppError.LocalWrite), useCase(study, cardId, remembered))
        assertEquals(
            Outcome.Success(store.saved.values.single()),
            useCase(study, cardId, remembered),
        )
        assertEquals(1, store.writes)
    }

    /** Тот же ответ может подтвердиться между поиском оценки и чтением срока. */
    @Test
    fun concurrentIdenticalReviewIsFoundAfterScheduleChanges() = runTest {
        val study = session()
        val cardId = study.cards.single().id
        val current = store.cards.single()
        val schedule = ScheduleRule.next(current.schedule, remembered, startTime)
        val event = ReviewEvent(ReviewKey(study.id, cardId), study.deckId, 0, remembered, startTime)
        val result = SavedReview(event, schedule)
        store.beforeReadCard = {
            store.saved[event.key] = result
            store.cards[0] = current.copy(schedule = schedule)
        }
        assertEquals(
            Outcome.Success(result),
            GradeCard(store) {
                startTime
            }(study, cardId, remembered),
        )
        assertEquals(0, store.writes)
    }

    /** Версия контролируется и при изменении между чтением и записью. */
    @Test
    fun changedVersionDuringCommitDoesNotOverwriteSchedule() = runTest {
        val study = session()
        val cardId = study.cards.single().id
        val current = store.cards.single()
        val revised = current.copy(schedule = current.schedule.copy(version = 1))
        store.beforeCommit = { store.cards[0] = revised }
        assertEquals(
            Outcome.Failure(AppError.ReviewConflict),
            GradeCard(store) {
                startTime
            }(study, cardId, retry),
        )
        assertEquals(revised, store.cards.single())
        assertTrue(store.saved.isEmpty())
    }

    /** Изменившийся текст и отсутствующая в очереди карточка исключают запись. */
    @Test
    fun rejectsChangedCardAndUnknownCard() = runTest {
        val study = session()
        val card = store.cards.single()
        store.cards[0] = card.copy(card = card.card.copy(question = "Уточнение", revision = 1))
        val useCase = GradeCard(store) { startTime }
        assertEquals(
            Outcome.Failure(AppError.ReviewConflict),
            useCase(study, card.card.id, remembered),
        )
        assertEquals(
            Outcome.Failure(AppError.CardNotFound),
            useCase(study, LocalCardId(9), remembered),
        )
        assertEquals(0, store.writes)
    }

    /** Итог учитывает оценки той же карточки повторно и округляет процент. */
    @Test
    fun statisticsCountRepeatsAndFilterBySession() = runTest {
        for (index in 1..4) {
            val id = StudySessionId(UUID(0, index.toLong()))
            val key = ReviewKey(id, LocalCardId(1))
            val grade = if (index == 1) retry else remembered
            val event = ReviewEvent(key, studyDeck, 0, grade, startTime)
            store.saved[key] = SavedReview(event, ReviewSchedule.initial(startTime))
        }
        val useCase = ReadStatistics(store)
        val all = (useCase() as Outcome.Success).value
        assertEquals(4, all.total)
        assertEquals(StudySummary(3, 1), all)
        assertEquals(75.0, all.rememberedPercent)
        assertEquals(Outcome.Success(StudySummary(0, 1)), useCase(firstSession))
        assertEquals(66.7, StudySummary(2, 1).rememberedPercent)
        assertEquals(6.3, StudySummary(1, 15).rememberedPercent)
    }

    /** Пустая история отличается от нулевого процента и от ошибки. */
    @Test
    fun statisticsSeparateEmptyHistoryAndReadFailure() = runTest {
        val useCase = ReadStatistics(store)
        assertEquals(null, (useCase() as Outcome.Success).value.rememberedPercent)
        store.readsError = AppError.LocalRead
        assertEquals(Outcome.Failure(AppError.LocalRead), useCase())
    }
}
