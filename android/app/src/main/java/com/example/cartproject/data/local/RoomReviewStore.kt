package com.example.cartproject.data.local

import androidx.room.withTransaction
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.study.ReviewGrade
import com.example.cartproject.domain.study.ReviewKey
import com.example.cartproject.domain.study.ReviewStore
import com.example.cartproject.domain.study.ReviewWrite
import com.example.cartproject.domain.study.SavedReview
import com.example.cartproject.domain.study.ScheduledCard
import com.example.cartproject.domain.study.StudySessionId

/** Читает расписания и сохраняет оценки вместе с результатом пересчёта. */
internal class RoomReviewStore(private val database: ZapomniDatabase) : ReviewStore {
    private val reviews = database.reviews()
    private val decks = database.decks()

    override suspend fun scheduledCards(deckId: LocalDeckId): Outcome<List<ScheduledCard>> =
        sqliteOutcome(AppError.LocalRead) {
            database.withTransaction {
                if (decks.deck(deckId.value) == null) {
                    return@withTransaction Outcome.Failure(AppError.DeckNotFound)
                }
                Outcome.Success(
                    reviews.scheduledCards(deckId.value).map(ScheduledCardRow::toDomain),
                )
            }
        }

    override suspend fun readScheduledCard(
        deckId: LocalDeckId,
        cardId: LocalCardId,
    ): Outcome<ScheduledCard> = sqliteOutcome(AppError.LocalRead) {
        val row = reviews.scheduledCard(deckId.value, cardId.value)
        if (row == null) Outcome.Failure(AppError.CardNotFound) else Outcome.Success(row.toDomain())
    }

    override suspend fun findReview(key: ReviewKey): Outcome<SavedReview?> =
        sqliteOutcome(AppError.LocalRead) {
            Outcome.Success(loadReview(key))
        }

    override suspend fun commitReview(write: ReviewWrite): Outcome<SavedReview> =
        sqliteOutcome(AppError.LocalWrite) {
            database.withTransaction {
                val result = write.result
                val event = result.event
                val previous = loadReview(event.key)
                if (previous != null) {
                    return@withTransaction if (previous.event.copy(gradedAt = event.gradedAt) ==
                        event
                    ) {
                        Outcome.Success(previous)
                    } else {
                        Outcome.Failure(AppError.ReviewConflict)
                    }
                }

                val cardId = event.key.cardId.value
                val current = reviews.scheduledCard(event.deckId.value, cardId)
                    ?: return@withTransaction Outcome.Failure(AppError.CardNotFound)
                if (current.card.revision != event.cardRevision ||
                    current.version != write.expectedScheduleVersion ||
                    result.schedule.version != current.version + 1
                ) {
                    return@withTransaction Outcome.Failure(AppError.ReviewConflict)
                }

                reviews.updateSchedule(result.schedule.toEntity(cardId))
                val row = result.toEntity()
                reviews.insertReview(row)
                Outcome.Success(ReviewRow(row, event.deckId.value).toDomain())
            }
        }

    override suspend fun grades(sessionId: StudySessionId?): Outcome<List<ReviewGrade>> =
        sqliteOutcome(AppError.LocalRead) {
            val stored = if (sessionId ==
                null
            ) {
                reviews.grades()
            } else {
                reviews.grades(sessionId.value.toString())
            }
            Outcome.Success(stored.map { ReviewGrade.valueOf(it) })
        }

    private suspend fun loadReview(key: ReviewKey): SavedReview? =
        reviews.review(key.sessionId.value.toString(), key.cardId.value)?.toDomain()
}
