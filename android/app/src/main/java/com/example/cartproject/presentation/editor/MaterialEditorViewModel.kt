package com.example.cartproject.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.Card
import com.example.cartproject.domain.decks.DeckOrigin
import com.example.cartproject.domain.decks.DeleteOwnMaterial
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckContent
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.decks.MaterialField
import com.example.cartproject.domain.decks.MaterialIssue
import com.example.cartproject.domain.decks.ReadDeck
import com.example.cartproject.domain.decks.SaveOwnCard
import com.example.cartproject.domain.decks.SaveOwnDeck
import com.example.cartproject.domain.flatMap
import com.example.cartproject.domain.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Материал, для которого открыта форма. Пустой ID означает создание записи. */
internal sealed interface EditorTarget {
    /** Создание набора или изменение набора по его ID. */
    data class Deck(val id: LocalDeckId?) : EditorTarget

    /** Создание карточки в наборе или изменение карточки по её ID. */
    data class Card(val deckId: LocalDeckId, val id: LocalCardId?) : EditorTarget
}

/** Действие записи, которое сейчас выполняется или завершилось ошибкой. */
internal enum class EditorOperation { Save, Delete }

/** Успешное завершение записи для перехода между экранами. */
internal sealed interface EditorCompletion {
    /** Сохранённый набор с локальным идентификатором. */
    data class DeckSaved(val id: LocalDeckId) : EditorCompletion

    /** Сохранённая карточка возвращает пользователя к набору. */
    data object CardSaved : EditorCompletion

    /** Выбранный набор или карточка удалены. */
    data object Deleted : EditorCompletion
}

/** Загрузка исходных данных и состояние редактирования. */
internal sealed interface EditorUiState {
    /** Исходный материал ещё читается из базы. */
    data object Loading : EditorUiState

    /** Форму нельзя заполнить из-за ошибки чтения. */
    data class ReadError(val error: AppError) : EditorUiState

    /** Черновик полей, список карточек и результаты проверки ввода. */
    data class Form(
        val primaryText: String = "",
        val secondaryText: String = "",
        val cards: List<Card> = emptyList(),
        val cardListError: Boolean = false,
        val invalidFields: Map<MaterialField, MaterialIssue> = emptyMap(),
        val operation: EditorOperation? = null,
        val failedOperation: EditorOperation? = null,
        val completion: EditorCompletion? = null,
    ) : EditorUiState {
        val editable: Boolean get() = operation == null && completion == null
    }
}

