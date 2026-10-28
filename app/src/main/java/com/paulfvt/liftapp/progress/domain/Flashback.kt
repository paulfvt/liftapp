package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.Exercise
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private val FLASHBACK_MONTHS = listOf(12, 6, 3, 1)

// Une séance « d'il y a 6 mois » peut tomber quelques jours avant ou après la date exacte.
private const val WINDOW_DAYS = 7L

// La performance actuelle doit être récente : la dernière séance de l'exercice date de moins d'un mois.
private const val RECENT_DAYS = 30L

// Souvenir : la mesure principale d'un exercice (1RM estimé, reps, durée ou distance) à sa dernière séance, comparée
// à une séance d'il y a 12, 6, 3 ou 1 mois.
data class Flashback(
    val exercise: Exercise,
    val metric: ExerciseMetric,
    val months: Int,
    val then: ProgressPoint,
    val now: ProgressPoint
) {
    val change: Double get() = now.value - then.value
}

// Le souvenir le plus ancien possible, puis l'exercice fait le plus récemment. Il est affiché même sans progrès
// (décision du 28 octobre 2026), avec une formulation neutre.
fun flashback(histories: List<ExerciseHistory>, today: LocalDate, zone: ZoneId): Flashback? {
    val recentFrom = today.minusDays(RECENT_DAYS)
    return histories.mapNotNull { (exercise, sets) ->
        val metric = metricsFor(exercise.trackingType).first()
        val points = progressPoints(sets, metric, exercise.trackingType)
        val now = points.lastOrNull() ?: return@mapNotNull null
        val nowDate = now.date.atZone(zone).toLocalDate()
        if (nowDate.isBefore(recentFrom)) return@mapNotNull null
        FLASHBACK_MONTHS.firstNotNullOfOrNull { months ->
            val target = nowDate.minusMonths(months.toLong())
            points.map { it to abs(ChronoUnit.DAYS.between(target, it.date.atZone(zone).toLocalDate())) }
                .filter { (_, gap) -> gap <= WINDOW_DAYS }
                .minByOrNull { (_, gap) -> gap }
                ?.let { (then, _) -> Flashback(exercise, metric, months, then, now) }
        }
    }.sortedWith(
        compareByDescending<Flashback> { it.months }
            .thenByDescending { it.now.date }
            .thenBy { it.exercise.name }
    ).firstOrNull()
}
