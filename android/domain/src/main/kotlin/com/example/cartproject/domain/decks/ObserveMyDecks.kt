package com.example.cartproject.domain.decks

import kotlinx.coroutines.flow.Flow

/** Предоставляет локальные наборы без привязки к способу их хранения. */
class ObserveMyDecks(private val store: LocalDeckList) {
    /** Возвращает поток списка локальных наборов. */
    operator fun invoke(): Flow<List<Deck>> = store.observeDecks()
}
