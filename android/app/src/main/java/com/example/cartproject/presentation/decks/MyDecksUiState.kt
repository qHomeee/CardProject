package com.example.cartproject.presentation.decks

import com.example.cartproject.domain.decks.Deck

/** Состояние стартового экрана со списком локальных наборов. */
internal sealed interface MyDecksUiState {
    /** Первое значение списка ещё не получено. */
    data object Loading : MyDecksUiState

    /** Актуальный список, который может быть пустым. */
    data class Content(val decks: List<Deck>) : MyDecksUiState
}
