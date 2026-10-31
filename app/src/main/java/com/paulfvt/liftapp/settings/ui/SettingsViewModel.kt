package com.paulfvt.liftapp.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.core.di.ApplicationScope
import com.paulfvt.liftapp.core.domain.LengthUnit
import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.progress.data.WeeklyGoalRepository
import com.paulfvt.liftapp.settings.data.AppSettings
import com.paulfvt.liftapp.settings.data.AppSettingsRepository
import com.paulfvt.liftapp.settings.data.ThemeMode
import com.paulfvt.liftapp.workout.data.WorkoutSettings
import com.paulfvt.liftapp.workout.data.WorkoutSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val app: AppSettings = AppSettings(),
    val workout: WorkoutSettings = WorkoutSettings(),
    // Objectif de la semaine en cours.
    val weeklyGoal: Int = 0
)

// Écran Réglages. Les écritures partent dans le scope de l'application : un réglage changé juste avant de quitter
// l'écran est bien enregistré.
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettingsRepository,
    private val workoutSettings: WorkoutSettingsRepository,
    private val weeklyGoals: WeeklyGoalRepository,
    @param:ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        appSettings.settings,
        workoutSettings.settings,
        weeklyGoals.observeStreak().map { it.currentWeek.goal }.distinctUntilChanged()
    ) { app, workout, goal ->
        SettingsUiState(isLoading = false, app = app, workout = workout, weeklyGoal = goal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    fun onThemeModeChange(mode: ThemeMode) = write { appSettings.setThemeMode(mode) }

    fun onWeightUnitChange(unit: WeightUnit) = write { appSettings.setWeightUnit(unit) }

    fun onLengthUnitChange(unit: LengthUnit) = write { appSettings.setLengthUnit(unit) }

    // Comme depuis l'accueil : le nouvel objectif compte dès la semaine en cours.
    fun onWeeklyGoalChange(goal: Int) = write { weeklyGoals.changeGoal(goal) }

    fun onDefaultRestChange(seconds: Int) = write { appSettings.setDefaultRestSeconds(seconds) }

    fun onRestSoundChange(enabled: Boolean) = write { workoutSettings.setRestSound(enabled) }

    fun onRestVibrationChange(enabled: Boolean) = write { workoutSettings.setRestVibration(enabled) }

    private fun write(block: suspend () -> Unit) {
        appScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
