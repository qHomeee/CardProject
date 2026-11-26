package com.example.cartproject.presentation.settings

import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.settings.ObserveSettings
import com.example.cartproject.domain.settings.ReminderPreference
import com.example.cartproject.domain.settings.SetStudyGestures
import com.example.cartproject.domain.settings.SetThemePreference
import com.example.cartproject.domain.settings.SettingsStore
import com.example.cartproject.domain.settings.ThemePreference
import com.example.cartproject.domain.settings.UserSettings
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Test
    fun failedThemeWriteRetainsSavedChoiceAndCanBeRetried() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val saved = UserSettings(
            theme = ThemePreference.Light,
            gesturesEnabled = true,
            reminder = ReminderPreference.On(LocalTime.NOON),
        )
        val store = SettingsFake(saved)
        val model = SettingsViewModel(
            ObserveSettings(store),
            SetStudyGestures(store),
            SetThemePreference(store),
        )
        try {
            runCurrent()
            store.reject = true
            model.changeTheme(ThemePreference.Dark)
            runCurrent()

            assertEquals(saved, model.uiState.value.settings)
            assertTrue(model.uiState.value.writeFailed)

            store.reject = false
            model.retryWrite()
            runCurrent()

            assertEquals(saved.copy(theme = ThemePreference.Dark), model.uiState.value.settings)
            assertFalse(model.uiState.value.writeFailed)
        } finally {
            model.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private class SettingsFake(initial: UserSettings) : SettingsStore {
        private val state = MutableStateFlow(initial)
        var reject = false

        override fun observe() = state.map { Outcome.Success(it) }

        override suspend fun save(settings: UserSettings): Outcome<Unit> {
            if (reject) return Outcome.Failure(AppError.LocalWrite)
            state.value = settings
            return Outcome.Success(Unit)
        }

        override suspend fun saveGesturesEnabled(enabled: Boolean) =
            save(state.value.copy(gesturesEnabled = enabled))

        override suspend fun saveTheme(preference: ThemePreference) =
            save(state.value.copy(theme = preference))

        override suspend fun saveReminder(preference: ReminderPreference) =
            save(state.value.copy(reminder = preference))
    }
}
