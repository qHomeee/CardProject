package com.example.cartproject.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.CatalogDeckSummary
import com.example.cartproject.domain.decks.MaterialField
import com.example.cartproject.domain.decks.MaterialIssue
import com.example.cartproject.domain.decks.SearchCatalog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class CatalogUiState(val query: String, val result: CatalogResult)

internal sealed interface CatalogResult {
    data object Loading : CatalogResult
    data object Offline : CatalogResult
    data class Content(val decks: List<CatalogDeckSummary>) : CatalogResult
    data class InvalidQuery(val issue: MaterialIssue?) : CatalogResult
    data class Error(val reason: AppError) : CatalogResult
}

/** Выполняет последний запрос, сохраняя введённый текст между запусками экрана. */
internal class CatalogViewModel(
    private val search: SearchCatalog,
    private val savedState: SavedStateHandle,
    private val hasConnection: () -> Boolean,
) : ViewModel() {
    private val state = MutableStateFlow(
        CatalogUiState(savedState[QUERY_KEY] ?: "", CatalogResult.Loading),
    )
    val uiState = state.asStateFlow()
    private var request: Job? = null

    init {
        retry()
    }

    fun updateQuery(query: String) {
        if (query == state.value.query) return
        savedState[QUERY_KEY] = query
        load(query, waitForTyping = true)
    }

    fun retry() {
        load(state.value.query, waitForTyping = false)
    }

    private fun load(query: String, waitForTyping: Boolean) {
        request?.cancel()
        state.value = CatalogUiState(query, CatalogResult.Loading)
        request = viewModelScope.launch {
            if (waitForTyping) delay(SEARCH_DELAY_MS)
            val result = if (hasConnection()) searchResult(query) else CatalogResult.Offline
            ensureActive()
            state.value = CatalogUiState(query, result)
        }
    }

    private suspend fun searchResult(query: String): CatalogResult =
        when (val outcome = search(query)) {
            is Outcome.Success -> CatalogResult.Content(outcome.value)
            is Outcome.Failure -> when (val error = outcome.error) {
                is AppError.InvalidFields -> CatalogResult.InvalidQuery(
                    error.fields[MaterialField.Search],
                )
                AppError.Network, AppError.CatalogTimeout -> {
                    if (hasConnection()) CatalogResult.Error(error) else CatalogResult.Offline
                }
                else -> CatalogResult.Error(error)
            }
        }

    private companion object {
        const val QUERY_KEY = "catalogQuery"
        const val SEARCH_DELAY_MS = 300L
    }
}
