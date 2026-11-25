package com.example.cartproject.presentation.catalog

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.cartproject.R
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.CatalogDeckSummary
import com.example.cartproject.domain.decks.MaterialIssue
import com.example.cartproject.presentation.components.DeckCard
import com.example.cartproject.presentation.components.DeckCardOrigin
import com.example.cartproject.presentation.components.StatusAction
import com.example.cartproject.presentation.components.StatusKind
import com.example.cartproject.presentation.components.StatusMessage
import com.example.cartproject.presentation.components.ZapomniButton
import com.example.cartproject.presentation.components.ZapomniTextField
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme

/** Показывает серверные наборы и состояние последнего запроса. */
@Composable
internal fun CatalogScreen(
    state: CatalogUiState,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenDecks: () -> Unit,
    onOpenDeck: (CatalogDeckId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val defaultQuery = state.query.isBlank()
    val result = state.result
    val decks = (result as? CatalogResult.Content)?.decks.orEmpty()
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
            contentPadding = PaddingValues(ZapomniSize.space16),
            verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12),
        ) {
            item("title") {
                Column(verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12)) {
                    Text(
                        text = stringResource(R.string.tab_catalog),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    HorizontalDivider(color = ZapomniTheme.colors.border)
                }
            }
            if (decks.isNotEmpty() && defaultQuery) {
                item("introduction") { CatalogIntroduction() }
            }
            item("search") {
                CatalogSearch(state, onQueryChange)
            }
            if (decks.isNotEmpty()) {
                item("results-heading") {
                    Text(
                        text = stringResource(
                            if (defaultQuery) R.string.catalog_all else R.string.catalog_results,
                        ),
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(decks, key = { it.id.value.toString() }) { deck ->
                    CatalogDeckCard(deck) { onOpenDeck(deck.id) }
                }
            } else if (result !is CatalogResult.InvalidQuery) {
                item("status") {
                    CatalogStatus(
                        result = result,
                        defaultQuery = defaultQuery,
                        onRetry = onRetry,
                        onOpenDecks = onOpenDecks,
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogSearch(state: CatalogUiState, onQueryChange: (String) -> Unit) {
    val failure = state.result as? CatalogResult.InvalidQuery
    ZapomniTextField(
        value = state.query,
        onValueChange = onQueryChange,
        label = stringResource(R.string.catalog_search_label),
        placeholder = stringResource(R.string.catalog_search_hint),
        errorMessage = failure?.let {
            stringResource(
                if (it.issue == MaterialIssue.TooLong) {
                    R.string.catalog_search_too_long
                } else {
                    R.string.catalog_search_invalid
                },
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
                tint = ZapomniTheme.colors.textMuted,
            )
        },
    )
}

@Composable
private fun CatalogIntroduction() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = ZapomniTheme.colors.primarySoft,
        contentColor = ZapomniTheme.colors.onPrimarySoft,
    ) {
        Column(
            Modifier.padding(ZapomniSize.space16),
            verticalArrangement = Arrangement.spacedBy(ZapomniSize.space4),
        ) {
            Text(
                stringResource(R.string.catalog_intro_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(R.string.catalog_intro_body),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun CatalogDeckCard(deck: CatalogDeckSummary, onClick: () -> Unit) {
    val count = stringResource(R.string.deck_card_count, deck.cardCount)
    val details = if (deck.description.isBlank()) {
        count
    } else {
        stringResource(R.string.catalog_deck_details, count, deck.description)
    }
    DeckCard(deck.title, DeckCardOrigin.Catalog, details = details, onClick = onClick)
}

@Composable
private fun CatalogStatus(
    result: CatalogResult,
    defaultQuery: Boolean,
    onRetry: () -> Unit,
    onOpenDecks: () -> Unit,
) {
    when (result) {
        CatalogResult.Loading -> StatusMessage(
            kind = StatusKind.Loading,
            title = stringResource(R.string.catalog_loading),
        )
        is CatalogResult.Content -> StatusMessage(
            kind = StatusKind.Empty,
            title = stringResource(
                if (defaultQuery) R.string.catalog_empty else R.string.catalog_no_matches,
            ),
            message = if (defaultQuery) null else stringResource(R.string.catalog_no_matches_help),
        )
        CatalogResult.Offline -> Column(
            verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12),
        ) {
            StatusMessage(
                kind = StatusKind.Offline,
                title = stringResource(R.string.catalog_offline),
                message = stringResource(R.string.catalog_offline_help),
                action = StatusAction(stringResource(R.string.retry), onRetry),
            )
            ZapomniButton(
                label = stringResource(R.string.catalog_open_my_decks),
                onClick = onOpenDecks,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is CatalogResult.Error -> StatusMessage(
            kind = StatusKind.Error,
            title = stringResource(R.string.catalog_load_error),
            message = stringResource(
                when (result.reason) {
                    AppError.Network -> R.string.catalog_network_error
                    AppError.CatalogTimeout -> R.string.catalog_timeout
                    AppError.InvalidCatalogData -> R.string.catalog_bad_response
                    else -> R.string.catalog_server_error
                },
            ),
            action = StatusAction(stringResource(R.string.retry), onRetry),
        )
        is CatalogResult.InvalidQuery -> Unit
    }
}
