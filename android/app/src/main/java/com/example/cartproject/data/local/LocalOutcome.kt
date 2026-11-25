package com.example.cartproject.data.local

import android.database.sqlite.SQLiteException
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Преобразует сбой SQLite после отката транзакции, сохраняя отмену корутины. */
internal suspend inline fun <T> sqliteOutcome(
    error: AppError,
    block: () -> Outcome<T>,
): Outcome<T> = try {
    block()
} catch (_: SQLiteException) {
    currentCoroutineContext().ensureActive()
    Outcome.Failure(error)
} catch (_: IllegalStateException) {
    currentCoroutineContext().ensureActive()
    Outcome.Failure(error)
}
