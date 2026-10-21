package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.PersonalRecordType
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.workout.domain.SetValues
import java.time.Instant

private const val MAX_REPS_FOR_1RM = 12
private const val EPLEY_DIVISOR = 30.0

// Série validée d'une séance terminée, base de tous les records et de toutes les courbes.
// performedAt : début de la séance (une séance compte pour sa date de début).
data class PerformedSet(
    val setId: Long,
    val workoutId: Long,
    val performedAt: Instant,
    val type: SetType,
    val values: SetValues
)

// 1RM estimé (Epley), seulement de 1 à 12 reps : au-delà, la formule devient trop imprécise.
// Une série à 1 rep vaut son poids : la formule donnerait 103,3 kg pour 100 kg soulevés une fois.
fun estimatedOneRepMax(weightKg: Double?, reps: Int?): Double? {
    if (weightKg == null || weightKg <= 0 || reps == null || reps !in 1..MAX_REPS_FOR_1RM) return null
    return if (reps == 1) weightKg else weightKg * (1 + reps / EPLEY_DIVISOR)
}

// Mesures d'une série, suivies dans les records et les courbes.
enum class ExerciseMetric {
    ESTIMATED_1RM,
    MAX_WEIGHT,
    VOLUME,
    MAX_REPS,
    MAX_DURATION,
    MAX_DISTANCE
}

// Mesures proposées selon le suivi de l'exercice (les autres n'ont pas de sens pour lui).
fun metricsFor(trackingType: TrackingType): List<ExerciseMetric> = when (trackingType) {
    TrackingType.WEIGHT_REPS -> listOf(ExerciseMetric.ESTIMATED_1RM, ExerciseMetric.MAX_WEIGHT, ExerciseMetric.VOLUME)
    TrackingType.REPS -> listOf(ExerciseMetric.MAX_REPS, ExerciseMetric.MAX_WEIGHT)
    TrackingType.DURATION -> listOf(ExerciseMetric.MAX_DURATION)
    TrackingType.DISTANCE -> listOf(ExerciseMetric.MAX_DISTANCE)
}

// Valeur d'une série pour une mesure, null si elle ne compte pas. Les échauffements ne comptent jamais ; une série à
// 0 rep non plus (rien n'a été soulevé). Au poids du corps, weightKg est le lest : MAX_WEIGHT est le lest le plus
// lourd, et les reps max ne comptent que sans assistance (lest nul ou positif).
fun ExerciseMetric.valueOf(set: PerformedSet, trackingType: TrackingType): Double? {
    if (set.type == SetType.WARMUP) return null
    val weight = set.values.weightKg
    val reps = set.values.reps
    val lifted = reps != null && reps >= 1
    return when (this) {
        ExerciseMetric.ESTIMATED_1RM ->
            if (trackingType == TrackingType.WEIGHT_REPS) estimatedOneRepMax(weight, reps) else null
        ExerciseMetric.MAX_WEIGHT -> weight.takeIf { lifted && it != null && it > 0 && trackingType.isLoaded() }
        ExerciseMetric.VOLUME -> if (trackingType == TrackingType.WEIGHT_REPS &&
            reps != null &&
            reps >= 1 &&
            weight != null &&
            weight > 0
        ) {
            weight * reps
        } else {
            null
        }
        ExerciseMetric.MAX_REPS ->
            reps?.toDouble().takeIf { trackingType == TrackingType.REPS && lifted && (weight ?: 0.0) >= 0 }
        ExerciseMetric.MAX_DURATION ->
            set.values.durationSec?.toDouble().takeIf { trackingType == TrackingType.DURATION && (it ?: 0.0) > 0 }
        ExerciseMetric.MAX_DISTANCE ->
            set.values.distanceM.takeIf { trackingType == TrackingType.DISTANCE && (it ?: 0.0) > 0 }
    }
}

private fun TrackingType.isLoaded() = this == TrackingType.WEIGHT_REPS || this == TrackingType.REPS

// Record calculé. weightKg n'est renseigné que pour MAX_REPS_AT_WEIGHT (la charge de ce record de reps).
data class PersonalRecord(
    val type: PersonalRecordType,
    val value: Double,
    val weightKg: Double?,
    val setId: Long,
    val achievedAt: Instant
)

private val RECORD_METRICS = listOf(
    PersonalRecordType.MAX_WEIGHT to ExerciseMetric.MAX_WEIGHT,
    PersonalRecordType.MAX_ESTIMATED_1RM to ExerciseMetric.ESTIMATED_1RM,
    PersonalRecordType.MAX_SET_VOLUME to ExerciseMetric.VOLUME,
    PersonalRecordType.MAX_REPS to ExerciseMetric.MAX_REPS,
    PersonalRecordType.MAX_DURATION to ExerciseMetric.MAX_DURATION,
    PersonalRecordType.MAX_DISTANCE to ExerciseMetric.MAX_DISTANCE
)

// Records d'un exercice, recalculés à partir de toutes ses séries. À égalité, la première fois garde le record :
// les séries sont triées par date et une valeur ne remplace la précédente que si elle est strictement plus grande.
fun computeRecords(sets: List<PerformedSet>, trackingType: TrackingType): List<PersonalRecord> {
    val ordered = sets.sortedBy { it.performedAt }
    val records = RECORD_METRICS.mapNotNull { (type, metric) ->
        bestOf(ordered) { metric.valueOf(it, trackingType) }?.let { (set, value) ->
            PersonalRecord(type, value, null, set.setId, set.performedAt)
        }
    }
    return records + repsAtEachWeight(ordered, trackingType)
}

// « Maximum de reps à une charge donnée » : un record par charge soulevée (poids × reps uniquement).
private fun repsAtEachWeight(ordered: List<PerformedSet>, trackingType: TrackingType): List<PersonalRecord> {
    if (trackingType != TrackingType.WEIGHT_REPS) return emptyList()
    return ordered.mapNotNull { set -> ExerciseMetric.MAX_WEIGHT.valueOf(set, trackingType)?.let { it to set } }
        .groupBy({ it.first }, { it.second })
        .toSortedMap()
        .mapNotNull { (weight, sameWeight) ->
            bestOf(sameWeight) { it.values.reps?.toDouble() }?.let { (set, reps) ->
                PersonalRecord(PersonalRecordType.MAX_REPS_AT_WEIGHT, reps, weight, set.setId, set.performedAt)
            }
        }
}

private fun bestOf(ordered: List<PerformedSet>, value: (PerformedSet) -> Double?): Pair<PerformedSet, Double>? {
    var best: Pair<PerformedSet, Double>? = null
    for (set in ordered) {
        val candidate = value(set) ?: continue
        if (best == null || candidate > best.second) best = set to candidate
    }
    return best
}

// Record mis en avant dans la liste de l'onglet Progrès.
fun headlineRecordType(trackingType: TrackingType): PersonalRecordType = when (trackingType) {
    TrackingType.WEIGHT_REPS -> PersonalRecordType.MAX_ESTIMATED_1RM
    TrackingType.REPS -> PersonalRecordType.MAX_REPS
    TrackingType.DURATION -> PersonalRecordType.MAX_DURATION
    TrackingType.DISTANCE -> PersonalRecordType.MAX_DISTANCE
}

// Exercice déjà fait, tel que listé dans l'onglet Progrès.
data class PerformedExercise(
    val exercise: Exercise,
    val lastPerformedAt: Instant,
    val workoutCount: Int,
    val headline: PersonalRecord?
)
