package com.paulfvt.liftapp.exercise.data

import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.TrackingType
import kotlinx.serialization.Serializable

// Format du fichier assets/catalog/exercises.json, produit par tools/catalog/build_catalog.py.
@Serializable
data class CatalogDto(val version: Int, val source: String, val exercises: List<CatalogExerciseDto>)

@Serializable
data class CatalogExerciseDto(
    val key: String,
    val name: String,
    val equipment: Equipment,
    val category: ExerciseCategory,
    val trackingType: TrackingType,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle>,
    val instructions: List<String>,
    val imageCount: Int
)
