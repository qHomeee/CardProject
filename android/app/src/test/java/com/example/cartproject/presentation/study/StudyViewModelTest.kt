package com.example.cartproject.presentation.study

import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.Card
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.settings.ObserveSettings
import com.example.cartproject.domain.settings.ReminderPreference
import com.example.cartproject.domain.settings.SettingsStore
import com.example.cartproject.domain.settings.ThemePreference
import com.example.cartproject.domain.settings.UserSettings
import com.example.cartproject.domain.study.GestureAvailability
import com.example.cartproject.domain.study.GestureSource
import com.example.cartproject.domain.study.GradeCard
import com.example.cartproject.domain.study.ObserveStudyGestures
import com.example.cartproject.domain.study.ReviewGrade
import com.example.cartproject.domain.study.ReviewKey
import com.example.cartproject.domain.study.ReviewSchedule
import com.example.cartproject.domain.study.ReviewStore
import com.example.cartproject.domain.study.ReviewWrite
import com.example.cartproject.domain.study.SavedReview
import com.example.cartproject.domain.study.ScheduledCard
import com.example.cartproject.domain.study.StartStudy
import com.example.cartproject.domain.study.StudyGestureSignal
import com.example.cartproject.domain.study.StudySessionId
import com.example.cartproject.domain.study.StudySummary
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModelTest {
    @Test
    fun stopsGestureSubscriptionInBackgroundAndOnExitPrompt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val settings = SettingsFake()
        val store = ReviewFake()
        val signals = MutableSharedFlow<StudyGestureSignal>(extraBufferCapacity = 1)
        var listening = false
        val study = viewModel(store, settings) {
            flow {
                listening = true
                try {
                    emit(StudyGestureSignal.Availability(GestureAvailability.Available))
                    emitAll(signals)
                } finally {
                    listening = false
                }
            }
        }
        try {
            study.setForeground(true)
            runCurrent()
            assertFalse(listening)
            settings.saveGesturesEnabled(true)
            runCurrent()
            assertTrue(listening)

            signals.emit(StudyGestureSignal.Action.Remembered)
            runCurrent()
            assertFalse((study.uiState.value as StudyUiState.Active).answered)
            assertEquals(0, store.writes.size)

            study.setForeground(false)
            runCurrent()
            assertFalse(listening)
            signals.emit(StudyGestureSignal.Action.Reveal)
            runCurrent()
            assertFalse((study.uiState.value as StudyUiState.Active).answered)

            study.setForeground(true)
            runCurrent()
            signals.emit(StudyGestureSignal.Action.Reveal)
            runCurrent()
            assertTrue((study.uiState.value as StudyUiState.Active).answered)

            study.setExitPrompt(true)
            runCurrent()
            assertFalse(listening)
            study.setExitPrompt(false)
            runCurrent()
            assertTrue(listening)
            signals.emit(StudyGestureSignal.Action.Remembered)
            runCurrent()
            assertEquals(StudyUiState.Complete(StudySummary(1, 0)), study.uiState.value)
            assertEquals(1, store.writes.size)
            assertFalse(listening)
        } finally {
            study.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun repeatsExactlyTheFailedGradeAndKeepsTheAnswerVisible() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ReviewFake().apply { failWrite = true }
        val study = viewModel(store, SettingsFake()) { flowOf() }
        try {
            runCurrent()
            study.reveal()
            study.grade(ReviewGrade.NeedsReview)
            study.grade(ReviewGrade.Remembered)
            runCurrent()
            val failed = study.uiState.value as StudyUiState.Active
            assertTrue(failed.answered)
            assertEquals(AppError.LocalWrite, failed.writeError)
            assertEquals(1, store.writes.size)

            store.failWrite = false
            study.retryGrade()
            runCurrent()
            assertEquals(store.writes[0], store.writes[1])
            assertEquals(StudyUiState.Complete(StudySummary(0, 1)), study.uiState.value)
        } finally {
            study.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun unavailableSensorStillAllowsButtons() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val settings = SettingsFake()
        settings.saveGesturesEnabled(true)
        val study = viewModel(ReviewFake(), settings) {
            flowOf(StudyGestureSignal.Availability(GestureAvailability.SensorUnavailable))
        }
        try {
            study.setForeground(true)
            runCurrent()
            assertEquals(GestureAvailability.SensorUnavailable, study.gestureAvailability.value)
            study.reveal()
            study.grade(ReviewGrade.Remembered)
            runCurrent()
            assertEquals(StudyUiState.Complete(StudySummary(1, 0)), study.uiState.value)
        } finally {
            study.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(store: ReviewFake, settings: SettingsFake, gestures: GestureSource) =
        StudyViewModel(
            Deck,
            StartStudy(store) { Now },
            GradeCard(store) { Now },
            ObserveSettings(settings),
            ObserveStudyGestures(gestures),
        )

    private class SettingsFake : SettingsStore {
        private val state = MutableStateFlow(UserSettings())
        override fun observe() = state.map { Outcome.Success(it) }

        override suspend fun save(settings: UserSettings): Outcome<Unit> {
            state.value = settings
            return Outcome.Success(Unit)
        }

        override suspend fun saveGesturesEnabled(enabled: Boolean): Outcome<Unit> =
            save(state.value.copy(gesturesEnabled = enabled))

        override suspend fun saveTheme(preference: ThemePreference): Outcome<Unit> =
            save(state.value.copy(theme = preference))

        override suspend fun saveReminder(preference: ReminderPreference): Outcome<Unit> =
            save(state.value.copy(reminder = preference))
    }

    private class ReviewFake : ReviewStore {
        private val card = ScheduledCard(
            Card(LocalCardId(1), Deck, "Вопрос", "Ответ"),
            ReviewSchedule.initial(Now),
        )
        val writes = mutableListOf<ReviewWrite>()
        var failWrite = false
        private var review: SavedReview? = null

        override suspend fun scheduledCards(deckId: LocalDeckId) = Outcome.Success(listOf(card))

        override suspend fun readScheduledCard(deckId: LocalDeckId, cardId: LocalCardId) =
            Outcome.Success(card)

        override suspend fun findReview(key: ReviewKey) = Outcome.Success(review)

        override suspend fun commitReview(write: ReviewWrite): Outcome<SavedReview> {
            writes += write
            if (failWrite) return Outcome.Failure(AppError.LocalWrite)
            review = write.result
            return Outcome.Success(write.result)
        }

        override suspend fun grades(sessionId: StudySessionId?) =
            Outcome.Success(listOfNotNull(review?.event?.grade))
    }

    private companion object {
        val Deck = LocalDeckId(1)
        val Now: Instant = Instant.parse("2026-11-25T10:00:00Z")
    }
}
