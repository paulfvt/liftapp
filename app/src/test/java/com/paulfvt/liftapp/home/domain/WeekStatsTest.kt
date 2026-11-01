package com.paulfvt.liftapp.home.domain

import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.progress.domain.ExerciseHistory
import com.paulfvt.liftapp.progress.domain.PerformedSet
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.exercise
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeekStatsTest {
    private val paris = ZoneId.of("Europe/Paris")

    // Semaine du lundi 26 octobre au dimanche 1er novembre 2026, à cheval sur deux mois. La semaine précédente
    // contient le passage à l'heure d'hiver (dimanche 25 octobre).
    private val monday = LocalDate.parse("2026-10-26")

    private fun session(start: String, minutes: Long) =
        FinishedSession(null, Instant.parse(start), Instant.parse(start).plusSeconds(minutes * 60))

    private fun set(workoutId: Long, start: String, weightKg: Double, reps: Int, type: SetType = SetType.WORK) =
        PerformedSet(workoutId, workoutId, Instant.parse(start), type, SetValues(weightKg = weightKg, reps = reps))

    private val squat = exercise(1)

    @Test
    fun `the current week counts its sessions, their duration and their tonnage without warm-ups`() {
        val sessions = listOf(session("2026-10-27T17:00:00Z", 60), session("2026-10-31T23:30:00Z", 64))
        val histories = listOf(
            ExerciseHistory(
                squat,
                listOf(
                    set(1, "2026-10-27T17:00:00Z", 100.0, 5),
                    set(1, "2026-10-27T17:00:00Z", 60.0, 10, SetType.WARMUP),
                    // Samedi 31 à 23 h 30 UTC : déjà dimanche 1er novembre à Paris, même semaine.
                    set(2, "2026-10-31T23:30:00Z", 80.0, 10, SetType.DROP)
                )
            )
        )

        val stats = weekStats(sessions, histories, monday, now = Instant.parse("2026-11-01T10:00:00Z"), zone = paris)

        assertEquals(WeekTotals(1_300.0, Duration.ofMinutes(124), 2), stats.current)
        assertEquals(43, stats.previousWeekNumber)
    }

    @Test
    fun `the change compares with the previous week up to the same day and hour`() {
        val sessions = listOf(
            session("2026-10-20T17:00:00Z", 60),
            // Jeudi de la semaine précédente, après le même moment : pas comptée.
            session("2026-10-22T17:00:00Z", 60),
            session("2026-10-27T17:00:00Z", 60)
        )
        val histories = listOf(
            ExerciseHistory(
                squat,
                listOf(
                    set(1, "2026-10-20T17:00:00Z", 100.0, 10),
                    set(2, "2026-10-22T17:00:00Z", 100.0, 10),
                    set(3, "2026-10-27T17:00:00Z", 106.0, 10)
                )
            )
        )

        // Mercredi 28 octobre à midi : comparé au mercredi 21 octobre à midi.
        val stats = weekStats(sessions, histories, monday, now = Instant.parse("2026-10-28T11:00:00Z"), zone = paris)

        assertEquals(0.06, stats.tonnageChange!!, 1e-9)
    }

    @Test
    fun `an empty week and a week without previous tonnage show no change`() {
        val emptyNow = weekStats(emptyList(), emptyList(), monday, Instant.parse("2026-10-26T08:00:00Z"), paris)
        assertEquals(WeekTotals(0.0, Duration.ZERO, 0), emptyNow.current)
        assertNull(emptyNow.tonnageChange)

        val firstWeek = weekStats(
            listOf(session("2026-10-27T17:00:00Z", 45)),
            listOf(ExerciseHistory(squat, listOf(set(1, "2026-10-27T17:00:00Z", 100.0, 5)))),
            monday,
            Instant.parse("2026-10-28T11:00:00Z"),
            paris
        )
        assertNull(firstWeek.tonnageChange)
    }

    @Test
    fun `body weight and cardio exercises add duration but no tonnage`() {
        val pullUp = exercise(2, TrackingType.REPS)
        val run = exercise(3, TrackingType.DISTANCE)
        val histories = listOf(
            // Traction avec 10 kg d'assistance : le poids du corps n'entre pas dans le tonnage.
            ExerciseHistory(pullUp, listOf(set(1, "2026-10-27T17:00:00Z", -10.0, 8))),
            ExerciseHistory(run, listOf(set(1, "2026-10-27T17:00:00Z", 0.0, 0)))
        )

        val stats = weekStats(
            listOf(session("2026-10-27T17:00:00Z", 30)),
            histories,
            monday,
            Instant.parse("2026-10-28T11:00:00Z"),
            paris
        )

        assertEquals(WeekTotals(0.0, Duration.ofMinutes(30), 1), stats.current)
    }
}
