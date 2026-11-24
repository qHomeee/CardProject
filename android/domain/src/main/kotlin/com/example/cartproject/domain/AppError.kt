package com.example.cartproject.domain

import com.example.cartproject.domain.decks.MaterialField
import com.example.cartproject.domain.decks.MaterialIssue

/** Ошибки с данными, достаточными для выбора сообщения или действия в UI. */
sealed interface AppError {
    /** Ошибки отдельных полей с сохранением исходной формы у вызывающей стороны. */
    data class InvalidFields(val fields: Map<MaterialField, MaterialIssue>) : AppError

    data object DeckNotFound : AppError
    data object CardNotFound : AppError
    data object ReadOnlyDeck : AppError
    data object Network : AppError
    data object CatalogUnavailable : AppError
    data object InvalidCatalogData : AppError
    data object LocalRead : AppError
    data object LocalWrite : AppError
    data object ReviewConflict : AppError
}
