package com.example.cartproject.di

import com.example.cartproject.domain.decks.ObserveMyDecks
import com.example.cartproject.presentation.decks.MyDecksViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val appModule = module {
    includes(catalogModule, storageModule)

    factory { ObserveMyDecks(get()) }
    viewModel { MyDecksViewModel(get()) }
}
