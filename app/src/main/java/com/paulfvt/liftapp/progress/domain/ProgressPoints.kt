package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.TrackingType
import java.time.Instant
import java.time.ZoneId

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

fun List<ProgressPoint>.within(period: ChartPeriod, now: Instant, zone: ZoneId): List<ProgressPoint> {
    val months = period.months ?: return this
    val start = now.atZone(zone).minusMonths(months).toInstant()
    return filter { !it.date.isBefore(start) }
}
