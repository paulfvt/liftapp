package com.paulfvt.liftapp.workout.notification

import android.content.Context
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.weightInput
import com.paulfvt.liftapp.workout.domain.NextSetNotice
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.distanceUnitFor
import com.paulfvt.liftapp.workout.domain.formatDistance
import com.paulfvt.liftapp.workout.domain.formatDuration

// Description de la prochaine série, partagée par la notification et le widget.
// « Série 3 sur 4 · 80 kg × 8 », « Échauffement · 40 kg × 12 », « Série 1 sur 3 · 1:30 ».
fun describeNextSet(context: Context, next: NextSetNotice, unit: WeightUnit): String {
    val position = describeSetPosition(context, next)
    val values = describeSetValues(context, next.values, next.exercise, unit)
    return if (values.isEmpty()) position else "$position · $values"
}

// « Série 3 sur 4 », « Échauffement », « Dégressive ».
fun describeSetPosition(context: Context, next: NextSetNotice): String = if (next.type == SetType.WORK) {
    context.getString(R.string.notification_set_position, next.position, next.count)
} else {
    context.getString(if (next.type == SetType.WARMUP) R.string.set_type_warmup else R.string.set_type_drop)
}

// « 80 kg × 8 », « +10 kg × 12 », « 1:30 » : les valeurs d'une série, vide si elles ne sont pas saisies.
fun describeSetValues(context: Context, values: SetValues, exercise: Exercise, unit: WeightUnit): String =
    when (exercise.trackingType) {
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
            val reps = values.reps
            val weight = values.weightKg
            when {
                reps == null -> ""
                weight == null -> context.resources.getQuantityString(R.plurals.workout_previous_reps, reps, reps)
                exercise.trackingType == TrackingType.REPS && weight > 0 ->
                    context.getString(R.string.weight_reps, "+" + weightWithUnit(context, weight, unit), reps)
                else -> context.getString(R.string.weight_reps, weightWithUnit(context, weight, unit), reps)
            }
        }
        TrackingType.DURATION -> formatDuration(values.durationSec)
        TrackingType.DISTANCE -> listOf(
            formatDistance(values.distanceM, distanceUnitFor(exercise.category)),
            formatDuration(values.durationSec)
        ).filter { it.isNotEmpty() }.joinToString(" · ")
    }

// « 80 kg », « 176,4 lb » : poids saisi, dans l'unité choisie.
private fun weightWithUnit(context: Context, kg: Double, unit: WeightUnit): String =
    context.getString(R.string.value_with_unit, weightInput(kg, unit), context.getString(unit.labelRes))