/** Хранит черновик редактора и запускает локальные операции записи. */
internal class MaterialEditorViewModel(
    val target: EditorTarget,
    private val readDeck: ReadDeck,
    private val saveOwnDeck: SaveOwnDeck,
    private val saveOwnCard: SaveOwnCard,
    private val deleteOwnMaterial: DeleteOwnMaterial,
) : ViewModel() {
    private val deckId = when (target) {
        is EditorTarget.Deck -> target.id
        is EditorTarget.Card -> target.deckId
    }
    private val state = MutableStateFlow<EditorUiState>(EditorUiState.Loading)
    val uiState = state.asStateFlow()
    private var readJob: Job? = null

    init {
        retryRead()
    }

    /** Загружает исходные поля, если форма ещё не открыта. */
    fun retryRead() {
        if (readJob?.isActive == true || state.value is EditorUiState.Form) return
        val id = deckId
        if (id == null) {
            state.value = EditorUiState.Form()
            return
        }
        state.value = EditorUiState.Loading
        readJob = viewModelScope.launch {
            state.value = when (val result = readOwnDeck(id).flatMap(::makeForm)) {
                is Outcome.Success -> result.value
                is Outcome.Failure -> EditorUiState.ReadError(result.error)
            }
        }
    }

    /** Обновляет карточки после возврата из формы карточки, сохраняя черновик набора. */
    fun refreshCards() {
        val id = (target as? EditorTarget.Deck)?.id ?: return
        if (readJob?.isActive == true || state.value !is EditorUiState.Form) return
        readJob = viewModelScope.launch {
            val result = readOwnDeck(id)
            updateForm { form ->
                when (result) {
                    is Outcome.Success -> form.copy(
                        cards = result.value.cards,
                        cardListError = false,
                    )
                    is Outcome.Failure -> form.copy(cardListError = true)
                }
            }
        }
    }

    /** Меняет первое поле и убирает только его прежнюю ошибку. */
    fun updatePrimary(text: String) = updateForm { form ->
        if (!form.editable) return@updateForm form
        val field = if (target is EditorTarget.Deck) MaterialField.Title else MaterialField.Question
        form.copy(primaryText = text, invalidFields = form.invalidFields - field)
    }

    /** Меняет второе поле и убирает только его прежнюю ошибку. */
    fun updateSecondary(text: String) = updateForm { form ->
        if (!form.editable) return@updateForm form
        val field = if (target is EditorTarget.Deck) {
            MaterialField.Description
        } else {
            MaterialField.Answer
        }
        form.copy(secondaryText = text, invalidFields = form.invalidFields - field)
    }

    /** Валидирует и сохраняет текущие поля. */
    fun save() = write(EditorOperation.Save) { form ->
        val primary = form.primaryText
        val secondary = form.secondaryText
        when (target) {
            is EditorTarget.Deck -> saveOwnDeck(target.id, primary, secondary)
                .map { EditorCompletion.DeckSaved(it) }
            is EditorTarget.Card -> saveOwnCard(target.deckId, target.id, primary, secondary)
                .map { EditorCompletion.CardSaved }
        }
    }

    /** Удаляет существующий материал после подтверждения на экране. */
    fun delete() {
        when (target) {
            is EditorTarget.Deck -> {
                val id = target.id ?: return
                write(EditorOperation.Delete) {
                    deleteOwnMaterial.deck(id).map { EditorCompletion.Deleted }
                }
            }
            is EditorTarget.Card -> {
                val id = target.id ?: return
                write(EditorOperation.Delete) {
                    deleteOwnMaterial.card(target.deckId, id).map { EditorCompletion.Deleted }
                }
            }
        }
    }

    private fun write(
        kind: EditorOperation,
        operation: suspend (EditorUiState.Form) -> Outcome<EditorCompletion>,
    ) {
        val form = state.value as? EditorUiState.Form ?: return
        if (!form.editable) return
        state.value =
            form.copy(operation = kind, failedOperation = null, invalidFields = emptyMap())
        viewModelScope.launch {
            val result = operation(form)
            updateForm { current ->
                when (result) {
                    is Outcome.Success -> current.copy(operation = null, completion = result.value)
                    is Outcome.Failure -> {
                        val invalid = result.error as? AppError.InvalidFields
                        current.copy(
                            operation = null,
                            invalidFields = invalid?.fields.orEmpty(),
                            failedOperation = kind.takeIf { invalid == null },
                        )
                    }
                }
            }
        }
    }

    private suspend fun readOwnDeck(id: LocalDeckId): Outcome<LocalDeckContent> =
        readDeck(id).flatMap { content ->
            if (content.deck.origin == DeckOrigin.Owned) {
                Outcome.Success(content)
            } else {
                Outcome.Failure(AppError.ReadOnlyDeck)
            }
        }

    private fun makeForm(content: LocalDeckContent): Outcome<EditorUiState.Form> = when (target) {
        is EditorTarget.Deck -> Outcome.Success(
            EditorUiState.Form(content.deck.title, content.deck.description, content.cards),
        )
        is EditorTarget.Card -> {
            val cardId = target.id
            if (cardId == null) {
                Outcome.Success(EditorUiState.Form())
            } else {
                val card = content.cards.find { it.id == cardId }
                if (card == null) {
                    Outcome.Failure(AppError.CardNotFound)
                } else {
                    Outcome.Success(EditorUiState.Form(card.question, card.answer))
                }
            }
        }
    }

    private fun updateForm(transform: (EditorUiState.Form) -> EditorUiState.Form) {
        state.update { current ->
            if (current is EditorUiState.Form) transform(current) else current
        }
    }
}
