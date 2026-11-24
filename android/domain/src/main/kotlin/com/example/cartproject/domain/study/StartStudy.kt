package com.example.cartproject.domain.study

import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.map

/** Формирует очередь карточек, срок которых наступил к началу занятия. */
class StartStudy(private val store: ReviewStore, private val clock: Clock = SystemClock) {
    /** Сортирует снимки по сроку и ID или возвращает ближайший будущий срок. */
    suspend operator fun invoke(
        deckId: LocalDeckId,
        sessionId: StudySessionId,
    ): Outcome<StudyStart> {
        val startedAt = clock.now()
        return store.scheduledCards(deckId).map { cards ->
            val queue = cards
                .filter { !it.schedule.dueAt.isAfter(startedAt) }
                .sortedWith(compareBy({ it.schedule.dueAt }, { it.card.id.value }))
                .distinctBy { it.card.id }
                .map { (card, schedule) ->
                    StudyCardSnapshot(
                        card.id,
                        card.question,
                        card.answer,
                        card.revision,
                        schedule.version,
                    )
                }
            if (queue.isEmpty()) {
                StudyStart.Empty(cards.minOfOrNull { it.schedule.dueAt })
            } else {
                StudyStart.Ready(StudySession(sessionId, deckId, startedAt, queue))
            }
        }
    }
}
