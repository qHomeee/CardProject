package com.example.cartproject.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.settings.ReminderPreference
import com.example.cartproject.domain.settings.SettingsStore
import com.example.cartproject.domain.settings.ThemePreference
import com.example.cartproject.domain.settings.UserSettings
import java.io.IOException
import java.time.DateTimeException
import java.time.LocalTime
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.zapomniPreferences by preferencesDataStore(name = "zapomni_settings")

/** Открывает один DataStore настроек в процессе приложения. */
internal fun openSettingsStore(context: Context): SettingsStore =
    DataStoreSettingsStore(context.applicationContext.zapomniPreferences)

/** Читает и атомарно обновляет пользовательские предпочтения. */
internal class DataStoreSettingsStore(private val preferences: DataStore<Preferences>) :
    SettingsStore {
    override fun observe(): Flow<Outcome<UserSettings>> = preferences.data
        .map(::decodeSettings)
        .catch { failure ->
            if (failure !is IOException) throw failure
            currentCoroutineContext().ensureActive()
            emit(Outcome.Failure(AppError.LocalRead))
        }

    override suspend fun save(settings: UserSettings): Outcome<Unit> = write { stored ->
        stored[Theme] = settings.theme.name
        stored[Gestures] = settings.gesturesEnabled
        val time = when (val reminder = settings.reminder) {
            is ReminderPreference.On -> reminder.time
            is ReminderPreference.Off -> reminder.lastTime
        }
        stored[ReminderEnabled] = settings.reminder is ReminderPreference.On
        if (time ==
            null
        ) {
            stored.remove(ReminderTime)
        } else {
            stored[ReminderTime] = time.toString()
        }
    }

    override suspend fun saveGesturesEnabled(enabled: Boolean): Outcome<Unit> = write { stored ->
        stored[Gestures] = enabled
    }

    private suspend fun write(update: (MutablePreferences) -> Unit): Outcome<Unit> = try {
        preferences.edit { update(it) }
        Outcome.Success(Unit)
    } catch (_: IOException) {
        currentCoroutineContext().ensureActive()
        Outcome.Failure(AppError.LocalWrite)
    }

    private fun decodeSettings(stored: Preferences): Outcome<UserSettings> = try {
        val theme = stored[Theme]?.let { ThemePreference.valueOf(it) } ?: ThemePreference.System
        val time = stored[ReminderTime]?.let(LocalTime::parse)
        val reminder = if (stored[ReminderEnabled] == true) {
            ReminderPreference.On(requireNotNull(time))
        } else {
            ReminderPreference.Off(time)
        }
        Outcome.Success(UserSettings(theme, stored[Gestures] ?: false, reminder))
    } catch (failure: RuntimeException) {
        when (failure) {
            is IllegalArgumentException, is DateTimeException, is ClassCastException ->
                Outcome.Failure(AppError.LocalRead)
            else -> throw failure
        }
    }

    private companion object {
        val Theme = stringPreferencesKey("theme")
        val Gestures = booleanPreferencesKey("gestures")
        val ReminderEnabled = booleanPreferencesKey("reminder_enabled")
        val ReminderTime = stringPreferencesKey("reminder_local_time")
    }
}
