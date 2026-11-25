package com.example.cartproject.data.local

import com.example.cartproject.domain.decks.Card
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.Deck
import com.example.cartproject.domain.decks.DeckOrigin
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.study.ReviewSchedule
import com.example.cartproject.domain.study.ScheduledCard
import java.time.Instant
import java.util.UUID

/** Восстанавливает происхождение локального набора. */
internal fun DeckEntity.toDomain(): Deck = Deck(
    id = LocalDeckId(id),
    title = title,
    description = description,
    origin = sourceId?.let { DeckOrigin.Downloaded(CatalogDeckId(UUID.fromString(it))) }
        ?: DeckOrigin.Owned,
)

/** Преобразует строку карточки в модель предметной области. */
internal fun CardEntity.toDomain(): Card = Card(
    LocalCardId(id),
    LocalDeckId(deckId),
    question,
    answer,
    revision,
)

/** Сохраняет карточку с её текущим ID и ревизией. */
internal fun Card.toEntity(): CardEntity =
    CardEntity(id.value, deckId.value, question, answer, revision)

/** Преобразует совмещённую строку карточки и её срока. */
internal fun ScheduledCardRow.toDomain(): ScheduledCard = ScheduledCard(
    card.toDomain(),
    ReviewSchedule(stage, Instant.ofEpochMilli(dueAt), version),
)

/** Хранит срок как миллисекунды UTC. */
internal fun ReviewSchedule.toEntity(cardId: Long): ReviewScheduleEntity =
    ReviewScheduleEntity(cardId, stage, dueAt.toEpochMilli(), version)
