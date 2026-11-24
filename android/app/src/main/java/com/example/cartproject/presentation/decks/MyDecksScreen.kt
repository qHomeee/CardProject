package com.example.cartproject.presentation.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.cartproject.R
import com.example.cartproject.domain.decks.DeckOrigin
import com.example.cartproject.presentation.components.DeckCard
import com.example.cartproject.presentation.components.DeckCardOrigin
import com.example.cartproject.presentation.components.StatusKind
import com.example.cartproject.presentation.components.StatusMessage
import com.example.cartproject.presentation.theme.ZapomniSize

/** Отображает состояние чтения локальных наборов. */
@Composable
internal fun MyDecksScreen(state: MyDecksUiState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
        when (state) {
            MyDecksUiState.Loading -> item {
                StatusMessage(StatusKind.Loading, stringResource(R.string.my_decks_loading))
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
