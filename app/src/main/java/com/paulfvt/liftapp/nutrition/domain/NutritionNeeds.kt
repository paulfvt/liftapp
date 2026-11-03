package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.core.model.ActivityLevel
import com.paulfvt.liftapp.core.model.NutritionObjective
import com.paulfvt.liftapp.profile.domain.Sex
import java.time.LocalDate
import kotlin.math.roundToInt

// Données du profil nécessaires au calcul des besoins. Null tant que l'une d'elles manque.
data class BodyProfile(val sex: Sex, val age: Int, val heightCm: Double, val weightKg: Double)

// Réglages de « Mes besoins ». manualKcal : calories saisies à la main à la place du calcul.
data class GoalSettings(
    val activityLevel: ActivityLevel = ActivityLevel.ACTIVE,
    val objective: NutritionObjective = NutritionObjective.MAINTAIN,
    val proteinPerKg: Double = DEFAULT_PROTEIN_PER_KG,
    val fatPerKg: Double = DEFAULT_FAT_PER_KG,
    val manualKcal: Int? = null
)

data class NutritionTarget(val kcal: Int, val proteinGrams: Int, val carbsGrams: Int, val fatGrams: Int)

// Objectif historisé : il vaut à partir de effectiveFrom.
data class NutritionGoal(val effectiveFrom: LocalDate, val target: NutritionTarget, val settings: GoalSettings) {
    // Dépense que suppose cet objectif : l'objectif sans l'écart de la sèche ou de la prise de masse.
    val impliedExpenditure: Int get() = target.kcal - settings.objective.kcalOffset
}

// Détail du calcul affiché dans « Mes besoins », arrondi à 10 kcal (la formule n'est pas plus précise).
data class NeedsBreakdown(val basalMetabolism: Int, val expenditure: Int, val offset: Int, val goalKcal: Int)

const val DEFAULT_PROTEIN_PER_KG = 2.0
const val DEFAULT_FAT_PER_KG = 1.0
const val MACRO_PER_KG_STEP = 0.1
val PROTEIN_PER_KG_RANGE = 0.5..4.0
val FAT_PER_KG_RANGE = 0.3..2.5
val GOAL_KCAL_RANGE = 800..8_000

private const val KCAL_PER_GRAM_PROTEIN = 4
private const val KCAL_PER_GRAM_CARBS = 4
private const val KCAL_PER_GRAM_FAT = 9

// Métabolisme de base de Mifflin-St Jeor (1990), en kcal par jour : la formule la plus juste en moyenne pour un
// adulte, à environ 10 % près.
fun basalMetabolicRate(profile: BodyProfile): Double {
    val base = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.age
    return base + if (profile.sex == Sex.MALE) 5 else -161
}

fun needsBreakdown(profile: BodyProfile, activityLevel: ActivityLevel, objective: NutritionObjective): NeedsBreakdown {
    val basal = basalMetabolicRate(profile)
    val expenditure = basal * activityLevel.factor
    return NeedsBreakdown(
        basalMetabolism = roundToTen(basal),
        expenditure = roundToTen(expenditure),
        offset = objective.kcalOffset,
        goalKcal = roundToTen(expenditure + objective.kcalOffset)
    )
}

// Protéines et lipides selon le poids, glucides pour le reste des calories (jamais moins de 0).
fun macroTarget(kcal: Int, weightKg: Double, proteinPerKg: Double, fatPerKg: Double): NutritionTarget {
    val protein = (weightKg * proteinPerKg).roundToInt()
    val fat = (weightKg * fatPerKg).roundToInt()
    val carbsKcal = kcal - protein * KCAL_PER_GRAM_PROTEIN - fat * KCAL_PER_GRAM_FAT
    return NutritionTarget(
        kcal = kcal,
        proteinGrams = protein,
        carbsGrams = (carbsKcal.toDouble() / KCAL_PER_GRAM_CARBS).roundToInt().coerceAtLeast(0),
        fatGrams = fat
    )
}

// Vrai si protéines et lipides tiennent dans les calories : sinon les glucides seraient négatifs.
fun NutritionTarget.isConsistent(): Boolean =
    proteinGrams * KCAL_PER_GRAM_PROTEIN + fatGrams * KCAL_PER_GRAM_FAT <= kcal

// Objectif complet à partir du profil et des réglages ; les calories saisies à la main remplacent le calcul.
fun nutritionTarget(profile: BodyProfile, settings: GoalSettings): NutritionTarget {
    val kcal = settings.manualKcal
        ?: needsBreakdown(profile, settings.activityLevel, settings.objective).goalKcal
    return macroTarget(kcal, profile.weightKg, settings.proteinPerKg, settings.fatPerKg)
}

// Objectif en vigueur un jour donné (goals triés par date) : le dernier commencé ce jour-là ou avant.
fun goalOn(date: LocalDate, goals: List<NutritionGoal>): NutritionGoal? = goals.lastOrNull { it.effectiveFrom <= date }

// Pas de 0,1 g/kg sans erreur d'arrondi (1,9 + 0,1 donne exactement 2,0).
fun stepPerKg(value: Double, steps: Int, range: ClosedFloatingPointRange<Double>): Double =
    (((value / MACRO_PER_KG_STEP).roundToInt() + steps) * MACRO_PER_KG_STEP)
        .let { (it * 10).roundToInt() / 10.0 }
        .coerceIn(range)

private fun roundToTen(value: Double): Int = (value / 10).roundToInt() * 10
