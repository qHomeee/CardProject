package com.example.cartproject.data.local

import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.study.ReviewEvent
import com.example.cartproject.domain.study.ReviewGrade
import com.example.cartproject.domain.study.ReviewKey
import com.example.cartproject.domain.study.ReviewSchedule
import com.example.cartproject.domain.study.SavedReview
import com.example.cartproject.domain.study.StudySessionId
import java.time.Instant
import java.util.UUID

/** Записывает историческую оценку вместе с рассчитанным сроком. */
internal fun SavedReview.toEntity(): ReviewEntity = ReviewEntity(
    sessionId = event.key.sessionId.value.toString(),
    cardId = event.key.cardId.value,
    cardRevision = event.cardRevision,
    grade = event.grade.name,
    gradedAt = event.gradedAt.toEpochMilli(),
    nextStage = schedule.stage,
    nextDueAt = schedule.dueAt.toEpochMilli(),
    nextVersion = schedule.version,
)

/** Восстанавливает исходный результат, включая время и срок из истории. */
internal fun ReviewRow.toDomain(): SavedReview {
    val key =
        ReviewKey(StudySessionId(UUID.fromString(review.sessionId)), LocalCardId(review.cardId))
    val event = ReviewEvent(
        key,
        LocalDeckId(deckId),
        review.cardRevision,
        ReviewGrade.valueOf(review.grade),
        Instant.ofEpochMilli(review.gradedAt),
    )
    val schedule = ReviewSchedule(
        review.nextStage,
        Instant.ofEpochMilli(review.nextDueAt),
        review.nextVersion,
    )
    return SavedReview(event, schedule)
}
