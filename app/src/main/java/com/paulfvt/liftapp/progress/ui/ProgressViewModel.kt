package com.paulfvt.liftapp.progress.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.profile.data.ProfileRepository
import com.paulfvt.liftapp.progress.data.BodyMapRepository
import com.paulfvt.liftapp.progress.data.MotivationRepository
import com.paulfvt.liftapp.progress.data.RecordRepository
import com.paulfvt.liftapp.progress.data.WeeklyGoalRepository
import com.paulfvt.liftapp.progress.domain.ExerciseHistory
import com.paulfvt.liftapp.progress.domain.PerformedExercise
import com.paulfvt.liftapp.progress.domain.WeeklyStreak
import com.paulfvt.liftapp.progress.domain.muscleSets
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgressUiState(
    val isLoading: Boolean = true,
    val streak: WeeklyStreak? = null,
    val muscleSets: Map<Muscle, Double> = emptyMap(),
    val bodyFigures: List<BodyFigure> = emptyList(),
    val exercises: List<PerformedExercise> = emptyList()
)

// Ce qui se calcule à partir de l'historique de tous les exercices.
private data class HistoryState(val muscleSets: Map<Muscle, Double>)

// Onglet Progrès : la semaine en cours et la motivation en haut (l'accueil est masqué jusqu'à la phase 9), puis
// les exercices déjà faits, le plus récent d'abord.
@HiltViewModel
class ProgressViewModel @Inject constructor(
    records: RecordRepository,
    motivation: MotivationRepository,
    profiles: ProfileRepository,
    bodyMaps: BodyMapRepository,
    private val weeklyGoals: WeeklyGoalRepository,
    private val clock: Clock
) : ViewModel() {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val today: LocalDate get() = clock.instant().atZone(zone).toLocalDate()

    private val historyState = motivation.observeHistories()
        .map { histories -> historyState(histories) }
        .flowOn(Dispatchers.Default)

    // Silhouette selon le sexe du profil ; les tracés sont convertis hors du fil principal.
    private val bodyFigures = profiles.profile
        .map { it.sex }
        .distinctUntilChanged()
        .map { sex -> bodyMaps.diagrams(sex).map { it.toFigure() } }
        .flowOn(Dispatchers.Default)

    val uiState: StateFlow<ProgressUiState> = combine(
        weeklyGoals.observeStreak(),
        historyState,
        bodyFigures,
        records.observePerformedExercises()
    ) { streak, history, figures, exercises ->
        ProgressUiState(
            isLoading = false,
            streak = streak,
            muscleSets = history.muscleSets,
            bodyFigures = figures,
            exercises = exercises
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProgressUiState())

    private fun historyState(histories: List<ExerciseHistory>) = HistoryState(
        muscleSets = muscleSets(histories, today, zone)
    )

    fun onWeeklyGoalChange(goal: Int) {
        viewModelScope.launch { weeklyGoals.changeGoal(goal) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
