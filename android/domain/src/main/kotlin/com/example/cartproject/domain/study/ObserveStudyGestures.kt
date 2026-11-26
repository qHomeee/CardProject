package com.example.cartproject.domain.study

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Доступность жестов для объяснения причины в интерфейсе. */
enum class GestureAvailability { Available, GravityOnly, Disabled, SensorUnavailable }

/** Состояние датчика или распознанный жест без Android-типов. */
sealed interface StudyGestureSignal {
    /** Смена доступности управления жестами. */
    data class Availability(val value: GestureAvailability) : StudyGestureSignal

    /** Событие жеста, передаваемое слою презентации. */
    enum class Action : StudyGestureSignal { Reveal, Remembered, NeedsReview }
}

/** Определяет, какое движение разрешено на текущей карточке. */
enum class StudyGestureMode { Reveal, Grade }

/** Источник событий, освобождающий датчик при отмене подписки. */
fun interface GestureSource {
    /** Начинает получение событий при подписке на поток. */
    fun events(mode: () -> StudyGestureMode): Flow<StudyGestureSignal>
}

/** Предоставляет события жестов на время активной подписки. */
class ObserveStudyGestures(private val source: GestureSource) {
    /** Запрашивает события при сборе потока и передаёт ему отмену подписки. */
    operator fun invoke(mode: () -> StudyGestureMode): Flow<StudyGestureSignal> =
        flow { emitAll(source.events(mode)) }
}
