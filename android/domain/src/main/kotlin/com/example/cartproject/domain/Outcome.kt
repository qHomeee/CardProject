package com.example.cartproject.domain

/** Успех или ожидаемая ошибка операции предметной области. */
sealed interface Outcome<out T> {
    /** Значение завершившейся операции. */
    data class Success<T>(val value: T) : Outcome<T>

    /** Причина, по которой операция не завершилась. */
    data class Failure(val error: AppError) : Outcome<Nothing>
}

/** Преобразует значение успеха, сохраняя исходную ошибку. */
inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

/** Запускает следующий шаг после успеха. */
inline fun <T, R> Outcome<T>.flatMap(next: (T) -> Outcome<R>): Outcome<R> = when (this) {
    is Outcome.Success -> next(value)
    is Outcome.Failure -> this
}
