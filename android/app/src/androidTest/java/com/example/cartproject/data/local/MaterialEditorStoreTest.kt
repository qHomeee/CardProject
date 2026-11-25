package com.example.cartproject.data.local

import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.CatalogDeckContent
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.CatalogDeckSummary
import com.example.cartproject.domain.decks.CatalogReader
import com.example.cartproject.domain.decks.DeleteOwnMaterial
import com.example.cartproject.domain.decks.ReadDeck
import com.example.cartproject.domain.decks.SaveOwnCard
import com.example.cartproject.domain.decks.SaveOwnDeck
import com.example.cartproject.presentation.editor.EditorCompletion
import com.example.cartproject.presentation.editor.EditorOperation
import com.example.cartproject.presentation.editor.EditorTarget
import com.example.cartproject.presentation.editor.EditorUiState
import com.example.cartproject.presentation.editor.MaterialEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MaterialEditorStoreTest {
    @Test
    fun failedRoomWriteKeepsDraftAndRetryCreatesOnlyOneDeck() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ZapomniDatabase::class.java,
        ).build()
        val store = RoomLocalDeckStore(db)
        val editor = MaterialEditorViewModel(
            EditorTarget.Deck(null),
            ReadDeck(store, NoCatalog),
            SaveOwnDeck(store),
            SaveOwnCard(store),
            DeleteOwnMaterial(store),
        )
        try {
            withContext(Dispatchers.IO) {
                db.openHelper.writableDatabase.execSQL(
                    """
                    CREATE TRIGGER fail_editor_insert BEFORE INSERT ON decks
                    BEGIN SELECT RAISE(ABORT, 'test deck write failure'); END
                    """.trimIndent(),
                )
            }
            withContext(Dispatchers.Main) {
                editor.updatePrimary("Черновик")
                editor.updateSecondary("Описание")
                editor.save()
            }

            val failed = withTimeout(5_000) {
                editor.uiState.filterIsInstance<EditorUiState.Form>()
                    .first { it.failedOperation == EditorOperation.Save }
            }
            assertEquals("Черновик", failed.primaryText)
            assertEquals("Описание", failed.secondaryText)
            assertEquals(Outcome.Success(emptyList<Any>()), store.observeDecks().first())

            withContext(Dispatchers.IO) {
                db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_editor_insert")
            }
            withContext(Dispatchers.Main) {
                editor.save()
                editor.save()
            }
            val saved = withTimeout(5_000) {
                editor.uiState.filterIsInstance<EditorUiState.Form>()
                    .first { it.completion is EditorCompletion.DeckSaved }
            }
            val id = (saved.completion as EditorCompletion.DeckSaved).id
            val decks = (store.observeDecks().first() as Outcome.Success).value
            assertEquals(id, decks.single().id)
            assertEquals("Описание", decks.single().description)
        } finally {
            editor.viewModelScope.cancel()
            db.close()
        }
    }

    private data object NoCatalog : CatalogReader {
        override suspend fun search(query: String): Outcome<List<CatalogDeckSummary>> =
            error("The editor is local")

        override suspend fun readDeck(id: CatalogDeckId): Outcome<CatalogDeckContent> =
            error("The editor is local")
    }
}
