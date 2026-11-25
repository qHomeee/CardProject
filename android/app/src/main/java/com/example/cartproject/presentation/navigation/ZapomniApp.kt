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
private data class LocalDeckRoute(val id: Long)

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

    Scaffold(
        containerColor = ZapomniTheme.colors.background,
        bottomBar = {
            if (selected != null && WindowInsets.ime.getBottom(LocalDensity.current) == 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(
                        start = ZapomniSize.space16,
                        end = ZapomniSize.space16,
                        bottom = ZapomniSize.space8,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    ZapomniNavigationBar(
                        selected = selected,
                        onSelect = startTab,
                        modifier = Modifier.widthIn(max = 328.dp).fillMaxWidth(),
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
                MyDecksScreen(state, myDecks::retry, onOpenDeck = { id ->
                    navController.navigate(LocalDeckRoute(id.value)) { launchSingleTop = true }
                })
            }
            composable<SettingsRoute> { SettingsDestination() }
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
                )
            }
        }
    }
}

@Composable
private fun DeckDetailDestination(
    source: DeckSource,
    onBack: () -> Unit,
    onOpenCopy: (LocalDeckId) -> Unit = {},
) {
    val viewModel = koinViewModel<DeckDetailViewModel> { parametersOf(source) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
    )
}

@Composable
private fun SettingsDestination() {
    Column(
        modifier = Modifier.padding(ZapomniSize.space16),
    ) {
        Text(
            text = stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
    }
}
