package com.paulfvt.liftapp.workout.domain

import java.math.BigDecimal
import java.math.RoundingMode

// Pas des boutons − et + du mode salle. 2,5 kg : la plus petite paire de disques courante (2 × 1,25 kg).
const val WEIGHT_STEP_KG = 2.5
const val REPS_STEP = 1
const val DURATION_STEP_SECONDS = 15
const val KILOMETER_STEP_METERS = 100.0
const val METER_STEP_METERS = 5.0
private const val MAX_REPS = 999
private const val DECIMALS = 2

// Poids après un appui. Un champ vide part de 0. Le lest d'un exercice au poids du corps peut devenir négatif
// (assistance) ; une charge, jamais. Arrondi à 2 décimales pour éviter les 82,49999 des nombres à virgule.
fun stepWeight(current: Double?, steps: Int, allowNegative: Boolean): Double {
    val next = round((current ?: 0.0) + steps * WEIGHT_STEP_KG)
    return if (allowNegative) next else next.coerceAtLeast(0.0)
}

fun stepReps(current: Int?, steps: Int): Int = ((current ?: 0) + steps * REPS_STEP).coerceIn(0, MAX_REPS)

fun stepDuration(current: Int?, steps: Int): Int = ((current ?: 0) + steps * DURATION_STEP_SECONDS).coerceAtLeast(0)

// 100 m pour le cardio compté en kilomètres, 5 m pour le reste (traîneau, marche du fermier).
fun distanceStepMeters(unit: DistanceUnit): Double =
    if (unit == DistanceUnit.KILOMETER) KILOMETER_STEP_METERS else METER_STEP_METERS

fun stepDistance(current: Double?, steps: Int, unit: DistanceUnit): Double =
    round((current ?: 0.0) + steps * distanceStepMeters(unit)).coerceAtLeast(0.0)

private fun round(value: Double): Double = BigDecimal.valueOf(value).setScale(DECIMALS, RoundingMode.HALF_UP).toDouble()
