package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.core.domain.roundForDisplay
import com.paulfvt.liftapp.core.domain.toKg
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.workout.domain.parseDecimal
import java.time.Instant

enum class GoalTier {
    BRONZE,
    SILVER,
    GOLD
}

// Objectif à paliers sur le 1RM estimé d'un exercice poids × reps (« 100 kg au squat »). Un palier atteint reste
// acquis avec sa date, même si l'historique est modifié ensuite. L'objectif est terminé quand l'or est atteint.
data class TieredGoal(
    val id: Long,
    val exerciseId: Long,
    val bronzeKg: Double,
    val silverKg: Double,
    val goldKg: Double,
    val createdAt: Instant,
    val reachedAt: Map<GoalTier, Instant> = emptyMap()
) {
    val isCompleted: Boolean get() = GoalTier.GOLD in reachedAt

    fun valueOf(tier: GoalTier): Double = when (tier) {
        GoalTier.BRONZE -> bronzeKg
        GoalTier.SILVER -> silverKg
        GoalTier.GOLD -> goldKg
    }

    // Prochain palier à atteindre, null une fois l'or atteint.
    val nextTier: GoalTier? get() = GoalTier.entries.firstOrNull { it !in reachedAt }
}

// Paliers proposés (décision du 28 octobre 2026) : bronze et argent à 1/3 et 2/3 du chemin entre le 1RM estimé actuel
// et la cible, arrondis au demi-kilo. L'utilisateur peut les modifier.
fun suggestedTiers(currentKg: Double, goldKg: Double): Pair<Double, Double> {
    val gap = goldKg - currentKg
    return roundForDisplay(currentKg + gap / 3) to roundForDisplay(currentKg + 2 * gap / 3)
}

const val MAX_GOAL_KG = 1_000.0

enum class GoalError {
    INVALID_NUMBER,
    NOT_ABOVE_CURRENT,
    NOT_INCREASING
}

data class GoalTiers(val bronzeKg: Double, val silverKg: Double, val goldKg: Double)

// Paliers saisis, en texte tant qu'ils ne sont pas validés, dans l'unité choisie (kilos ou livres).
data class GoalDraft(val bronze: String = "", val silver: String = "", val gold: String = "") {
    // Paliers en kilos, l'unité de stockage.
    fun parsed(unit: WeightUnit = WeightUnit.KG): GoalTiers? {
        val bronzeKg = parseDecimal(bronze)?.let(unit::toKg) ?: return null
        val silverKg = parseDecimal(silver)?.let(unit::toKg) ?: return null
        val goldKg = parseDecimal(gold)?.let(unit::toKg) ?: return null
        return GoalTiers(bronzeKg, silverKg, goldKg)
    }
}

// Un objectif doit viser plus haut que le 1RM estimé actuel, avec des paliers croissants : sinon un palier serait
// atteint à la création, sans effort.
fun GoalDraft.problem(currentKg: Double, unit: WeightUnit = WeightUnit.KG): GoalError? {
    val tiers = parsed(unit) ?: return GoalError.INVALID_NUMBER
    if (listOf(tiers.bronzeKg, tiers.silverKg, tiers.goldKg).any { it <= 0 || it > MAX_GOAL_KG }) {
        return GoalError.INVALID_NUMBER
    }
    if (tiers.bronzeKg <= currentKg) return GoalError.NOT_ABOVE_CURRENT
    if (tiers.bronzeKg >= tiers.silverKg || tiers.silverKg >= tiers.goldKg) return GoalError.NOT_INCREASING
    return null
}

// Meilleur 1RM estimé de chaque séance (échauffements exclus), dans l'ordre.
private fun oneRepMaxPerSession(sets: List<PerformedSet>) =
    progressPoints(sets, ExerciseMetric.ESTIMATED_1RM, TrackingType.WEIGHT_REPS)

// Paliers pas encore acquis que l'historique atteint : la première séance dont le meilleur 1RM estimé atteint le
// palier lui donne sa date, jamais antérieure à la création de l'objectif.
fun newlyReachedTiers(goal: TieredGoal, sets: List<PerformedSet>): Map<GoalTier, Instant> {
    val sessions = oneRepMaxPerSession(sets)
    return GoalTier.entries.filter { it !in goal.reachedAt }.mapNotNull { tier ->
        sessions.firstOrNull { it.value >= goal.valueOf(tier) }?.let { tier to maxOf(it.date, goal.createdAt) }
    }.toMap()
}

// Où en est un objectif : la barre va du 1RM estimé à la création (start) jusqu'à l'or.
data class GoalStatus(val goal: TieredGoal, val startKg: Double, val bestKg: Double) {
    fun fractionOf(valueKg: Double): Float {
        val span = goal.goldKg - startKg
        if (span <= 0) return 1f
        return ((valueKg - startKg) / span).coerceIn(0.0, 1.0).toFloat()
    }

    val progress: Float get() = fractionOf(bestKg)
}

// start : meilleur 1RM des séances commencées avant la création, plafonné au bronze (objectif créé pendant une
// séance qui l'a ensuite atteint).
fun goalStatus(goal: TieredGoal, sets: List<PerformedSet>): GoalStatus {
    val sessions = oneRepMaxPerSession(sets)
    val start = sessions.filter { it.date.isBefore(goal.createdAt) }.maxOfOrNull { it.value } ?: 0.0
    return GoalStatus(goal, start.coerceAtMost(goal.bronzeKg), sessions.maxOfOrNull { it.value } ?: 0.0)
}
