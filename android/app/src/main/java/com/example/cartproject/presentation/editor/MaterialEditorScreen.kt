package com.example.cartproject.presentation.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cartproject.R
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.MaterialField
import com.example.cartproject.domain.decks.MaterialIssue
import com.example.cartproject.presentation.components.ConfirmRemovalDialog
import com.example.cartproject.presentation.components.DeckCard
import com.example.cartproject.presentation.components.StatusAction
import com.example.cartproject.presentation.components.StatusKind
import com.example.cartproject.presentation.components.StatusMessage
import com.example.cartproject.presentation.components.SubscreenTopBar
import com.example.cartproject.presentation.components.ZapomniButton
import com.example.cartproject.presentation.components.ZapomniTextField
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme

/** Отображает форму набора или карточки и локальные ошибки операций. */
@Composable
internal fun MaterialEditorScreen(
    viewModel: MaterialEditorViewModel,
    state: EditorUiState,
    onBack: () -> Unit,
    onOpenCard: (LocalCardId?) -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            SubscreenTopBar(R.string.editor_title, onBack)
            when (state) {
                EditorUiState.Loading -> LazyColumn(
                    contentPadding = PaddingValues(ZapomniSize.space16),
                ) {
                    item {
                        StatusMessage(
                            StatusKind.Loading,
                            stringResource(R.string.deck_detail_loading),
                        )
                    }
                }
                is EditorUiState.ReadError -> LazyColumn(
                    contentPadding = PaddingValues(ZapomniSize.space16),
                ) {
                    item {
                        EditorReadError(state.error, viewModel::retryRead)
                    }
                }
                is EditorUiState.Form -> EditorForm(viewModel, state, onOpenCard)
            }
        }
    }
}

@Composable
private fun EditorReadError(error: AppError, onRetry: () -> Unit) {
    val message = when (error) {
        AppError.CardNotFound -> R.string.editor_card_missing
        AppError.DeckNotFound -> R.string.deck_detail_missing
        AppError.ReadOnlyDeck -> R.string.editor_owned_only
        else -> R.string.deck_detail_read_error
    }
    StatusMessage(
        kind = StatusKind.Error,
        title = stringResource(message),
        action = if (error == AppError.LocalRead) {
            StatusAction(stringResource(R.string.retry), onRetry)
        } else {
            null
        },
    )
}

