package com.example.cartproject.domain

import com.example.cartproject.domain.decks.MaterialField
import com.example.cartproject.domain.decks.MaterialIssue

/** Ошибки с данными, достаточными для выбора сообщения или действия в UI. */
sealed interface AppError {
    /** Ошибки отдельных полей с сохранением исходной формы у вызывающей стороны. */
    data class InvalidFields(val fields: Map<MaterialField, MaterialIssue>) : AppError

    /** Выбранный набор отсутствует в источнике. */
    data object DeckNotFound : AppError

    /** Карточка отсутствует в выбранном наборе. */
    data object CardNotFound : AppError

    /** Скачанный набор доступен для чтения и повторения. */
    data object ReadOnlyDeck : AppError

    /** Сетевое соединение с каталогом прервалось или не установилось. */
    data object Network : AppError

    /** Каталог не ответил в пределах допустимого времени. */
    data object CatalogTimeout : AppError

    /** Каталог отклонил запрос или ожидаемый маршрут недоступен. */
    data object CatalogRequestRejected : AppError

    /** Сервер каталога временно не смог выполнить запрос. */
    data object CatalogUnavailable : AppError

    /** Ответ каталога не соответствует ожидаемому контракту. */
    data object InvalidCatalogData : AppError

    /** Локальное хранилище не удалось прочитать. */
    data object LocalRead : AppError

    /** Запись в локальное хранилище завершилась ошибкой. */
    data object LocalWrite : AppError

    /** Запись оценки конфликтует с прежней оценкой или версиями карточки. */
    data object ReviewConflict : AppError

    /** Android не смог согласовать расписание уведомлений. */
    data object ReminderSchedule : AppError
}
