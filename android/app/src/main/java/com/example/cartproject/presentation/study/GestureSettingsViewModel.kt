package com.example.cartproject.presentation.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.settings.ObserveSettings
import com.example.cartproject.domain.settings.SetStudyGestures
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Состояние переключателя жестов в настройках. */
internal data class GestureSettingsState(
    val enabled: Boolean? = null,
    val saving: Boolean = false,
    val readFailed: Boolean = false,
    val writeFailed: Boolean = false,
)

/** Наблюдает и сохраняет предпочтение жестов. */
internal class GestureSettingsViewModel(
    private val observe: ObserveSettings,
    private val setGestures: SetStudyGestures,
) : ViewModel() {
    private val state = MutableStateFlow(GestureSettingsState())
    val uiState = state.asStateFlow()
    private var reading: Job? = null

    init {
        reload()
    }

    /** Повторяет чтение настройки. */
    fun reload() {
        if (reading?.isActive == true) return
        reading = viewModelScope.launch {
            observe().collect { result ->
                state.update {
                    when (result) {
                        is Outcome.Success -> it.copy(
                            enabled = result.value.gesturesEnabled,
                            readFailed = false,
                        )
                        is Outcome.Failure -> it.copy(enabled = null, readFailed = true)
                    }
                }
            }
        }
    }

    /** Записывает только переключатель, не затрагивая другие настройки. */
    fun change(enabled: Boolean) {
        val current = state.value
        if (current.enabled == null || current.saving) return
        state.value = current.copy(saving = true, writeFailed = false)
        viewModelScope.launch {
            val result = setGestures(enabled)
            state.update {
                when (result) {
                    is Outcome.Success -> it.copy(enabled = enabled, saving = false)
                    is Outcome.Failure -> it.copy(saving = false, writeFailed = true)
                }
            }
        }
    }
}
