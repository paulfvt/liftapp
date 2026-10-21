package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.domain.DailyValue
import com.paulfvt.liftapp.core.model.TrackingType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

// Un point de courbe : la valeur d'une séance pour une mesure.
data class ProgressPoint(val workoutId: Long, val date: Instant, val value: Double)

// Une valeur par séance : le meilleur de ses séries, sauf le volume qui est leur somme. Les séances où la mesure
// n'a aucune valeur (que des échauffements, par exemple) ne donnent pas de point.
fun progressPoints(sets: List<PerformedSet>, metric: ExerciseMetric, trackingType: TrackingType): List<ProgressPoint> =
    sets.groupBy { it.workoutId }
        .mapNotNull { (workoutId, sessionSets) ->
            val values = sessionSets.mapNotNull { metric.valueOf(it, trackingType) }
            if (values.isEmpty()) return@mapNotNull null
            val value = if (metric == ExerciseMetric.VOLUME) values.sum() else values.max()
            ProgressPoint(workoutId, sessionSets.first().performedAt, value)
        }
        .sortedBy { it.date }

// Périodes des courbes. Les mois sont comptés dans le fuseau du téléphone (« 1 mois » le 31 mars part du 28 ou
// 29 février, comme le calendrier).
enum class ChartPeriod(val months: Long?) {
    MONTH(1),
    THREE_MONTHS(3),
    YEAR(12),
    ALL(null)
}

// Deux séances le même jour donnent un seul point de courbe : la meilleure valeur.
fun List<ProgressPoint>.bestPerDay(zone: ZoneId): List<DailyValue> = groupBy { it.date.atZone(zone).toLocalDate() }
    .map { (date, sameDay) -> DailyValue(date, sameDay.maxOf { it.value }) }
    .sortedBy { it.date }

// Même découpage pour une série déjà ramenée à une valeur par jour (mesures du corps).
fun List<DailyValue>.within(period: ChartPeriod, today: LocalDate): List<DailyValue> {
    val start = period.months?.let { today.minusMonths(it) } ?: return this
    return filter { !it.date.isBefore(start) }
}

fun List<ProgressPoint>.within(period: ChartPeriod, now: Instant, zone: ZoneId): List<ProgressPoint> {
    val months = period.months ?: return this
    val start = now.atZone(zone).minusMonths(months).toInstant()
    return filter { !it.date.isBefore(start) }
}

// Échelle verticale d'une courbe : des bornes et un pas « ronds » (1, 2, 2,5 ou 5 × une puissance de 10) autour des
// valeurs, pour que la progression se voie au lieu d'être écrasée en haut d'un axe qui part de 0.
data class ChartRange(val min: Double, val max: Double, val step: Double)

private const val TARGET_STEPS = 3.0
private val NICE_STEPS = listOf(1.0, 2.0, 2.5, 5.0, 10.0)

fun chartRange(values: List<Double>): ChartRange {
    require(values.isNotEmpty()) { "Pas de valeur à afficher" }
    val low = values.min()
    val high = values.max()
    // Une seule valeur (ou toutes égales) : une marge de 10 % autour d'elle, au moins 1.
    val spread = (high - low).takeIf { it > 0 } ?: maxOf(abs(high) * 0.1, 1.0)
    val step = niceStep(spread / TARGET_STEPS)
    var min = floor(low / step) * step
    if (min >= low) min -= step
    var max = ceil(high / step) * step
    if (max <= high) max += step
    return ChartRange(min.coerceAtLeast(0.0), max, step)
}

private fun niceStep(raw: Double): Double {
    val magnitude = 10.0.pow(floor(log10(raw)))
    return NICE_STEPS.first { it * magnitude >= raw } * magnitude
}
