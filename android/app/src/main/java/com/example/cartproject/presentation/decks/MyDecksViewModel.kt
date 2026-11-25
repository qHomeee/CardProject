package com.example.cartproject.presentation.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.ObserveMyDecks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Наблюдает локальные наборы и повторяет чтение по запросу пользователя. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class MyDecksViewModel(observeMyDecks: ObserveMyDecks) : ViewModel() {
    private val reloads = MutableStateFlow(0L)

    val uiState: StateFlow<MyDecksUiState> = reloads.flatMapLatest {
        observeMyDecks()
            .map { result ->
                when (result) {
                    is Outcome.Success -> MyDecksUiState.Content(result.value)
                    is Outcome.Failure -> MyDecksUiState.Error
                }
            }
            .onStart { emit(MyDecksUiState.Loading) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyDecksUiState.Loading)

    /** Запускает локальное чтение повторно после сбоя. */
    fun retry() {
        reloads.update { it + 1 }
    }
}
