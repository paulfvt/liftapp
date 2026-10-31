package com.paulfvt.liftapp.profile.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.domain.Units
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.profile.domain.toDisplay
import com.paulfvt.liftapp.workout.domain.formatDecimal
import kotlin.math.abs
import kotlin.math.roundToLong

private const val TENTHS = 10.0

// Unité d'une mesure selon les réglages : kilos ou livres, centimètres ou pouces, pourcentage.
@StringRes
fun MeasurementType.unitRes(units: Units): Int = when (this) {
    MeasurementType.BODY_WEIGHT -> units.weight.labelRes
    MeasurementType.BODY_FAT -> R.string.unit_percent
    else -> units.length.labelRes
}

// Mesures affichées au dixième (« 79,4 kg », « 38 cm », « 15,2 % ») : les moyennes n'ont pas de décimales sans fin.
fun roundToTenth(value: Double): Double = (value * TENTHS).roundToLong() / TENTHS

// « 79,4 kg », « 38 cm », « 15,2 % » pour une valeur déjà dans l'unité affichée.
private fun displayText(resources: Resources, type: MeasurementType, value: Double, units: Units): String {
    val number = formatDecimal(roundToTenth(value))
    return if (type == MeasurementType.BODY_FAT) {
        resources.getString(R.string.value_percent, number)
    } else {
        resources.getString(R.string.value_with_unit, number, resources.getString(type.unitRes(units)))
    }
}

// Valeur enregistrée (kilos, centimètres) telle qu'affichée dans l'unité choisie.
@Composable
fun measureText(type: MeasurementType, value: Double): String {
    val units = LocalUnits.current
    return displayText(LocalResources.current, type, type.toDisplay(value, units), units)
}

// Pour la courbe, dont les valeurs sont déjà converties : utilisable hors composition (axes et étiquettes).
@Composable
fun rememberMeasureFormatter(type: MeasurementType): (Double) -> String {
    val resources = LocalResources.current
    val units = LocalUnits.current
    return remember(type, resources, units) { { value -> displayText(resources, type, value, units) } }
}

// Écart avec la mesure précédente : « +0,4 cm », « −1,5 kg » ou « stable ».
@Composable
fun changeText(type: MeasurementType, change: Double): String {
    val rounded = roundToTenth(change)
    if (rounded == 0.0) return stringResource(R.string.measurement_change_stable)
    val sign = if (rounded > 0) "+" else "−"
    return sign + measureText(type, abs(rounded))
}
