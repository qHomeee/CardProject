package com.example.cartproject.domain.study

import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import java.time.Instant

/** Один ключ на каждую карточку занятия. */
data class ReviewKey(val sessionId: StudySessionId, val cardId: LocalCardId)

/** Подтверждённая оценка карточки. */
data class ReviewEvent(
    val key: ReviewKey,
    val deckId: LocalDeckId,
    val cardRevision: Long,
    val grade: ReviewGrade,
    val gradedAt: Instant,
)

/** Оценка со сроком, который был сохранён вместе с ней. */
data class SavedReview(val event: ReviewEvent, val schedule: ReviewSchedule)

/** Предложенная оценка и версия исходного расписания. */
data class ReviewWrite(val result: SavedReview, val expectedScheduleVersion: Long)

/** Доступ к карточкам для занятия, расписанию и истории оценок. */
interface ReviewStore {
    /** Читает расписания набора. Отсутствующий набор возвращает DeckNotFound. */
    suspend fun scheduledCards(deckId: LocalDeckId): Outcome<List<ScheduledCard>>

    /** Возвращает актуальные версии карточки и расписания. */
    suspend fun readScheduledCard(deckId: LocalDeckId, cardId: LocalCardId): Outcome<ScheduledCard>

    /** Ищет уже подтверждённую оценку независимо от текущего расписания. */
    suspend fun findReview(key: ReviewKey): Outcome<SavedReview?>

    /**
     * В одной транзакции проверяет наличие записи по ключу и возвращает исходную
     * оценку при совпадении набора, ревизии и результата. При новой оценке
     * сравнивает версии карточки и расписания, затем записывает обе сущности.
     * Иной результат с тем же ключом или изменившиеся версии дают ReviewConflict.
     */
    suspend fun commitReview(write: ReviewWrite): Outcome<SavedReview>

    /** Читает все оценки или только оценки указанного занятия. */
    suspend fun grades(sessionId: StudySessionId? = null): Outcome<List<ReviewGrade>>
}
