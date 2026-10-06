package com.paulfvt.liftapp.exercise.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.exercise.navigation.ExerciseDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ExerciseDetailUiState {
    data object Loading : ExerciseDetailUiState

    data object NotFound : ExerciseDetailUiState

    data class Loaded(val exercise: Exercise) : ExerciseDetailUiState
}

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository
) : ViewModel() {
    // L'identifiant arrive par la route de navigation, conservée même si Android recrée l'écran.
    private val exerciseId = savedStateHandle.toRoute<ExerciseDetailRoute>().exerciseId

    val uiState: StateFlow<ExerciseDetailUiState> = repository.observeExercise(exerciseId)
        .map { exercise ->
            exercise?.let { ExerciseDetailUiState.Loaded(it) }
                ?: ExerciseDetailUiState.NotFound
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ExerciseDetailUiState.Loading
        )

    fun saveNote(note: String) {
        viewModelScope.launch { repository.updateNote(exerciseId, note) }
    }

    fun setHidden(isHidden: Boolean) {
        viewModelScope.launch { repository.setHidden(exerciseId, isHidden) }
    }
}
