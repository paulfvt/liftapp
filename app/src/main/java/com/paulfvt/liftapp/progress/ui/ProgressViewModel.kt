package com.paulfvt.liftapp.progress.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.progress.data.RecordRepository
import com.paulfvt.liftapp.progress.domain.PerformedExercise
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(val isLoading: Boolean = true, val exercises: List<PerformedExercise> = emptyList())

// Onglet Progrès : les exercices déjà faits, le plus récent d'abord.
@HiltViewModel
class ProgressViewModel @Inject constructor(records: RecordRepository) : ViewModel() {
    val uiState: StateFlow<ProgressUiState> = records.observePerformedExercises()
        .map { ProgressUiState(isLoading = false, exercises = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProgressUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
