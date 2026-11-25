package com.example.cartproject.data.local

import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.study.ReviewEvent
import com.example.cartproject.domain.study.ReviewGrade
import com.example.cartproject.domain.study.ReviewKey
import com.example.cartproject.domain.study.ReviewSchedule
import com.example.cartproject.domain.study.SavedReview
import com.example.cartproject.domain.study.StudySessionId
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

/** Проверяет, что результат повторной оценки восстанавливается из сохранённых полей. */
class ReviewMappingsTest {
    /** Ключ, время, ревизия и исходный срок переживают преобразование в строки базы. */
    @Test
    fun roundTripsStoredReview() {
        val key = ReviewKey(StudySessionId(UUID(7, 6)), LocalCardId(9))
        val at = Instant.parse("2026-11-25T10:00:00.456Z")
        for (grade in ReviewGrade.entries) {
            val event = ReviewEvent(key, LocalDeckId(3), 2, grade, at)
            val result = SavedReview(event, ReviewSchedule(4, at.plusSeconds(8_000), 6))
            assertEquals(result, ReviewRow(result.toEntity(), 3).toDomain())
        }
    }
}
