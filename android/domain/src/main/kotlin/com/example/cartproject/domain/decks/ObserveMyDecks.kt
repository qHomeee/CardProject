package com.example.cartproject.domain.decks

import com.example.cartproject.domain.Outcome
import kotlinx.coroutines.flow.Flow

/** Предоставляет локальные наборы без привязки к способу их хранения. */
class ObserveMyDecks(private val store: LocalDeckList) {
    /** Возвращает поток списка локальных наборов. */
    operator fun invoke(): Flow<Outcome<List<Deck>>> = store.observeDecks()
}
