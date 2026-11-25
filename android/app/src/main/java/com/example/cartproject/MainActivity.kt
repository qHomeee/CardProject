package com.example.cartproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.settings.ObserveSettings
import com.example.cartproject.domain.settings.ThemePreference
import com.example.cartproject.domain.settings.UserSettings
import com.example.cartproject.presentation.decks.MyDecksScreen
import com.example.cartproject.presentation.decks.MyDecksViewModel
import com.example.cartproject.presentation.theme.ZapomniTheme
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Открывает стартовый экран приложения. */
class MainActivity : ComponentActivity() {
    private val myDecksViewModel: MyDecksViewModel by viewModel()
    private val observeSettings: ObserveSettings by inject()

    /** Подписывает экран на Room и применяет сохранённую тему. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsFlow = observeSettings()
        setContent {
            val state by myDecksViewModel.uiState.collectAsStateWithLifecycle()
            val saved by settingsFlow.collectAsStateWithLifecycle(Outcome.Success(UserSettings()))
            val preference = (saved as? Outcome.Success)?.value?.theme ?: ThemePreference.System
            val darkTheme = when (preference) {
                ThemePreference.System -> isSystemInDarkTheme()
                ThemePreference.Light -> false
                ThemePreference.Dark -> true
            }
            SideEffect {
                val bars = WindowCompat.getInsetsController(window, window.decorView)
                bars.isAppearanceLightStatusBars = !darkTheme
                bars.isAppearanceLightNavigationBars = !darkTheme
            }
            ZapomniTheme(darkTheme = darkTheme) {
                Scaffold { innerPadding ->
                    MyDecksScreen(state, myDecksViewModel::retry, Modifier.padding(innerPadding))
                }
            }
        }
    }
}
