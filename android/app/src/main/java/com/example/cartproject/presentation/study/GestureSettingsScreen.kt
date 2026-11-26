package com.example.cartproject.presentation.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cartproject.R
import com.example.cartproject.presentation.components.StatusAction
import com.example.cartproject.presentation.components.StatusKind
import com.example.cartproject.presentation.components.StatusMessage
import com.example.cartproject.presentation.components.ZapomniSwitch
import com.example.cartproject.presentation.settings.ReminderSettings
import com.example.cartproject.presentation.theme.ZapomniSize
import org.koin.compose.viewmodel.koinViewModel

/** Показывает настройки жестов и напоминаний о повторении. */
@Composable
internal fun GestureSettingsScreen() {
    val viewModel = koinViewModel<GestureSettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(ZapomniSize.space16),
        verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12),
    ) {
        Text(
            text = stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        if (state.readFailed) {
            StatusMessage(
                kind = StatusKind.Error,
                title = stringResource(R.string.gesture_settings_read_error),
                action = StatusAction(stringResource(R.string.retry), viewModel::reload),
            )
        } else {
            ZapomniSwitch(
                label = stringResource(R.string.gesture_settings_label),
                checked = state.enabled == true,
                onCheckedChange = viewModel::change,
                enabled = state.enabled != null && !state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.writeFailed) {
            StatusMessage(StatusKind.Error, stringResource(R.string.gesture_settings_write_error))
        }
        ReminderSettings()
    }
}
