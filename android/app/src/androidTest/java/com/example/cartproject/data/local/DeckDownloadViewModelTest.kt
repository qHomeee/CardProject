package com.example.cartproject.data.local

import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.CatalogCard
import com.example.cartproject.domain.decks.CatalogCardId
import com.example.cartproject.domain.decks.CatalogDeckContent
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.CatalogDeckSummary
import com.example.cartproject.domain.decks.CatalogReader
import com.example.cartproject.domain.decks.DownloadDeck
import com.example.cartproject.domain.decks.ReadDeck
import com.example.cartproject.domain.decks.RemoveDownloadedDeck
import com.example.cartproject.presentation.decks.DeckDetailAction
import com.example.cartproject.presentation.decks.DeckDetailViewModel
import com.example.cartproject.presentation.decks.DeckSource
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeckDownloadViewModelTest {
    @Test
    fun failedDownloadKeepsPreviewAndADoubleTapSavesOnlyOnce() = runBlocking {
        val deck = CatalogDeckContent(
            id = CatalogDeckId(UUID.fromString("80475c37-814f-4c67-88c0-d43c42aa6e46")),
            title = "География",
            description = "",
            cards = listOf(
                CatalogCard(
                    id = CatalogCardId(UUID.fromString("de05295d-dbc5-459d-a43a-838c1d88a3d0")),
                    question = "Столица Португалии?",
                    answer = "Лиссабон",
                ),
            ),
        )
        val reader = ControlledCatalog(deck)
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ZapomniDatabase::class.java,
        ).build()
        val store = RoomLocalDeckStore(db)
        val viewModel = withContext(Dispatchers.Main) {
            DeckDetailViewModel(
                DeckSource.Catalog(deck.id),
                ReadDeck(store, reader),
                DownloadDeck(reader, store),
                RemoveDownloadedDeck(store),
            )
        }
        try {
            val preview = withTimeout(5_000) {
                viewModel.uiState.first { it.result is Outcome.Success }.result
            }
            val interrupted = CompletableDeferred<Outcome<CatalogDeckContent>>()
            withContext(Dispatchers.Main) {
                reader.response = { interrupted.await() }
                viewModel.download()
                viewModel.download()
            }
            assertEquals(DeckDetailAction.Downloading, viewModel.uiState.value.action)

            interrupted.complete(Outcome.Failure(AppError.Network))
            val failed = withTimeout(5_000) {
                viewModel.uiState.first { it.action is DeckDetailAction.Failed }
            }
            assertEquals(preview, failed.result)
            assertEquals(Outcome.Success(null), store.findDownloadedDeck(deck.id))

            withContext(Dispatchers.Main) {
                reader.response = { Outcome.Success(deck) }
                viewModel.download()
                viewModel.download()
            }
            val saved = withTimeout(5_000) {
                viewModel.uiState.first { it.action is DeckDetailAction.Downloaded }
            }
            val id = (saved.action as DeckDetailAction.Downloaded).id
            assertEquals(Outcome.Success(id), store.findDownloadedDeck(deck.id))
            assertEquals(3, reader.reads)
            val local = (store.readDeck(id) as Outcome.Success).value
            assertTrue(local.cards.single().answer == "Лиссабон")
        } finally {
            viewModel.viewModelScope.cancel()
            db.close()
        }
    }

    private class ControlledCatalog(deck: CatalogDeckContent) : CatalogReader {
        var response: suspend () -> Outcome<CatalogDeckContent> = { Outcome.Success(deck) }
        var reads = 0
            private set

        override suspend fun search(query: String): Outcome<List<CatalogDeckSummary>> =
            error("Search is outside this scenario")

        override suspend fun readDeck(id: CatalogDeckId): Outcome<CatalogDeckContent> {
            reads++
            return response()
        }
    }
}
