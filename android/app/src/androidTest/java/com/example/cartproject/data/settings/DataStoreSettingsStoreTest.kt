package com.example.cartproject.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.settings.ReminderPreference
import com.example.cartproject.domain.settings.SettingsStore
import com.example.cartproject.domain.settings.ThemePreference
import com.example.cartproject.domain.settings.UserSettings
import java.io.File
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Проверяет файловые настройки DataStore на Android. */
@RunWith(AndroidJUnit4::class)
class DataStoreSettingsStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** После нового открытия сохраняются тема, жесты и время выключенного напоминания. */
    @Test
    fun restoresAllSettingsAfterReopen() = runBlocking {
        val file = settingsFile()
        val time = LocalTime.of(19, 30)
        val enabled = UserSettings(ThemePreference.Dark, true, ReminderPreference.On(time))
        val disabled = enabled.copy(reminder = ReminderPreference.Off(time))
        try {
            withStore(file) { store ->
                assertEquals(Outcome.Success(UserSettings()), store.observe().first())
                assertEquals(Outcome.Success(Unit), store.save(enabled))
            }
            withStore(file) { store ->
                assertEquals(Outcome.Success(enabled), store.observe().first())
                assertEquals(Outcome.Success(Unit), store.save(disabled))
            }
            withStore(file) { store ->
                assertEquals(Outcome.Success(disabled), store.observe().first())
            }
        } finally {
            file.delete()
        }
        Unit
    }

    /** Повреждённый файл остаётся доступен для восстановления, клиент получает ошибку. */
    @Test
    fun corruptPreferencesReportReadFailure() = runBlocking {
        val file = settingsFile()
        val invalid = byteArrayOf(0x0A, 0x7F)
        file.writeBytes(invalid)
        try {
            withStore(file) { store ->
                assertEquals(Outcome.Failure(AppError.LocalRead), store.observe().first())
            }
            assertArrayEquals(invalid, file.readBytes())
        } finally {
            file.delete()
        }
        Unit
    }

    /** Недоступное место записи возвращает доменную ошибку. */
    @Test
    fun inaccessibleFileReportsWriteFailure() = runBlocking {
        val file = settingsFile()
        check(file.mkdir())
        try {
            withStore(file) { store ->
                assertEquals(Outcome.Failure(AppError.LocalWrite), store.save(UserSettings()))
            }
        } finally {
            file.delete()
        }
        Unit
    }

    private fun settingsFile(): File =
        File(context.cacheDir, "settings-test-${UUID.randomUUID()}.preferences_pb")

    private suspend fun withStore(file: File, action: suspend (SettingsStore) -> Unit) {
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        try {
            val dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
            action(DataStoreSettingsStore(dataStore))
        } finally {
            job.cancelAndJoin()
        }
    }
}
