package com.paulfvt.liftapp.nutrition.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.core.model.ActivityLevel
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.core.model.NutritionObjective
import com.paulfvt.liftapp.nutrition.data.NutritionRepository
import com.paulfvt.liftapp.nutrition.domain.BodyProfile
import com.paulfvt.liftapp.nutrition.domain.FAT_PER_KG_RANGE
import com.paulfvt.liftapp.nutrition.domain.GOAL_KCAL_RANGE
import com.paulfvt.liftapp.nutrition.domain.GoalSettings
import com.paulfvt.liftapp.nutrition.domain.NeedsBreakdown
import com.paulfvt.liftapp.nutrition.domain.NutritionTarget
import com.paulfvt.liftapp.nutrition.domain.PROTEIN_PER_KG_RANGE
import com.paulfvt.liftapp.nutrition.domain.isConsistent
import com.paulfvt.liftapp.nutrition.domain.macroTarget
import com.paulfvt.liftapp.nutrition.domain.needsBreakdown
import com.paulfvt.liftapp.nutrition.domain.stepPerKg
import com.paulfvt.liftapp.profile.data.MeasurementRepository
import com.paulfvt.liftapp.profile.data.ProfileRepository
import com.paulfvt.liftapp.profile.domain.BodyMeasurement
import com.paulfvt.liftapp.profile.domain.Sex
import com.paulfvt.liftapp.profile.domain.ageOn
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Ce que le profil fournit au calcul. weight : dernière pesée. Sans sexe, date de naissance ou taille, pas de
// calcul : les calories se saisissent à la main. Sans poids, pas d'objectif possible (protéines et lipides par kg).
data class NeedsProfile(
    val sex: Sex? = null,
    val age: Int? = null,
    val heightCm: Double? = null,
    val weight: BodyMeasurement? = null
) {
    val body: BodyProfile?
        get() = if (sex != null && age != null && heightCm != null && weight != null) {
            BodyProfile(sex, age, heightCm, weight.value)
        } else {
            null
        }
}

// « Mes besoins » : objectif calculé depuis le profil (Mifflin-St Jeor × activité + écart de l'objectif), calories
// modifiables à la main. Enregistré à partir d'aujourd'hui, les jours passés gardent leur objectif.
@HiltViewModel
class NeedsViewModel @Inject constructor(
    profiles: ProfileRepository,
    measurements: MeasurementRepository,
    private val repository: NutritionRepository,
    private val clock: Clock
) : ViewModel() {
    private val today: LocalDate get() = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    val profile: StateFlow<NeedsProfile?> = combine(
        profiles.profile,
        measurements.observe(MeasurementType.BODY_WEIGHT)
    ) { profile, weights ->
        NeedsProfile(
            sex = profile.sex,
            age = profile.birthDate?.let { ageOn(it, today) },
            heightCm = profile.heightCm,
            // Les mesures arrivent de la plus récente à la plus ancienne.
            weight = weights.firstOrNull()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    var settings by mutableStateOf(GoalSettings())
        private set

    // Calories tapées à la main (texte du champ) ; vide = calcul.
    var manualKcalText by mutableStateOf("")
        private set

    var isSaved by mutableStateOf(false)
        private set

    private var isSaving = false

    init {
        // Réglages de l'objectif en vigueur, pour rouvrir l'écran tel qu'il a été enregistré.
        viewModelScope.launch {
            repository.observeGoals().first().lastOrNull()?.let { goal ->
                settings = goal.settings
                manualKcalText = goal.settings.manualKcal?.toString().orEmpty()
            }
        }
    }

    fun breakdown(profile: NeedsProfile): NeedsBreakdown? =
        profile.body?.let { needsBreakdown(it, settings.activityLevel, settings.objective) }

    // Objectif complet, ou null s'il manque le poids ou les calories (profil incomplet sans saisie à la main).
    fun target(profile: NeedsProfile): NutritionTarget? {
        val weight = profile.weight?.value ?: return null
        val kcal = manualKcal() ?: breakdown(profile)?.goalKcal ?: return null
        return macroTarget(kcal, weight, settings.proteinPerKg, settings.fatPerKg)
    }

    fun canSave(profile: NeedsProfile): Boolean = target(profile)?.isConsistent() == true && isManualKcalValid()

    fun isManualKcalValid(): Boolean = manualKcalText.isEmpty() || manualKcal() != null

    private fun manualKcal(): Int? = manualKcalText.toIntOrNull()?.takeIf { it in GOAL_KCAL_RANGE }

    fun onActivityChange(level: ActivityLevel) {
        settings = settings.copy(activityLevel = level)
    }

    fun onObjectiveChange(objective: NutritionObjective) {
        settings = settings.copy(objective = objective)
    }

    fun stepProtein(steps: Int) {
        settings = settings.copy(proteinPerKg = stepPerKg(settings.proteinPerKg, steps, PROTEIN_PER_KG_RANGE))
    }

    fun stepFat(steps: Int) {
        settings = settings.copy(fatPerKg = stepPerKg(settings.fatPerKg, steps, FAT_PER_KG_RANGE))
    }

    fun onManualKcalChange(text: String) {
        manualKcalText = text.filter(Char::isDigit).take(MAX_KCAL_DIGITS)
    }

    fun resetToCalculation() {
        manualKcalText = ""
    }

    fun save() {
        val profile = profile.value ?: return
        val target = target(profile)?.takeIf { canSave(profile) } ?: return
        if (isSaving) return
        isSaving = true
        viewModelScope.launch {
            repository.saveGoal(today, target, settings.copy(manualKcal = manualKcal()))
            isSaved = true
        }
    }

    private companion object {
        const val MAX_KCAL_DIGITS = 4
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
