package com.example.cartproject.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cartproject.domain.Outcome
import com.example.cartproject.domain.study.ReadStatistics
import com.example.cartproject.domain.study.StudySummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Загружает общие итоги из истории оценок. */
internal class StatisticsViewModel(private val readStatistics: ReadStatistics) : ViewModel() {
    private val state = MutableStateFlow<Outcome<StudySummary>?>(null)
    val uiState = state.asStateFlow()
    private var reading: Job? = null

    init {
        load()
    }

    /** Повторяет чтение после ошибки. */
    fun load() {
        if (reading?.isActive == true) return
        state.value = null
        reading = viewModelScope.launch { state.value = readStatistics() }
    }
}
