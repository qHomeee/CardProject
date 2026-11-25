package com.example.cartproject.di

import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.AppError
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.decks.LocalDeckList
import com.example.cartproject.presentation.decks.MyDecksUiState
import com.example.cartproject.presentation.decks.MyDecksViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/** Проверяет запуск ViewModel и обработку ошибки локального чтения. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppModuleTest {
    /** ViewModel из графа показывает ошибку и получает данные после повтора. */
    @Test
    fun resolvesViewModelAndRetriesLocalRead() = runTest {
        var unavailable = true
        val storageStub = module {
            single<LocalDeckList> {
                LocalDeckList {
                    if (unavailable) {
                        flowOf(Outcome.Failure(AppError.LocalRead))
                    } else {
                        flowOf(Outcome.Success(emptyList()))
                    }
                }
            }
        }
        val app = koinApplication { modules(appModule, storageStub) }
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = app.koin.get<MyDecksViewModel>()
            try {
                viewModel.uiState.first { it == MyDecksUiState.Error }
                unavailable = false
                viewModel.retry()
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
