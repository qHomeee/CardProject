package com.example.cartproject.domain.decks

import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.flatMap
import com.example.cartproject.domain.study.Clock
import com.example.cartproject.domain.study.SystemClock

/** Проверяет поля и записывает собственный набор. */
class SaveOwnDeck(private val store: LocalDeckStore) {
    /** Создаёт набор при пустом ID или редактирует существующий. */
    suspend operator fun invoke(
        id: LocalDeckId?,
        title: String,
        description: String,
    ): Outcome<LocalDeckId> =
        MaterialValidator.deck(title, description).flatMap { store.saveOwnDeck(id, it) }
}

/** Проверяет поля карточки, передаёт текст и время изменения в хранилище. */
class SaveOwnCard(private val store: LocalDeckStore, private val clock: Clock = SystemClock) {
    /** Создаёт или меняет карточку собственного набора. */
    suspend operator fun invoke(
        deckId: LocalDeckId,
        cardId: LocalCardId?,
        question: String,
        answer: String,
    ): Outcome<LocalCardId> = MaterialValidator.card(question, answer).flatMap { fields ->
        store.saveOwnCard(OwnCardWrite(deckId, cardId, fields, clock.now()))
    }
}

/** Удаляет свои материалы после подтверждения в интерфейсе. */
class DeleteOwnMaterial(private val store: LocalDeckStore) {
    /** Удаляет набор и связанные данные. */
    suspend fun deck(id: LocalDeckId): Outcome<Unit> = store.deleteOwnDeck(id)

    /** Удаляет карточку и данные её повторений. */
    suspend fun card(deckId: LocalDeckId, cardId: LocalCardId): Outcome<Unit> =
        store.deleteOwnCard(deckId, cardId)
}

/** Удаляет локальный снимок общего набора. */
class RemoveDownloadedDeck(private val store: LocalDeckStore) {
    /** Удаляет снимок и его историю на устройстве. */
    suspend operator fun invoke(id: LocalDeckId): Outcome<Unit> = store.removeDownloadedDeck(id)
}
