package com.example.cartproject.domain.decks

import java.util.UUID

/** Идентификатор набора на устройстве. */
@JvmInline
value class LocalDeckId(val value: Long)

/** Идентификатор карточки на устройстве. */
@JvmInline
value class LocalCardId(val value: Long)

/** Идентификатор набора в публичном каталоге. */
@JvmInline
value class CatalogDeckId(val value: UUID)

/** Идентификатор карточки в публичном каталоге. */
@JvmInline
value class CatalogCardId(val value: UUID)

/** Происхождение локального набора определяет возможность его редактирования. */
sealed interface DeckOrigin {
    /** Набор, созданный на устройстве. */
    data object Owned : DeckOrigin

    /** Локальный снимок с идентификатором оригинала. */
    data class Downloaded(val sourceId: CatalogDeckId) : DeckOrigin
}

/** Локальный набор карточек, доступный на устройстве. */
data class Deck(
    val id: LocalDeckId,
    val title: String,
    val description: String,
    val origin: DeckOrigin = DeckOrigin.Owned,
)

/** Карточка с устойчивым ID и ревизией вопроса и ответа. */
data class Card(
    val id: LocalCardId,
    val deckId: LocalDeckId,
    val question: String,
    val answer: String,
    val revision: Long = 0,
)

/** Набор и все его карточки, прочитанные вместе. */
data class LocalDeckContent(val deck: Deck, val cards: List<Card>)
