package com.paulfvt.liftapp.home.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.home.data.Dashboard
import com.paulfvt.liftapp.home.data.DashboardRepository
import com.paulfvt.liftapp.progress.data.WeeklyGoalRepository
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// liveWorkoutName : nom de la séance en cours, null sinon. La carte « Prochaine séance » propose alors de la reprendre.
data class HomeUiState(
    val isLoading: Boolean = true,
    val dashboard: Dashboard? = null,
    val liveWorkoutName: String? = null
)

// Accueil : la semaine, la prochaine séance, le dernier record et le souvenir.
@HiltViewModel
class HomeViewModel @Inject constructor(
    dashboard: DashboardRepository,
    private val workouts: WorkoutRepository,
    private val weeklyGoals: WeeklyGoalRepository
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        dashboard.observe(),
        workouts.observeInProgress().map { it?.name }.distinctUntilChanged()
    ) { board, liveName ->
        HomeUiState(isLoading = false, dashboard = board, liveWorkoutName = liveName)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    // Vrai une fois la séance créée (ou déjà en cours) : l'écran l'ouvre puis appelle onWorkoutOpened.
    var isWorkoutReady by mutableStateOf(false)
        private set

    private var isStarting = false

    // templateId null : séance vide, nommée par l'écran (texte traduit). Une séance déjà en cours est simplement
    // rouverte : une seule à la fois.
    fun start(templateId: Long?, emptyName: String) {
        if (isStarting) return
        isStarting = true
        viewModelScope.launch {
            if (workouts.inProgressId() == null) {
                if (templateId != null) workouts.startFromTemplate(templateId) else workouts.startEmpty(emptyName)
            }
            isWorkoutReady = true
            isStarting = false
        }
    }

    fun onWorkoutOpened() {
        isWorkoutReady = false
    }

    fun onWeeklyGoalChange(goal: Int) {
        viewModelScope.launch { weeklyGoals.changeGoal(goal) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
