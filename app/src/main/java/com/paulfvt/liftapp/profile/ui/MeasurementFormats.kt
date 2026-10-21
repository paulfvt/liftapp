package com.paulfvt.liftapp.profile.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.workout.domain.formatDecimal
import kotlin.math.abs
import kotlin.math.roundToLong

private const val TENTHS = 10.0

// Unité d'une mesure : kilos, centimètres ou pourcentage (cm et kg jusqu'au choix des unités en phase 10).
@get:StringRes
val MeasurementType.unitRes: Int
    get() = when (this) {
        MeasurementType.BODY_WEIGHT -> R.string.unit_kg
        MeasurementType.BODY_FAT -> R.string.unit_percent
        else -> R.string.unit_cm
    }

// Mesures affichées au dixième (« 79,4 kg », « 38 cm », « 15,2 % ») : les moyennes n'ont pas de décimales sans fin.
fun roundToTenth(value: Double): Double = (value * TENTHS).roundToLong() / TENTHS

// Ressource « %1$s kg », « %1$s cm » ou « %1$s % » selon le type.
@get:StringRes
private val MeasurementType.valueRes: Int
    get() = when (this) {
        MeasurementType.BODY_WEIGHT -> R.string.weight_kg
        MeasurementType.BODY_FAT -> R.string.value_percent
        else -> R.string.value_cm
    }

@Composable
fun measureText(type: MeasurementType, value: Double): String =
    stringResource(type.valueRes, formatDecimal(roundToTenth(value)))

// Même mise en forme, utilisable hors composition (axes et étiquettes de la courbe).
@Composable
fun rememberMeasureFormatter(type: MeasurementType): (Double) -> String {
    val resources = LocalResources.current
    return remember(type, resources) {
        { value -> resources.getString(type.valueRes, formatDecimal(roundToTenth(value))) }
    }
}

// Écart avec la mesure précédente : « +0,4 cm », « −1,5 kg » ou « stable ».
@Composable
fun changeText(type: MeasurementType, change: Double): String {
    val rounded = roundToTenth(change)
    if (rounded == 0.0) return stringResource(R.string.measurement_change_stable)
    val sign = if (rounded > 0) "+" else "−"
    return sign + measureText(type, abs(rounded))
}
