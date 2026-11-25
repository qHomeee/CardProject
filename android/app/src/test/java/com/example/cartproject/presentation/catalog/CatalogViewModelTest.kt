package com.example.cartproject.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.CatalogDeckContent
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.CatalogDeckSummary
import com.example.cartproject.domain.decks.CatalogReader
import com.example.cartproject.domain.decks.MaterialIssue
import com.example.cartproject.domain.decks.SearchCatalog
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {
    private val deck = CatalogDeckSummary(
        CatalogDeckId(UUID.fromString("da5132e9-6557-421e-bda0-a50795c24d34")),
        "Столицы Европы",
        "География",
        12,
    )

    @Test
    fun debouncesTypingAndRejectsResultFromCancelledRequest() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val reader = StubReader { query ->
                if (query.isEmpty()) withContext(NonCancellable) { delay(1_000) }
                Outcome.Success(if (query == "СТОЛ") listOf(deck) else emptyList())
            }
            val viewModel = CatalogViewModel(SearchCatalog(reader), SavedStateHandle()) { true }
            runCurrent()
            viewModel.updateQuery("СТ")
            viewModel.updateQuery("  СТОЛ  ")
            advanceUntilIdle()

            assertEquals(listOf("", "СТОЛ"), reader.queries)
            assertEquals(
                CatalogUiState("  СТОЛ  ", CatalogResult.Content(listOf(deck))),
                viewModel.uiState.value,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun offlineSkipsNetworkAndRetryKeepsQueryAfterConnectionReturns() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var connected = false
            var failure = true
            val reader = StubReader {
                if (failure) Outcome.Failure(AppError.Network) else Outcome.Success(emptyList())
            }
            val saved = SavedStateHandle()
            val viewModel = CatalogViewModel(SearchCatalog(reader), saved) { connected }
            viewModel.updateQuery("Планеты")
            advanceUntilIdle()
            assertEquals(CatalogResult.Offline, viewModel.uiState.value.result)
            assertEquals(emptyList<String>(), reader.queries)

            connected = true
            viewModel.retry()
            advanceUntilIdle()
            assertEquals(CatalogResult.Error(AppError.Network), viewModel.uiState.value.result)

            failure = false
            val restored = CatalogViewModel(SearchCatalog(reader), saved) { connected }
            advanceUntilIdle()
            assertEquals(
                CatalogUiState("Планеты", CatalogResult.Content(emptyList())),
                restored.uiState.value,
            )
            assertEquals(listOf("Планеты", "Планеты"), reader.queries)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun invalidQueryDoesNotReachCatalog() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val reader = StubReader { Outcome.Success(emptyList()) }
            val viewModel = CatalogViewModel(SearchCatalog(reader), SavedStateHandle()) { true }
            viewModel.updateQuery("я".repeat(101))
            advanceUntilIdle()

            assertEquals(
                CatalogResult.InvalidQuery(MaterialIssue.TooLong),
                viewModel.uiState.value.result,
            )
            assertEquals(emptyList<String>(), reader.queries)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class StubReader(
        private val search: suspend (String) -> Outcome<List<CatalogDeckSummary>>,
    ) : CatalogReader {
        val queries = mutableListOf<String>()

        override suspend fun search(query: String): Outcome<List<CatalogDeckSummary>> {
            queries += query
            return search.invoke(query)
        }

        override suspend fun readDeck(id: CatalogDeckId): Outcome<CatalogDeckContent> =
            error("Only catalog search is expected")
    }
}
