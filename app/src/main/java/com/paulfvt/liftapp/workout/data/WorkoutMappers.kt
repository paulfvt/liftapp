package com.paulfvt.liftapp.workout.data

import com.paulfvt.liftapp.core.database.entity.WorkoutDetails
import com.paulfvt.liftapp.core.database.entity.WorkoutEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutSetEntity
import com.paulfvt.liftapp.exercise.data.toDomain
import com.paulfvt.liftapp.workout.domain.PlannedSet
import com.paulfvt.liftapp.workout.domain.RestPeriod
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.WorkoutExercise
import com.paulfvt.liftapp.workout.domain.WorkoutSet

// Conversions entre les lignes Room et le modèle de la séance, partagées par la séance en cours et l'historique.

internal fun WorkoutDetails.toDomain() = Workout(
    id = workout.id,
    templateId = workout.templateId,
    name = workout.name,
    startedAt = workout.startedAt,
    endedAt = workout.endedAt,
    note = workout.note,
    rest = workout.restPeriod(),
    exercises = exercises.sortedBy { it.workoutExercise.sortOrder }.map { details ->
        WorkoutExercise(
            id = details.workoutExercise.id,
            exercise = details.exercise.toDomain(),
            restSeconds = details.workoutExercise.restSeconds,
            sets = details.sets.sortedBy { it.sortOrder }.map { it.toDomain() }
        )
    }
)

internal fun WorkoutEntity.restPeriod(): RestPeriod? {
    val startedAt = restStartedAt ?: return null
    val endsAt = restEndsAt ?: return null
    return RestPeriod(startedAt, endsAt)
}

internal fun WorkoutSetEntity.toValues() = SetValues(weightKg, reps, durationSec, distanceM)

internal fun WorkoutSetEntity.toDomain() = WorkoutSet(id, type, toValues(), rpe, targetReps, isCompleted)

internal fun List<PlannedSet>.toEntities(workoutExerciseId: Long, firstOrder: Int = 0) = mapIndexed { index, planned ->
    WorkoutSetEntity(
        workoutExerciseId = workoutExerciseId,
        sortOrder = firstOrder + index,
        type = planned.type,
        weightKg = planned.values.weightKg,
        reps = planned.values.reps,
        durationSec = planned.values.durationSec,
        distanceM = planned.values.distanceM,
        rpe = null,
        isCompleted = false,
        completedAt = null,
        targetReps = planned.targetReps
    )
}
