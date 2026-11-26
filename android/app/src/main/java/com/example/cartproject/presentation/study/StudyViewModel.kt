package com.example.cartproject.presentation.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.domain.settings.ObserveSettings
import com.example.cartproject.domain.study.GestureAvailability
import com.example.cartproject.domain.study.GradeCard
import com.example.cartproject.domain.study.ObserveStudyGestures
import com.example.cartproject.domain.study.ReviewGrade
import com.example.cartproject.domain.study.StartStudy
import com.example.cartproject.domain.study.StudyGestureMode
import com.example.cartproject.domain.study.StudyGestureSignal
import com.example.cartproject.domain.study.StudySession
import com.example.cartproject.domain.study.StudySessionId
import com.example.cartproject.domain.study.StudyStart
import com.example.cartproject.domain.study.StudySummary
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Состояния занятия с неизменной очередью текущего запуска. */
internal sealed interface StudyUiState {
    data object Loading : StudyUiState
    data class ReadError(val reason: AppError) : StudyUiState
    data class Empty(val dueAt: Instant?) : StudyUiState
    data class Complete(val summary: StudySummary) : StudyUiState

    data class Active(
        val session: StudySession,
        val index: Int = 0,
        val answered: Boolean = false,
        val remembered: Int = 0,
        val pendingGrade: ReviewGrade? = null,
        val writeError: AppError? = null,
        val confirmExit: Boolean = false,
    ) : StudyUiState {
        val saving: Boolean get() = pendingGrade != null && writeError == null
        val stale: Boolean
            get() = writeError in listOf(
                AppError.ReviewConflict,
                AppError.CardNotFound,
                AppError.DeckNotFound,
            )
    }
}

/** Выполняет действия занятия через общий путь для кнопок и жестов. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class StudyViewModel(
    private val deckId: LocalDeckId,
    private val startStudy: StartStudy,
    private val gradeCard: GradeCard,
    observeSettings: ObserveSettings,
    observeGestures: ObserveStudyGestures,
) : ViewModel() {
    private val state = MutableStateFlow<StudyUiState>(StudyUiState.Loading)
    val uiState = state.asStateFlow()
    private val foreground = MutableStateFlow(false)
    private val gestures = MutableStateFlow(GestureAvailability.Disabled)
    val gestureAvailability = gestures.asStateFlow()
    private var startJob: Job? = null

    init {
        load()
        val inSession = state.map { it is StudyUiState.Active && !it.confirmExit }
        viewModelScope.launch {
            combine(foreground, inSession) { foreground, active -> foreground && active }
                .distinctUntilChanged()
                .flatMapLatest { active ->
                    if (active) {
                        observeSettings().map {
                            (it as? Outcome.Success)?.value?.gesturesEnabled ==
                                true
                        }
                            .distinctUntilChanged()
                            .flatMapLatest { enabled ->
                                if (enabled) {
                                    observeGestures(::gestureMode)
                                } else {
                                    flowOf(
                                        StudyGestureSignal.Availability(
                                            GestureAvailability.Disabled,
                                        ),
                                    )
                                }
                            }
                    } else {
                        emptyFlow()
                    }
                }
                .collect(::onGestureSignal)
        }
    }

    /** Разрешает подписку только на активном, видимом экране. */
    fun setForeground(visible: Boolean) {
        foreground.value = visible
    }

    /** Начинает новую очередь при запуске или после ошибки чтения. */
    fun load() {
        if (startJob?.isActive == true) return
        val current = state.value
        if (current is StudyUiState.Active && !current.stale) return
        state.value = StudyUiState.Loading
        startJob = viewModelScope.launch {
            val sessionId = StudySessionId(UUID.randomUUID())
            state.value = when (val result = startStudy(deckId, sessionId)) {
                is Outcome.Failure -> StudyUiState.ReadError(result.error)
                is Outcome.Success -> when (val start = result.value) {
                    is StudyStart.Empty -> StudyUiState.Empty(start.nextDueAt)
                    is StudyStart.Ready -> StudyUiState.Active(start.session)
                }
            }
        }
    }

    /** Открывает ответ до первой попытки оценки. */
    fun reveal() {
        val current = state.value as? StudyUiState.Active ?: return
        if (!current.answered && !current.confirmExit) state.value = current.copy(answered = true)
    }

    /** Принимает единственную оценку текущей открытой карточки. */
    fun grade(grade: ReviewGrade) {
        val current = state.value as? StudyUiState.Active ?: return
        if (!current.answered || current.pendingGrade != null || current.confirmExit) return
        save(current, grade)
    }

    /** Повторяет прежнюю оценку без смены карточки и ключа. */
    fun retryGrade() {
        val current = state.value as? StudyUiState.Active ?: return
        val grade = current.pendingGrade ?: return
        if (current.writeError == null || current.confirmExit || current.stale) return
        save(current, grade)
    }

    /** Показывает или скрывает подтверждение выхода. */
    fun setExitPrompt(visible: Boolean) {
        val current = state.value as? StudyUiState.Active ?: return
        if (!current.saving) state.value = current.copy(confirmExit = visible)
    }

    private fun gestureMode(): StudyGestureMode =
        if ((state.value as? StudyUiState.Active)?.answered == true) {
            StudyGestureMode.Grade
        } else {
            StudyGestureMode.Reveal
        }

    private fun onGestureSignal(signal: StudyGestureSignal) {
        if (!foreground.value) return
        when (signal) {
            is StudyGestureSignal.Availability -> gestures.value = signal.value
            StudyGestureSignal.Action.Reveal -> reveal()
            StudyGestureSignal.Action.Remembered -> grade(ReviewGrade.Remembered)
            StudyGestureSignal.Action.NeedsReview -> grade(ReviewGrade.NeedsReview)
        }
    }

    private fun save(current: StudyUiState.Active, grade: ReviewGrade) {
        state.value = current.copy(pendingGrade = grade, writeError = null)
        viewModelScope.launch {
            val card = current.session.cards[current.index]
            state.value = when (val result = gradeCard(current.session, card.id, grade)) {
                is Outcome.Failure -> current.copy(pendingGrade = grade, writeError = result.error)
                is Outcome.Success -> {
                    val remembered = current.remembered +
                        if (result.value.event.grade == ReviewGrade.Remembered) 1 else 0
                    val next = current.index + 1
                    if (next == current.session.cards.size) {
                        StudyUiState.Complete(StudySummary(remembered, next - remembered))
                    } else {
                        StudyUiState.Active(current.session, index = next, remembered = remembered)
                    }
                }
            }
        }
    }
}
