package com.paulfvt.liftapp.history.domain

import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.WorkoutExercise
import com.paulfvt.liftapp.workout.domain.WorkoutSet
import com.paulfvt.liftapp.workout.domain.canComplete

// Modification d'une séance terminée, faite en mémoire jusqu'à « Enregistrer » (comme l'éditeur de template).
// Une séance terminée ne garde que des séries validées : les séries ajoutées le sont d'office.
// newId fournit des identifiants provisoires (négatifs) aux séries et exercices ajoutés, le temps de l'édition.

fun Workout.updateSet(setId: Long, change: (WorkoutSet) -> WorkoutSet): Workout = copy(
    exercises = exercises.map { item -> item.copy(sets = item.sets.map { if (it.id == setId) change(it) else it }) }
)

fun Workout.updateSetValues(setId: Long, change: (SetValues) -> SetValues): Workout =
    updateSet(setId) { it.copy(values = change(it.values)) }

// Série ajoutée : une série de travail qui reprend les valeurs de la dernière série de l'exercice.
fun Workout.addSet(workoutExerciseId: Long, newId: () -> Long): Workout = copy(
    exercises = exercises.map { item ->
        if (item.id != workoutExerciseId) return@map item
        val last = item.sets.lastOrNull()
        val added =
            WorkoutSet(id = newId(), type = SetType.WORK, values = last?.values ?: SetValues(), isCompleted = true)
        item.copy(sets = item.sets + added)
    }
)

// Un exercice garde au moins une série : pour l'enlever entièrement, on retire l'exercice.
fun Workout.removeSet(setId: Long): Workout = copy(
    exercises = exercises.map { item ->
        if (item.sets.size > 1) item.copy(sets = item.sets.filterNot { it.id == setId }) else item
    }
)

fun Workout.removeExercise(workoutExerciseId: Long): Workout =
    copy(exercises = exercises.filterNot { it.id == workoutExerciseId })

// Exercices oubliés pendant la séance : une série vide chacun, à remplir. Un exercice déjà présent n'est pas ajouté.
fun Workout.addExercises(added: List<Exercise>, newId: () -> Long): Workout {
    val present = exercises.map { it.exercise.id }.toSet()
    val newOnes = added.filterNot { it.id in present }.map { exercise ->
        WorkoutExercise(
            id = newId(),
            exercise = exercise,
            restSeconds = null,
            sets = listOf(WorkoutSet(id = newId(), type = SetType.WORK, isCompleted = true))
        )
    }
    return copy(exercises = exercises + newOnes)
}

enum class EditProblem {
    // Une séance enregistrée garde au moins un exercice ; pour tout retirer, on la supprime.
    NO_EXERCISE,

    // Chaque série doit avoir sa valeur principale (reps, durée ou distance), comme pour la valider en séance.
    MISSING_VALUES,

    BLANK_NAME
}

fun Workout.editProblem(): EditProblem? = when {
    name.isBlank() -> EditProblem.BLANK_NAME
    exercises.isEmpty() -> EditProblem.NO_EXERCISE
    exercises.any { item -> item.sets.any { !canComplete(it.values, item.exercise.trackingType) } } ->
        EditProblem.MISSING_VALUES
    else -> null
}
