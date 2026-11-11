package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.core.domain.DailyValue
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

// Dépense réelle estimée (décision du 3 novembre 2026) : ce qui a été mangé, corrigé par l'évolution du poids.
// Si le poids monte de 0,3 kg par semaine avec 2 980 kcal par jour, l'excédent vaut 0,3 × 7 700 / 7 = 330 kcal :
// la dépense est d'environ 2 650 kcal.
const val EXPENDITURE_WINDOW_DAYS = 28
const val MIN_LOGGED_DAYS = 21
const val MIN_WEIGH_INS = 4
const val MIN_WEIGH_IN_SPAN_DAYS = 21
const val KCAL_PER_KG = 7_700
const val PROPOSAL_THRESHOLD_KCAL = 100

sealed interface ExpenditureEstimate {
    data class Ready(
        val kcalPerDay: Int,
        val averageIntake: Int,
        val weeklyWeightChangeKg: Double,
        val loggedDays: Int
    ) : ExpenditureEstimate

    // Pas encore assez de données : ce qui manque, pour l'expliquer à l'écran.
    data class NotEnoughData(val loggedDays: Int, val weighIns: Int, val weighInSpanDays: Int) : ExpenditureEstimate
}

// Fenêtre : les 28 jours avant aujourd'hui (la journée en cours n'est pas finie). Les jours où rien n'est noté sont
// ignorés : un oubli ne doit pas passer pour un jeûne. intakes : calories de chaque jour noté ; weights : poids
// moyen de chaque jour pesé.
fun estimateExpenditure(today: LocalDate, intakes: List<DailyValue>, weights: List<DailyValue>): ExpenditureEstimate {
    val from = today.minusDays(EXPENDITURE_WINDOW_DAYS.toLong())
    val logged = intakes.filter { it.date >= from && it.date < today && it.value > 0 }
    val weighIns = weights.filter { it.date >= from && it.date <= today }.sortedBy { it.date }
    val span = weighIns.takeIf { it.size >= 2 }
        ?.let { ChronoUnit.DAYS.between(it.first().date, it.last().date).toInt() } ?: 0
    if (logged.size < MIN_LOGGED_DAYS || weighIns.size < MIN_WEIGH_INS || span < MIN_WEIGH_IN_SPAN_DAYS) {
        return ExpenditureEstimate.NotEnoughData(logged.size, weighIns.size, span)
    }
    val averageIntake = logged.map { it.value }.average()
    val slopePerDay = leastSquaresSlope(weighIns.map { ChronoUnit.DAYS.between(from, it.date).toDouble() to it.value })
    val expenditure = averageIntake - slopePerDay * KCAL_PER_KG
    return ExpenditureEstimate.Ready(
        kcalPerDay = (expenditure / 10).roundToInt() * 10,
        averageIntake = averageIntake.roundToInt(),
        weeklyWeightChangeKg = slopePerDay * 7,
        loggedDays = logged.size
    )
}

// Nouvel objectif proposé quand la dépense réelle s'écarte de plus de 100 kcal de celle que suppose l'objectif
// actuel : même écart de sèche ou de prise de masse, appliqué à la dépense réelle. Null sinon.
fun proposedGoalKcal(estimate: ExpenditureEstimate.Ready, goal: NutritionGoal): Int? {
    if (abs(estimate.kcalPerDay - goal.impliedExpenditure) <= PROPOSAL_THRESHOLD_KCAL) return null
    return (estimate.kcalPerDay + goal.settings.objective.kcalOffset).coerceIn(GOAL_KCAL_RANGE)
}
