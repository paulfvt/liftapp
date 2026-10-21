package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.SetType
import java.time.Instant

// Valeurs saisies pour une série, en unités métriques (kg, secondes, mètres). Le sens de weightKg dépend du suivi :
// charge pour « poids × reps », lest pour « reps seules » (négatif = assistance), inutilisé sinon.
data class SetValues(
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSec: Int? = null,
    val distanceM: Double? = null
)

data class WorkoutSet(
    val id: Long,
    val type: SetType,
    val values: SetValues = SetValues(),
    val rpe: Double? = null,
    val targetReps: Int? = null,
    val isCompleted: Boolean = false
)

data class WorkoutExercise(val id: Long, val exercise: Exercise, val restSeconds: Int?, val sets: List<WorkoutSet>)

data class Workout(
    val id: Long,
    val templateId: Long?,
    val name: String,
    val startedAt: Instant,
    val note: String?,
    val exercises: List<WorkoutExercise>,
    val rest: RestPeriod? = null
)

// Série faite lors d'une séance terminée, base du pré-remplissage et de la colonne « Précédent ».
data class PreviousSet(val type: SetType, val values: SetValues)

// Série à créer : type et cible (template), valeurs pré-remplies (dernière fois).
data class PlannedSet(val type: SetType, val targetReps: Int? = null, val values: SetValues = SetValues())
