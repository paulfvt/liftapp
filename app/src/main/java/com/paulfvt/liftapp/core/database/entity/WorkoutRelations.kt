package com.paulfvt.liftapp.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

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
