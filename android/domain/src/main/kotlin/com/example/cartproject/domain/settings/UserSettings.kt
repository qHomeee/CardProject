package com.example.cartproject.domain.settings

import java.time.LocalTime

/** Выбор темы приложения. */
enum class ThemePreference { System, Light, Dark }

/** Настройка напоминаний хранит локальное время отдельно от времени оценок в UTC. */
sealed interface ReminderPreference {
    /** Напоминания выключены. Последний выбор времени можно сохранить. */
    data class Off(val lastTime: LocalTime? = null) : ReminderPreference

    /** Напоминания включены на выбранное местное время. */
    data class On(val time: LocalTime) : ReminderPreference
}

/** Пользовательские настройки, которые сохраняются вместе. */
data class UserSettings(
    val theme: ThemePreference = ThemePreference.System,
    val gesturesEnabled: Boolean = false,
    val reminder: ReminderPreference = ReminderPreference.Off(),
)
