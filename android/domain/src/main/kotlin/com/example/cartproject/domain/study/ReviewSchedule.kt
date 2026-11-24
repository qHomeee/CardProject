package com.example.cartproject.domain.study

import com.example.cartproject.domain.decks.Card
import com.example.cartproject.domain.decks.CardText
import java.time.Instant

/** Стадия, срок и версия записи для проверки одновременных изменений. */
data class ReviewSchedule(val stage: Int, val dueAt: Instant, val version: Long = 0) {
    init {
        require(stage in 0..5)
        require(version >= 0)
    }

    companion object {
        /** Создаёт расписание новой карточки, доступной для занятия сразу. */
        fun initial(createdAt: Instant): ReviewSchedule =
            ReviewSchedule(stage = 0, dueAt = createdAt)
    }
}

/** Карточка и расписание, прочитанные одним снимком. */
data class ScheduledCard(val card: Card, val schedule: ReviewSchedule) {
    /** Смена текста повышает ревизию и сбрасывает срок, прежний текст сохраняет прогресс. */
    fun revise(text: CardText, editedAt: Instant): ScheduledCard {
        if (card.question == text.question && card.answer == text.answer) return this

        return ScheduledCard(
            card = card.copy(
                question = text.question,
                answer = text.answer,
                revision = card.revision + 1,
            ),
            schedule = ReviewSchedule(
                stage = 0,
                dueAt = editedAt,
                version = schedule.version + 1,
            ),
        )
    }
}
