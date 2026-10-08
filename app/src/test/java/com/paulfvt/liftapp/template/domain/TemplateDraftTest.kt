package com.paulfvt.liftapp.template.domain

import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.SetType
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
    fun `added exercises get three empty work sets and the default rest`() {
        val added = TemplateDraft().addExercises(listOf(exercise(1))).exercises.single()
        assertEquals(List(DEFAULT_SET_COUNT) { TemplateSetDraft(SetType.WORK, targetReps = null) }, added.sets)
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
        val updated = draft.updateExercise(2) { it.copy(restSeconds = 90) }
        assertEquals(90, updated.exercises[1].restSeconds)
        assertEquals(draft.exercises[0], updated.exercises[0])
    }

    private val item = draft.exercises.first()

    @Test
    fun `an added set is an empty work set, even after a warm-up`() {
        val withWarmup = item.updateSet(2) { it.copy(type = SetType.WARMUP, targetReps = 12) }
        assertEquals(TemplateSetDraft(SetType.WORK, targetReps = null), withWarmup.addSet().sets.last())
        assertEquals(DEFAULT_SET_COUNT + 1, withWarmup.addSet().sets.size)
    }

    @Test
    fun `sets cannot exceed the maximum`() {
        var full = item
        repeat(MAX_SET_COUNT) { full = full.addSet() }
        assertEquals(MAX_SET_COUNT, full.sets.size)
        assertEquals(full, full.addSet())
    }

    @Test
    fun `a set is removed by its position, but the last one stays`() {
        val pyramid = item.updateSet(0) { it.copy(targetReps = 12) }.updateSet(1) { it.copy(targetReps = 10) }
        assertEquals(listOf(12, null), pyramid.removeSet(1).sets.map { it.targetReps })

        var single = item
        repeat(DEFAULT_SET_COUNT) { single = single.removeSet(0) }
        assertEquals(MIN_SET_COUNT, single.sets.size)
    }

    @Test
    fun `removing a set outside the list changes nothing`() {
        assertEquals(item, item.removeSet(DEFAULT_SET_COUNT))
    }

    @Test
    fun `one set changes without touching the others`() {
        val updated = item.updateSet(1) { it.copy(type = SetType.DROP, targetReps = 8) }
        assertEquals(TemplateSetDraft(SetType.DROP, 8), updated.sets[1])
        assertEquals(item.sets[0], updated.sets[0])
        assertEquals(item.sets[2], updated.sets[2])
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
