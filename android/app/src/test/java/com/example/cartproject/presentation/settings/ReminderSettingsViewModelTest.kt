package com.example.cartproject.presentation.settings

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.settings.ObserveSettings
import com.example.cartproject.domain.settings.ReminderPreference
import com.example.cartproject.domain.settings.ReminderStatus
import com.example.cartproject.domain.settings.SettingsStore
import com.example.cartproject.domain.settings.ThemePreference
import com.example.cartproject.domain.settings.UpdateSettings
import com.example.cartproject.domain.settings.UserSettings
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderSettingsViewModelTest {
    @Test
    fun denialKeepsPreferenceAndReturningAfterGrantRefreshesStatus() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val store = SettingsFake()
            var status = ReminderStatus.Disabled
            val viewModel = viewModel(store) { Outcome.Success(status) }
            viewModel.refresh()
            runCurrent()

            status = ReminderStatus.PermissionRequired
            viewModel.changeEnabled(true)
            runCurrent()
            assertTrue(viewModel.uiState.value.enabled)
            assertEquals(ReminderStatus.PermissionRequired, viewModel.uiState.value.status)

            status = ReminderStatus.Scheduled
            viewModel.refresh()
            runCurrent()
            assertEquals(ReminderStatus.Scheduled, viewModel.uiState.value.status)
            assertEquals(ReminderPreference.On(LocalTime.of(20, 0)), store.settings.reminder)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun retriesTheSelectedTimeAfterAWriteFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val time = LocalTime.of(19, 35)
            val store = SettingsFake()
            val viewModel = viewModel(store) { Outcome.Success(ReminderStatus.Disabled) }
            viewModel.refresh()
            runCurrent()

            store.fail = true
            viewModel.changeTime(time)
            runCurrent()
            assertEquals(ReminderSettingsError.Write, viewModel.uiState.value.error)
            assertEquals(ReminderPreference.Off(), store.settings.reminder)

            store.fail = false
            viewModel.retry()
            runCurrent()
            assertEquals(ReminderPreference.Off(time), store.settings.reminder)
            assertEquals(time, viewModel.uiState.value.time)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun keepsSavedTimeWhenSchedulingFailsAndRetriesWithoutWritingAgain() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val time = LocalTime.of(8, 15)
            val store = SettingsFake(UserSettings(reminder = ReminderPreference.On(LocalTime.NOON)))
            var result: Outcome<ReminderStatus> = Outcome.Success(ReminderStatus.Scheduled)
            val viewModel = viewModel(store) { result }
            viewModel.refresh()
            runCurrent()

            result = Outcome.Failure(AppError.ReminderSchedule)
            viewModel.changeTime(time)
            runCurrent()
            assertEquals(ReminderSettingsError.Schedule, viewModel.uiState.value.error)
            assertEquals(ReminderPreference.On(time), store.settings.reminder)

            store.fail = true
            result = Outcome.Success(ReminderStatus.Scheduled)
            viewModel.retry()
            runCurrent()
            assertEquals(
                ReminderSettingsState(ReminderPreference.On(time), ReminderStatus.Scheduled),
                viewModel.uiState.value,
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(store: SettingsFake, reconcile: () -> Outcome<ReminderStatus>) =
        ReminderSettingsViewModel(
            ObserveSettings(store),
            UpdateSettings(store) {
                reconcile()
            },
        ) { reconcile() }

    private class SettingsFake(var settings: UserSettings = UserSettings()) : SettingsStore {
        var fail = false

        override fun observe() = flowOf(Outcome.Success(settings))

        override suspend fun save(settings: UserSettings): Outcome<Unit> {
            if (fail) return Outcome.Failure(AppError.LocalWrite)
            this.settings = settings
            return Outcome.Success(Unit)
        }

        override suspend fun saveGesturesEnabled(enabled: Boolean) =
            save(settings.copy(gesturesEnabled = enabled))

        override suspend fun saveTheme(preference: ThemePreference) =
            save(settings.copy(theme = preference))

        override suspend fun saveReminder(preference: ReminderPreference) =
            save(settings.copy(reminder = preference))
    }
}
