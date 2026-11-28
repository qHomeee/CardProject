package com.example.cartproject.presentation.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cartproject.R
import com.example.cartproject.domain.decks.DeckOrigin
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.presentation.components.DeckCard
import com.example.cartproject.presentation.components.DeckCardOrigin
import com.example.cartproject.presentation.components.StatusAction
import com.example.cartproject.presentation.components.StatusKind
import com.example.cartproject.presentation.components.StatusMessage
import com.example.cartproject.presentation.components.ZapomniButton
import com.example.cartproject.presentation.theme.ZapomniSize

/** Отображает состояние чтения локальных наборов. */
@Composable
internal fun MyDecksScreen(
    state: MyDecksUiState,
    onRetry: () -> Unit,
    onOpenDeck: (LocalDeckId) -> Unit,
    onCreateDeck: () -> Unit,
    onOpenStatistics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
            contentPadding = PaddingValues(ZapomniSize.space16),
            verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12),
        ) {
            item {
                Text(
                    text = stringResource(R.string.my_decks_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            item {
                ZapomniButton(
                    label = stringResource(R.string.create_deck),
                    onClick = onCreateDeck,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                ZapomniButton(
                    label = stringResource(R.string.statistics_action),
                    onClick = onOpenStatistics,
                    secondary = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            when (state) {
                MyDecksUiState.Loading -> item {
                    StatusMessage(StatusKind.Loading, stringResource(R.string.my_decks_loading))
                }
                MyDecksUiState.Error -> item {
                    StatusMessage(
                        kind = StatusKind.Error,
                        title = stringResource(R.string.my_decks_read_error),
                        action = StatusAction(stringResource(R.string.retry), onRetry),
                    )
                }
                is MyDecksUiState.Content -> {
                    if (state.decks.isEmpty()) {
                        item {
                            StatusMessage(StatusKind.Empty, stringResource(R.string.my_decks_empty))
                        }
                    } else {
                        items(state.decks, key = { it.id.value }) { deck ->
                            DeckCard(
                                title = deck.title,
                                details = deck.description,
                                onClick = { onOpenDeck(deck.id) },
                                origin = when (deck.origin) {
                                    DeckOrigin.Owned -> DeckCardOrigin.Owned
                                    is DeckOrigin.Downloaded -> DeckCardOrigin.Downloaded
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
