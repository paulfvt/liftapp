package com.paulfvt.liftapp.template.domain

import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemplateDraftTest {
    private fun exercise(id: Long) = Exercise(
        id = id,
        catalogKey = null,
        name = "Exercice $id",
        equipment = Equipment.BARBELL,
        category = ExerciseCategory.FREE_WEIGHT,
        trackingType = TrackingType.WEIGHT_REPS,
        primaryMuscles = listOf(Muscle.CHEST),
        secondaryMuscles = emptyList(),
        instructions = emptyList(),
        imageCount = 0,
        isCustom = true,
        isHidden = false,
        note = null
    )

    private fun TemplateDraft.ids() = exercises.map { it.exercise.id }

    private val draft = TemplateDraft(name = "Push A").addExercises(listOf(exercise(1), exercise(2), exercise(3)))

    @Test
    fun `added exercises get the default settings`() {
        val added = TemplateDraft().addExercises(listOf(exercise(1))).exercises.single()
        assertEquals(DEFAULT_SET_COUNT, added.setCount)
        assertNull(added.targetReps)
        assertEquals(DEFAULT_REST_SECONDS, added.restSeconds)
    }

    @Test
    fun `exercises already in the template are not added twice`() {
        val updated = draft.addExercises(listOf(exercise(2), exercise(4), exercise(4)))
        assertEquals(listOf(1L, 2L, 3L, 4L), updated.ids())
    }

    @Test
    fun `an exercise can be removed`() {
        assertEquals(listOf(1L, 3L), draft.removeExercise(2).ids())
    }

    @Test
    fun `an exercise is moved to the position of the target`() {
        assertEquals(listOf(2L, 3L, 1L), draft.moveExercise(fromId = 1, toId = 3).ids())
        assertEquals(listOf(3L, 1L, 2L), draft.moveExercise(fromId = 3, toId = 1).ids())
    }

    @Test
    fun `moving an unknown exercise changes nothing`() {
        assertEquals(draft, draft.moveExercise(fromId = 9, toId = 1))
    }

    @Test
    fun `settings of one exercise change without touching the others`() {
        val updated = draft.updateExercise(2) { it.copy(targetReps = 10, restSeconds = 90) }
        assertEquals(10, updated.exercises[1].targetReps)
        assertEquals(90, updated.exercises[1].restSeconds)
        assertEquals(draft.exercises[0], updated.exercises[0])
    }

    @Test
    fun `set count stays between the limits`() {
        val item = draft.exercises.first()
        assertEquals(MIN_SET_COUNT, item.withSetCount(0).setCount)
        assertEquals(MAX_SET_COUNT, item.withSetCount(MAX_SET_COUNT + 1).setCount)
        assertEquals(5, item.withSetCount(5).setCount)
    }

    @Test
    fun `target reps accept up to three digits and treat empty or 0 as no target`() {
        assertNull(parseTargetReps(""))
        assertNull(parseTargetReps("0"))
        assertEquals(8, parseTargetReps("8"))
        assertEquals(12, parseTargetReps("1a2"))
        assertEquals(123, parseTargetReps("1234"))
    }

    @Test
    fun `the name is required`() {
        assertEquals(NameError.EMPTY, TemplateDraft(name = " ").nameError())
        assertNull(draft.nameError())
    }
}
