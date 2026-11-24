package com.example.cartproject.presentation.decks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cartproject.R

/** Отображает состояние чтения локальных наборов. */
@Composable
internal fun MyDecksScreen(state: MyDecksUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.my_decks_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        when (state) {
            MyDecksUiState.Loading -> CircularProgressIndicator()
            is MyDecksUiState.Content -> {
                if (state.decks.isEmpty()) {
                    Text(stringResource(R.string.my_decks_empty))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.decks, key = { it.id }) { deck -> Text(deck.title) }
                    }
                }
            }
        }
    }
}
