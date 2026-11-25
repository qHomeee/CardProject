package com.example.cartproject.di

import com.example.cartproject.data.local.RoomLocalDeckStore
import com.example.cartproject.data.local.RoomReviewStore
import com.example.cartproject.data.local.openZapomniDatabase
import com.example.cartproject.data.settings.openSettingsStore
import com.example.cartproject.domain.decks.DeleteOwnMaterial
import com.example.cartproject.domain.decks.DownloadDeck
import com.example.cartproject.domain.decks.LocalDeckList
import com.example.cartproject.domain.decks.LocalDeckStore
import com.example.cartproject.domain.decks.ReadDeck
import com.example.cartproject.domain.decks.RemoveDownloadedDeck
import com.example.cartproject.domain.decks.SaveOwnCard
import com.example.cartproject.domain.decks.SaveOwnDeck
import com.example.cartproject.domain.settings.ObserveSettings
import com.example.cartproject.domain.study.GradeCard
import com.example.cartproject.domain.study.ReadStatistics
import com.example.cartproject.domain.study.ReviewStore
import com.example.cartproject.domain.study.StartStudy
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose

/** Связывает одну файловую базу и DataStore со сценариями приложения. */
internal val storageModule = module {
    single { openZapomniDatabase(androidContext()) } onClose { it?.close() }
    single<LocalDeckStore> { RoomLocalDeckStore(get()) } bind LocalDeckList::class
    single<ReviewStore> { RoomReviewStore(get()) }
    single { openSettingsStore(androidContext()) }

    factory { ReadDeck(get(), get()) }
    factory { DownloadDeck(get(), get()) }
    factory { SaveOwnDeck(get()) }
    factory { SaveOwnCard(get()) }
    factory { DeleteOwnMaterial(get()) }
    factory { RemoveDownloadedDeck(get()) }
    factory { StartStudy(get()) }
    factory { GradeCard(get()) }
    factory { ReadStatistics(get()) }
    factory { ObserveSettings(get()) }
}
