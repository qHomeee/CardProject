package com.example.cartproject.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.presentation.study.StudyScreen
import com.example.cartproject.presentation.study.StudyViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Маршрут занятия по идентификатору локального набора. */
@Serializable
internal data class StudyRoute(val deckId: Long)

/** Привязывает занятие к маршруту и жизненному циклу его экрана. */
internal fun NavGraphBuilder.studyDestination(navController: NavHostController) {
    composable<StudyRoute> { entry ->
        val deckId = LocalDeckId(entry.toRoute<StudyRoute>().deckId)
        val viewModel = koinViewModel<StudyViewModel> { parametersOf(deckId) }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val gestures by viewModel.gestureAvailability.collectAsStateWithLifecycle()
        LifecycleResumeEffect(viewModel) {
            viewModel.setForeground(true)
            onPauseOrDispose { viewModel.setForeground(false) }
        }
        StudyScreen(viewModel, state, gestures) {
            if (navController.currentBackStackEntry == entry) navController.popBackStack()
        }
    }
}
