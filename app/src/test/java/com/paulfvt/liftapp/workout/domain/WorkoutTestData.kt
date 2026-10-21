package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType

// Données de test partagées par les tests de la séance.
fun exercise(id: Long, trackingType: TrackingType = TrackingType.WEIGHT_REPS) = Exercise(
    id = id,
    catalogKey = null,
    name = "Exercice $id",
    equipment = Equipment.BARBELL,
    category = ExerciseCategory.FREE_WEIGHT,
    trackingType = trackingType,
    primaryMuscles = listOf(Muscle.CHEST),
    secondaryMuscles = emptyList(),
    instructions = emptyList(),
    imageCount = 0,
    isCustom = true,
    isHidden = false,
    note = null
)

fun previous(type: SetType, weightKg: Double?, reps: Int?) =
    PreviousSet(type, SetValues(weightKg = weightKg, reps = reps))

fun set(
    type: SetType = SetType.WORK,
    weightKg: Double? = null,
    reps: Int? = null,
    completed: Boolean = true,
    targetReps: Int? = null,
    id: Long = 0
) = WorkoutSet(
    id = id,
    type = type,
    values = SetValues(weightKg = weightKg, reps = reps),
    targetReps = targetReps,
    isCompleted = completed
)
