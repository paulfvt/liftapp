package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.workout.domain.parseDecimal
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.roundToInt

// Contrôle d'une étiquette lue (11 novembre 2026) : les calories annoncées doivent coller avec celles des macros,
// 4 kcal par gramme de protéines et de glucides, 9 de lipides, 2 de fibres (règle européenne de l'étiquetage).

// Valeur que l'app peut désigner comme mal lue.
enum class LabelField { ENERGY, PROTEIN, CARBS, FAT }

// Calories annoncées qui ne collent pas : expectedKcal d'après les macros. suspect et suggestion quand un seul chiffre
// mal lu explique l'écart (virgule perdue, « g » lu « 9 », kJ pris pour des kcal) ; null sinon.
data class EnergyMismatch(val expectedKcal: Double, val suspect: LabelField? = null, val suggestion: Double? = null)

private const val KCAL_PER_GRAM_PROTEIN = 4.0
private const val KCAL_PER_GRAM_CARBS = 4.0
private const val KCAL_PER_GRAM_FAT = 9.0
private const val KCAL_PER_GRAM_FIBER = 2.0
private const val KJ_PER_KCAL = 4.184
private const val MAX_GRAMS_PER_100G = 100.0
private const val TEN = 10.0
private const val HUNDREDTHS = 100.0

// Arrondis de l'étiquette, polyols ou fibres non lus : un écart jusqu'à 12 % des calories (25 kcal au moins) est normal.
private const val TOLERANCE_RATIO = 0.12
private const val TOLERANCE_MIN_KCAL = 25.0

fun macrosKcal(protein: Double, carbs: Double, fat: Double, fiber: Double = 0.0): Double =
    protein * KCAL_PER_GRAM_PROTEIN + carbs * KCAL_PER_GRAM_CARBS + fat * KCAL_PER_GRAM_FAT +
        fiber * KCAL_PER_GRAM_FIBER

// Null si les calories collent. Sinon l'écart, avec la correction d'un seul chiffre qui l'explique quand elle est
// unique ; si plusieurs valeurs pourraient être en cause, l'app ne devine pas.
fun energyMismatch(kcal: Double, protein: Double, carbs: Double, fat: Double, fiber: Double = 0.0): EnergyMismatch? {
    val expected = macrosKcal(protein, carbs, fat, fiber)
    if (isConsistent(kcal, expected)) return null
    val repairs = buildList {
        misreadings(protein).forEach { add(Repair(LabelField.PROTEIN, it, kcal, macrosKcal(it, carbs, fat, fiber))) }
        misreadings(carbs).forEach { add(Repair(LabelField.CARBS, it, kcal, macrosKcal(protein, it, fat, fiber))) }
        misreadings(fat).forEach { add(Repair(LabelField.FAT, it, kcal, macrosKcal(protein, carbs, it, fiber))) }
        // kJ pris pour des kcal.
        val fromKj = (kcal / KJ_PER_KCAL).roundToInt().toDouble()
        add(Repair(LabelField.ENERGY, fromKj, fromKj, expected))
    }.filter { isConsistent(it.kcal, it.expectedKcal) }
    val field = repairs.map { it.field }.distinct().singleOrNull()
        ?: return EnergyMismatch(expected)
    val best = repairs.minBy { abs(it.kcal - it.expectedKcal) }
    return EnergyMismatch(expected, field, best.value)
}

// Contrôle d'un aliment en cours de saisie : seulement quand les quatre valeurs sont des nombres.
fun FoodDraft.energyMismatch(fiber: Double = 0.0): EnergyMismatch? {
    val kcal = parseDecimal(kcal) ?: return null
    val protein = parseDecimal(protein) ?: return null
    val carbs = parseDecimal(carbs) ?: return null
    val fat = parseDecimal(fat) ?: return null
    return energyMismatch(kcal, protein, carbs, fat, fiber)
}

private class Repair(val field: LabelField, val value: Double, val kcal: Double, val expectedKcal: Double)

private fun isConsistent(kcal: Double, expected: Double): Boolean =
    abs(kcal - expected) <= maxOf(TOLERANCE_MIN_KCAL, kcal * TOLERANCE_RATIO)

// Ce qu'un chiffre de macro serait sans les erreurs de lecture courantes : virgule perdue ou en trop, « g » final lu
// « 9 » (« 5,2g » lu « 5,29 »).
private fun misreadings(value: Double): List<Double> {
    val text = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    val withoutNine = if (text.length > 1 && text.endsWith("9")) text.dropLast(1).removeSuffix(".").toDouble() else null
    return listOfNotNull(value * TEN, value / TEN, withoutNine)
        .map { (it * HUNDREDTHS).roundToInt() / HUNDREDTHS }
        .filter { it != value && it in 0.0..MAX_GRAMS_PER_100G }
        .distinct()
}
