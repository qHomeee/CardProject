package com.example.cartproject.data.local

import androidx.room.withTransaction
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.CatalogDeckContent
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.Deck
import com.example.cartproject.domain.decks.DeckText
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckContent
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.decks.LocalDeckStore
import com.example.cartproject.domain.decks.OwnCardWrite
import com.example.cartproject.domain.study.ReviewSchedule
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Сохраняет свои наборы и скачанные снимки в SQLite через Room. */
internal class RoomLocalDeckStore(private val database: ZapomniDatabase) : LocalDeckStore {
    private val decks = database.decks()
    private val reviews = database.reviews()

    override fun observeDecks(): Flow<List<Deck>> =
        decks.observeDecks().map { rows -> rows.map(DeckEntity::toDomain) }

    override suspend fun readDeck(id: LocalDeckId): Outcome<LocalDeckContent> =
        sqliteOutcome(AppError.LocalRead) {
            database.withTransaction {
                val deck = decks.deck(id.value)
                    ?: return@withTransaction Outcome.Failure(AppError.DeckNotFound)
                val cards = decks.cards(id.value).map(CardEntity::toDomain)
                Outcome.Success(LocalDeckContent(deck.toDomain(), cards))
            }
        }

    override suspend fun saveOwnDeck(id: LocalDeckId?, text: DeckText): Outcome<LocalDeckId> =
        sqliteOutcome(AppError.LocalWrite) {
            database.withTransaction {
                if (id == null) {
                    val created = decks.insertDeck(
                        DeckEntity(title = text.title, description = text.description),
                    )
                    Outcome.Success(LocalDeckId(created))
                } else {
                    withOwnedDeck(id) { deck ->
                        decks.updateDeck(
                            deck.copy(title = text.title, description = text.description),
                        )
                        Outcome.Success(id)
                    }
                }
            }
        }

    override suspend fun saveOwnCard(write: OwnCardWrite): Outcome<LocalCardId> =
        sqliteOutcome(AppError.LocalWrite) {
            database.withTransaction {
                withOwnedDeck(write.deckId) {
                    val id = write.cardId
                    if (id == null) insertOwnCard(write) else reviseOwnCard(write, id)
                }
            }
        }

    private suspend fun insertOwnCard(write: OwnCardWrite): Outcome<LocalCardId> {
        val entity = CardEntity(
            deckId = write.deckId.value,
            question = write.text.question,
            answer = write.text.answer,
        )
        val cardId = decks.insertCard(entity)
        reviews.insertSchedule(ReviewSchedule.initial(write.editedAt).toEntity(cardId))
        return Outcome.Success(LocalCardId(cardId))
    }

    private suspend fun reviseOwnCard(write: OwnCardWrite, id: LocalCardId): Outcome<LocalCardId> {
        val original = reviews.scheduledCard(write.deckId.value, id.value)?.toDomain()
            ?: return Outcome.Failure(AppError.CardNotFound)
        val updated = original.revise(write.text, write.editedAt)
        if (updated != original) {
            decks.updateCard(updated.card.toEntity())
            reviews.updateSchedule(updated.schedule.toEntity(id.value))
        }
        return Outcome.Success(id)
    }

    override suspend fun findDownloadedDeck(sourceId: CatalogDeckId): Outcome<LocalDeckId?> =
        sqliteOutcome(AppError.LocalRead) {
            Outcome.Success(decks.downloadedDeckId(sourceId.value.toString())?.let(::LocalDeckId))
        }

    override suspend fun saveDownloadedDeck(
        content: CatalogDeckContent,
        receivedAt: Instant,
    ): Outcome<LocalDeckId> = sqliteOutcome(AppError.LocalWrite) {
        database.withTransaction {
            val sourceId = content.id.value.toString()
            val saved = decks.downloadedDeckId(sourceId)
            if (saved != null) return@withTransaction Outcome.Success(LocalDeckId(saved))

            val entity =
                DeckEntity(
                    title = content.title,
                    description = content.description,
                    sourceId = sourceId,
                )
            val deckId = decks.insertDeck(entity)
            val cards = content.cards.map {
                CardEntity(deckId = deckId, question = it.question, answer = it.answer)
            }
            val cardIds = decks.insertCards(cards)
            val schedule = ReviewSchedule.initial(receivedAt)
            reviews.insertSchedules(cardIds.map(schedule::toEntity))
            Outcome.Success(LocalDeckId(deckId))
        }
    }

    override suspend fun deleteOwnDeck(id: LocalDeckId): Outcome<Unit> =
        sqliteOutcome(AppError.LocalWrite) {
            database.withTransaction {
                withOwnedDeck(id) {
                    decks.deleteDeck(id.value)
                    Outcome.Success(Unit)
                }
            }
        }

    override suspend fun deleteOwnCard(deckId: LocalDeckId, cardId: LocalCardId): Outcome<Unit> =
        sqliteOutcome(AppError.LocalWrite) {
            database.withTransaction {
                withOwnedDeck(deckId) {
                    if (decks.deleteCard(deckId.value, cardId.value) == 0) {
                        Outcome.Failure(AppError.CardNotFound)
                    } else {
                        Outcome.Success(Unit)
                    }
                }
            }
        }

    override suspend fun removeDownloadedDeck(id: LocalDeckId): Outcome<Unit> =
        sqliteOutcome(AppError.LocalWrite) {
            database.withTransaction {
                val deck = decks.deck(id.value)
                    ?: return@withTransaction Outcome.Failure(AppError.DeckNotFound)
                if (deck.sourceId ==
                    null
                ) {
                    return@withTransaction Outcome.Failure(AppError.ReadOnlyDeck)
                }
                decks.deleteDeck(id.value)
                Outcome.Success(Unit)
            }
        }

    private suspend inline fun <T> withOwnedDeck(
        id: LocalDeckId,
        action: (DeckEntity) -> Outcome<T>,
    ): Outcome<T> {
        val deck = decks.deck(id.value) ?: return Outcome.Failure(AppError.DeckNotFound)
        if (deck.sourceId != null) return Outcome.Failure(AppError.ReadOnlyDeck)
        return action(deck)
    }
}
