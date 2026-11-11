package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.core.domain.DailyValue
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

// Tendance de la nutrition (étape 6 de la phase 12, 11 novembre 2026) : la semaine de calories et le poids des
// 4 dernières semaines. La dépense réelle estimée est dans RealExpenditure.kt.

const val TREND_DAYS = 7
const val WEIGHT_TREND_DAYS = 28
private const val DAYS_PER_WEEK = 7

// Ce qui a été mangé un jour noté.
data class DailyNutrients(val date: LocalDate, val nutrients: Nutrients)

// Une barre du graphique : les calories d'un jour (0 s'il n'est pas noté). Le jour en cours n'est pas fini.
data class DayIntake(val date: LocalDate, val kcal: Double, val isToday: Boolean)

// Les 7 derniers jours, aujourd'hui compris. La moyenne et les protéines ne comptent que les jours terminés et notés :
// la journée en cours n'est pas finie, et un oubli ne doit pas passer pour un jeûne (même règle que la dépense réelle).
// goalKcal : l'objectif d'aujourd'hui ; les protéines suivent l'objectif de chaque jour.
data class CalorieWeek(
    val days: List<DayIntake>,
    val averageKcal: Int?,
    val goalKcal: Int?,
    val proteinReachedDays: Int,
    val loggedDays: Int
)

fun calorieWeek(today: LocalDate, daily: List<DailyNutrients>, goals: List<NutritionGoal>): CalorieWeek {
    val byDate = daily.associateBy { it.date }
    val days = (TREND_DAYS - 1 downTo 0).map { back ->
        val date = today.minusDays(back.toLong())
        DayIntake(date, byDate[date]?.nutrients?.kcal ?: 0.0, isToday = back == 0)
    }
    val logged = days.filter { !it.isToday && it.kcal > 0 }.mapNotNull { byDate[it.date] }
    val proteinReached = logged.count { day ->
        goalOn(day.date, goals)?.let { day.nutrients.protein >= it.target.proteinGrams } == true
    }
    return CalorieWeek(
        days = days,
        averageKcal = logged.takeIf { it.isNotEmpty() }?.map { it.nutrients.kcal }?.average()?.roundToInt(),
        goalKcal = goalOn(today, goals)?.target?.kcal,
        proteinReachedDays = proteinReached,
        loggedDays = logged.size
    )
}

// Poids des 4 dernières semaines : la dernière pesée et la pente de la droite des moindres carrés, en kg sur la
// période pesée et par semaine. Null avec moins de deux jours pesés.
data class WeightTrend(
    val latestKg: Double,
    val changeKg: Double,
    val weeklyChangeKg: Double,
    val days: List<DailyValue>
)

fun weightTrend(today: LocalDate, weights: List<DailyValue>): WeightTrend? {
    val from = today.minusDays(WEIGHT_TREND_DAYS.toLong())
    val days = weights.filter { it.date >= from && it.date <= today }.sortedBy { it.date }
    if (days.size < 2) return null
    val slopePerDay = leastSquaresSlope(days.map { ChronoUnit.DAYS.between(from, it.date).toDouble() to it.value })
    val span = ChronoUnit.DAYS.between(days.first().date, days.last().date)
    return WeightTrend(
        latestKg = days.last().value,
        changeKg = slopePerDay * span,
        weeklyChangeKg = slopePerDay * DAYS_PER_WEEK,
        days = days
    )
}

// Pente de la droite des moindres carrés (unité par jour) : moins sensible qu'un simple écart entre la première et la
// dernière pesée aux variations d'un jour à l'autre (eau, repas salé…).
internal fun leastSquaresSlope(points: List<Pair<Double, Double>>): Double {
    val meanX = points.map { it.first }.average()
    val meanY = points.map { it.second }.average()
    val numerator = points.sumOf { (x, y) -> (x - meanX) * (y - meanY) }
    val denominator = points.sumOf { (x, _) -> (x - meanX) * (x - meanX) }
    return if (denominator == 0.0) 0.0 else numerator / denominator
}
