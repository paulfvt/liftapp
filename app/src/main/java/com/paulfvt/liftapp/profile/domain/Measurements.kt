package com.paulfvt.liftapp.profile.domain

import com.paulfvt.liftapp.core.model.BodyFatMethod
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.workout.domain.parseDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// Une mesure datée. Valeur en kg pour le poids, en cm pour les tours, en % pour la masse grasse (cahier § 5).
data class BodyMeasurement(
    val id: Long,
    val type: MeasurementType,
    val value: Double,
    val method: BodyFatMethod?,
    val measuredAt: Instant
)

// Un point de courbe par jour : la moyenne des saisies de ce jour (pesée du matin et du soir à parts égales).
data class DailyMeasure(val date: LocalDate, val value: Double)

const val MOVING_AVERAGE_DAYS = 7

fun dailyValues(measurements: List<BodyMeasurement>, zone: ZoneId): List<DailyMeasure> =
    measurements.groupBy { it.measuredAt.atZone(zone).toLocalDate() }
        .map { (date, sameDay) -> DailyMeasure(date, sameDay.map { it.value }.average()) }
        .sortedBy { it.date }

// Moyenne mobile : pour chaque jour mesuré, la moyenne des valeurs des 7 derniers jours, ce jour compris. Les jours
// sans pesée sont simplement absents (pas de valeur inventée), et la fenêtre suit le calendrier, mois compris.
fun movingAverage(daily: List<DailyMeasure>, days: Int = MOVING_AVERAGE_DAYS): List<DailyMeasure> {
    val sorted = daily.sortedBy { it.date }
    return sorted.map { day ->
        val windowStart = day.date.minusDays(days - 1L)
        val window = sorted.filter { !it.date.isBefore(windowStart) && !it.date.isAfter(day.date) }
        DailyMeasure(day.date, window.map { it.value }.average())
    }
}

// Dernière mesure d'un type et écart avec la précédente (null pour la toute première).
data class MeasurementSummary(val latest: BodyMeasurement, val change: Double?)

// Une ligne par type déjà mesuré, dans l'ordre du cahier (poids, tours, masse grasse).
fun summaries(measurements: List<BodyMeasurement>): List<MeasurementSummary> =
    MeasurementType.entries.mapNotNull { type ->
        val ofType = measurements.filter { it.type == type }.sortedByDescending { it.measuredAt }
        val latest = ofType.firstOrNull() ?: return@mapNotNull null
        MeasurementSummary(latest, ofType.getOrNull(1)?.let { latest.value - it.value })
    }

// Bornes larges : elles n'écartent que les fautes de frappe (« 805 » au lieu de « 80,5 »).
fun MeasurementType.validRange(): ClosedFloatingPointRange<Double> = when (this) {
    MeasurementType.BODY_WEIGHT -> 20.0..400.0
    MeasurementType.BODY_FAT -> 2.0..70.0
    MeasurementType.ARM,
    MeasurementType.CHEST,
    MeasurementType.WAIST,
    MeasurementType.HIPS,
    MeasurementType.THIGH,
    MeasurementType.CALF -> 10.0..250.0
}

// Saisie d'une nouvelle mesure. La méthode ne concerne que la masse grasse.
data class MeasurementDraft(
    val type: MeasurementType,
    val value: String = "",
    val method: BodyFatMethod? = null,
    val date: LocalDate
)

enum class MeasurementError {
    VALUE_MISSING,
    VALUE_OUT_OF_RANGE,
    METHOD_MISSING,
    DATE_IN_FUTURE
}

fun MeasurementDraft.errors(today: LocalDate): Set<MeasurementError> = buildSet {
    val number = parseDecimal(value)
    when {
        number == null -> add(MeasurementError.VALUE_MISSING)
        number !in type.validRange() -> add(MeasurementError.VALUE_OUT_OF_RANGE)
    }
    if (type == MeasurementType.BODY_FAT && method == null) add(MeasurementError.METHOD_MISSING)
    if (date.isAfter(today)) add(MeasurementError.DATE_IN_FUTURE)
}

// Heure enregistrée : maintenant pour une mesure du jour, midi pour un jour passé (l'heure exacte est inconnue, midi
// garde la mesure sur le bon jour quel que soit le fuseau du téléphone ensuite).
fun measuredAtFor(date: LocalDate, now: Instant, zone: ZoneId): Instant =
    if (date == now.atZone(zone).toLocalDate()) now else date.atTime(LocalTime.NOON).atZone(zone).toInstant()

// À n'appeler que sur un brouillon sans erreur.
fun MeasurementDraft.toMeasurement(now: Instant, zone: ZoneId) = BodyMeasurement(
    id = 0,
    type = type,
    value = parseDecimal(value)!!,
    method = method.takeIf { type == MeasurementType.BODY_FAT },
    measuredAt = measuredAtFor(date, now, zone)
)
