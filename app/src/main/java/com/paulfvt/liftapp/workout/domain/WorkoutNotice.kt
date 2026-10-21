package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.SetType
import java.time.Instant

// Ce que montre la notification de séance, réduit à l'essentiel : comparer deux valeurs suffit pour savoir s'il faut
// la reconstruire. Une saisie qui ne change ni la série suivante ni le repos ne la touche donc pas.
data class WorkoutNotice(
    val workoutName: String,
    val startedAt: Instant,
    val rest: RestPeriod?,
    val next: NextSetNotice?
)

// canComplete : l'action « Série validée » n'est proposée que si la série a ses valeurs (sinon il faut ouvrir l'app).
data class NextSetNotice(
    val setId: Long,
    val exercise: Exercise,
    val type: SetType,
    val position: Int,
    val count: Int,
    val values: SetValues,
    val canComplete: Boolean
)

fun Workout.toNotice(): WorkoutNotice = WorkoutNotice(
    workoutName = name,
    startedAt = startedAt,
    rest = rest,
    next = nextSet()?.let { next ->
        NextSetNotice(
            setId = next.set.id,
            exercise = next.exercise.exercise,
            type = next.set.type,
            position = next.position,
            count = next.count,
            values = next.set.values,
            canComplete = canComplete(next.set.values, next.exercise.exercise.trackingType)
        )
    }
)
