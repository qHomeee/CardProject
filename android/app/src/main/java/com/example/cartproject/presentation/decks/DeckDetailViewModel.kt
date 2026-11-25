package com.example.cartproject.presentation.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.DeckOrigin
import com.example.cartproject.domain.decks.DownloadDeck
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.decks.ReadDeck
import com.example.cartproject.domain.decks.RemoveDownloadedDeck
import com.example.cartproject.domain.map
import com.example.cartproject.presentation.components.DeckCardOrigin
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal sealed interface DeckSource {
    data class Catalog(val id: CatalogDeckId) : DeckSource
    data class Local(val id: LocalDeckId) : DeckSource
}

internal data class DeckDetails(
    val title: String,
    val description: String,
    val origin: DeckCardOrigin,
    val cards: List<DeckDetailCard>,
)

internal data class DeckDetailCard(val key: String, val question: String, val answer: String)

internal sealed interface DeckDetailAction {
    data object Idle : DeckDetailAction
    data object Downloading : DeckDetailAction
    data class Downloaded(val id: LocalDeckId) : DeckDetailAction
    data object Removing : DeckDetailAction
    data object Removed : DeckDetailAction
    data class Failed(val reason: AppError) : DeckDetailAction
}

internal data class DeckDetailUiState(
    val result: Outcome<DeckDetails>? = null,
    val savedCopy: LocalDeckId? = null,
    val action: DeckDetailAction = DeckDetailAction.Idle,
)

/** Читает один выбранный набор и выполняет операции с его локальной копией. */
internal class DeckDetailViewModel(
    private val source: DeckSource,
    private val readDeck: ReadDeck,
    private val downloadDeck: DownloadDeck,
    private val removeDownload: RemoveDownloadedDeck,
) : ViewModel() {
    private val state = MutableStateFlow(DeckDetailUiState())
    val uiState = state.asStateFlow()
    private var readJob: Job? = null

    init {
        retryRead()
    }

    fun retryRead() {
        if (readJob?.isActive == true || state.value.result is Outcome.Success) return
        state.update { it.copy(result = null) }
        readJob = viewModelScope.launch {
            val result = when (source) {
                is DeckSource.Catalog -> readCatalog(source.id)
                is DeckSource.Local -> readLocal(source.id)
            }
            state.update { it.copy(result = result) }
        }
    }

    fun download() {
        val id = (source as? DeckSource.Catalog)?.id ?: return
        val current = state.value
        if (current.result !is Outcome.Success || current.savedCopy != null) return
        if (current.action !is DeckDetailAction.Idle &&
            current.action !is DeckDetailAction.Failed
        ) {
            return
        }

        state.value = current.copy(action = DeckDetailAction.Downloading)
        viewModelScope.launch {
            when (val result = downloadDeck(id)) {
                is Outcome.Success -> state.update {
                    it.copy(
                        savedCopy = result.value,
                        action = DeckDetailAction.Downloaded(result.value),
                    )
                }
                is Outcome.Failure -> state.update {
                    it.copy(action = DeckDetailAction.Failed(result.error))
                }
            }
        }
    }

    fun removeSavedCopy() {
        val id = (source as? DeckSource.Local)?.id ?: return
        val current = state.value
        val detail = (current.result as? Outcome.Success)?.value ?: return
        if (detail.origin != DeckCardOrigin.Downloaded) return
        if (current.action !is DeckDetailAction.Idle &&
            current.action !is DeckDetailAction.Failed
        ) {
            return
        }

        state.value = current.copy(action = DeckDetailAction.Removing)
        viewModelScope.launch {
            val action = when (val result = removeDownload(id)) {
                is Outcome.Success -> DeckDetailAction.Removed
                is Outcome.Failure -> DeckDetailAction.Failed(result.error)
            }
            state.update { it.copy(action = action) }
        }
    }

    private suspend fun readCatalog(id: CatalogDeckId): Outcome<DeckDetails> {
        val saved = (readDeck.findDownloadedCopy(id) as? Outcome.Success)?.value
        state.update { it.copy(savedCopy = saved) }
        return readDeck(id).map { content ->
            DeckDetails(
                title = content.title,
                description = content.description,
                origin = DeckCardOrigin.Catalog,
                cards = content.cards.map {
                    DeckDetailCard(it.id.value.toString(), it.question, it.answer)
                },
            )
        }
    }

    private suspend fun readLocal(id: LocalDeckId): Outcome<DeckDetails> =
        readDeck(id).map { content ->
            DeckDetails(
                title = content.deck.title,
                description = content.deck.description,
                origin = when (content.deck.origin) {
                    DeckOrigin.Owned -> DeckCardOrigin.Owned
                    is DeckOrigin.Downloaded -> DeckCardOrigin.Downloaded
                },
                cards = content.cards.map {
                    DeckDetailCard(it.id.value.toString(), it.question, it.answer)
                },
            )
        }
}
