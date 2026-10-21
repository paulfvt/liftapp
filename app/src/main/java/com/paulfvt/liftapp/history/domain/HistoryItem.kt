package com.paulfvt.liftapp.history.domain

import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.summarize
import java.time.Duration
import java.time.Instant

// Ligne de l'historique : ce qu'on voit d'une séance sans l'ouvrir.
data class HistoryItem(
    val id: Long,
    val name: String,
    val startedAt: Instant,
    val duration: Duration,
    val tonnageKg: Double,
    val setCount: Int,
    val exerciseNames: List<String>
)

// Mêmes chiffres que le résumé de fin de séance (échauffements exclus). Une séance terminée a toujours une fin ;
// à défaut, sa durée est nulle plutôt que fausse.
fun Workout.toHistoryItem(): HistoryItem {
    val summary = summarize(this, endedAt ?: startedAt)
    return HistoryItem(
        id = id,
        name = name,
        startedAt = startedAt,
        duration = summary.duration,
        tonnageKg = summary.tonnageKg,
        setCount = summary.completedSets,
        exerciseNames = exercises.map { it.exercise.name }
    )
}
