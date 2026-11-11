package com.paulfvt.liftapp.nutrition.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LabelVotesTest {
    private fun votes(vararg readings: LabelReading) = readings.fold(LabelVotes()) { votes, it -> votes.add(it) }

    @Test
    fun `a value needs two agreeing readings`() {
        assertNull(votes(LabelReading(kcal = 362.0)).result.kcal)
        assertEquals(362.0, votes(LabelReading(kcal = 362.0), LabelReading(kcal = 362.0)).result.kcal!!, 0.0)
    }

    @Test
    fun `an isolated misreading never wins`() {
        val result = votes(
            LabelReading(kcal = 362.0, protein = 13.0),
            LabelReading(kcal = 862.0, protein = 13.0),
            LabelReading(kcal = 362.0, protein = 139.0)
        ).result
        assertEquals(LabelReading(kcal = 362.0, protein = 13.0), result)
    }

    @Test
    fun `the most recent value wins a tie`() {
        val result = votes(
            LabelReading(fat = 1.8),
            LabelReading(fat = 18.0),
            LabelReading(fat = 1.8),
            LabelReading(fat = 18.0)
        ).result
        assertEquals(18.0, result.fat!!, 0.0)
    }

    @Test
    fun `empty readings do not vote and old readings leave the window`() {
        val start = votes(LabelReading(carbs = 58.0), LabelReading(carbs = 58.0), LabelReading())
        assertEquals(58.0, start.result.carbs!!, 0.0)
        // Douze lectures plus récentes sans glucides font sortir les deux premières.
        val later = (1..12).fold(start) { votes, _ -> votes.add(LabelReading(kcal = 362.0)) }
        assertNull(later.result.carbs)
        assertEquals(362.0, later.result.kcal!!, 0.0)
    }
}
