package com.paulfvt.liftapp.progress.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.progress.data.RecordRepository
import com.paulfvt.liftapp.progress.data.WeeklyGoalRepository
import com.paulfvt.liftapp.progress.domain.PerformedExercise
import com.paulfvt.liftapp.progress.domain.WeeklyStreak
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgressUiState(
    val isLoading: Boolean = true,
    val streak: WeeklyStreak? = null,
    val exercises: List<PerformedExercise> = emptyList()
)

// Onglet Progrès : la semaine en cours et la motivation en haut (l'accueil est masqué jusqu'à la phase 9), puis
// les exercices déjà faits, le plus récent d'abord.
@HiltViewModel
class ProgressViewModel @Inject constructor(records: RecordRepository, private val weeklyGoals: WeeklyGoalRepository) :
    ViewModel() {
    val uiState: StateFlow<ProgressUiState> = combine(
        weeklyGoals.observeStreak(),
        records.observePerformedExercises()
    ) { streak, exercises ->
        ProgressUiState(isLoading = false, streak = streak, exercises = exercises)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProgressUiState())

    fun onWeeklyGoalChange(goal: Int) {
        viewModelScope.launch { weeklyGoals.changeGoal(goal) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
