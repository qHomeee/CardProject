package com.example.cartproject.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.cartproject.domain.decks.LocalCardId
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.presentation.editor.EditorCompletion
import com.example.cartproject.presentation.editor.EditorTarget
import com.example.cartproject.presentation.editor.EditorUiState
import com.example.cartproject.presentation.editor.MaterialEditorScreen
import com.example.cartproject.presentation.editor.MaterialEditorViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Маршрут создания набора. */
@Serializable
internal data object NewDeckRoute

/** Маршрут редактирования собственного набора по его ID. */
@Serializable
internal data class EditDeckRoute(val deckId: Long)

/** Маршрут карточки. Пустой ID карточки открывает создание. */
@Serializable
internal data class CardEditorRoute(val deckId: Long, val cardId: Long = 0L)

/** Добавляет маршруты редактора из локального раздела. */
internal fun NavGraphBuilder.materialEditorDestinations(navController: NavHostController) {
    composable<NewDeckRoute> { entry ->
        EditorDestination(EditorTarget.Deck(null), entry, navController)
    }
    composable<EditDeckRoute> { entry ->
        val id = LocalDeckId(entry.toRoute<EditDeckRoute>().deckId)
        EditorDestination(EditorTarget.Deck(id), entry, navController)
    }
    composable<CardEditorRoute> { entry ->
        val route = entry.toRoute<CardEditorRoute>()
        val card = route.cardId.takeIf { it > 0 }?.let(::LocalCardId)
        EditorDestination(EditorTarget.Card(LocalDeckId(route.deckId), card), entry, navController)
    }
}

@Composable
private fun EditorDestination(
    target: EditorTarget,
    entry: NavBackStackEntry,
    navController: NavHostController,
) {
    val viewModel = koinViewModel<MaterialEditorViewModel> { parametersOf(target) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val completion = (state as? EditorUiState.Form)?.completion

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshCards() }
    LaunchedEffect(completion) {
        if (navController.currentBackStackEntry != entry) return@LaunchedEffect
        when (completion) {
            is EditorCompletion.DeckSaved -> {
                if (target is EditorTarget.Deck && target.id == null) {
                    navController.navigate(LocalDeckRoute(completion.id.value)) {
                        popUpTo<NewDeckRoute> { inclusive = true }
                    }
                } else {
                    navController.popBackStack()
                }
            }
            EditorCompletion.CardSaved -> navController.popBackStack()
            EditorCompletion.Deleted -> {
                if (target is EditorTarget.Deck) {
                    navController.popBackStack<LocalDeckRoute>(inclusive = true)
                } else {
                    navController.popBackStack()
                }
            }
            null -> Unit
        }
    }
    MaterialEditorScreen(
        viewModel = viewModel,
        state = state,
        onBack = {
            if (navController.currentBackStackEntry == entry) navController.popBackStack()
        },
        onOpenCard = { cardId ->
            val deckId = (target as? EditorTarget.Deck)?.id
            if (deckId != null && navController.currentBackStackEntry == entry) {
                navController.navigate(CardEditorRoute(deckId.value, cardId?.value ?: 0L)) {
                    launchSingleTop = true
                }
            }
        },
    )
}
