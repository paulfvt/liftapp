package com.paulfvt.liftapp.core.model

// Modèle métier, sans dépendance Android. Room enregistre les énumérations par leur nom :
// ne jamais renommer une constante existante sans migration, sinon les données ne se relisent plus.

enum class Equipment {
    BARBELL,
    DUMBBELL,
    EZ_BAR,
    KETTLEBELL,
    CABLE,
    MACHINE,
    BAND,
    MEDICINE_BALL,
    EXERCISE_BALL,
    BODYWEIGHT,
    OTHER
}

// Les quatre catégories du cahier des charges, plus OTHER pour le matériel inclassable (élastiques, traîneau…).
enum class ExerciseCategory {
    FREE_WEIGHT,
    MACHINE,
    BODYWEIGHT,
    CARDIO,
    OTHER
}

// DISTANCE enregistre la distance et la durée (course, rameur…).
enum class TrackingType {
    WEIGHT_REPS,
    REPS,
    DURATION,
    DISTANCE
}

enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    ARMS,
    CORE,
    LEGS,
    NECK
}

// Les 17 muscles de free-exercise-db, rangés par groupe pour les filtres (« jambes », « dos »…).
enum class Muscle(val group: MuscleGroup) {
    ABDOMINALS(MuscleGroup.CORE),
    ABDUCTORS(MuscleGroup.LEGS),
    ADDUCTORS(MuscleGroup.LEGS),
    BICEPS(MuscleGroup.ARMS),
    CALVES(MuscleGroup.LEGS),
    CHEST(MuscleGroup.CHEST),
    FOREARMS(MuscleGroup.ARMS),
    GLUTES(MuscleGroup.LEGS),
    HAMSTRINGS(MuscleGroup.LEGS),
    LATS(MuscleGroup.BACK),
    LOWER_BACK(MuscleGroup.BACK),
    MIDDLE_BACK(MuscleGroup.BACK),
    NECK(MuscleGroup.NECK),
    QUADRICEPS(MuscleGroup.LEGS),
    SHOULDERS(MuscleGroup.SHOULDERS),
    TRAPS(MuscleGroup.BACK),
    TRICEPS(MuscleGroup.ARMS)
}

enum class MuscleRole {
    PRIMARY,
    SECONDARY
}

data class Exercise(
    val id: Long,
    // Identifiant dans free-exercise-db, null pour un exercice créé par l'utilisateur.
    val catalogKey: String?,
    val name: String,
    val equipment: Equipment,
    val category: ExerciseCategory,
    val trackingType: TrackingType,
    val primaryMuscles: List<Muscle>,
    val secondaryMuscles: List<Muscle>,
    val instructions: List<String>,
    val imageCount: Int,
    val isCustom: Boolean,
    val isHidden: Boolean,
    val note: String?
)
