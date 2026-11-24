package com.example.cartproject.domain.decks

import kotlinx.coroutines.flow.Flow

/** Источник набора данных для экрана «Мои наборы». */
interface LocalDeckStore {
    /** Наблюдает список локальных наборов. */
    fun observeDecks(): Flow<List<Deck>>
}
