package com.example.cartproject.presentation.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cartproject.R
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.study.StudySummary
import com.example.cartproject.presentation.components.ReviewSummary
import com.example.cartproject.presentation.components.StatusAction
import com.example.cartproject.presentation.components.StatusKind
import com.example.cartproject.presentation.components.StatusMessage
import com.example.cartproject.presentation.components.SubscreenTopBar
import com.example.cartproject.presentation.components.ZapomniButton
import com.example.cartproject.presentation.theme.ZapomniSize

/** Показывает сводку по всем сохранённым занятиям. */
@Composable
internal fun StatisticsScreen(
    state: Outcome<StudySummary>?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            SubscreenTopBar(R.string.statistics_title, onBack)
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(ZapomniSize.space16),
                verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12),
            ) {
                when (state) {
                    null -> StatusMessage(
                        StatusKind.Loading,
                        stringResource(R.string.statistics_loading),
                    )
                    is Outcome.Failure -> StatusMessage(
                        kind = StatusKind.Error,
                        title = stringResource(R.string.statistics_error),
                        action = StatusAction(stringResource(R.string.retry), onRetry),
                    )
                    is Outcome.Success -> if (state.value.total == 0) {
                        StatusMessage(
                            kind = StatusKind.Empty,
                            title = stringResource(R.string.statistics_empty),
                            message = stringResource(R.string.statistics_empty_help),
                        )
                    } else {
                        ReviewSummary(state.value, stringResource(R.string.statistics_summary))
                    }
                }
                ZapomniButton(
                    label = stringResource(R.string.catalog_open_my_decks),
                    onClick = onBack,
                    secondary = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
