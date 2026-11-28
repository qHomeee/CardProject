package com.example.cartproject.presentation.statistics

import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.study.ReadStatistics
import com.example.cartproject.domain.study.ReviewGrade
import com.example.cartproject.domain.study.ReviewKey
import com.example.cartproject.domain.study.ReviewStore
import com.example.cartproject.domain.study.ReviewWrite
import com.example.cartproject.domain.study.StudySessionId
import com.example.cartproject.domain.study.StudySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Проверяет загрузку общей истории после сбоя локального чтения. */
@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {
    @Test
    fun retriesAllGradesAfterReadFailure() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = HistoryFake()
        val model = StatisticsViewModel(ReadStatistics(store))
        try {
            assertNull(model.uiState.value)
            runCurrent()
            assertEquals(Outcome.Failure(AppError.LocalRead), model.uiState.value)

            store.result = Outcome.Success(listOf(ReviewGrade.NeedsReview, ReviewGrade.Remembered))
            model.load()
            runCurrent()
            assertEquals(Outcome.Success(StudySummary(1, 1)), model.uiState.value)
        } finally {
            model.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private class HistoryFake : ReviewStore {
        var result: Outcome<List<ReviewGrade>> = Outcome.Failure(AppError.LocalRead)

        override suspend fun grades(sessionId: StudySessionId?): Outcome<List<ReviewGrade>> {
            assertNull(sessionId)
            return result
        }

        override suspend fun scheduledCards(deckId: LocalDeckId): Nothing = error("History only")

        override suspend fun readScheduledCard(deckId: LocalDeckId, cardId: LocalCardId): Nothing =
            error("History only")

        override suspend fun findReview(key: ReviewKey): Nothing = error("History only")

        override suspend fun commitReview(write: ReviewWrite): Nothing = error("History only")
    }
}
