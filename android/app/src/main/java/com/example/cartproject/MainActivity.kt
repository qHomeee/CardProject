package com.example.cartproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cartproject.presentation.decks.MyDecksScreen
import com.example.cartproject.presentation.decks.MyDecksViewModel
import com.example.cartproject.presentation.theme.ZapomniTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Открывает стартовый экран приложения. */
class MainActivity : ComponentActivity() {
    private val myDecksViewModel: MyDecksViewModel by viewModel()

    /** Подписывает Compose на состояние ViewModel с учётом жизненного цикла. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by myDecksViewModel.uiState.collectAsStateWithLifecycle()
            ZapomniTheme {
                Scaffold { innerPadding ->
                    MyDecksScreen(state, Modifier.padding(innerPadding))
                }
            }
        }
    }
}
