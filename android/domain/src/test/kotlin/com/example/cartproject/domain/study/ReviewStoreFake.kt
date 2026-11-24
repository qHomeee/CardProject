package com.example.cartproject.domain.study

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.Card
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import java.time.Instant
import java.util.UUID

internal val startTime: Instant = Instant.parse("2026-11-24T10:00:00Z")
internal val studyDeck = LocalDeckId(1)
internal val firstSession = StudySessionId(UUID(0, 1))

internal fun sampleCard(id: Long, dueAt: Instant = startTime): ScheduledCard = ScheduledCard(
    Card(LocalCardId(id), studyDeck, "Вопрос $id", "Ответ $id"),
    ReviewSchedule.initial(dueAt),
)

/** Позволяет подменить результат чтения или поведение атомарной записи. */
internal class ReviewStoreFake(cards: List<ScheduledCard> = emptyList()) : ReviewStore {
    val cards = cards.toMutableList()
    val saved = mutableMapOf<ReviewKey, SavedReview>()
    var readsError: AppError? = null
    var rejectWrite = false
    var loseConfirmation = false
    var beforeReadCard: () -> Unit = {}
    var beforeCommit: (ReviewWrite) -> Unit = {}
    var writes = 0
        private set

    override suspend fun scheduledCards(deckId: LocalDeckId): Outcome<List<ScheduledCard>> =
        readsError?.let { Outcome.Failure(it) } ?: Outcome.Success(cards)

    override suspend fun readScheduledCard(
        deckId: LocalDeckId,
        cardId: LocalCardId,
    ): Outcome<ScheduledCard> {
        beforeReadCard()
        return cards.firstOrNull { it.card.deckId == deckId && it.card.id == cardId }
            ?.let { Outcome.Success(it) } ?: Outcome.Failure(AppError.CardNotFound)
    }

    override suspend fun findReview(key: ReviewKey): Outcome<SavedReview?> =
        Outcome.Success(saved[key])

    override suspend fun commitReview(write: ReviewWrite): Outcome<SavedReview> {
        writes++
        beforeCommit(write)
        val result = write.result
        val event = result.event
        val previous = saved[event.key]
        if (previous != null) {
            return if (previous.event.copy(gradedAt = event.gradedAt) == event) {
                Outcome.Success(previous)
            } else {
                Outcome.Failure(AppError.ReviewConflict)
            }
        }
        if (rejectWrite) return Outcome.Failure(AppError.LocalWrite)
        val index = cards.indexOfFirst {
            it.card.id == event.key.cardId &&
                it.card.deckId == event.deckId
        }
        if (index < 0) return Outcome.Failure(AppError.CardNotFound)
        val current = cards[index]
        if (current.card.revision != event.cardRevision ||
            current.schedule.version != write.expectedScheduleVersion
        ) {
            return Outcome.Failure(AppError.ReviewConflict)
        }
        cards[index] = current.copy(schedule = result.schedule)
        saved[event.key] = result
        return if (loseConfirmation) {
            Outcome.Failure(
                AppError.LocalWrite,
            )
        } else {
            Outcome.Success(result)
        }
    }

    override suspend fun grades(sessionId: StudySessionId?): Outcome<List<ReviewGrade>> =
        readsError?.let { Outcome.Failure(it) } ?: Outcome.Success(
            saved.values.filter {
                sessionId == null || it.event.key.sessionId == sessionId
            }.map { it.event.grade },
        )
}
