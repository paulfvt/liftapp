package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.core.domain.inputValue
import com.paulfvt.liftapp.core.domain.toKg
import org.junit.Assert.assertEquals
import org.junit.Test

class SteppersTest {
    @Test
    fun `weight moves by 2,5 kg and an empty field starts from zero`() {
        assertEquals(85.0, stepWeight(82.5, 1, allowNegative = false), 0.0)
        assertEquals(80.0, stepWeight(82.5, -1, allowNegative = false), 0.0)
        assertEquals(2.5, stepWeight(null, 1, allowNegative = false), 0.0)
    }

    @Test
    fun `a load never goes below zero but a bodyweight extra load can become an assistance`() {
        assertEquals(0.0, stepWeight(1.0, -1, allowNegative = false), 0.0)
        assertEquals(0.0, stepWeight(null, -1, allowNegative = false), 0.0)
        assertEquals(-2.5, stepWeight(null, -1, allowNegative = true), 0.0)
        assertEquals(-12.5, stepWeight(-10.0, -1, allowNegative = true), 0.0)
    }

    @Test
    fun `weights keep clean decimals`() {
        var weight = 0.0
        repeat(33) { weight = stepWeight(weight, 1, allowNegative = false) }
        assertEquals(82.5, weight, 0.0)
        assertEquals(83.7, stepWeight(81.2, 1, allowNegative = false), 0.0)
    }

    @Test
    fun `reps stay between 0 and 999`() {
        assertEquals(9, stepReps(8, 1))
        assertEquals(0, stepReps(0, -1))
        assertEquals(1, stepReps(null, 1))
        assertEquals(999, stepReps(999, 1))
    }

    @Test
    fun `duration moves by 15 seconds and never goes negative`() {
        assertEquals(105, stepDuration(90, 1))
        assertEquals(0, stepDuration(10, -1))
        assertEquals(15, stepDuration(null, 1))
    }

    @Test
    fun `distance moves by 100 m in kilometers and by 5 m otherwise`() {
        assertEquals(5_100.0, stepDistance(5_000.0, 1, DistanceUnit.KILOMETER), 0.0)
        assertEquals(25.0, stepDistance(20.0, 1, DistanceUnit.METER), 0.0)
        assertEquals(0.0, stepDistance(null, -1, DistanceUnit.METER), 0.0)
    }

    @Test
    fun `in pounds the step is five pounds and lands on round values`() {
        val start = WeightUnit.LB.toKg(225.0)
        val next = stepWeight(start, 1, allowNegative = false, unit = WeightUnit.LB)
        assertEquals(230.0, WeightUnit.LB.inputValue(next), 0.0)
        assertEquals(
            WeightUnit.LB.toKg(220.0),
            stepWeight(start, -1, allowNegative = false, unit = WeightUnit.LB),
            1e-9
        )
    }
}
