package com.example.cartproject.domain.study

import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import java.time.Instant
import java.util.Collections
import java.util.UUID

/** Идентификатор одного запуска занятия. */
@JvmInline
value class StudySessionId(val value: UUID)

/** Вопрос, ответ и версии записи на момент начала занятия. */
data class StudyCardSnapshot(
    val id: LocalCardId,
    val question: String,
    val answer: String,
    val revision: Long,
    val scheduleVersion: Long,
)

/** Занятие с зафиксированным порядком карточек. */
class StudySession(
    val id: StudySessionId,
    val deckId: LocalDeckId,
    val startedAt: Instant,
    cards: List<StudyCardSnapshot>,
) {
    val cards: List<StudyCardSnapshot> = Collections.unmodifiableList(cards.toList())
}

/** Результат подготовки очереди занятия. */
sealed interface StudyStart {
    /** Есть карточки, доступные для занятия. */
    data class Ready(val session: StudySession) : StudyStart

    /** Очередь пуста. Срок указан, если в наборе есть будущие карточки. */
    data class Empty(val nextDueAt: Instant?) : StudyStart
}
