package com.example.cartproject.domain.study

import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.map

/** Итог занятия или история. При отсутствии оценок процент отсутствует. */
data class StudySummary(val remembered: Int, val needsReview: Int) {
    val total: Int get() = remembered + needsReview

    val rememberedPercent: Double?
        get() = if (total == 0) null else ((remembered * 1_000L + total / 2) / total) / 10.0
}

/** Считает все оценки, включая повторные занятия по тем же карточкам. */
class ReadStatistics(private val store: ReviewStore) {
    /** Возвращает сводку по занятию или полную историю без фильтра. */
    suspend operator fun invoke(sessionId: StudySessionId? = null): Outcome<StudySummary> =
        store.grades(sessionId).map { grades ->
            val remembered = grades.count { it == ReviewGrade.Remembered }
            StudySummary(remembered, grades.size - remembered)
        }
}
