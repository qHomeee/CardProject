package com.example.cartproject.di

import android.net.ConnectivityManager
import com.example.cartproject.domain.decks.ObserveMyDecks
import com.example.cartproject.presentation.catalog.CatalogViewModel
import com.example.cartproject.presentation.decks.DeckDetailViewModel
import com.example.cartproject.presentation.decks.MyDecksViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val appModule = module {
    includes(catalogModule, storageModule)

    factory { ObserveMyDecks(get()) }
    viewModel { MyDecksViewModel(get()) }
    viewModel { params -> DeckDetailViewModel(params.get(), get(), get(), get()) }
    viewModel {
        val connectivity = androidContext().getSystemService(ConnectivityManager::class.java)
        CatalogViewModel(get(), get()) { connectivity.activeNetwork != null }
    }
}
