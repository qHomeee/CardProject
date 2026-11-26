package com.example.cartproject.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cartproject.R
import com.example.cartproject.domain.settings.ThemePreference
import com.example.cartproject.presentation.components.StatusAction
import com.example.cartproject.presentation.components.StatusKind
import com.example.cartproject.presentation.components.StatusMessage
import com.example.cartproject.presentation.components.ZapomniButton
import com.example.cartproject.presentation.components.ZapomniSwitch
import com.example.cartproject.presentation.theme.ZapomniSize
import org.koin.compose.viewmodel.koinViewModel

/** Настройки внешнего вида, жестов и напоминаний. */
@Composable
internal fun SettingsScreen() {
    val viewModel = koinViewModel<SettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(ZapomniSize.space16),
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
                    title = stringResource(R.string.settings_read_error),
                    action = StatusAction(stringResource(R.string.retry), viewModel::reload),
                )
            } else {
                val settings = state.settings
                val editable = settings != null && !state.saving
                ThemePicker(
                    settings?.theme ?: ThemePreference.System,
                    editable,
                    viewModel::changeTheme,
                )
                ZapomniSwitch(
                    label = stringResource(R.string.gesture_settings_label),
                    checked = settings?.gesturesEnabled == true,
                    onCheckedChange = viewModel::changeGestures,
                    enabled = editable,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.writeFailed) {
                StatusMessage(
                    kind = StatusKind.Error,
                    title = stringResource(R.string.settings_write_error),
                    action = StatusAction(stringResource(R.string.retry), viewModel::retryWrite),
                )
            }
            ReminderSettings()
        }
    }
}

@Composable
private fun ThemePicker(
    preference: ThemePreference,
    enabled: Boolean,
    onSelect: (ThemePreference) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        ZapomniButton(
            label = stringResource(
                R.string.theme_settings_label,
                stringResource(preference.label()),
            ),
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            secondary = true,
            enabled = enabled,
        )
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            modifier = Modifier.selectableGroup(),
        ) {
            for (option in ThemePreference.entries) {
                val current = option == preference
                DropdownMenuItem(
                    text = { Text(stringResource(option.label())) },
                    leadingIcon = { RadioButton(selected = current, onClick = null) },
                    onClick = {
                        expanded = false
                        if (!current) onSelect(option)
                    },
                    modifier = Modifier.semantics {
                        selected = current
                        role = Role.RadioButton
                    },
                )
            }
        }
    }
}

private fun ThemePreference.label(): Int = when (this) {
    ThemePreference.System -> R.string.theme_system
    ThemePreference.Light -> R.string.theme_light
    ThemePreference.Dark -> R.string.theme_dark
}
