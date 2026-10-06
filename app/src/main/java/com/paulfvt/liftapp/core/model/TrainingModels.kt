package com.paulfvt.liftapp.core.model

// Énumérations des tables des phases suivantes (séances, records, mesures, objectifs).
// Elles sont créées dès la version 1 du schéma pour éviter des migrations pendant le développement.

enum class WorkoutStatus {
    IN_PROGRESS,
    FINISHED,
    DISCARDED
}

// Les séries d'échauffement sont exclues des records et des statistiques.
enum class SetType {
    WARMUP,
    WORK,
    DROP
}

enum class PersonalRecordType {
    MAX_WEIGHT,
    MAX_ESTIMATED_1RM,
    MAX_SET_VOLUME,
    MAX_REPS_AT_WEIGHT
}

enum class MeasurementType {
    BODY_WEIGHT,
    ARM,
    CHEST,
    WAIST,
    HIPS,
    THIGH,
    CALF,
    BODY_FAT
}

enum class BodyFatMethod {
    SCALE,
    CALIPER,
    ESTIMATE
}

enum class PhotoPose {
    FRONT,
    SIDE,
    BACK
}

enum class GoalMetric {
    ESTIMATED_1RM
}
