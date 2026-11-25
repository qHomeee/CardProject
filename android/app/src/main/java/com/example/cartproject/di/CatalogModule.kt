package com.example.cartproject.di

import com.example.cartproject.BuildConfig
import com.example.cartproject.data.catalog.RemoteCatalogReader
import com.example.cartproject.data.catalog.createCatalogHttpClient
import com.example.cartproject.domain.decks.CatalogReader
import com.example.cartproject.domain.decks.SearchCatalog
import io.ktor.http.Url
import org.koin.dsl.module
import org.koin.dsl.onClose

internal val catalogModule = module {
    single { createCatalogHttpClient() } onClose { it?.close() }
    single<CatalogReader> { RemoteCatalogReader(get(), Url(BuildConfig.CATALOG_BASE_URL)) }
    factory { SearchCatalog(get()) }
}
