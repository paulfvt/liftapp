package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.SetType.DROP
import com.paulfvt.liftapp.core.model.SetType.WARMUP
import com.paulfvt.liftapp.core.model.SetType.WORK
import com.paulfvt.liftapp.template.domain.DEFAULT_SET_COUNT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreviousPerformanceTest {
    // Dernière fois : É 40×10 · 1) 80×8 · 2) 80×7
    private val lastTime = listOf(previous(WARMUP, 40.0, 10), previous(WORK, 80.0, 8), previous(WORK, 80.0, 7))

    @Test
    fun `each set takes the same rank of the same type`() {
        val matched = matchPrevious(listOf(WARMUP, WORK, WORK), lastTime)
        assertEquals(lastTime, matched)
    }

    @Test
    fun `an extra set takes the last set of its type`() {
        val matched = matchPrevious(listOf(WARMUP, WORK, WORK, WORK), lastTime)
        assertEquals(previous(WORK, 80.0, 7), matched[3])
    }

    @Test
    fun `an added warm-up does not shift the work sets`() {
        val matched = matchPrevious(listOf(WARMUP, WARMUP, WORK), lastTime)
        assertEquals(listOf(lastTime[0], lastTime[0], lastTime[1]), matched)
    }

    @Test
    fun `a type never done gives no value`() {
        assertNull(matchPrevious(listOf(DROP), lastTime).single())
    }

    @Test
    fun `first time on the exercise gives no value at all`() {
        assertEquals(listOf(null, null), matchPrevious(listOf(WORK, WORK), emptyList()))
    }

    @Test
    fun `prefill keeps the template type and target and takes last time's values`() {
        val planned = listOf(PlannedSet(WARMUP, targetReps = 12), PlannedSet(WORK, targetReps = 8))
        val filled = prefill(planned, lastTime)
        assertEquals(PlannedSet(WARMUP, 12, SetValues(weightKg = 40.0, reps = 10)), filled[0])
        assertEquals(PlannedSet(WORK, 8, SetValues(weightKg = 80.0, reps = 8)), filled[1])
    }

    @Test
    fun `an exercise added during the workout copies last time's sets`() {
        val sets = setsForAddedExercise(lastTime)
        assertEquals(listOf(WARMUP, WORK, WORK), sets.map { it.type })
        assertEquals(SetValues(weightKg = 80.0, reps = 7), sets[2].values)
    }

    @Test
    fun `an exercise never done is added with three empty work sets`() {
        assertEquals(List(DEFAULT_SET_COUNT) { PlannedSet(WORK) }, setsForAddedExercise(emptyList()))
    }

    @Test
    fun `an added set is a work set prefilled with the next work set of last time`() {
        assertEquals(PlannedSet(WORK, values = SetValues(80.0, 7)), addedSet(listOf(WARMUP, WORK), lastTime))
        assertEquals(PlannedSet(WORK), addedSet(listOf(WORK), emptyList()))
    }
}
