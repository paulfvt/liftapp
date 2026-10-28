package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.SetType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgesTest {
    private val bench = testExercise(1, catalogKey = "Barbell_Bench_Press_-_Medium_Grip")
    private val smithBench = testExercise(2, catalogKey = "Smith_Machine_Bench_Press")
    private val deadlift = testExercise(3, catalogKey = "Sumo_Deadlift")

    // n semaines d'affilée à 3 séances (lundi, mercredi, vendredi) à partir du lundi donné.
    private fun weeks(firstMonday: String, count: Int): List<String> = (0 until count).flatMap { week ->
        val monday = LocalDate.parse(firstMonday).plusWeeks(week.toLong())
        listOf(monday, monday.plusDays(2), monday.plusDays(4)).map { it.toString() }
    }

    private fun badges(dates: List<String>, histories: List<ExerciseHistory> = emptyList()) =
        earnedBadges(dates.map(::at), emptyList(), histories, PARIS)

    @Test
    fun `session badges are dated by the nth session`() {
        val dates = (1..12).map { LocalDate.parse("2026-01-05").plusWeeks(it.toLong()).toString() }
        val earned = badges(dates)

        assertEquals(at(dates[0]), earned[Badge.WORKOUTS_1])
        assertEquals(at(dates[9]), earned[Badge.WORKOUTS_10])
        assertFalse(Badge.WORKOUTS_50 in earned)
    }

    @Test
    fun `four weeks in a row unlock the streak badge on the session that held the fourth week`() {
        val dates = weeks("2026-09-28", 4)
        val earned = badges(dates)

        assertEquals(at("2026-10-23"), earned[Badge.STREAK_4])
        assertFalse(Badge.STREAK_12 in earned)
    }

    @Test
    fun `a missed week breaks the run`() {
        // 3 semaines, une semaine à 2 séances, puis 3 semaines : jamais 4 d'affilée.
        val dates = weeks("2026-09-07", 3) + listOf("2026-09-28", "2026-09-30") + weeks("2026-10-05", 3)
        assertFalse(Badge.STREAK_4 in badges(dates))
    }

    @Test
    fun `a club needs the real load on at least one rep, warm-ups excluded`() {
        val history = ExerciseHistory(
            bench,
            listOf(
                performed("2026-10-01", 100.0, 1, SetType.WARMUP),
                performed("2026-10-05", 100.0, 0),
                performed("2026-10-08", 97.5, 3),
                performed("2026-10-12", 100.0, 1),
                performed("2026-10-19", 102.5, 1)
            )
        )
        assertEquals(at("2026-10-12"), badges(listOf("2026-10-12"), listOf(history))[Badge.CLUB_BENCH_100])
    }

    @Test
    fun `an estimated one rep max does not open a club`() {
        // 90 × 10 donne 120 de 1RM estimé, mais 90 kg seulement soulevés.
        val history = ExerciseHistory(bench, listOf(performed("2026-10-12", 90.0, 10)))
        assertFalse(Badge.CLUB_BENCH_100 in badges(listOf("2026-10-12"), listOf(history)))
    }

    @Test
    fun `only the classic barbell lifts count`() {
        val smith = ExerciseHistory(smithBench, listOf(performed("2026-10-12", 110.0, 5)))
        val sumo = ExerciseHistory(deadlift, listOf(performed("2026-10-12", 180.0, 1)))
        val earned = badges(listOf("2026-10-12"), listOf(smith, sumo))

        assertFalse(Badge.CLUB_BENCH_100 in earned)
        assertTrue(Badge.CLUB_DEADLIFT_180 in earned)
    }
}
