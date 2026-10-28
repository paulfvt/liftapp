package com.paulfvt.liftapp.progress.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.core.domain.DailyValue
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.PersonalRecordType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.progress.data.GoalRepository
import com.paulfvt.liftapp.progress.data.RecordRepository
import com.paulfvt.liftapp.progress.domain.ChartPeriod
import com.paulfvt.liftapp.progress.domain.ExerciseMetric
import com.paulfvt.liftapp.progress.domain.GoalStatus
import com.paulfvt.liftapp.progress.domain.GoalTiers
import com.paulfvt.liftapp.progress.domain.PerformedSet
import com.paulfvt.liftapp.progress.domain.PersonalRecord
import com.paulfvt.liftapp.progress.domain.ProgressPoint
import com.paulfvt.liftapp.progress.domain.TieredGoal
import com.paulfvt.liftapp.progress.domain.bestPerDay
import com.paulfvt.liftapp.progress.domain.goalStatus
import com.paulfvt.liftapp.progress.domain.metricsFor
import com.paulfvt.liftapp.progress.domain.progressPoints
import com.paulfvt.liftapp.progress.domain.within
import com.paulfvt.liftapp.progress.navigation.ExerciseProgressRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExerciseProgressUiState(
    val isLoading: Boolean = true,
    val exercise: Exercise? = null,
    val records: List<PersonalRecord> = emptyList(),
    val metrics: List<ExerciseMetric> = emptyList(),
    val metric: ExerciseMetric? = null,
    val period: ChartPeriod = DEFAULT_PERIOD,
    val points: List<ProgressPoint> = emptyList(),
    // Les mêmes points ramenés à un par jour, pour la courbe.
    val chartDays: List<DailyValue> = emptyList(),
    val hasHistory: Boolean = false,
    // Objectifs à paliers : seulement pour les exercices poids × reps (barre basée sur le 1RM estimé).
    val canHaveGoal: Boolean = false,
    val currentOneRepMaxKg: Double = 0.0,
    val activeGoal: GoalStatus? = null,
    val completedGoals: List<TieredGoal> = emptyList()
)

// Données de l'exercice, réunies avant d'y appliquer la mesure et la période choisies.
private data class ExerciseData(
    val exercise: Exercise?,
    val records: List<PersonalRecord>,
    val sets: List<PerformedSet>,
    val goals: List<TieredGoal>
)

private val DEFAULT_PERIOD = ChartPeriod.THREE_MONTHS

// Progression d'un exercice : ses records et la courbe de la mesure et de la période choisies.
@HiltViewModel
class ExerciseProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    exercises: ExerciseRepository,
    records: RecordRepository,
    private val goals: GoalRepository,
    private val clock: Clock
) : ViewModel() {
    private val exerciseId = savedStateHandle.toRoute<ExerciseProgressRoute>().exerciseId

    // Choix gardés dans le SavedStateHandle : ils survivent à une rotation et à l'arrêt de l'app en arrière-plan.
    private val selectedMetric = savedStateHandle.getStateFlow<ExerciseMetric?>(METRIC_KEY, null)
    private val selectedPeriod = savedStateHandle.getStateFlow(PERIOD_KEY, DEFAULT_PERIOD)
    private val handle = savedStateHandle

    private val data = combine(
        exercises.observeExercise(exerciseId),
        records.observeRecords(exerciseId),
        records.observePerformedSets(exerciseId),
        goals.observeGoals().map { all -> all.filter { it.exerciseId == exerciseId } }
    ) { exercise, recorded, sets, exerciseGoals -> ExerciseData(exercise, recorded, sets, exerciseGoals) }

    val uiState: StateFlow<ExerciseProgressUiState> = combine(
        data,
        selectedMetric,
        selectedPeriod
    ) { (exercise, recorded, sets, exerciseGoals), chosenMetric, period ->
        val metrics = exercise?.let { metricsFor(it.trackingType) }.orEmpty()
        val metric = chosenMetric?.takeIf { it in metrics } ?: metrics.firstOrNull()
        val points = if (exercise != null && metric != null) {
            progressPoints(sets, metric, exercise.trackingType).within(period, clock.instant(), ZoneId.systemDefault())
        } else {
            emptyList()
        }
        ExerciseProgressUiState(
            isLoading = false,
            exercise = exercise,
            records = recorded,
            metrics = metrics,
            metric = metric,
            period = period,
            points = points,
            chartDays = points.bestPerDay(ZoneId.systemDefault()),
            hasHistory = sets.isNotEmpty(),
            canHaveGoal = exercise?.trackingType == TrackingType.WEIGHT_REPS,
            currentOneRepMaxKg = recorded.firstOrNull { it.type == PersonalRecordType.MAX_ESTIMATED_1RM }?.value ?: 0.0,
            activeGoal = exerciseGoals.firstOrNull { !it.isCompleted }?.let { goalStatus(it, sets) },
            completedGoals = exerciseGoals.filter { it.isCompleted }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ExerciseProgressUiState())

    fun onCreateGoal(tiers: GoalTiers) {
        viewModelScope.launch { goals.create(exerciseId, tiers) }
    }

    fun onDeleteGoal(goalId: Long) {
        viewModelScope.launch { goals.delete(goalId) }
    }

    fun onMetricSelected(metric: ExerciseMetric) {
        handle[METRIC_KEY] = metric
    }

    fun onPeriodSelected(period: ChartPeriod) {
        handle[PERIOD_KEY] = period
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val METRIC_KEY = "metric"
        const val PERIOD_KEY = "period"
    }
}
