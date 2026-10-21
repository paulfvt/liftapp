package com.paulfvt.liftapp.history.domain

import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.WorkoutExercise
import com.paulfvt.liftapp.workout.domain.exercise
import com.paulfvt.liftapp.workout.domain.set
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutEditTest {
    private var nextId = -1L
    private val newId = { nextId-- }

    private val workout = Workout(
        id = 1,
        templateId = null,
        name = "Push",
        startedAt = Instant.parse("2026-10-01T18:00:00Z"),
        note = null,
        exercises = listOf(
            WorkoutExercise(
                1,
                exercise(10),
                120,
                listOf(set(SetType.WARMUP, 40.0, 10, id = 100), set(weightKg = 80.0, reps = 8, id = 101))
            ),
            WorkoutExercise(2, exercise(20), 90, listOf(set(weightKg = 20.0, reps = 12, id = 200)))
        )
    )

    @Test
    fun `an added set is a validated work set with the values of the last set`() {
        val edited = workout.addSet(1, newId)
        val added = edited.exercises.first().sets.last()
        assertEquals(SetType.WORK, added.type)
        assertEquals(SetValues(weightKg = 80.0, reps = 8), added.values)
        assertTrue(added.isCompleted)
        assertEquals(-1L, added.id)
    }

    @Test
    fun `the last set of an exercise cannot be removed, the exercise can`() {
        assertEquals(1, workout.removeSet(200).exercises[1].sets.size)
        assertEquals(1, workout.removeSet(100).exercises[0].sets.size)
        assertEquals(listOf(1L), workout.removeExercise(2).exercises.map { it.id })
    }

    @Test
    fun `values are changed on the right set only`() {
        val edited = workout.updateSetValues(101) { it.copy(reps = 10) }
        assertEquals(SetValues(weightKg = 80.0, reps = 10), edited.exercises[0].sets[1].values)
        assertEquals(SetValues(weightKg = 40.0, reps = 10), edited.exercises[0].sets[0].values)
        assertEquals(12, edited.exercises[1].sets[0].values.reps)
    }

    @Test
    fun `added exercises get one empty set and are not duplicated`() {
        val edited = workout.addExercises(listOf(exercise(20), exercise(30)), newId)
        assertEquals(listOf(10L, 20L, 30L), edited.exercises.map { it.exercise.id })
        assertEquals(SetValues(), edited.exercises.last().sets.single().values)
        assertEquals(EditProblem.MISSING_VALUES, edited.editProblem())
    }

    @Test
    fun `a workout must keep a name, an exercise and values to be saved`() {
        assertNull(workout.editProblem())
        assertEquals(EditProblem.BLANK_NAME, workout.copy(name = "  ").editProblem())
        assertEquals(EditProblem.NO_EXERCISE, workout.removeExercise(1).removeExercise(2).editProblem())
        // 0 rep est une valeur : une série ratée reste enregistrable.
        assertNull(workout.updateSetValues(200) { it.copy(reps = 0) }.editProblem())
        assertEquals(EditProblem.MISSING_VALUES, workout.updateSetValues(200) { it.copy(reps = null) }.editProblem())
    }

    @Test
    fun `a duration exercise needs its duration`() {
        val plank = WorkoutExercise(3, exercise(30, TrackingType.DURATION), null, listOf(set(id = 300)))
        assertEquals(EditProblem.MISSING_VALUES, workout.copy(exercises = listOf(plank)).editProblem())
    }
}
