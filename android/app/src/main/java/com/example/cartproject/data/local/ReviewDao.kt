package com.example.cartproject.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/** Карточка с расписанием из согласованного чтения. */
internal data class ScheduledCardRow(
    @Embedded val card: CardEntity,
    @ColumnInfo(name = "schedule_stage") val stage: Int,
    @ColumnInfo(name = "schedule_due_at") val dueAt: Long,
    @ColumnInfo(name = "schedule_version") val version: Long,
)

/** Строка истории вместе с ID набора карточки. */
internal data class ReviewRow(
    @Embedded val review: ReviewEntity,
    @ColumnInfo(name = "deck_id") val deckId: Long,
)

private const val SELECT_SCHEDULES = """
    SELECT cards.*, review_schedules.stage AS schedule_stage,
           review_schedules.due_at AS schedule_due_at, review_schedules.version AS schedule_version
    FROM cards JOIN review_schedules ON cards.id = review_schedules.card_id
"""

/** Доступ к срокам и истории повторений. */
@Dao
internal interface ReviewDao {
    /** Читает все карточки набора вместе со сроками. */
    @Query(SELECT_SCHEDULES + " WHERE cards.deck_id = :deckId ORDER BY cards.id")
    suspend fun scheduledCards(deckId: Long): List<ScheduledCardRow>

    /** Читает одну карточку и её актуальные версии. */
    @Query(SELECT_SCHEDULES + " WHERE cards.deck_id = :deckId AND cards.id = :cardId")
    suspend fun scheduledCard(deckId: Long, cardId: Long): ScheduledCardRow?

    /** Проверяет наличие хотя бы одной карточки со сроком повторения. */
    @Query("SELECT EXISTS(SELECT 1 FROM review_schedules WHERE due_at <= :now)")
    suspend fun hasDueCards(now: Long): Boolean

    /** Записывает начальное расписание новой карточки. */
    @Insert
    suspend fun insertSchedule(entity: ReviewScheduleEntity)

    /** Записывает начальные расписания скачанных карточек. */
    @Insert
    suspend fun insertSchedules(entities: List<ReviewScheduleEntity>)

    /** Записывает новую стадию и срок существующей карточки. */
    @Update
    suspend fun updateSchedule(entity: ReviewScheduleEntity)

    /** Ищет предыдущую запись по ключу занятия и карточки. */
    @Query(
        """
        SELECT reviews.*, cards.deck_id FROM reviews
        JOIN cards ON reviews.card_id = cards.id
        WHERE reviews.session_id = :sessionId AND reviews.card_id = :cardId
        """,
    )
    suspend fun review(sessionId: String, cardId: Long): ReviewRow?

    /** Записывает оценку. Повтор ключа нарушает ограничение базы. */
    @Insert
    suspend fun insertReview(entity: ReviewEntity)

    /** Читает оценки указанного занятия. */
    @Query("SELECT grade FROM reviews WHERE session_id = :sessionId")
    suspend fun grades(sessionId: String): List<String>

    /** Читает оценки всех занятий. */
    @Query("SELECT grade FROM reviews")
    suspend fun grades(): List<String>
}
