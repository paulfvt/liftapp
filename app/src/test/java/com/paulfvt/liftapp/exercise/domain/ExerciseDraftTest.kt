package com.paulfvt.liftapp.exercise.domain

import com.paulfvt.liftapp.core.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseDraftTest {
    private val valid = ExerciseDraft(name = "Hip thrust machine", primaryMuscles = setOf(Muscle.GLUTES))

    @Test
    fun `valid draft has no error`() {
        assertTrue(valid.validate().isEmpty())
    }

    @Test
    fun `blank name is rejected`() {
        assertEquals(setOf(DraftError.NAME_EMPTY), valid.copy(name = "   ").validate())
    }

    @Test
    fun `name longer than the limit is rejected, surrounding spaces excluded`() {
        val atLimit = "a".repeat(EXERCISE_NAME_MAX_LENGTH)
        assertTrue(valid.copy(name = "  $atLimit  ").validate().isEmpty())
        assertEquals(setOf(DraftError.NAME_TOO_LONG), valid.copy(name = atLimit + "a").validate())
    }

    @Test
    fun `at least one primary muscle is required`() {
        assertEquals(setOf(DraftError.NO_PRIMARY_MUSCLE), valid.copy(primaryMuscles = emptySet()).validate())
    }

    @Test
    fun `a muscle moved to primary leaves the secondary list`() {
        val draft = valid.toggleSecondary(Muscle.HAMSTRINGS).togglePrimary(Muscle.HAMSTRINGS)
        assertEquals(setOf(Muscle.GLUTES, Muscle.HAMSTRINGS), draft.primaryMuscles)
        assertTrue(draft.secondaryMuscles.isEmpty())
    }

    @Test
    fun `a muscle moved to secondary leaves the primary list`() {
        val draft = valid.toggleSecondary(Muscle.GLUTES)
        assertTrue(draft.primaryMuscles.isEmpty())
        assertEquals(setOf(Muscle.GLUTES), draft.secondaryMuscles)
    }

    @Test
    fun `toggling twice removes the muscle`() {
        assertEquals(valid, valid.togglePrimary(Muscle.CALVES).togglePrimary(Muscle.CALVES))
    }
}
