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

// MAX_WEIGHT est la charge pour « poids × reps », le lest pour un exercice au poids du corps. Les trois derniers
// types (phase 6) servent aux exercices sans charge. Stockés par leur nom : en ajouter ne demande pas de migration.
enum class PersonalRecordType {
    MAX_WEIGHT,
    MAX_ESTIMATED_1RM,
    MAX_SET_VOLUME,
    MAX_REPS_AT_WEIGHT,
    MAX_REPS,
    MAX_DURATION,
    MAX_DISTANCE
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
