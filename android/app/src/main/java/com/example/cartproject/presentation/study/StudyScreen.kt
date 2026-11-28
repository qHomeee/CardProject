package com.example.cartproject.presentation.study

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.cartproject.R
import com.example.cartproject.domain.study.GestureAvailability
import com.example.cartproject.domain.study.ReviewGrade
import com.example.cartproject.presentation.components.ReviewSummary
import com.example.cartproject.presentation.components.StatusAction
import com.example.cartproject.presentation.components.StatusKind
import com.example.cartproject.presentation.components.StatusMessage
import com.example.cartproject.presentation.components.StudyCard
import com.example.cartproject.presentation.components.SubscreenTopBar
import com.example.cartproject.presentation.components.ZapomniButton
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Показывает занятие, состояние сохранения и итог текущей очереди. */
@Composable
internal fun StudyScreen(
    viewModel: StudyViewModel,
    state: StudyUiState,
    gestures: GestureAvailability,
    onClose: () -> Unit,
) {
    val active = state as? StudyUiState.Active
    val onBack: () -> Unit = {
        if (active != null) viewModel.setExitPrompt(true) else onClose()
    }
    BackHandler(enabled = active != null, onBack = onBack)
    if (active?.confirmExit == true) {
        AlertDialog(
            onDismissRequest = { viewModel.setExitPrompt(false) },
            title = { Text(stringResource(R.string.study_exit_title)) },
            text = { Text(stringResource(R.string.study_exit_help)) },
            confirmButton = {
                TextButton(onClick = onClose) { Text(stringResource(R.string.study_exit_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setExitPrompt(false) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    val scroll = rememberScrollState()
    var previousIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val loading = state == StudyUiState.Loading
    LaunchedEffect(loading, active?.index) {
        if (!loading && active?.index != previousIndex) {
            scroll.scrollTo(0)
            previousIndex = active?.index
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 880.dp).fillMaxSize()) {
            SubscreenTopBar(R.string.study_title, onBack)
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(
                    scroll,
                ).padding(ZapomniSize.space16),
                verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12),
            ) {
                when (state) {
                    StudyUiState.Loading -> StatusMessage(
                        StatusKind.Loading,
                        stringResource(R.string.study_loading),
                    )
                    is StudyUiState.ReadError -> StatusMessage(
                        kind = StatusKind.Error,
                        title = stringResource(R.string.study_read_error),
                        action = StatusAction(stringResource(R.string.retry), viewModel::load),
                    )
                    is StudyUiState.Empty -> {
                        val next = state.dueAt?.let {
                            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                                .withZone(ZoneId.systemDefault()).format(it)
                        }
                        StatusMessage(
                            kind = StatusKind.Empty,
                            title = stringResource(R.string.study_empty),
                            message = next?.let { stringResource(R.string.study_next_due, it) },
                        )
                        StudyButton(R.string.study_back_to_deck, onClose)
                    }
                    is StudyUiState.Complete -> {
                        ReviewSummary(state.summary, stringResource(R.string.study_complete))
                        StudyButton(R.string.study_back_to_deck, onClose)
                    }
                    is StudyUiState.Active -> CurrentStudyCard(state, gestures, viewModel, onBack)
                }
            }
        }
    }
}

@Composable
private fun CurrentStudyCard(
    active: StudyUiState.Active,
    gestures: GestureAvailability,
    viewModel: StudyViewModel,
    onExit: () -> Unit,
) {
    val current = active.session.cards[active.index]
    val viewport = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val wideLayout = density.fontScale < 1.5f &&
        with(density) { viewport.width.toDp() >= 600.dp }
    val card: @Composable (Modifier) -> Unit = { modifier ->
        StudyCard(
            question = current.question,
            position = active.index + 1,
            total = active.session.cards.size,
            modifier = modifier,
            answer = current.answer.takeIf { active.answered },
            hint = when {
                active.answered -> null
                gestures == GestureAvailability.Available -> stringResource(
                    R.string.study_gesture_turn,
                )
                gestures == GestureAvailability.GravityOnly -> stringResource(
                    R.string.study_gesture_flip,
                )
                else -> null
            },
        )
    }
    if (wideLayout) {
        Row(horizontalArrangement = Arrangement.spacedBy(ZapomniSize.space16)) {
            card(Modifier.weight(1f))
            Column(
                modifier = Modifier.weight(1f)
                    .align(if (active.answered) Alignment.Bottom else Alignment.Top),
                verticalArrangement = Arrangement.spacedBy(ZapomniSize.space12),
            ) {
                StudyActions(active, gestures, viewModel, onExit)
            }
        }
    } else {
        card(Modifier.fillMaxWidth())
        StudyActions(active, gestures, viewModel, onExit)
    }
}

@Composable
private fun StudyActions(
    active: StudyUiState.Active,
    gestures: GestureAvailability,
    viewModel: StudyViewModel,
    onExit: () -> Unit,
) {
    when (gestures) {
        GestureAvailability.Available, GestureAvailability.GravityOnly -> if (active.answered) {
            Text(
                text = stringResource(R.string.study_gesture_grade),
                color = ZapomniTheme.colors.textMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        GestureAvailability.Disabled -> StatusMessage(
            kind = StatusKind.GesturesUnavailable,
            title = stringResource(R.string.study_gestures_disabled),
            message = stringResource(R.string.study_gestures_enable_help),
        )
        GestureAvailability.SensorUnavailable -> StatusMessage(
            kind = StatusKind.GesturesUnavailable,
            title = stringResource(R.string.study_sensor_unavailable),
            message = stringResource(R.string.study_use_buttons),
        )
    }
    when {
        active.saving -> ZapomniButton(
            label = stringResource(R.string.study_saving),
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            loading = true,
        )
        active.stale -> StatusMessage(
            kind = StatusKind.Error,
            title = stringResource(R.string.study_changed),
            action = StatusAction(stringResource(R.string.study_restart), viewModel::load),
        )
        active.writeError != null -> StatusMessage(
            kind = StatusKind.Error,
            title = stringResource(R.string.study_save_failed),
            action = StatusAction(stringResource(R.string.study_retry_save), viewModel::retryGrade),
        )
        active.answered -> {
            StudyButton(R.string.study_remembered, { viewModel.grade(ReviewGrade.Remembered) })
            StudyButton(
                R.string.study_needs_review,
                { viewModel.grade(ReviewGrade.NeedsReview) },
                secondary = true,
            )
        }
        else -> StudyButton(R.string.study_reveal, viewModel::reveal)
    }
    ZapomniButton(
        label = stringResource(R.string.study_exit),
        onClick = onExit,
        modifier = Modifier.fillMaxWidth(),
        secondary = true,
        enabled = !active.saving,
    )
}

@Composable
private fun StudyButton(
    @androidx.annotation.StringRes label: Int,
    onClick: () -> Unit,
    secondary: Boolean = false,
) {
    ZapomniButton(
        label = stringResource(label),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        secondary = secondary,
    )
}
