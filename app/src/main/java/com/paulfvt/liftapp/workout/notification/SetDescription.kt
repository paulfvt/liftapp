package com.paulfvt.liftapp.workout.notification

import android.content.Context
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.workout.domain.NextSetNotice
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.distanceUnitFor
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.domain.formatDistance
import com.paulfvt.liftapp.workout.domain.formatDuration

// Description de la prochaine série, partagée par la notification et le widget.
// « Série 3 sur 4 · 80 kg × 8 », « Échauffement · 40 kg × 12 », « Série 1 sur 3 · 1:30 ».
fun describeNextSet(context: Context, next: NextSetNotice): String {
    val position = if (next.type == SetType.WORK) {
        context.getString(R.string.notification_set_position, next.position, next.count)
    } else {
        context.getString(if (next.type == SetType.WARMUP) R.string.set_type_warmup else R.string.set_type_drop)
    }
    val values = describeValues(context, next.values, next)
    return if (values.isEmpty()) position else "$position · $values"
}

private fun describeValues(context: Context, values: SetValues, next: NextSetNotice): String =
    when (next.exercise.trackingType) {
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
            val reps = values.reps
            val weight = values.weightKg
            when {
                reps == null -> ""
                weight == null -> context.resources.getQuantityString(R.plurals.workout_previous_reps, reps, reps)
                next.exercise.trackingType == TrackingType.REPS && weight > 0 ->
                    context.getString(R.string.notification_weight_reps, "+" + formatDecimal(weight), reps)
                else -> context.getString(R.string.notification_weight_reps, formatDecimal(weight), reps)
            }
        }
        TrackingType.DURATION -> formatDuration(values.durationSec)
        TrackingType.DISTANCE -> listOf(
            formatDistance(values.distanceM, distanceUnitFor(next.exercise.category)),
            formatDuration(values.durationSec)
        ).filter { it.isNotEmpty() }.joinToString(" · ")
    }
