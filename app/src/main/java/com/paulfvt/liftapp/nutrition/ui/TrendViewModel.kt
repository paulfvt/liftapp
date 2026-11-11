package com.paulfvt.liftapp.nutrition.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.core.domain.DailyValue
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.nutrition.data.NutritionRepository
import com.paulfvt.liftapp.nutrition.domain.CalorieWeek
import com.paulfvt.liftapp.nutrition.domain.EXPENDITURE_WINDOW_DAYS
import com.paulfvt.liftapp.nutrition.domain.ExpenditureEstimate
import com.paulfvt.liftapp.nutrition.domain.NutritionGoal
import com.paulfvt.liftapp.nutrition.domain.WeightTrend
import com.paulfvt.liftapp.nutrition.domain.calorieWeek
import com.paulfvt.liftapp.nutrition.domain.estimateExpenditure
import com.paulfvt.liftapp.nutrition.domain.goalOn
import com.paulfvt.liftapp.nutrition.domain.macroTarget
import com.paulfvt.liftapp.nutrition.domain.proposedGoalKcal
import com.paulfvt.liftapp.nutrition.domain.weightTrend
import com.paulfvt.liftapp.profile.data.MeasurementRepository
import com.paulfvt.liftapp.profile.domain.dailyValues
import com.paulfvt.liftapp.settings.data.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrendUiState(
    val isLoading: Boolean = true,
    val week: CalorieWeek? = null,
    // Masqué dans les réglages : la carte du poids disparaît, la dépense réelle reste (elle n'affiche aucun poids).
    val isWeightHidden: Boolean = false,
    val weight: WeightTrend? = null,
    val expenditure: ExpenditureEstimate? = null,
    val goal: NutritionGoal? = null,
    val proposedKcal: Int? = null
)

// Tendance de la nutrition (étape 6 de la phase 12) : semaine de calories, poids des 4 dernières semaines et dépense
// réelle estimée, avec le nouvel objectif proposé. Rien n'est appliqué sans « Passer à ».
@HiltViewModel
class TrendViewModel @Inject constructor(
    private val repository: NutritionRepository,
    measurements: MeasurementRepository,
    settings: AppSettingsRepository,
    private val clock: Clock
) : ViewModel() {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val today: LocalDate get() = LocalDate.now(clock.withZone(zone))

    // « Plus tard » : la proposition se replie jusqu'à la prochaine ouverture de l'écran.
    private val isProposalDismissed = MutableStateFlow(false)

    private var latestWeightKg: Double? = null

    val uiState: StateFlow<TrendUiState> = combine(
        repository.observeDailyNutrients(today.minusDays(EXPENDITURE_WINDOW_DAYS.toLong()), today),
        repository.observeGoals(),
        measurements.observe(MeasurementType.BODY_WEIGHT),
        settings.settings,
        isProposalDismissed
    ) { daily, goals, weighIns, appSettings, isDismissed ->
        val day = today
        val weights = dailyValues(weighIns, zone)
        latestWeightKg = weights.lastOrNull()?.value
        val goal = goalOn(day, goals)
        val expenditure = estimateExpenditure(day, daily.map { DailyValue(it.date, it.nutrients.kcal) }, weights)
        TrendUiState(
            isLoading = false,
            week = calorieWeek(day, daily, goals),
            isWeightHidden = appSettings.hideBodyWeight,
            weight = weightTrend(day, weights),
            expenditure = expenditure,
            goal = goal,
            proposedKcal = if (isDismissed || goal == null || expenditure !is ExpenditureEstimate.Ready) {
                null
            } else {
                proposedGoalKcal(expenditure, goal)
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TrendUiState())

    // Nouvel objectif en vigueur dès aujourd'hui (les jours passés gardent le leur) : les calories proposées, saisies
    // comme à la main, et les macros recalculées avec les mêmes réglages de protéines et de lipides.
    fun applyProposal() {
        val state = uiState.value
        val kcal = state.proposedKcal ?: return
        val goal = state.goal ?: return
        val weightKg = latestWeightKg ?: return
        val goalSettings = goal.settings.copy(manualKcal = kcal)
        viewModelScope.launch {
            repository.saveGoal(
                today,
                macroTarget(kcal, weightKg, goalSettings.proteinPerKg, goalSettings.fatPerKg),
                goalSettings
            )
        }
    }

    fun dismissProposal() {
        isProposalDismissed.value = true
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
