package com.example.cartproject.domain.study

import java.time.Duration
import java.time.Instant

/** Результат ответа на карточку. */
enum class ReviewGrade { Remembered, NeedsReview }

/** Рассчитывает следующий срок согласно таблице повторений. */
object ScheduleRule {
    private val successIntervals = listOf(1L, 3L, 7L, 14L, 30L).map(Duration::ofDays)
    private val retryInterval = Duration.ofMinutes(10)

    /** Возвращает новую стадию, срок в UTC и следующую версию. */
    fun next(current: ReviewSchedule, grade: ReviewGrade, gradedAt: Instant): ReviewSchedule {
        val stage = when (grade) {
            ReviewGrade.Remembered -> (current.stage + 1).coerceAtMost(successIntervals.size)
            ReviewGrade.NeedsReview -> 0
        }
        val interval = if (stage == 0) retryInterval else successIntervals[stage - 1]
        return ReviewSchedule(stage, gradedAt.plus(interval), current.version + 1)
    }
}
