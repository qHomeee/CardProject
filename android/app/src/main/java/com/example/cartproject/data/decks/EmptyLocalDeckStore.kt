package com.example.cartproject.data.decks

import com.example.cartproject.domain.decks.Deck
import com.example.cartproject.domain.decks.LocalDeckStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Возвращает пустой список до подключения хранилища Room. */
internal class EmptyLocalDeckStore : LocalDeckStore {
    override fun observeDecks(): Flow<List<Deck>> = flowOf(emptyList())
}
