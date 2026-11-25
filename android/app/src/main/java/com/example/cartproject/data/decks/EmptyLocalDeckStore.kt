package com.example.cartproject.data.decks

import com.example.cartproject.domain.decks.Deck
import com.example.cartproject.domain.decks.LocalDeckList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Возвращает пустой список до подключения хранилища Room. */
internal class EmptyLocalDeckStore : LocalDeckList {
    override fun observeDecks(): Flow<List<Deck>> = flowOf(emptyList())
}