@Composable
private fun EditorForm(
    editor: MaterialEditorViewModel,
    form: EditorUiState.Form,
    onOpenCard: (LocalCardId?) -> Unit,
) {
    val target = editor.target
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var confirmRemoval by rememberSaveable { mutableStateOf(false) }
    val isDeck = target is EditorTarget.Deck
    val existing = when (target) {
        is EditorTarget.Deck -> target.id != null
        is EditorTarget.Card -> target.id != null
    }
    val title = when (target) {
        is EditorTarget.Deck -> {
            if (existing) R.string.editor_deck_update else R.string.editor_deck_new
        }
        is EditorTarget.Card -> {
            if (existing) R.string.editor_card_update else R.string.editor_card_new
        }
    }

    LaunchedEffect(form.invalidFields) {
        if (form.invalidFields.isNotEmpty()) listState.scrollToItem(1)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(ZapomniSize.space16),
        verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12),
    ) {
        item(key = "title") { EditorHeading(title) }
        item(key = "fields") {
            Column(verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12)) {
                EditorFields(isDeck, form, editor::updatePrimary, editor::updateSecondary)
            }
        }
        if (isDeck) {
            item(key = "cards-heading") { EditorHeading(R.string.deck_detail_cards) }
            if (form.cardListError) {
                item(key = "cards-error") {
                    StatusMessage(
                        kind = StatusKind.Error,
                        title = stringResource(R.string.editor_cards_failed),
                        action = StatusAction(stringResource(R.string.retry), editor::refreshCards),
                    )
                }
            } else {
                items(form.cards, key = { card -> card.id.value }) { card ->
                    DeckCard(
                        title = card.question,
                        origin = null,
                        details = card.answer,
                        onClick = if (form.editable) {
                            { onOpenCard(card.id) }
                        } else {
                            null
                        },
                    )
                }
            }
            item(key = "add-card") {
                if (existing) {
                    ZapomniButton(
                        label = stringResource(R.string.editor_add_card),
                        onClick = { onOpenCard(null) },
                        secondary = true,
                        enabled = form.editable,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    EditorNote(R.string.editor_after_saving_set)
                }
            }
        } else if (existing) {
            item(key = "schedule-note") { EditorNote(R.string.editor_schedule_reset) }
        }
        form.failedOperation?.let { failed ->
            item(key = "write-error") {
                StatusMessage(
                    kind = StatusKind.Error,
                    title = stringResource(
                        if (failed == EditorOperation.Save) {
                            R.string.editor_save_failed
                        } else {
                            R.string.editor_delete_failed
                        },
                    ),
                    message = if (failed == EditorOperation.Save) {
                        stringResource(R.string.editor_draft_kept)
                    } else {
                        null
                    },
                )
            }
        }
        item(key = "save") {
            val label = when {
                form.operation == EditorOperation.Save -> R.string.editor_saving
                isDeck -> R.string.editor_save_deck
                else -> R.string.editor_save_card
            }
            ZapomniButton(
                label = stringResource(label),
                onClick = {
                    focusManager.clearFocus()
                    editor.save()
                },
                modifier = Modifier.fillMaxWidth(),
                loading = form.operation == EditorOperation.Save,
                enabled = form.editable,
            )
        }
        if (existing) {
            item(key = "remove") {
                val label = when {
                    form.operation == EditorOperation.Delete -> R.string.editor_removing
                    isDeck -> R.string.editor_delete_deck
                    else -> R.string.editor_delete_card
                }
                ZapomniButton(
                    label = stringResource(label),
                    onClick = { confirmRemoval = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = form.editable,
                    loading = form.operation == EditorOperation.Delete,
                    danger = true,
                )
            }
        }
    }
    if (confirmRemoval) {
        ConfirmRemovalDialog(
            title = if (isDeck) {
                R.string.editor_delete_deck_title
            } else {
                R.string.editor_delete_card_title
            },
            message = if (isDeck) {
                R.string.deck_remove_confirm_message
            } else {
                R.string.editor_delete_card_help
            },
            onConfirm = {
                confirmRemoval = false
                editor.delete()
            },
            onDismiss = { confirmRemoval = false },
        )
    }
}

@Composable
private fun EditorFields(
    isDeck: Boolean,
    form: EditorUiState.Form,
    onPrimary: (String) -> Unit,
    onSecondary: (String) -> Unit,
) {
    val primary = if (isDeck) MaterialField.Title else MaterialField.Question
    val secondary = if (isDeck) MaterialField.Description else MaterialField.Answer
    ZapomniTextField(
        value = form.primaryText,
        onValueChange = onPrimary,
        label = stringResource(if (isDeck) R.string.editor_name else R.string.editor_question),
        placeholder = stringResource(
            if (isDeck) R.string.editor_enter_name else R.string.editor_enter_question,
        ),
        errorMessage = editorFieldError(primary, form.invalidFields[primary]),
        enabled = form.editable,
        singleLine = isDeck,
    )
    ZapomniTextField(
        value = form.secondaryText,
        onValueChange = onSecondary,
        label = stringResource(if (isDeck) R.string.editor_description else R.string.editor_answer),
        placeholder = stringResource(
            if (isDeck) R.string.editor_optional else R.string.editor_enter_answer,
        ),
        errorMessage = editorFieldError(secondary, form.invalidFields[secondary]),
        enabled = form.editable,
        singleLine = false,
    )
}

@Composable
private fun editorFieldError(field: MaterialField, issue: MaterialIssue?): String? = when (issue) {
    MaterialIssue.TooLong -> stringResource(R.string.editor_too_long, field.maxLength)
    MaterialIssue.UnsupportedCharacter -> stringResource(R.string.editor_invalid_characters)
    MaterialIssue.Required -> {
        val message = when (field) {
            MaterialField.Title -> R.string.editor_enter_name
            MaterialField.Question -> R.string.editor_enter_question
            MaterialField.Answer -> R.string.editor_enter_answer
            else -> R.string.editor_required
        }
        stringResource(message)
    }
    null -> null
}

@Composable
private fun EditorHeading(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun EditorNote(@StringRes label: Int) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.bodyMedium,
        color = ZapomniTheme.colors.textMuted,
    )
}
