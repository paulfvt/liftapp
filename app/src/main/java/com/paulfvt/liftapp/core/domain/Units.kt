package com.paulfvt.liftapp.core.domain

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.roundToLong

// Unités choisies dans les réglages. Tout est stocké en métrique (kg, cm) ; la conversion et l'arrondi n'ont lieu
// qu'à l'affichage et à la saisie. Le nom des constantes est enregistré dans les réglages : ne pas les renommer.
//
// displayStep : arrondi d'affichage des valeurs calculées (records, 1RM), 0,5 kg ou 1,25 lb (décision du
// 6 octobre 2026). stepperStep : pas des boutons − et + du mode salle, la plus petite paire de disques courante.
enum class WeightUnit(val kgPerUnit: Double, val displayStep: Double, val stepperStep: Double) {
    KG(1.0, 0.5, 2.5),
    LB(0.45359237, 1.25, 5.0)
}

enum class LengthUnit(val cmPerUnit: Double) {
    CM(1.0),
    IN(2.54)
}

data class Units(val weight: WeightUnit = WeightUnit.KG, val length: LengthUnit = LengthUnit.CM)

fun WeightUnit.fromKg(kg: Double): Double = kg / kgPerUnit

fun WeightUnit.toKg(value: Double): Double = value * kgPerUnit

// Valeur déjà dans l'unité, arrondie au pas d'affichage.
fun WeightUnit.roundToStep(value: Double): Double = (value / displayStep).roundToLong() * displayStep

// Valeur calculée telle qu'affichée : 116,5 kg ou 257,5 lb.
fun WeightUnit.displayValue(kg: Double): Double = roundToStep(fromKg(kg))

// Valeur d'un champ de saisie ou d'une série faite : telle quelle en kilos (81,25 reste 81,25), au dixième en livres
// pour que 225 lb tapées se relisent 225 et non 224,99.
fun WeightUnit.inputValue(kg: Double): Double = when (this) {
    WeightUnit.KG -> kg
    WeightUnit.LB -> round(fromKg(kg), 1)
}

fun LengthUnit.fromCm(cm: Double): Double = cm / cmPerUnit

fun LengthUnit.toCm(value: Double): Double = value * cmPerUnit

// Mesure ou taille affichée au dixième dans l'unité choisie.
fun LengthUnit.displayValue(cm: Double): Double = round(fromCm(cm), 1)

internal fun round(value: Double, decimals: Int): Double =
    BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).toDouble()
