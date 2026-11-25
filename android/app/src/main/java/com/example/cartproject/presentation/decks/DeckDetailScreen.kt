package com.example.cartproject.presentation.decks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cartproject.R
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.presentation.components.ConfirmRemovalDialog
import com.example.cartproject.presentation.components.DeckCardOrigin
import com.example.cartproject.presentation.components.StatusAction
import com.example.cartproject.presentation.components.StatusKind
import com.example.cartproject.presentation.components.StatusMessage
import com.example.cartproject.presentation.components.SubscreenTopBar
import com.example.cartproject.presentation.components.ZapomniButton
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme

/** Показывает вопросы и ответы выбранного набора и доступные действия. */
@Composable
internal fun DeckDetailScreen(
    state: DeckDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
    onOpenCopy: (LocalDeckId) -> Unit,
    onRemoveCopy: () -> Unit,
    onEdit: (() -> Unit)?,
    onAddCard: (() -> Unit)?,
    onStartStudy: (() -> Unit)?,
) {
    var confirmRemoval by rememberSaveable { mutableStateOf(false) }
    val detail = (state.result as? Outcome.Success)?.value
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            SubscreenTopBar(R.string.deck_detail_title, onBack)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(ZapomniSize.space16),
                verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12),
            ) {
                when (val result = state.result) {
                    null -> item {
                        StatusMessage(
                            StatusKind.Loading,
                            stringResource(R.string.deck_detail_loading),
                        )
                    }
                    is Outcome.Failure -> item {
                        StatusMessage(
                            kind = StatusKind.Error,
                            title = stringResource(
                                if (result.error == AppError.DeckNotFound) {
                                    R.string.deck_detail_missing
                                } else {
                                    R.string.deck_detail_read_error
                                },
                            ),
                            action = StatusAction(stringResource(R.string.retry), onRetry),
                        )
                    }
                    is Outcome.Success -> {
                        val deck = result.value
                        item { DeckSummary(deck) }
                        if (deck.origin == DeckCardOrigin.Downloaded) {
                            item {
                                StatusMessage(
                                    kind = StatusKind.Success,
                                    title = stringResource(R.string.deck_detail_saved),
                                    message = stringResource(R.string.deck_detail_saved_help),
                                )
                            }
                        }
                        if (deck.cards.isEmpty()) {
                            item {
                                StatusMessage(
                                    kind = StatusKind.Empty,
                                    title = stringResource(R.string.deck_detail_empty),
                                    action = if (deck.origin == DeckCardOrigin.Owned &&
                                        onAddCard != null
                                    ) {
                                        StatusAction(
                                            stringResource(R.string.editor_add_card),
                                            onAddCard,
                                        )
                                    } else {
                                        null
                                    },
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = stringResource(R.string.deck_detail_cards),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.semantics { heading() },
                                )
                            }
                            items(deck.cards, key = { it.key }) { card -> QuestionAndAnswer(card) }
                        }
                    }
                }
            }
            val canStudy = onStartStudy != null && detail?.cards?.isNotEmpty() == true
            if (canStudy) {
                ZapomniButton(
                    label = stringResource(R.string.study_start),
                    onClick = onStartStudy,
                    modifier = Modifier.fillMaxWidth().padding(
                        start = ZapomniSize.space16,
                        end = ZapomniSize.space16,
                        bottom = ZapomniSize.space8,
                    ),
                )
            }
            if (detail?.origin == DeckCardOrigin.Owned && onEdit != null) {
                ZapomniButton(
                    label = stringResource(R.string.editor_edit_deck),
                    onClick = onEdit,
                    secondary = canStudy,
                    modifier = Modifier.fillMaxWidth().padding(ZapomniSize.space16),
                )
            } else {
                DeckDetailActions(
                    state = state,
                    origin = detail?.origin,
                    onDownload = onDownload,
                    onOpenCopy = onOpenCopy,
                    onRemove = { confirmRemoval = true },
                )
            }
        }
    }
    if (confirmRemoval) {
        ConfirmRemovalDialog(
            title = R.string.deck_remove_confirm_title,
            message = R.string.deck_remove_confirm_message,
            onConfirm = {
                confirmRemoval = false
                onRemoveCopy()
            },
            onDismiss = { confirmRemoval = false },
        )
    }
}

@Composable
private fun DeckSummary(detail: DeckDetails) {
    val palette = ZapomniTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.primarySoft,
        contentColor = palette.onPrimarySoft,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(ZapomniSize.space16),
            verticalArrangement = Arrangement.spacedBy(ZapomniSize.space4),
        ) {
            Text(stringResource(detail.origin.label), style = MaterialTheme.typography.labelLarge)
            Text(
                text = detail.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            if (detail.description.isNotBlank()) {
                Text(detail.description, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                stringResource(R.string.deck_card_count, detail.cards.size),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun QuestionAndAnswer(card: DeckDetailCard) {
    val palette = ZapomniTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = palette.surface,
        contentColor = palette.text,
        border = BorderStroke(1.dp, palette.border),
    ) {
        Column(
            modifier = Modifier.padding(ZapomniSize.space12),
            verticalArrangement = Arrangement.spacedBy(ZapomniSize.space4),
        ) {
            Text(card.question, style = MaterialTheme.typography.bodyLarge)
            Text(
                card.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted,
            )
        }
    }
}

@Composable
private fun DeckDetailActions(
    state: DeckDetailUiState,
    origin: DeckCardOrigin?,
    onDownload: () -> Unit,
    onOpenCopy: (LocalDeckId) -> Unit,
    onRemove: () -> Unit,
) {
    val buttonLabel: Int
    val action: () -> Unit
    val removing = state.action == DeckDetailAction.Removing
    val downloading = state.action == DeckDetailAction.Downloading
    val failure = state.action as? DeckDetailAction.Failed
    val copy = state.savedCopy
    when {
        copy != null -> {
            buttonLabel = R.string.deck_open_copy
            action = { onOpenCopy(copy) }
        }
        origin == DeckCardOrigin.Catalog -> {
            buttonLabel = when {
                downloading -> R.string.deck_downloading
                failure != null -> R.string.deck_download_retry
                else -> R.string.deck_download
            }
            action = onDownload
        }
        origin == DeckCardOrigin.Downloaded -> {
            buttonLabel = if (removing) R.string.deck_removing else R.string.deck_remove
            action = onRemove
        }
        else -> return
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            start = ZapomniSize.space16,
            end = ZapomniSize.space16,
            bottom = ZapomniSize.space16,
        ),
        verticalArrangement = Arrangement.spacedBy(ZapomniSize.space8),
    ) {
        if (failure != null) {
            val message = when {
                origin == DeckCardOrigin.Downloaded -> R.string.deck_remove_failed
                failure.reason == AppError.Network -> R.string.deck_download_network
                else -> R.string.deck_download_failed
            }
            Text(
                stringResource(message),
                color = ZapomniTheme.colors.danger,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        ZapomniButton(
            label = stringResource(buttonLabel),
            onClick = action,
            modifier = Modifier.fillMaxWidth(),
            loading = removing || downloading,
            secondary = origin == DeckCardOrigin.Downloaded,
            enabled = state.action != DeckDetailAction.Removed,
        )
    }
}
