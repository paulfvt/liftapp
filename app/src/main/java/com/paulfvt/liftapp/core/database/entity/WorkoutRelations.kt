package com.paulfvt.liftapp.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation
import com.paulfvt.liftapp.core.model.SetType
import java.time.Instant

// Relations chargées en une requête par Room : une séance, ses exercices (avec leur fiche) et leurs séries.
// L'ordre des listes n'est pas garanti par Room : il est rétabli avec sortOrder lors de la conversion.
data class WorkoutExerciseDetails(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id", entity = ExerciseEntity::class)
    val exercise: ExerciseWithMuscles,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<WorkoutSetEntity>
)

data class WorkoutDetails(
    @Embedded val workout: WorkoutEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutId", entity = WorkoutExerciseEntity::class)
    val exercises: List<WorkoutExerciseDetails>
)

// Série validée d'une séance terminée, avec la séance et sa date : base des records et des courbes.
data class PerformedSetRow(
    val setId: Long,
    val workoutId: Long,
    val performedAt: Instant,
    val type: SetType,
    val weightKg: Double?,
    val reps: Int?,
    val durationSec: Int?,
    val distanceM: Double?
)

// La même série avec son exercice, pour les calculs qui portent sur plusieurs exercices à la fois.
data class ExercisePerformedSetRow(val exerciseId: Long, @Embedded val set: PerformedSetRow)

// Exercice fait au moins une fois : dernière date et nombre de séances (onglet Progrès).
data class PerformedExerciseRow(val exerciseId: Long, val lastPerformedAt: Instant, val workoutCount: Int)

// Séance terminée : son template (null pour une séance libre), son début et sa fin.
data class FinishedSessionRow(val templateId: Long?, val startedAt: Instant, val endedAt: Instant?)

// Heure de validation d'une série, gardée quand une séance passée est réécrite.
data class SetCompletionRow(val id: Long, val completedAt: Instant?)
