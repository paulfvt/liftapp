package com.paulfvt.liftapp.history.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.core.di.ApplicationScope
import com.paulfvt.liftapp.history.data.HistoryRepository
import com.paulfvt.liftapp.history.navigation.WorkoutDetailRoute
import com.paulfvt.liftapp.workout.domain.Workout
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutDetailUiState(val isLoading: Boolean = true, val workout: Workout? = null)

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val history: HistoryRepository,
    @param:ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {
    val workoutId: Long = savedStateHandle.toRoute<WorkoutDetailRoute>().workoutId

    val uiState: StateFlow<WorkoutDetailUiState> = history.observeWorkout(workoutId)
        .map { WorkoutDetailUiState(isLoading = false, workout = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkoutDetailUiState())

    // Gardée dans le ViewModel pour rester ouverte après une rotation.
    var isDeleteDialogOpen by mutableStateOf(false)
        private set

    fun openDeleteDialog() {
        isDeleteDialogOpen = true
    }

    fun dismissDeleteDialog() {
        isDeleteDialogOpen = false
    }

    // Dans le scope de l'application : la suppression va au bout même si l'écran se ferme aussitôt.
    // L'écran se ferme de lui-même quand la séance disparaît de la base.
    fun delete() {
        isDeleteDialogOpen = false
        appScope.launch { history.delete(workoutId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
