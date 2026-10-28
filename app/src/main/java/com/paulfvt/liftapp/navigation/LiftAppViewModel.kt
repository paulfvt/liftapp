package com.paulfvt.liftapp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.onboarding.data.OnboardingRepository
import com.paulfvt.liftapp.progress.data.WeeklyGoalRepository
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Ce qu'affiche le bandeau EN DIRECT : le nom de la séance en cours et son heure de début.
data class LiveWorkout(val name: String, val startedAt: Instant)

// État partagé par tous les onglets : écran de bienvenue à montrer, séance en cours.
@HiltViewModel
class LiftAppViewModel @Inject constructor(
    private val workouts: WorkoutRepository,
    private val onboarding: OnboardingRepository,
    private val weeklyGoals: WeeklyGoalRepository
) : ViewModel() {
    // null tant que la préférence n'est pas lue : rien n'est affiché pendant ces quelques millisecondes,
    // pour ne pas montrer l'app une fraction de seconde avant l'écran de bienvenue.
    val showWelcome: StateFlow<Boolean?> = onboarding.isWelcomeDone
        .map { done -> !done }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val liveWorkout: StateFlow<LiveWorkout?> = workouts.observeInProgress()
        .map { workout -> workout?.let { LiveWorkout(it.name, it.startedAt) } }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    // L'objectif est enregistré avant de quitter l'écran de bienvenue : l'anneau l'affiche dès le premier écran.
    fun finishWelcome(weeklyGoal: Int) {
        viewModelScope.launch {
            weeklyGoals.setInitialGoal(weeklyGoal)
            onboarding.markWelcomeDone()
        }
    }

    // Au lancement, une séance encore en cours (app tuée, téléphone redémarré) est rouverte directement.
    suspend fun hasWorkoutInProgress(): Boolean = workouts.inProgressId() != null

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
