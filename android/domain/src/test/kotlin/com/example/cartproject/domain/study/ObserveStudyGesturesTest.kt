package com.example.cartproject.domain.study

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверяет доменные сигналы и отмену подписки на жесты. */
class ObserveStudyGesturesTest {
    /** Источник начинает работу при подписке и освобождается при её отмене. */
    @Test
    fun collectionStartsAndReleasesSource() = runTest {
        var opened = false
        var closed = false
        val useCase = ObserveStudyGestures {
            opened = true
            flow {
                try {
                    emit(StudyGestureSignal.Availability(GestureAvailability.Available))
                    emit(StudyGestureSignal.Action.Reveal)
                    awaitCancellation()
                } finally {
                    closed = true
                }
            }
        }
        val signals = useCase()
        assertFalse(opened)
        val collected = signals.take(2).toList()
        assertTrue(opened)
        assertTrue(closed)
        assertEquals(StudyGestureSignal.Action.Reveal, collected.last())
    }

    /** Отсутствие датчика передаётся как состояние, независимо от Android. */
    @Test
    fun observesUnavailableSensor() = runTest {
        val unavailable = StudyGestureSignal.Availability(GestureAvailability.SensorUnavailable)
        assertEquals(unavailable, ObserveStudyGestures { flowOf(unavailable) }().first())
    }
}
