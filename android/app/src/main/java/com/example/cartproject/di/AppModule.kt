package com.example.cartproject.di

import com.example.cartproject.data.decks.EmptyLocalDeckStore
import com.example.cartproject.domain.decks.LocalDeckList
import com.example.cartproject.domain.decks.ObserveMyDecks
import com.example.cartproject.presentation.decks.MyDecksViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val appModule = module {
    includes(catalogModule)

    single<LocalDeckList> { EmptyLocalDeckStore() }
    factory { ObserveMyDecks(get()) }
    viewModel { MyDecksViewModel(get()) }
}
