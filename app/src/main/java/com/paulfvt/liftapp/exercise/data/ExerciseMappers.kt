package com.paulfvt.liftapp.exercise.data

import com.paulfvt.liftapp.core.database.entity.ExerciseEntity
import com.paulfvt.liftapp.core.database.entity.ExerciseMuscleEntity
import com.paulfvt.liftapp.core.database.entity.ExerciseWithMuscles
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.MuscleRole
import com.paulfvt.liftapp.exercise.domain.ExerciseDraft

// Conversions entre les tables Room (entity) et le modèle métier utilisé par l'interface.

fun ExerciseWithMuscles.toDomain(): Exercise = Exercise(
    id = exercise.id,
    catalogKey = exercise.catalogKey,
    name = exercise.name,
    equipment = exercise.equipment,
    category = exercise.category,
    trackingType = exercise.trackingType,
    primaryMuscles = musclesWithRole(MuscleRole.PRIMARY),
    secondaryMuscles = musclesWithRole(MuscleRole.SECONDARY),
    instructions = exercise.instructions?.split('\n').orEmpty(),
    imageCount = exercise.imageCount,
    isCustom = exercise.isCustom,
    isHidden = exercise.isHidden,
    note = exercise.note
)

private fun ExerciseWithMuscles.musclesWithRole(role: MuscleRole): List<Muscle> =
    muscles.filter { it.role == role }.map { it.muscle }.sorted()

// Lors d'une mise à jour du catalogue, on garde l'id, la note et le masquage choisis par l'utilisateur.
fun CatalogExerciseDto.toEntity(existing: ExerciseEntity?): ExerciseEntity = ExerciseEntity(
    id = existing?.id ?: 0,
    catalogKey = key,
    name = name,
    equipment = equipment,
    category = category,
    trackingType = trackingType,
    instructions = instructions.joinToString("\n"),
    imageCount = imageCount,
    isCustom = false,
    isHidden = existing?.isHidden ?: false,
    note = existing?.note
)

fun CatalogExerciseDto.toMuscleEntities(exerciseId: Long): List<ExerciseMuscleEntity> =
    muscleEntities(exerciseId, primaryMuscles.toSet(), secondaryMuscles.toSet())

fun ExerciseDraft.toEntity(existing: ExerciseEntity?): ExerciseEntity = ExerciseEntity(
    id = existing?.id ?: 0,
    catalogKey = null,
    name = name.trim(),
    equipment = equipment,
    category = category,
    trackingType = trackingType,
    instructions = null,
    imageCount = 0,
    isCustom = true,
    isHidden = existing?.isHidden ?: false,
    note = existing?.note
)

fun ExerciseDraft.toMuscleEntities(exerciseId: Long): List<ExerciseMuscleEntity> =
    muscleEntities(exerciseId, primaryMuscles, secondaryMuscles)

// Un muscle à la fois principal et secondaire ne garde que le rôle principal (clé primaire exerciseId + muscle).
private fun muscleEntities(exerciseId: Long, primary: Set<Muscle>, secondary: Set<Muscle>) =
    primary.map { ExerciseMuscleEntity(exerciseId, it, MuscleRole.PRIMARY) } +
        (secondary - primary).map { ExerciseMuscleEntity(exerciseId, it, MuscleRole.SECONDARY) }

fun Exercise.toDraft(): ExerciseDraft = ExerciseDraft(
    name = name,
    category = category,
    equipment = equipment,
    trackingType = trackingType,
    primaryMuscles = primaryMuscles.toSet(),
    secondaryMuscles = secondaryMuscles.toSet()
)
