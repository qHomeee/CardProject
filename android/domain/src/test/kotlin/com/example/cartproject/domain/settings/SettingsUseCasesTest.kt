package com.example.cartproject.domain.settings

import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Проверяет настройки и обработку сбоев согласования напоминаний. */
class SettingsUseCasesTest {
    private val store = SettingsStoreFake()
    private val changed = UserSettings(
        theme = ThemePreference.Dark,
        gesturesEnabled = true,
        reminder = ReminderPreference.On(LocalTime.of(19, 30)),
    )

    /** По умолчанию напоминания выключены. Подписка видит сохранённый выбор. */
    @Test
    fun observesDefaultsAndUpdates() = runTest {
        val defaults = (ObserveSettings(store)().first() as Outcome.Success).value
        assertEquals(ReminderPreference.Off(), defaults.reminder)
        val scheduler = ReminderScheduler { Outcome.Success(ReminderStatus.Scheduled) }
        UpdateSettings(store, scheduler)(changed)
        assertEquals(Outcome.Success(changed), ObserveSettings(store)().first())
    }

    /** Планирование происходит после успешной записи настроек. */
    @Test
    fun permissionDenialKeepsSettings() = runTest {
        val decision = Outcome.Success(ReminderStatus.PermissionRequired)
        val scheduler = ReminderScheduler {
            assertEquals(Outcome.Success(changed), store.observe().first())
            decision
        }
        assertEquals(
            Outcome.Success(SettingsUpdate(changed, decision)),
            UpdateSettings(store, scheduler)(changed),
        )
    }

    /** Сбой хранилища не меняет будущую задачу. */
    @Test
    fun doesNotScheduleAfterStorageError() = runTest {
        var scheduled = false
        store.reject = true
        val scheduler = ReminderScheduler {
            scheduled = true
            Outcome.Success(ReminderStatus.Scheduled)
        }
        assertEquals(
            Outcome.Failure(AppError.LocalWrite),
            UpdateSettings(store, scheduler)(changed),
        )
        assertFalse(scheduled)
        assertEquals(Outcome.Success(UserSettings()), ObserveSettings(store)().first())
    }

    /** Тема сохраняется независимо от событий напоминаний и жестов. */
    @Test
    fun changesOnlyTheme() = runTest {
        store.save(changed)
        val expected = changed.copy(theme = ThemePreference.Light)
        assertEquals(Outcome.Success(Unit), SetThemePreference(store)(expected.theme))
        assertEquals(Outcome.Success(expected), ObserveSettings(store)().first())
    }

    /** Изменение напоминания оставляет тему и жесты прежними. */
    @Test
    fun changesOnlyReminderAndReturnsSchedulingStatus() = runTest {
        store.save(changed)
        val reminder = ReminderPreference.Off((changed.reminder as ReminderPreference.On).time)
        val status = Outcome.Success(ReminderStatus.Disabled)
        val update = UpdateSettings(store) { status }
        assertEquals(
            Outcome.Success(ReminderUpdate(reminder, status)),
            update.setReminder(reminder),
        )
        assertEquals(Outcome.Success(changed.copy(reminder = reminder)), store.observe().first())
    }

    /** Ошибка планирования не отменяет уже сохранённый выбор. */
    @Test
    fun reportsSchedulingFailureSeparatelyFromSavedSettings() = runTest {
        val error = Outcome.Failure(AppError.ReminderSchedule)
        val useCase = UpdateSettings(store) { error }
        assertEquals(Outcome.Success(SettingsUpdate(changed, error)), useCase(changed))
        assertEquals(Outcome.Success(changed), ObserveSettings(store)().first())
    }
}

/** Подменяет DataStore в проверках use cases. */
private class SettingsStoreFake : SettingsStore {
    private val state = MutableStateFlow<Outcome<UserSettings>>(Outcome.Success(UserSettings()))
    var reject = false

    override fun observe() = state

    override suspend fun saveGesturesEnabled(enabled: Boolean): Outcome<Unit> {
        val current = (state.value as Outcome.Success).value
        return save(current.copy(gesturesEnabled = enabled))
    }

    override suspend fun saveTheme(preference: ThemePreference): Outcome<Unit> {
        val current = (state.value as Outcome.Success).value
        return save(current.copy(theme = preference))
    }

    override suspend fun saveReminder(preference: ReminderPreference): Outcome<Unit> {
        val current = (state.value as Outcome.Success).value
        return save(current.copy(reminder = preference))
    }

    override suspend fun save(settings: UserSettings): Outcome<Unit> {
        if (reject) return Outcome.Failure(AppError.LocalWrite)
        state.value = Outcome.Success(settings)
        return Outcome.Success(Unit)
    }
}
