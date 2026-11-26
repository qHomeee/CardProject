package com.example.cartproject.di

import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.net.ConnectivityManager
import com.example.cartproject.data.settings.StudyReminderController
import com.example.cartproject.data.study.AndroidStudyGestureSource
import com.example.cartproject.domain.decks.ObserveMyDecks
import com.example.cartproject.domain.settings.ReminderScheduler
import com.example.cartproject.domain.settings.UpdateSettings
import com.example.cartproject.domain.study.GestureSource
import com.example.cartproject.domain.study.ObserveStudyGestures
import com.example.cartproject.presentation.catalog.CatalogViewModel
import com.example.cartproject.presentation.decks.DeckDetailViewModel
import com.example.cartproject.presentation.decks.MyDecksViewModel
import com.example.cartproject.presentation.editor.MaterialEditorViewModel
import com.example.cartproject.presentation.settings.ReminderSettingsViewModel
import com.example.cartproject.presentation.settings.SettingsViewModel
import com.example.cartproject.presentation.study.StudyViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

internal val appModule = module {
    includes(catalogModule, storageModule)

    factory { ObserveMyDecks(get()) }
    single { StudyReminderController(androidContext(), get(), get()) } bind ReminderScheduler::class
    factory { UpdateSettings(get(), get()) }
    single<GestureSource> {
        AndroidStudyGestureSource(
            androidContext().getSystemService(SensorManager::class.java),
            androidContext().getSystemService(DisplayManager::class.java),
        )
    }
    factory { ObserveStudyGestures(get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
    viewModel {
        val reminders = get<StudyReminderController>()
        ReminderSettingsViewModel(get(), get(), reminders::restoreSchedule)
    }
    viewModel { params -> StudyViewModel(params.get(), get(), get(), get(), get()) }
    viewModel { MyDecksViewModel(get()) }
    viewModel { params -> DeckDetailViewModel(params.get(), get(), get(), get()) }
    viewModel { params -> MaterialEditorViewModel(params.get(), get(), get(), get(), get()) }
    viewModel {
        val connectivity = androidContext().getSystemService(ConnectivityManager::class.java)
        CatalogViewModel(get(), get()) { connectivity.activeNetwork != null }
    }
}
