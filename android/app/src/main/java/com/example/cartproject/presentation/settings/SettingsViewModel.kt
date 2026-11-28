package com.example.cartproject.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.settings.ObserveSettings
import com.example.cartproject.domain.settings.SetStudyGestures
import com.example.cartproject.domain.settings.SetThemePreference
import com.example.cartproject.domain.settings.ThemePreference
import com.example.cartproject.domain.settings.UserSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Состояние настроек темы и жестов. */
internal data class SettingsUiState(
    val settings: UserSettings? = null,
    val saving: Boolean = false,
    val readFailed: Boolean = false,
    val writeFailed: Boolean = false,
)

/** Наблюдает настройки и меняет отдельные предпочтения. */
internal class SettingsViewModel(
    private val observe: ObserveSettings,
    private val setGestures: SetStudyGestures,
    private val setTheme: SetThemePreference,
) : ViewModel() {
    private val state = MutableStateFlow(SettingsUiState())
    val uiState = state.asStateFlow()
    private var reading: Job? = null
    private var failedWrite: (suspend () -> Outcome<Unit>)? = null

    init {
        reload()
    }

    /** Повторяет чтение настроек после ошибки. */
    fun reload() {
        if (reading?.isActive == true && !state.value.readFailed) return
        reading?.cancel()
        reading = viewModelScope.launch {
            observe().collect { result ->
                state.update {
                    when (result) {
                        is Outcome.Success -> it.copy(settings = result.value, readFailed = false)
                        is Outcome.Failure -> it.copy(settings = null, readFailed = true)
                    }
                }
            }
        }
    }

    /** Записывает настройку управления жестами. */
    fun changeGestures(enabled: Boolean) = save { setGestures(enabled) }

    /** Сохраняет выбранную тему и даёт экрану применить её. */
    fun changeTheme(preference: ThemePreference) = save { setTheme(preference) }

    /** Повторяет запись последнего выбранного значения. */
    fun retryWrite() {
        failedWrite?.let(::save)
    }

    private fun save(write: suspend () -> Outcome<Unit>) {
        val current = state.value
        if (current.settings == null || current.saving) return
        state.value = current.copy(saving = true, writeFailed = false)
        viewModelScope.launch {
            val failed = write() is Outcome.Failure
            failedWrite = if (failed) write else null
            state.update { it.copy(saving = false, writeFailed = failed) }
        }
    }
}
