package com.paulfvt.liftapp.progress.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyGoalsTest {
    private val paris = ZoneId.of("Europe/Paris")

    // Séance commencée à 18 h, heure de Paris.
    private fun at(date: String): Instant = LocalDate.parse(date).atTime(18, 0).atZone(paris).toInstant()

    private fun sessions(vararg dates: String) = dates.map(::at)

    @Test
    fun `weeks go from monday to sunday`() {
        assertEquals(LocalDate.parse("2026-10-26"), weekStart(LocalDate.parse("2026-10-26")))
        assertEquals(LocalDate.parse("2026-10-26"), weekStart(LocalDate.parse("2026-11-01")))
        assertEquals(LocalDate.parse("2026-11-02"), weekStart(LocalDate.parse("2026-11-02")))
    }

    @Test
    fun `a session counts for its start date in the phone time zone`() {
        // Dimanche 23 h 30 à Paris : c'est déjà lundi matin à Tokyo.
        val sundayEvening = LocalDate.parse("2026-11-01").atTime(23, 30).atZone(paris).toInstant()
        assertEquals(LocalDate.parse("2026-10-26"), weekOf(sundayEvening, paris))
        assertEquals(LocalDate.parse("2026-11-02"), weekOf(sundayEvening, ZoneId.of("Asia/Tokyo")))
    }

    @Test
    fun `the goal of a week is the last one set before it, three by default`() {
        val goals = listOf(
            WeeklyGoal(4, LocalDate.parse("2026-10-12")),
            WeeklyGoal(2, LocalDate.parse("2026-10-26"))
        )
        assertEquals(DEFAULT_WEEKLY_GOAL, goalForWeek(goals, LocalDate.parse("2026-10-05")))
        assertEquals(4, goalForWeek(goals, LocalDate.parse("2026-10-19")))
        assertEquals(2, goalForWeek(goals, LocalDate.parse("2026-10-26")))
        assertEquals(DEFAULT_WEEKLY_GOAL, goalForWeek(emptyList(), LocalDate.parse("2026-10-26")))
        assertEquals(5, goalForWeek(listOf(WeeklyGoal(5, GOAL_SINCE_ALWAYS)), LocalDate.parse("2020-01-06")))
    }

    @Test
    fun `the current week never breaks the streak`() {
        // Deux semaines atteintes (3 séances), semaine en cours à 1 séance sur 3 : la streak reste à 2.
        val starts = sessions("2026-10-12", "2026-10-14", "2026-10-16") +
            sessions("2026-10-19", "2026-10-21", "2026-10-23") +
            sessions("2026-10-26")
        val streak = weeklyStreak(starts, emptyList(), at("2026-10-28"), paris)

        assertEquals(2, streak.weeks)
        assertEquals(WeekProgress(LocalDate.parse("2026-10-26"), 1, 3), streak.currentWeek)
        assertFalse(streak.currentWeek.isReached)
    }

    @Test
    fun `the current week adds to the streak once its goal is reached`() {
        val starts = sessions("2026-10-19", "2026-10-21", "2026-10-23", "2026-10-26", "2026-10-27", "2026-10-28")
        val streak = weeklyStreak(starts, emptyList(), at("2026-10-28"), paris)

        assertEquals(2, streak.weeks)
        assertTrue(streak.currentWeek.isReached)
    }

    @Test
    fun `a missed week resets the streak but keeps the best one`() {
        val starts = sessions("2026-09-28", "2026-09-29", "2026-09-30") +
            sessions("2026-10-05", "2026-10-06", "2026-10-07") +
            // Semaine du 12 octobre : 2 séances sur 3.
            sessions("2026-10-12", "2026-10-13") +
            sessions("2026-10-19", "2026-10-20", "2026-10-21")
        val streak = weeklyStreak(starts, emptyList(), at("2026-10-28"), paris)

        assertEquals(1, streak.weeks)
        assertEquals(2, streak.bestWeeks)
    }

    @Test
    fun `a week spanning two months is one week`() {
        // Semaine du lundi 28 septembre au dimanche 4 octobre.
        val starts = sessions("2026-09-29", "2026-10-01", "2026-10-04")
        val streak = weeklyStreak(starts, emptyList(), at("2026-10-06"), paris)

        assertEquals(1, streak.weeks)
        assertEquals(listOf(LocalDate.parse("2026-09-28")), reachedWeeks(starts, emptyList(), paris).map { it.monday })
    }

    @Test
    fun `a goal change applies from its week and past weeks keep their goal`() {
        // 2 séances par semaine. L'objectif passe de 3 à 2 le mercredi 21 octobre (lundi 19 octobre).
        val starts = sessions("2026-10-13", "2026-10-15", "2026-10-20", "2026-10-22")
        val goals = listOf(WeeklyGoal(3, GOAL_SINCE_ALWAYS), WeeklyGoal(2, LocalDate.parse("2026-10-19")))
        val streak = weeklyStreak(starts, goals, at("2026-10-27"), paris)

        // La semaine du 12 octobre garde son objectif de 3 : seule celle du 19 compte.
        assertEquals(listOf(LocalDate.parse("2026-10-19")), reachedWeeks(starts, goals, paris).map { it.monday })
        assertEquals(1, streak.weeks)
        assertEquals(2, streak.currentWeek.goal)
    }

    @Test
    fun `a reached week is dated by the session that reached it`() {
        val starts = sessions("2026-10-19", "2026-10-21", "2026-10-23", "2026-10-25")
        assertEquals(at("2026-10-23"), reachedWeeks(starts, emptyList(), paris).single().reachedAt)
    }

    @Test
    fun `no session gives an empty week and no streak`() {
        val streak = weeklyStreak(emptyList(), emptyList(), at("2026-10-28"), paris)
        assertEquals(0, streak.weeks)
        assertEquals(0, streak.bestWeeks)
        assertEquals(0, streak.currentWeek.sessions)
    }

    @Test
    fun `runs are consecutive reached weeks`() {
        val mondays = listOf("2026-09-07", "2026-09-14", "2026-09-28", "2026-10-05", "2026-10-12")
        val runs = streakRuns(mondays.map { ReachedWeek(LocalDate.parse(it), at(it)) })
        assertEquals(listOf(2, 3), runs.map { it.size })
    }
}
