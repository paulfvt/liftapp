package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.TrackingType
import java.time.Instant

// Records célébrés et comptés comme « battus » (décision du 28 octobre 2026) : les mesures des courbes, soit charge
// max, 1RM estimé et meilleure série en volume ; sans charge, reps, lest, durée et distance. Les reps à une charge
// donnée n'en font pas partie : elles tomberaient presque à chaque séance.
fun celebratedMetrics(trackingType: TrackingType): List<ExerciseMetric> = metricsFor(trackingType)

// Record battu lors d'une séance terminée : sa meilleure valeur dépasse strictement la meilleure des séances d'avant.
data class RecordBreak(
    val workoutId: Long,
    val achievedAt: Instant,
    val metric: ExerciseMetric,
    val previous: Double,
    val value: Double
)

// Rejoue l'historique d'un exercice, séance par séance, et liste chaque record battu. Le cache des records ne garde
// que le meilleur actuel ; il ne dit pas quand un ancien record a été battu, ce qu'il faut au résumé et au récap.
// La première fois qu'une mesure a une valeur, rien n'est battu (pas de célébration à la première séance).
fun recordBreaks(sets: List<PerformedSet>, trackingType: TrackingType): List<RecordBreak> {
    val best = mutableMapOf<ExerciseMetric, Double>()
    val breaks = mutableListOf<RecordBreak>()
    val sessions = sets.groupBy { it.workoutId }.values.sortedBy { it.first().performedAt }
    sessions.forEach { session ->
        celebratedMetrics(trackingType).forEach { metric ->
            val value = session.mapNotNull { metric.valueOf(it, trackingType) }.maxOrNull() ?: return@forEach
            val previous = best[metric]
            if (previous != null && value > previous) {
                breaks += RecordBreak(session.first().workoutId, session.first().performedAt, metric, previous, value)
            }
            if (previous == null || value > previous) best[metric] = value
        }
    }
    return breaks
}

// Série de la séance en cours qui bat un record d'avant la séance.
data class LiveRecord(val setId: Long, val metric: ExerciseMetric, val previous: Double, val value: Double)

// Pour chaque mesure, la première série validée qui atteint le meilleur de la séance, si ce meilleur dépasse le
// record d'avant (previousBest, tiré des séances terminées). Battre deux fois son record dans la même séance ne
// garde que la seconde série. Sans record d'avant, rien n'est célébré.
fun liveRecords(
    sessionSets: List<PerformedSet>,
    trackingType: TrackingType,
    previousBest: Map<ExerciseMetric, Double>
): List<LiveRecord> = celebratedMetrics(trackingType).mapNotNull { metric ->
    val previous = previousBest[metric] ?: return@mapNotNull null
    val candidates = sessionSets.mapNotNull { set -> metric.valueOf(set, trackingType)?.let { set to it } }
    val best = candidates.maxOfOrNull { it.second } ?: return@mapNotNull null
    if (best <= previous) return@mapNotNull null
    val set = candidates.first { it.second == best }.first
    LiveRecord(set.setId, metric, previous, best)
}
