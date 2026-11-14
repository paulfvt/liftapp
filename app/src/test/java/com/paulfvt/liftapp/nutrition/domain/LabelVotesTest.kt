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
            LabelReading(fat = 7.0),
            LabelReading(fat = 7.5),
            LabelReading(fat = 7.0),
            LabelReading(fat = 7.5)
        ).result
        assertEquals(7.5, result.fat!!, 0.0)
    }

    @Test
    fun `a comma lost on some images still gives the value with its comma`() {
        // « 5,8 » lu « 58 » deux fois sur trois : sans calories pour trancher, la virgule gagne.
        val result = votes(LabelReading(fat = 58.0), LabelReading(fat = 5.8), LabelReading(fat = 58.0)).result
        assertEquals(5.8, result.fat!!, 0.0)
    }

    @Test
    fun `calories decide between a value and the same without its comma`() {
        fun reading(fat: Double) = LabelReading(kcal = 250.0, protein = 10.0, carbs = 35.0, fat = fat)
        // 5,8 g de lipides : 40 + 140 + 52 = 232 kcal, ça colle ; 58 g en ferait 702.
        assertEquals(5.8, votes(reading(58.0), reading(58.0), reading(5.8)).result.fat!!, 0.0)

        fun cereal(carbs: Double) = LabelReading(kcal = 362.0, protein = 13.0, carbs = carbs, fat = 6.5)
        // Ici, c'est 58 g de glucides qui colle : une virgule ajoutée par erreur ne gagne pas.
        assertEquals(58.0, votes(cereal(58.0), cereal(5.8), cereal(5.8)).result.carbs!!, 0.0)
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
