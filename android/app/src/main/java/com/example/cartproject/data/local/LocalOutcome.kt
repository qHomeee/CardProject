package com.example.cartproject.data.local

import android.database.sqlite.SQLiteException
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** Распознаёт сбои чтения и записи SQLite, включая закрытую базу. */
internal fun Throwable.isLocalDatabaseFailure(): Boolean =
    this is SQLiteException || (this is IllegalStateException && this !is CancellationException)

/** Преобразует сбой после выхода из транзакции и сохраняет семантику отмены. */
internal suspend inline fun <T> sqliteOutcome(
    error: AppError,
    block: () -> Outcome<T>,
): Outcome<T> = try {
    block()
} catch (failure: Exception) {
    if (!failure.isLocalDatabaseFailure()) throw failure
    currentCoroutineContext().ensureActive()
    Outcome.Failure(error)
}
