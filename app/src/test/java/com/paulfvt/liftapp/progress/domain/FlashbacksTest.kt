package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.TrackingType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlashbacksTest {
    private val today = LocalDate.parse("2026-10-28")
    private val squat = testExercise(1, name = "Squat")
    private val pullUp = testExercise(2, TrackingType.REPS, name = "Traction")

    @Test
    fun `the oldest flashback wins`() {
        val history = ExerciseHistory(
            squat,
            listOf(
                performed("2025-10-30", 80.0, 1),
                performed("2026-04-25", 95.0, 1),
                performed("2026-10-26", 110.0, 1)
            )
        )
        val flashback = flashback(listOf(history), today, PARIS)!!

        assertEquals(12, flashback.months)
        assertEquals(80.0, flashback.then.value, 0.0)
        assertEquals(110.0, flashback.now.value, 0.0)
        assertEquals(30.0, flashback.change, 0.0)
        assertEquals(ExerciseMetric.ESTIMATED_1RM, flashback.metric)
    }

    @Test
    fun `the past session must fall within a week of the date`() {
        // Le 26 octobre moins 6 mois : le 26 avril. Le 15 avril est trop loin, le 3 mai aussi.
        val far = ExerciseHistory(squat, listOf(performed("2026-04-15", 90.0, 1), performed("2026-10-26", 100.0, 1)))
        assertNull(flashback(listOf(far), today, PARIS))

        val near = ExerciseHistory(squat, listOf(performed("2026-05-02", 90.0, 1), performed("2026-10-26", 100.0, 1)))
        assertEquals(6, flashback(listOf(near), today, PARIS)!!.months)
    }

    @Test
    fun `the current performance must be recent`() {
        val old = ExerciseHistory(squat, listOf(performed("2025-08-01", 90.0, 1), performed("2026-08-01", 100.0, 1)))
        assertNull(flashback(listOf(old), today, PARIS))
    }

    @Test
    fun `a flashback is shown even without progress`() {
        val history =
            ExerciseHistory(squat, listOf(performed("2026-09-27", 100.0, 1), performed("2026-10-27", 95.0, 1)))
        val flashback = flashback(listOf(history), today, PARIS)!!
        assertEquals(1, flashback.months)
        assertEquals(-5.0, flashback.change, 0.0)
    }

    @Test
    fun `bodyweight exercises compare their reps`() {
        val history = ExerciseHistory(pullUp, listOf(performed("2026-07-27", 0.0, 6), performed("2026-10-27", 0.0, 10)))
        val flashback = flashback(listOf(history), today, PARIS)!!
        assertEquals(ExerciseMetric.MAX_REPS, flashback.metric)
        assertEquals(4.0, flashback.change, 0.0)
    }

    @Test
    fun `with equal periods the most recently trained exercise wins`() {
        val squats = ExerciseHistory(squat, listOf(performed("2026-07-20", 90.0, 1), performed("2026-10-20", 100.0, 1)))
        val pullUps = ExerciseHistory(pullUp, listOf(performed("2026-07-27", 0.0, 6), performed("2026-10-27", 0.0, 10)))
        assertEquals("Traction", flashback(listOf(squats, pullUps), today, PARIS)!!.exercise.name)
    }
}
