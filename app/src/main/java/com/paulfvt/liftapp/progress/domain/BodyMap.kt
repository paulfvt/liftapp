package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import java.time.LocalDate
import java.time.ZoneId

// Historique d'un exercice : sa fiche et ses séries validées des séances terminées, dans l'ordre où elles ont été
// faites. C'est la matière de la carte du corps, des souvenirs et du récap du mois.
data class ExerciseHistory(val exercise: Exercise, val sets: List<PerformedSet>)

const val BODY_MAP_DAYS = 7L
private const val PRIMARY_SHARE = 1.0
private const val SECONDARY_SHARE = 0.5

// Série effective : hors échauffement, avec quelque chose de fait (au moins une rep, une durée ou une distance).
fun PerformedSet.isEffective(trackingType: TrackingType): Boolean {
    if (type == SetType.WARMUP) return false
    return when (trackingType) {
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> (values.reps ?: 0) >= 1
        TrackingType.DURATION -> (values.durationSec ?: 0) > 0
        TrackingType.DISTANCE -> (values.distanceM ?: 0.0) > 0 || (values.durationSec ?: 0) > 0
    }
}

// Séries effectives par muscle sur les 7 derniers jours, aujourd'hui compris, dans le fuseau du téléphone :
// 1 série pour un muscle principal, 0,5 pour un muscle secondaire. On compte des séries plutôt que du tonnage, qui
// favoriserait les exercices lourds comme la presse à cuisses. Le cardio n'est pas compté (décision du
// 28 octobre 2026) : 5 km de course ne valent pas « 1 série de quadriceps ».
fun muscleSets(histories: List<ExerciseHistory>, today: LocalDate, zone: ZoneId): Map<Muscle, Double> {
    val from = today.minusDays(BODY_MAP_DAYS - 1).atStartOfDay(zone).toInstant()
    val totals = mutableMapOf<Muscle, Double>()
    histories.filter { it.exercise.category != ExerciseCategory.CARDIO }.forEach { (exercise, sets) ->
        val count = sets.count { !it.performedAt.isBefore(from) && it.isEffective(exercise.trackingType) }
        if (count == 0) return@forEach
        exercise.primaryMuscles.distinct().forEach { totals.merge(it, count * PRIMARY_SHARE, Double::plus) }
        (exercise.secondaryMuscles.distinct() - exercise.primaryMuscles.toSet()).forEach {
            totals.merge(it, count * SECONDARY_SHARE, Double::plus)
        }
    }
    return totals
}

// Intensité de couleur d'un muscle selon ses séries de la semaine. Repères larges : une dizaine de séries par muscle
// et par semaine est un volume d'entraînement courant.
enum class HeatLevel(val minSets: Double) {
    NONE(0.0),
    LOW(0.5),
    MEDIUM(3.0),
    HIGH(6.0),
    VERY_HIGH(10.0)
}

fun heatLevel(sets: Double): HeatLevel = HeatLevel.entries.last { sets >= it.minSets }

enum class BodyView {
    FRONT,
    BACK
}

// Silhouette de la carte du corps (MuscleMap, licence MIT) : tracés SVG dans un repère propre au dessin.
// bounds : zone à afficher (gauche, haut, largeur, hauteur), le corps entier avec une petite marge.
data class BodyDiagram(
    val view: BodyView,
    val bounds: List<Float>,
    val outline: List<String>,
    val zones: List<BodyZone>
)

data class BodyZone(val muscle: Muscle, val path: String)
