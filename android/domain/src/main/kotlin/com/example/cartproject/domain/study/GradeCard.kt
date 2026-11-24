package com.example.cartproject.domain.study

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.flatMap

/** Сохраняет результат ответа с учётом повторов и изменений карточки. */
class GradeCard(private val store: ReviewStore, private val clock: Clock = SystemClock) {
    /** Принимает оценку карточки из зафиксированной очереди. */
    suspend operator fun invoke(
        session: StudySession,
        cardId: LocalCardId,
        grade: ReviewGrade,
    ): Outcome<SavedReview> {
        val snapshot = session.cards.firstOrNull { it.id == cardId }
            ?: return Outcome.Failure(AppError.CardNotFound)
        val key = ReviewKey(session.id, snapshot.id)
        return store.findReview(key).flatMap { saved ->
            if (saved == null) {
                gradeNew(session.deckId, key, snapshot, grade)
            } else {
                replay(saved, session.deckId, snapshot, grade)
            }
        }
    }

    private suspend fun gradeNew(
        deckId: LocalDeckId,
        key: ReviewKey,
        snapshot: StudyCardSnapshot,
        grade: ReviewGrade,
    ): Outcome<SavedReview> = store.readScheduledCard(deckId, key.cardId).flatMap { current ->
        if (current.card.revision != snapshot.revision ||
            current.schedule.version != snapshot.scheduleVersion
        ) {
            // Другая попытка могла записать эту же оценку между двумя чтениями.
            store.findReview(key).flatMap { saved ->
                if (saved == null) {
                    Outcome.Failure(AppError.ReviewConflict)
                } else {
                    replay(saved, deckId, snapshot, grade)
                }
            }
        } else {
            val gradedAt = clock.now()
            val event = ReviewEvent(key, deckId, snapshot.revision, grade, gradedAt)
            val schedule = ScheduleRule.next(current.schedule, grade, gradedAt)
            store.commitReview(ReviewWrite(SavedReview(event, schedule), current.schedule.version))
        }
    }

    private fun replay(
        saved: SavedReview,
        deckId: LocalDeckId,
        snapshot: StudyCardSnapshot,
        grade: ReviewGrade,
    ): Outcome<SavedReview> {
        val event = saved.event
        return if (event.deckId == deckId && event.cardRevision == snapshot.revision &&
            event.grade == grade
        ) {
            Outcome.Success(saved)
        } else {
            Outcome.Failure(AppError.ReviewConflict)
        }
    }
}
