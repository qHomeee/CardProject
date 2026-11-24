package com.example.cartproject.domain.study

import java.time.Instant

/** Источник текущего момента, подменяемый в тестах. */
fun interface Clock {
    /** Возвращает текущий момент времени. */
    fun now(): Instant
}

/** Получает системное время с точностью до миллисекунд. */
object SystemClock : Clock {
    override fun now(): Instant = Instant.ofEpochMilli(System.currentTimeMillis())
}
