package com.paulfvt.liftapp.home.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.home.data.Dashboard
import com.paulfvt.liftapp.home.data.DashboardRepository
import com.paulfvt.liftapp.nutrition.data.NutritionRepository
import com.paulfvt.liftapp.nutrition.domain.Nutrients
import com.paulfvt.liftapp.nutrition.domain.NutritionTarget
import com.paulfvt.liftapp.nutrition.domain.goalOn
import com.paulfvt.liftapp.nutrition.domain.total
import com.paulfvt.liftapp.template.data.TemplateRepository
import com.paulfvt.liftapp.template.ui.list.TemplatePreview
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// liveWorkoutName : nom de la séance en cours, null sinon. La carte « Prochaine séance » propose alors de la reprendre.
// nutrition : la carte « Nutrition du jour », absente tant que la nutrition ne sert pas (rien noté, aucun objectif).
data class HomeUiState(
    val isLoading: Boolean = true,
    val dashboard: Dashboard? = null,
    val liveWorkoutName: String? = null,
    val nutrition: NutritionToday? = null
)

// Ce qui est noté aujourd'hui et l'objectif du jour (null sans objectif).
data class NutritionToday(val total: Nutrients, val goal: NutritionTarget?)

// Accueil : la semaine, la prochaine séance, la nutrition du jour, le dernier record et le souvenir.
@HiltViewModel
class HomeViewModel @Inject constructor(
    dashboard: DashboardRepository,
    private val workouts: WorkoutRepository,
    nutrition: NutritionRepository,
    private val templates: TemplateRepository,
    clock: Clock
) : ViewModel() {
    private val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    private val nutritionToday = combine(nutrition.observeDay(today), nutrition.observeGoals()) { entries, goals ->
        val goal = goalOn(today, goals)?.target
        if (entries.isEmpty() && goal == null) null else NutritionToday(entries.map { it.nutrients }.total(), goal)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        dashboard.observe(),
        workouts.observeInProgress().map { it?.name }.distinctUntilChanged(),
        nutritionToday
    ) { board, liveName, nutritionCard ->
        HomeUiState(isLoading = false, dashboard = board, liveWorkoutName = liveName, nutrition = nutritionCard)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    // Aperçu du template ouvert d'un appui sur la carte de la prochaine séance (même volet que l'onglet Séances).
    var preview by mutableStateOf<TemplatePreview?>(null)
        private set

    fun openPreview(templateId: Long) {
        viewModelScope.launch { templates.getDraft(templateId)?.let { preview = TemplatePreview(templateId, it) } }
    }

    fun closePreview() {
        preview = null
    }

    // Vrai une fois la séance créée (ou déjà en cours) : l'écran l'ouvre puis appelle onWorkoutOpened.
    var isWorkoutReady by mutableStateOf(false)
        private set

    private var isStarting = false

    // templateId null : séance vide, nommée par l'écran (texte traduit). Une séance déjà en cours est simplement
    // rouverte : une seule à la fois.
    fun start(templateId: Long?, emptyName: String) {
        if (isStarting) return
        isStarting = true
        preview = null
        viewModelScope.launch {
            isWorkoutReady = workouts.startOrResume(templateId, emptyName)
            isStarting = false
        }
    }

    fun onWorkoutOpened() {
        isWorkoutReady = false
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
