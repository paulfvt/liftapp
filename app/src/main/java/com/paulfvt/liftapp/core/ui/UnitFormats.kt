package com.paulfvt.liftapp.core.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.domain.LengthUnit
import com.paulfvt.liftapp.core.domain.Units
import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.core.domain.displayValue
import com.paulfvt.liftapp.core.domain.fromKg
import com.paulfvt.liftapp.core.domain.inputValue
import com.paulfvt.liftapp.core.domain.toKg
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.domain.parseDecimal

// Unités choisies dans les réglages, fournies à tous les écrans par MainActivity. Un CompositionLocal évite de les
// passer en paramètre à chaque composant qui affiche un poids ou une longueur.
val LocalUnits = staticCompositionLocalOf { Units() }

@get:StringRes
val WeightUnit.labelRes: Int
    get() = when (this) {
        WeightUnit.KG -> R.string.unit_kg
        WeightUnit.LB -> R.string.unit_lb
    }

@get:StringRes
val LengthUnit.labelRes: Int
    get() = when (this) {
        LengthUnit.CM -> R.string.unit_cm
        LengthUnit.IN -> R.string.unit_in
    }

// « 116,5 kg » ou « 257,5 lb » : valeur calculée (record, 1RM, objectif, projection), arrondie au pas de l'unité.
fun weightText(resources: Resources, kg: Double, unit: WeightUnit): String = resources.getString(
    R.string.value_with_unit,
    formatDecimal(unit.displayValue(kg)),
    resources.getString(unit.labelRes)
)

@Composable
fun weightText(kg: Double): String = weightText(LocalResources.current, kg, LocalUnits.current.weight)

// « 80 », « 81,25 » ou « 220,5 » : poids saisi ou fait, sans arrondi au pas (voir WeightUnit.inputValue).
fun weightInput(kg: Double?, unit: WeightUnit): String = kg?.let { formatDecimal(unit.inputValue(it)) }.orEmpty()

// Texte tapé dans l'unité choisie → kilos.
fun parseWeightInput(text: String, unit: WeightUnit): Double? = parseDecimal(text)?.let { unit.toKg(it) }

// Tonnage ou volume : « 9 840 kg » ou « 21 693 lb », sans décimale.
fun volumeText(resources: Resources, kg: Double, unit: WeightUnit): String =
    resources.getString(R.string.value_with_unit, wholeNumber(unit.fromKg(kg)), resources.getString(unit.labelRes))

@Composable
fun volumeText(kg: Double): String = volumeText(LocalResources.current, kg, LocalUnits.current.weight)

// « 181 cm » ou « 71,3 in ».
@Composable
fun lengthText(cm: Double): String {
    val unit = LocalUnits.current.length
    return stringResource(R.string.value_with_unit, formatDecimal(unit.displayValue(cm)), stringResource(unit.labelRes))
}
