package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.template.domain.DEFAULT_REST_SECONDS
import com.paulfvt.liftapp.template.domain.TemplateDraft
import com.paulfvt.liftapp.template.domain.TemplateExerciseDraft
import com.paulfvt.liftapp.template.domain.TemplateSetDraft
import java.time.Duration
import java.time.Instant

data class WorkoutSummary(val duration: Duration, val tonnageKg: Double, val completedSets: Int, val exerciseCount: Int)

// Les échauffements sont affichés mais exclus des statistiques. Le tonnage compte la charge soulevée :
// le poids du corps n'y entre pas, seul le lest positif compte pour les exercices au poids du corps.
fun summarize(workout: Workout, endedAt: Instant): WorkoutSummary {
    val counted = workout.exercises.flatMap { exercise ->
        exercise.sets.filter { it.isCompleted && it.type != SetType.WARMUP }.map { exercise to it }
    }
    val tonnage = counted.sumOf { (exercise, set) ->
        val loaded = exercise.exercise.trackingType == TrackingType.WEIGHT_REPS ||
            exercise.exercise.trackingType == TrackingType.REPS
        if (loaded) (set.values.weightKg ?: 0.0).coerceAtLeast(0.0) * (set.values.reps ?: 0) else 0.0
    }
    return WorkoutSummary(
        duration = Duration.between(workout.startedAt, endedAt),
        tonnageKg = tonnage,
        completedSets = counted.size,
        exerciseCount = workout.exercises.count { exercise -> exercise.sets.any { it.isCompleted } }
    )
}

fun Workout.incompleteSetCount(): Int = exercises.sumOf { exercise -> exercise.sets.count { !it.isCompleted } }

fun Workout.completedSetCount(): Int = exercises.sumOf { exercise -> exercise.sets.count { it.isCompleted } }

// Structure comparée avec le template : ordre des exercices et types de leurs séries (pas les valeurs).
// On la compare avant de retirer les séries non validées : seules les modifications volontaires comptent.
fun Workout.differsFrom(template: TemplateDraft): Boolean =
    exercises.map { it.exercise.id to it.sets.map { set -> set.type } } !=
        template.exercises.map { it.exercise.id to it.sets.map { set -> set.type } }

// Nouveau contenu du template, d'après la structure de la séance : reps cibles et repos repris de la séance.
fun Workout.toTemplateDraft(templateName: String): TemplateDraft = TemplateDraft(
    name = templateName,
    exercises = exercises.map { exercise ->
        TemplateExerciseDraft(
            exercise = exercise.exercise,
            sets = exercise.sets.map { TemplateSetDraft(it.type, it.targetReps) },
            restSeconds = exercise.restSeconds ?: DEFAULT_REST_SECONDS
        )
    }
)
