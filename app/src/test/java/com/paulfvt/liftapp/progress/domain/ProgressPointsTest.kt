package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.history.domain.groupByMonth
import com.paulfvt.liftapp.workout.domain.SetValues
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressPointsTest {
    private val paris = ZoneId.of("Europe/Paris")

    private fun set(workoutId: Long, at: String, weight: Double, reps: Int, type: SetType = SetType.WORK) =
        PerformedSet(0, workoutId, Instant.parse(at), type, SetValues(weightKg = weight, reps = reps))

    private val sessions = listOf(
        set(1, "2026-09-01T18:00:00Z", 40.0, 10, SetType.WARMUP),
        set(1, "2026-09-01T18:00:00Z", 80.0, 8),
        set(1, "2026-09-01T18:00:00Z", 85.0, 5),
        set(2, "2026-10-01T18:00:00Z", 90.0, 3),
        set(3, "2026-10-08T18:00:00Z", 30.0, 10, SetType.WARMUP)
    )

    @Test
    fun `one point per session with its best value`() {
        val points = progressPoints(sessions, ExerciseMetric.MAX_WEIGHT, TrackingType.WEIGHT_REPS)
        assertEquals(listOf(1L to 85.0, 2L to 90.0), points.map { it.workoutId to it.value })
    }

    @Test
    fun `volume adds up the sets of a session without warm-ups`() {
        val points = progressPoints(sessions, ExerciseMetric.VOLUME, TrackingType.WEIGHT_REPS)
        assertEquals(80.0 * 8 + 85.0 * 5, points.first().value, 0.0)
    }

    @Test
    fun `a session with only warm-ups gives no point`() {
        val points = progressPoints(sessions, ExerciseMetric.ESTIMATED_1RM, TrackingType.WEIGHT_REPS)
        assertTrue(points.none { it.workoutId == 3L })
    }

    @Test
    fun `periods count calendar months back from now`() {
        val points = progressPoints(sessions, ExerciseMetric.MAX_WEIGHT, TrackingType.WEIGHT_REPS)
        val now = Instant.parse("2026-10-21T12:00:00Z")
        assertEquals(listOf(2L), points.within(ChartPeriod.MONTH, now, paris).map { it.workoutId })
        assertEquals(listOf(1L, 2L), points.within(ChartPeriod.THREE_MONTHS, now, paris).map { it.workoutId })
        assertEquals(points, points.within(ChartPeriod.ALL, now, paris))
    }

    @Test
    fun `workouts are grouped by month in the phone time zone`() {
        // 31 octobre à 23 h 30 UTC = 1er novembre à 0 h 30 à Paris : la séance est en novembre.
        val dates = listOf("2026-11-02T18:00:00Z", "2026-10-31T23:30:00Z", "2026-10-31T21:00:00Z")
            .map(Instant::parse)
        val groups = groupByMonth(dates, paris) { it }
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10)), groups.map { it.month })
        assertEquals(2, groups.first().items.size)
    }

    @Test
    fun `chart range uses round bounds around the values`() {
        assertEquals(ChartRange(90.0, 105.0, 5.0), chartRange(listOf(92.0, 101.33)))
        // Une seule valeur : une marge autour d'elle.
        assertEquals(ChartRange(95.0, 105.0, 5.0), chartRange(listOf(100.0)))
        // Jamais sous 0.
        assertEquals(0.0, chartRange(listOf(1.0, 3.0)).min, 0.0)
        assertEquals(ChartRange(500.0, 2_000.0, 500.0), chartRange(listOf(895.0, 1_805.0)))
    }
}
