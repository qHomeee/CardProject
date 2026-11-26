package com.example.cartproject.domain.settings

import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.map
import kotlinx.coroutines.flow.Flow

/** Хранилище настроек независимо от реализации DataStore. */
interface SettingsStore {
    /** Наблюдает сохранённые настройки и ошибки чтения. */
    fun observe(): Flow<Outcome<UserSettings>>

    /** Записывает все настройки одной операцией. */
    suspend fun save(settings: UserSettings): Outcome<Unit>

    /** Меняет только предпочтение жестов, сохраняя остальные поля. */
    suspend fun saveGesturesEnabled(enabled: Boolean): Outcome<Unit>

    /** Меняет тему независимо от напоминаний и жестов. */
    suspend fun saveTheme(preference: ThemePreference): Outcome<Unit>

    /** Меняет только напоминание, сохраняя жесты и тему. */
    suspend fun saveReminder(preference: ReminderPreference): Outcome<Unit>
}

/** Результат согласования напоминаний с разрешением Android. */
enum class ReminderStatus { Disabled, Scheduled, PermissionRequired }

/** Управляет напоминанием по актуальным сохранённым настройкам. */
fun interface ReminderScheduler {
    /**
     * Использует последние настройки из хранилища. Отменяет или заменяет
     * будущую задачу. Повтор безопасен, а устаревшее время не заменяет новое.
     */
    suspend fun reconcileLatest(): Outcome<ReminderStatus>
}

/** Настройки сохранены. Результат согласования напоминания передаётся отдельно. */
data class SettingsUpdate(val settings: UserSettings, val reminder: Outcome<ReminderStatus>)

/** Напоминание сохранено, даже если его планирование не удалось. */
data class ReminderUpdate(val preference: ReminderPreference, val status: Outcome<ReminderStatus>)

/** Наблюдает изменения настроек. */
class ObserveSettings(private val store: SettingsStore) {
    /** Возвращает поток из хранилища. */
    operator fun invoke(): Flow<Outcome<UserSettings>> = store.observe()
}

/** Сохраняет включение жестов без изменения темы и напоминаний. */
class SetStudyGestures(private val store: SettingsStore) {
    /** Записывает только выбранное значение переключателя. */
    suspend operator fun invoke(enabled: Boolean): Outcome<Unit> =
        store.saveGesturesEnabled(enabled)
}

/** Сохраняет выбор темы без изменения других настроек. */
class SetThemePreference(private val store: SettingsStore) {
    /** Записывает системную, светлую или тёмную тему. */
    suspend operator fun invoke(preference: ThemePreference): Outcome<Unit> =
        store.saveTheme(preference)
}

/** Сохраняет настройки и затем согласует напоминание. */
class UpdateSettings(private val store: SettingsStore, private val reminders: ReminderScheduler) {
    /** Ошибка планирования оставляет новые настройки сохранёнными. */
    suspend operator fun invoke(settings: UserSettings): Outcome<SettingsUpdate> =
        store.save(settings).map { SettingsUpdate(settings, reminders.reconcileLatest()) }

    /** Записывает напоминание независимо от остальных настроек. */
    suspend fun setReminder(preference: ReminderPreference): Outcome<ReminderUpdate> =
        store.saveReminder(preference).map {
            ReminderUpdate(preference, reminders.reconcileLatest())
        }
}
