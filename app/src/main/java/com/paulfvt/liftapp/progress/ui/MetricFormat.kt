package com.paulfvt.liftapp.progress.ui

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.domain.roundForDisplay
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.core.ui.wholeNumber
import com.paulfvt.liftapp.progress.domain.ExerciseMetric
import com.paulfvt.liftapp.workout.domain.DistanceUnit
import com.paulfvt.liftapp.workout.domain.distanceUnitFor
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.domain.formatDistance
import com.paulfvt.liftapp.workout.domain.formatDuration
import kotlin.math.roundToInt

// Valeur d'une mesure telle qu'affichée : « 116,5 kg » (arrondi au demi-kilo), « 9 840 kg », « 12 reps », « 1:30 »,
// « 5,2 km ». Fonction simple (pas @Composable) : elle sert aussi dans le graphique, hors composition.
fun formatMetric(resources: Resources, metric: ExerciseMetric, value: Double, exercise: Exercise): String =
    when (metric) {
        ExerciseMetric.ESTIMATED_1RM, ExerciseMetric.MAX_WEIGHT ->
            resources.getString(R.string.weight_kg, formatDecimal(roundForDisplay(value)))
        ExerciseMetric.VOLUME -> resources.getString(R.string.weight_kg, wholeNumber(value))
        ExerciseMetric.MAX_REPS -> value.roundToInt().let {
            resources.getQuantityString(R.plurals.workout_previous_reps, it, it)
        }
        ExerciseMetric.MAX_DURATION -> formatDuration(value.roundToInt())
        ExerciseMetric.MAX_DISTANCE -> {
            val unit = distanceUnitFor(exercise.category)
            val unitLabel = resources.getString(
                if (unit == DistanceUnit.KILOMETER) R.string.workout_col_km else R.string.workout_col_m
            )
            "${formatDistance(value, unit)} $unitLabel"
        }
    }

@Composable
fun rememberMetricFormatter(metric: ExerciseMetric, exercise: Exercise): (Double) -> String {
    val resources = LocalResources.current
    return remember(resources, metric, exercise) { { value -> formatMetric(resources, metric, value, exercise) } }
}

// Nom de la mesure dans les puces de la courbe. Au poids du corps, la charge est un lest.
@StringRes
fun ExerciseMetric.chartLabelRes(trackingType: TrackingType): Int = when (this) {
    ExerciseMetric.ESTIMATED_1RM -> R.string.metric_1rm
    ExerciseMetric.MAX_WEIGHT ->
        if (trackingType == TrackingType.REPS) R.string.metric_max_extra_load else R.string.metric_max_weight
    ExerciseMetric.VOLUME -> R.string.metric_volume
    ExerciseMetric.MAX_REPS -> R.string.metric_max_reps
    ExerciseMetric.MAX_DURATION -> R.string.metric_max_duration
    ExerciseMetric.MAX_DISTANCE -> R.string.metric_max_distance
}

// Nom du record correspondant (le volume record est celui d'une seule série).
@StringRes
fun ExerciseMetric.recordLabelRes(trackingType: TrackingType): Int =
    if (this == ExerciseMetric.VOLUME) R.string.record_best_set_volume else chartLabelRes(trackingType)
