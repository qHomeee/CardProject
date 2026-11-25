package com.example.cartproject.domain.decks

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.study.ReviewSchedule
import com.example.cartproject.domain.study.ScheduledCard
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Хранилище для проверки вызовов и поведения контрактов без Room. */
internal class LocalDeckStoreFake : LocalDeckStore {
    private val deckRows = MutableStateFlow<List<Deck>>(emptyList())
    val cardRows = mutableMapOf<LocalCardId, ScheduledCard>()
    var failDownload = false
    var beforeDownloadWrite: () -> Unit = {}
    var downloadWrites = 0
        private set
    var ownWrites = 0
        private set
    var reads = 0
        private set

    fun add(deck: Deck) {
        deckRows.value += deck
    }

    override fun observeDecks() = deckRows.map { Outcome.Success(it) }

    override suspend fun readDeck(id: LocalDeckId): Outcome<LocalDeckContent> {
        reads++
        val deck = deckRows.value.firstOrNull { it.id == id }
            ?: return Outcome.Failure(AppError.DeckNotFound)
        return Outcome.Success(
            LocalDeckContent(
                deck,
                cardRows.values.map { it.card }.filter {
                    it.deckId ==
                        id
                },
            ),
        )
    }

    override suspend fun saveOwnDeck(id: LocalDeckId?, text: DeckText): Outcome<LocalDeckId> {
        ownWrites++
        if (id != null) {
            val error = ownDeckError(id)
            if (error != null) return Outcome.Failure(error)
        }
        val key = id ?: nextDeckId()
        val updated = Deck(key, text.title, text.description)
        deckRows.value = deckRows.value.filterNot { it.id == key } + updated
        return Outcome.Success(key)
    }

    override suspend fun saveOwnCard(write: OwnCardWrite): Outcome<LocalCardId> {
        ownWrites++
        val error = ownDeckError(write.deckId)
        if (error != null) return Outcome.Failure(error)
        val id = write.cardId ?: nextCardId()
        val value = if (write.cardId == null) {
            ScheduledCard(
                Card(id, write.deckId, write.text.question, write.text.answer),
                ReviewSchedule.initial(write.editedAt),
            )
        } else {
            val current = cardRows[id]?.takeIf { it.card.deckId == write.deckId }
                ?: return Outcome.Failure(AppError.CardNotFound)
            current.revise(write.text, write.editedAt)
        }
        cardRows[id] = value
        return Outcome.Success(id)
    }

    override suspend fun findDownloadedDeck(sourceId: CatalogDeckId): Outcome<LocalDeckId?> =
        Outcome.Success(
            deckRows.value.firstOrNull {
                it.origin == DeckOrigin.Downloaded(sourceId)
            }?.id,
        )

    override suspend fun saveDownloadedDeck(
        content: CatalogDeckContent,
        receivedAt: Instant,
    ): Outcome<LocalDeckId> {
        downloadWrites++
        beforeDownloadWrite()
        val existing = (findDownloadedDeck(content.id) as Outcome.Success).value
        if (existing != null) return Outcome.Success(existing)
        if (failDownload) return Outcome.Failure(AppError.LocalWrite)
        val localId = nextDeckId()
        val firstCardId = nextCardId().value
        val cards = content.cards.mapIndexed { index, card ->
            ScheduledCard(
                Card(LocalCardId(firstCardId + index), localId, card.question, card.answer),
                ReviewSchedule.initial(receivedAt),
            )
        }
        cardRows.putAll(cards.associateBy { it.card.id })
        add(Deck(localId, content.title, content.description, DeckOrigin.Downloaded(content.id)))
        return Outcome.Success(localId)
    }

    override suspend fun deleteOwnDeck(id: LocalDeckId): Outcome<Unit> {
        val error = ownDeckError(id)
        return if (error == null) remove(id) else Outcome.Failure(error)
    }

    override suspend fun deleteOwnCard(deckId: LocalDeckId, cardId: LocalCardId): Outcome<Unit> {
        val error = ownDeckError(deckId)
        if (error != null) return Outcome.Failure(error)
        if (cardRows[cardId]?.card?.deckId != deckId) return Outcome.Failure(AppError.CardNotFound)
        cardRows.remove(cardId)
        return Outcome.Success(Unit)
    }

    override suspend fun removeDownloadedDeck(id: LocalDeckId): Outcome<Unit> {
        val deck =
            deckRows.value.firstOrNull { it.id == id }
                ?: return Outcome.Failure(AppError.DeckNotFound)
        if (deck.origin == DeckOrigin.Owned) return Outcome.Failure(AppError.ReadOnlyDeck)
        return remove(id)
    }

    private fun ownDeckError(id: LocalDeckId): AppError? = when (
        deckRows.value.firstOrNull {
            it.id ==
                id
        }?.origin
    ) {
        DeckOrigin.Owned -> null
        is DeckOrigin.Downloaded -> AppError.ReadOnlyDeck
        null -> AppError.DeckNotFound
    }

    private fun nextDeckId(): LocalDeckId = LocalDeckId(
        (
            deckRows.value.maxOfOrNull {
                it.id.value
            } ?: 0
            ) + 1,
    )

    private fun nextCardId(): LocalCardId = LocalCardId(
        (
            cardRows.keys.maxOfOrNull {
                it.value
            } ?: 0
            ) + 1,
    )

    private fun remove(id: LocalDeckId): Outcome<Unit> {
        cardRows.values.removeAll { it.card.deckId == id }
        deckRows.value = deckRows.value.filterNot { it.id == id }
        return Outcome.Success(Unit)
    }
}
