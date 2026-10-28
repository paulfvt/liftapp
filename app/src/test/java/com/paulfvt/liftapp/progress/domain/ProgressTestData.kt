package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.workout.domain.SetValues
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Données de test partagées par les calculs de motivation (carte du corps, souvenirs, récap, badges).

val PARIS: ZoneId = ZoneId.of("Europe/Paris")

// Séance commencée à 18 h, heure de Paris.
fun at(date: String): Instant = LocalDate.parse(date).atTime(18, 0).atZone(PARIS).toInstant()

fun testExercise(
    id: Long,
    trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    primary: List<Muscle> = listOf(Muscle.CHEST),
    secondary: List<Muscle> = emptyList(),
    category: ExerciseCategory = ExerciseCategory.FREE_WEIGHT,
    catalogKey: String? = null,
    name: String = "Exercice $id"
) = Exercise(
    id = id,
    catalogKey = catalogKey,
    name = name,
    equipment = Equipment.BARBELL,
    category = category,
    trackingType = trackingType,
    primaryMuscles = primary,
    secondaryMuscles = secondary,
    instructions = emptyList(),
    imageCount = 0,
    isCustom = false,
    isHidden = false,
    note = null
)

private var nextSetId = 1L

// Série validée d'une séance : la séance est identifiée par sa date, sauf si workoutId est donné.
fun performed(
    date: String,
    weight: Double? = null,
    reps: Int? = null,
    type: SetType = SetType.WORK,
    workoutId: Long = LocalDate.parse(date).toEpochDay(),
    durationSec: Int? = null,
    distanceM: Double? = null
) = PerformedSet(nextSetId++, workoutId, at(date), type, SetValues(weight, reps, durationSec, distanceM))
