package com.paulfvt.liftapp.core.ui

import androidx.annotation.StringRes
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.model.BodyFatMethod
import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.MuscleGroup
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType

// Libellés affichés pour les énumérations du modèle. Les textes restent dans strings.xml pour la traduction.

@get:StringRes
val Muscle.labelRes: Int
    get() = when (this) {
        Muscle.ABDOMINALS -> R.string.muscle_abdominals
        Muscle.ABDUCTORS -> R.string.muscle_abductors
        Muscle.ADDUCTORS -> R.string.muscle_adductors
        Muscle.BICEPS -> R.string.muscle_biceps
        Muscle.CALVES -> R.string.muscle_calves
        Muscle.CHEST -> R.string.muscle_chest
        Muscle.FOREARMS -> R.string.muscle_forearms
        Muscle.GLUTES -> R.string.muscle_glutes
        Muscle.HAMSTRINGS -> R.string.muscle_hamstrings
        Muscle.LATS -> R.string.muscle_lats
        Muscle.LOWER_BACK -> R.string.muscle_lower_back
        Muscle.MIDDLE_BACK -> R.string.muscle_middle_back
        Muscle.NECK -> R.string.muscle_neck
        Muscle.QUADRICEPS -> R.string.muscle_quadriceps
        Muscle.SHOULDERS -> R.string.muscle_shoulders
        Muscle.TRAPS -> R.string.muscle_traps
        Muscle.TRICEPS -> R.string.muscle_triceps
    }

@get:StringRes
val MuscleGroup.labelRes: Int
    get() = when (this) {
        MuscleGroup.CHEST -> R.string.muscle_group_chest
        MuscleGroup.BACK -> R.string.muscle_group_back
        MuscleGroup.SHOULDERS -> R.string.muscle_group_shoulders
        MuscleGroup.ARMS -> R.string.muscle_group_arms
        MuscleGroup.CORE -> R.string.muscle_group_core
        MuscleGroup.LEGS -> R.string.muscle_group_legs
        MuscleGroup.NECK -> R.string.muscle_group_neck
    }

@get:StringRes
val Equipment.labelRes: Int
    get() = when (this) {
        Equipment.BARBELL -> R.string.equipment_barbell
        Equipment.DUMBBELL -> R.string.equipment_dumbbell
        Equipment.EZ_BAR -> R.string.equipment_ez_bar
        Equipment.KETTLEBELL -> R.string.equipment_kettlebell
        Equipment.CABLE -> R.string.equipment_cable
        Equipment.MACHINE -> R.string.equipment_machine
        Equipment.BAND -> R.string.equipment_band
        Equipment.MEDICINE_BALL -> R.string.equipment_medicine_ball
        Equipment.EXERCISE_BALL -> R.string.equipment_exercise_ball
        Equipment.BODYWEIGHT -> R.string.equipment_bodyweight
        Equipment.OTHER -> R.string.equipment_other
    }

@get:StringRes
val ExerciseCategory.labelRes: Int
    get() = when (this) {
        ExerciseCategory.FREE_WEIGHT -> R.string.category_free_weight
        ExerciseCategory.MACHINE -> R.string.category_machine
        ExerciseCategory.BODYWEIGHT -> R.string.category_bodyweight
        ExerciseCategory.CARDIO -> R.string.category_cardio
        ExerciseCategory.OTHER -> R.string.category_other
    }

@get:StringRes
val TrackingType.labelRes: Int
    get() = when (this) {
        TrackingType.WEIGHT_REPS -> R.string.tracking_weight_reps
        TrackingType.REPS -> R.string.tracking_reps
        TrackingType.DURATION -> R.string.tracking_duration
        TrackingType.DISTANCE -> R.string.tracking_distance
    }

@get:StringRes
val SetType.labelRes: Int
    get() = when (this) {
        SetType.WARMUP -> R.string.set_type_warmup
        SetType.WORK -> R.string.set_type_work
        SetType.DROP -> R.string.set_type_drop
    }

@get:StringRes
val MeasurementType.labelRes: Int
    get() = when (this) {
        MeasurementType.BODY_WEIGHT -> R.string.measurement_weight
        MeasurementType.ARM -> R.string.measurement_arm
        MeasurementType.CHEST -> R.string.measurement_chest
        MeasurementType.WAIST -> R.string.measurement_waist
        MeasurementType.HIPS -> R.string.measurement_hips
        MeasurementType.THIGH -> R.string.measurement_thigh
        MeasurementType.CALF -> R.string.measurement_calf
        MeasurementType.BODY_FAT -> R.string.measurement_body_fat
    }

@get:StringRes
val BodyFatMethod.labelRes: Int
    get() = when (this) {
        BodyFatMethod.SCALE -> R.string.body_fat_method_scale
        BodyFatMethod.CALIPER -> R.string.body_fat_method_caliper
        BodyFatMethod.ESTIMATE -> R.string.body_fat_method_estimate
    }
