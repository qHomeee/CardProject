package com.example.cartproject.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.cartproject.R
import com.example.cartproject.domain.decks.CatalogDeckId
import com.example.cartproject.domain.decks.LocalDeckId
import com.example.cartproject.presentation.catalog.CatalogScreen
import com.example.cartproject.presentation.catalog.CatalogViewModel
import com.example.cartproject.presentation.components.ZapomniNavigationBar
import com.example.cartproject.presentation.components.ZapomniTab
import com.example.cartproject.presentation.decks.DeckDetailAction
import com.example.cartproject.presentation.decks.DeckDetailScreen
import com.example.cartproject.presentation.decks.DeckDetailViewModel
import com.example.cartproject.presentation.decks.DeckSource
import com.example.cartproject.presentation.decks.MyDecksScreen
import com.example.cartproject.presentation.decks.MyDecksViewModel
import com.example.cartproject.presentation.study.GestureSettingsScreen
import com.example.cartproject.presentation.theme.ZapomniSize
import com.example.cartproject.presentation.theme.ZapomniTheme
import java.util.UUID
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
private data object CatalogRoute

@Serializable
private data object MyDecksRoute

@Serializable
private data object SettingsRoute

@Serializable
private data class CatalogDeckRoute(val id: String)

@Serializable
internal data class LocalDeckRoute(val id: Long)

/** Связывает вкладки и экраны выбранного набора с графом навигации. */
@Composable
internal fun ZapomniApp(catalog: CatalogViewModel, myDecks: MyDecksViewModel) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val selected = when {
        destination == null || destination.hasRoute<CatalogRoute>() -> ZapomniTab.Catalog
        destination.hasRoute<MyDecksRoute>() -> ZapomniTab.Decks
        destination.hasRoute<SettingsRoute>() -> ZapomniTab.Settings
        else -> null
    }
    val startTab: (ZapomniTab) -> Unit = { tab ->
        if (tab != selected) {
            val route = when (tab) {
                ZapomniTab.Catalog -> CatalogRoute
                ZapomniTab.Decks -> MyDecksRoute
                ZapomniTab.Settings -> SettingsRoute
            }
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val density = LocalDensity.current
    val largeText = density.fontScale >= 1.5f
    val navigationWidth = if (largeText) 520.dp else 328.dp
    val navigationMargin = if (largeText) ZapomniSize.space8 else ZapomniSize.space16
    Scaffold(
        containerColor = ZapomniTheme.colors.background,
        bottomBar = {
            if (selected != null && WindowInsets.ime.getBottom(density) == 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(
                        start = navigationMargin,
                        end = navigationMargin,
                        bottom = ZapomniSize.space8,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    ZapomniNavigationBar(
                        selected = selected,
                        onSelect = startTab,
                        modifier = Modifier.widthIn(max = navigationWidth).fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = CatalogRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding(),
        ) {
            composable<CatalogRoute> {
                val state by catalog.uiState.collectAsStateWithLifecycle()
                CatalogScreen(
                    state = state,
                    onQueryChange = catalog::updateQuery,
                    onRetry = catalog::retry,
                    onOpenDecks = { startTab(ZapomniTab.Decks) },
                    onOpenDeck = { id ->
                        navController.navigate(CatalogDeckRoute(id.value.toString())) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<MyDecksRoute> {
                val state by myDecks.uiState.collectAsStateWithLifecycle()
                MyDecksScreen(
                    state = state,
                    onRetry = myDecks::retry,
                    onOpenDeck = { id ->
                        navController.navigate(LocalDeckRoute(id.value)) { launchSingleTop = true }
                    },
                    onCreateDeck = {
                        navController.navigate(NewDeckRoute) { launchSingleTop = true }
                    },
                )
            }
            composable<SettingsRoute> { GestureSettingsScreen() }
            composable<CatalogDeckRoute> { deckEntry ->
                val id = CatalogDeckId(UUID.fromString(deckEntry.toRoute<CatalogDeckRoute>().id))
                DeckDetailDestination(
                    source = DeckSource.Catalog(id),
                    onBack = {
                        if (navController.currentBackStackEntry == deckEntry) {
                            navController.popBackStack()
                        }
                    },
                    onOpenCopy = { copy ->
                        if (navController.currentBackStackEntry == deckEntry) {
                            navController.navigate(LocalDeckRoute(copy.value)) {
                                popUpTo<CatalogDeckRoute> { inclusive = true }
                            }
                        }
                    },
                )
            }
            composable<LocalDeckRoute> { deckEntry ->
                val id = LocalDeckId(deckEntry.toRoute<LocalDeckRoute>().id)
                DeckDetailDestination(
                    source = DeckSource.Local(id),
                    onBack = {
                        if (navController.currentBackStackEntry == deckEntry) {
                            navController.popBackStack()
                        }
                    },
                    onEdit = {
                        if (navController.currentBackStackEntry == deckEntry) {
                            navController.navigate(EditDeckRoute(id.value)) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onAddCard = {
                        if (navController.currentBackStackEntry == deckEntry) {
                            navController.navigate(CardEditorRoute(id.value)) {
                                launchSingleTop =
                                    true
                            }
                        }
                    },
                    onStartStudy = {
                        if (navController.currentBackStackEntry == deckEntry) {
                            navController.navigate(StudyRoute(id.value)) { launchSingleTop = true }
                        }
                    },
                )
            }
            materialEditorDestinations(navController)
            studyDestination(navController)
        }
    }
}

@Composable
private fun DeckDetailDestination(
    source: DeckSource,
    onBack: () -> Unit,
    onOpenCopy: (LocalDeckId) -> Unit = {},
    onEdit: (() -> Unit)? = null,
    onAddCard: (() -> Unit)? = null,
    onStartStudy: (() -> Unit)? = null,
) {
    val viewModel = koinViewModel<DeckDetailViewModel> { parametersOf(source) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshLocal() }
    val action = state.action
    LaunchedEffect(action) {
        when (action) {
            is DeckDetailAction.Downloaded -> onOpenCopy(action.id)
            DeckDetailAction.Removed -> onBack()
            else -> Unit
        }
    }
    DeckDetailScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retryRead,
        onDownload = viewModel::download,
        onOpenCopy = onOpenCopy,
        onRemoveCopy = viewModel::removeSavedCopy,
        onEdit = onEdit,
        onAddCard = onAddCard,
        onStartStudy = onStartStudy,
    )
}
