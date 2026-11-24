package com.example.cartproject.presentation.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.decks.ObserveMyDecks
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Наблюдает локальные наборы в течение жизни экрана. */
internal class MyDecksViewModel(observeMyDecks: ObserveMyDecks) : ViewModel() {
    val uiState: StateFlow<MyDecksUiState> =
        observeMyDecks()
            .map(MyDecksUiState::Content)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyDecksUiState.Loading)
}
