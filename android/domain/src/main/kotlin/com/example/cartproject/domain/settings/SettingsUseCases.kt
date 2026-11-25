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

/** Наблюдает изменения настроек. */
class ObserveSettings(private val store: SettingsStore) {
    /** Возвращает поток из хранилища. */
    operator fun invoke(): Flow<Outcome<UserSettings>> = store.observe()
}

/** Сохраняет настройки и затем согласует напоминание. */
class UpdateSettings(private val store: SettingsStore, private val reminders: ReminderScheduler) {
    /** Ошибка планирования оставляет новые настройки сохранёнными. */
    suspend operator fun invoke(settings: UserSettings): Outcome<SettingsUpdate> =
        store.save(settings).map { SettingsUpdate(settings, reminders.reconcileLatest()) }
}
