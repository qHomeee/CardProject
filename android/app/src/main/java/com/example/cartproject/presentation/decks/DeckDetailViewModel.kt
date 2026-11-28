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

/** Источник содержимого, выбранный при открытии экрана. */
internal sealed interface DeckSource {
    /** Серверный набор с идентификатором общего каталога. */
    data class Catalog(val id: CatalogDeckId) : DeckSource

    /** Собственный или скачанный набор из локальной базы. */
    data class Local(val id: LocalDeckId) : DeckSource
}

/** Содержимое набора в форме, которую показывает экран. */
internal data class DeckDetails(
    val title: String,
    val description: String,
    val origin: DeckCardOrigin,
    val cards: List<DeckDetailCard>,
)

/** Вопрос и ответ со стабильным ключом для списка. */
internal data class DeckDetailCard(val key: String, val question: String, val answer: String)

/** Состояния загрузки и удаления локальной копии. */
internal sealed interface DeckDetailAction {
    /** Операция записи сейчас отсутствует. */
    data object Idle : DeckDetailAction

    /** Сетевая загрузка и запись снимка ещё выполняются. */
    data object Downloading : DeckDetailAction

    /** Снимок сохранён и доступен по локальному идентификатору. */
    data class Downloaded(val id: LocalDeckId) : DeckDetailAction

    /** Локальный снимок и связанные записи удаляются. */
    data object Removing : DeckDetailAction

    /** Локальная копия удалена, экран может закрыться. */
    data object Removed : DeckDetailAction

    /** Операция записи завершилась ошибкой и допускает повтор. */
    data class Failed(val reason: AppError) : DeckDetailAction
}

/** Результат чтения, ссылка на снимок и состояние записи. */
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

    /** Повторяет чтение при отсутствии загруженных данных. */
    fun retryRead() {
        if (readJob?.isActive == true || state.value.result is Outcome.Success) return
        state.update { it.copy(result = null) }
        read()
    }

    /** Перечитывает свой набор после сохранения редактора. */
    fun refreshLocal() {
        val current = state.value
        if (source !is DeckSource.Local ||
            current.action != DeckDetailAction.Idle ||
            current.result !is Outcome.Success ||
            current.result.value.origin != DeckCardOrigin.Owned ||
            readJob?.isActive == true
        ) {
            return
        }
        read()
    }

    private fun read() {
        readJob = viewModelScope.launch {
            val result = when (source) {
                is DeckSource.Catalog -> readCatalog(source.id)
                is DeckSource.Local -> readLocal(source.id)
            }
            state.update { it.copy(result = result) }
        }
    }

    /** Скачивает выбранный набор и сохраняет полный снимок в локальной базе. */
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

    /** Удаляет локальный снимок и относящуюся к нему историю. */
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
