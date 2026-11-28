package com.example.cartproject.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.settings.ObserveSettings
import com.example.cartproject.domain.settings.ReminderPreference
import com.example.cartproject.domain.settings.ReminderStatus
import com.example.cartproject.domain.settings.UpdateSettings
import java.time.LocalTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Операция настройки напоминания, которую нужно повторить. */
internal enum class ReminderSettingsError { Read, Write, Schedule }

/** Сохранённое предпочтение и состояние системного расписания. */
internal data class ReminderSettingsState(
    val preference: ReminderPreference? = null,
    val status: ReminderStatus? = null,
    val error: ReminderSettingsError? = null,
    val saving: Boolean = false,
) {
    val enabled: Boolean get() = preference is ReminderPreference.On
    val time: LocalTime get() = when (val selected = preference) {
        is ReminderPreference.Off -> selected.lastTime ?: DefaultReminderTime
        is ReminderPreference.On -> selected.time
        null -> DefaultReminderTime
    }
}

private val DefaultReminderTime = LocalTime.of(20, 0)

/** Сохраняет предпочтение и отдельно показывает фактическую доступность напоминания. */
internal class ReminderSettingsViewModel(
    private val observe: ObserveSettings,
    private val update: UpdateSettings,
    private val restoreSchedule: suspend () -> Outcome<ReminderStatus>,
) : ViewModel() {
    private val state = MutableStateFlow(ReminderSettingsState())
    val uiState = state.asStateFlow()
    private var reading: Job? = null
    private var failedWrite: ReminderPreference? = null

    /** Обновляет статус после возврата из системных настроек. */
    fun refresh() {
        if (state.value.saving || reading?.isActive == true) return
        reading = viewModelScope.launch {
            state.value = when (val saved = observe().first()) {
                is Outcome.Failure -> state.value.copy(error = ReminderSettingsError.Read)
                is Outcome.Success -> {
                    val current = reminderState(saved.value.reminder, restoreSchedule())
                    if (failedWrite ==
                        null
                    ) {
                        current
                    } else {
                        current.copy(error = ReminderSettingsError.Write)
                    }
                }
            }
        }
    }

    /** Повторяет незавершённую запись или согласует расписание. */
    fun retry() {
        val pending = failedWrite
        if (pending == null) refresh() else save(pending)
    }

    /** Меняет включение напоминания, сохраняя выбранное время. */
    fun changeEnabled(enabled: Boolean) {
        val current = state.value
        if (current.preference == null || current.enabled == enabled) return
        save(
            if (enabled) {
                ReminderPreference.On(
                    current.time,
                )
            } else {
                ReminderPreference.Off(current.time)
            },
        )
    }

    /** Сохраняет местное время и обновляет расписание для включённого напоминания. */
    fun changeTime(time: LocalTime) {
        val current = state.value
        if (current.preference == null) return
        save(if (current.enabled) ReminderPreference.On(time) else ReminderPreference.Off(time))
    }

    private fun save(preference: ReminderPreference) {
        if (state.value.saving) return
        reading?.cancel()
        failedWrite = null
        state.value = state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            state.value = when (val result = update.setReminder(preference)) {
                is Outcome.Success -> reminderState(result.value.preference, result.value.status)
                is Outcome.Failure -> {
                    failedWrite = preference
                    state.value.copy(saving = false, error = ReminderSettingsError.Write)
                }
            }
        }
    }

    private fun reminderState(preference: ReminderPreference, status: Outcome<ReminderStatus>) =
        when (status) {
            is Outcome.Success -> ReminderSettingsState(preference, status.value)
            is Outcome.Failure -> ReminderSettingsState(
                preference,
                error = ReminderSettingsError.Schedule,
            )
        }
}
