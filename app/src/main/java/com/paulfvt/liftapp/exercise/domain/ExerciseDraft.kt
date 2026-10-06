package com.paulfvt.liftapp.exercise.domain

import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.TrackingType

const val EXERCISE_NAME_MAX_LENGTH = 60

// Exercice personnalisé en cours de création ou de modification, avant enregistrement.
data class ExerciseDraft(
    val name: String = "",
    val category: ExerciseCategory = ExerciseCategory.FREE_WEIGHT,
    val equipment: Equipment = Equipment.BARBELL,
    val trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    val primaryMuscles: Set<Muscle> = emptySet(),
    val secondaryMuscles: Set<Muscle> = emptySet()
)

enum class DraftError {
    NAME_EMPTY,
    NAME_TOO_LONG,
    NO_PRIMARY_MUSCLE
}

fun ExerciseDraft.validate(): Set<DraftError> = buildSet {
    val trimmedName = name.trim()
    if (trimmedName.isEmpty()) add(DraftError.NAME_EMPTY)
    if (trimmedName.length > EXERCISE_NAME_MAX_LENGTH) add(DraftError.NAME_TOO_LONG)
    if (primaryMuscles.isEmpty()) add(DraftError.NO_PRIMARY_MUSCLE)
}

// Un muscle ne peut pas être à la fois principal et secondaire : le passer en principal le retire des secondaires.
fun ExerciseDraft.togglePrimary(muscle: Muscle): ExerciseDraft = if (muscle in primaryMuscles) {
    copy(primaryMuscles = primaryMuscles - muscle)
} else {
    copy(primaryMuscles = primaryMuscles + muscle, secondaryMuscles = secondaryMuscles - muscle)
}

fun ExerciseDraft.toggleSecondary(muscle: Muscle): ExerciseDraft = if (muscle in secondaryMuscles) {
    copy(secondaryMuscles = secondaryMuscles - muscle)
} else {
    copy(secondaryMuscles = secondaryMuscles + muscle, primaryMuscles = primaryMuscles - muscle)
}
