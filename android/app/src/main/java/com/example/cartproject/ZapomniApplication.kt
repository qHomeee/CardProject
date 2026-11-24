package com.example.cartproject

import android.app.Application
import com.example.cartproject.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** Запускает граф зависимостей при создании процесса приложения. */
class ZapomniApplication : Application() {
    /** Регистрирует Android-контекст и настройки Koin. */
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ZapomniApplication)
            modules(appModule)
        }
    }
}
