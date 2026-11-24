package com.example.cartproject.di

import androidx.lifecycle.viewModelScope
import com.example.cartproject.presentation.decks.MyDecksUiState
import com.example.cartproject.presentation.decks.MyDecksViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.dsl.koinApplication

/** Проверяет рабочий граф зависимостей стартового экрана. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppModuleTest {
    /** Разрешает ViewModel через Koin и получает результат чтения через весь граф. */
    @Test
    fun resolvesViewModelAndLoadsEmptyDecks() = runTest {
        val app = koinApplication { modules(appModule) }
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = app.koin.get<MyDecksViewModel>()
            try {
                val state = viewModel.uiState.filterIsInstance<MyDecksUiState.Content>().first()
                assertTrue(state.decks.isEmpty())
            } finally {
                viewModel.viewModelScope.coroutineContext.job.cancelAndJoin()
            }
        } finally {
            Dispatchers.resetMain()
            app.close()
        }
    }
}
