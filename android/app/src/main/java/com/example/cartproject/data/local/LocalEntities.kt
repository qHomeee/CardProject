package com.example.cartproject.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Локальный набор. UUID источника указан только у скачанного снимка. */
@Entity(tableName = "decks", indices = [Index(value = ["source_id"], unique = true)])
internal data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    @ColumnInfo(name = "source_id") val sourceId: String? = null,
)

/** Карточка с неизменным локальным ID и ревизией текста. */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deck_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("deck_id")],
)
internal data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "deck_id") val deckId: Long,
    val question: String,
    val answer: String,
    val revision: Long = 0,
)

/** Текущий срок повторения в миллисекундах UTC. */
@Entity(
    tableName = "review_schedules",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class ReviewScheduleEntity(
    @PrimaryKey @ColumnInfo(name = "card_id") val cardId: Long,
    val stage: Int,
    @ColumnInfo(name = "due_at") val dueAt: Long,
    val version: Long,
)

/** Сохранённая оценка и рассчитанный при её принятии срок. */
@Entity(
    tableName = "reviews",
    primaryKeys = ["session_id", "card_id"],
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("card_id")],
)
internal data class ReviewEntity(
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "card_id") val cardId: Long,
    @ColumnInfo(name = "card_revision") val cardRevision: Long,
    val grade: String,
    @ColumnInfo(name = "graded_at") val gradedAt: Long,
    @ColumnInfo(name = "next_stage") val nextStage: Int,
    @ColumnInfo(name = "next_due_at") val nextDueAt: Long,
    @ColumnInfo(name = "next_version") val nextVersion: Long,
)
