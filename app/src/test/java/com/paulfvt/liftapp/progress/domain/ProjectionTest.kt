package com.paulfvt.liftapp.progress.domain

import java.time.LocalDate
import kotlin.math.ln
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectionTest {
    private val today = LocalDate.parse("2026-10-28")

    // Une séance par date, avec son meilleur 1RM estimé.
    private fun sessions(vararg points: Pair<String, Double>) =
        points.mapIndexed { index, (date, value) -> ProgressPoint(index.toLong(), at(date), value) }

    // Progression qui ralentit : 100 + 10 × ln(1 + jours / 30), une séance par semaine sur 8 semaines.
    private fun slowingDown(firstDay: String = "2026-09-02", weeks: Int = 8): List<ProgressPoint> {
        val start = LocalDate.parse(firstDay)
        return (0 until weeks).map { week ->
            val days = week * 7L
            ProgressPoint(week.toLong(), at(start.plusDays(days).toString()), 100 + 10 * ln(1 + days / 30.0))
        }
    }

    @Test
    fun `fewer than six sessions give no projection`() {
        val five = slowingDown().take(5)
        assertEquals(ProjectionResult.NotEnoughData, projectOneRepMax(five, today, PARIS))
    }

    @Test
    fun `six sessions within three weeks are not enough`() {
        val dates = listOf("2026-10-07", "2026-10-09", "2026-10-12", "2026-10-14", "2026-10-16", "2026-10-21")
        val points = sessions(*dates.mapIndexed { i, d -> d to 100.0 + i }.toTypedArray())
        assertEquals(ProjectionResult.NotEnoughData, projectOneRepMax(points, today, PARIS))
    }

    @Test
    fun `the last session must be less than four weeks old`() {
        val old = slowingDown(firstDay = "2026-07-01")
        assertEquals(ProjectionResult.NotEnoughData, projectOneRepMax(old, today, PARIS))
    }

    @Test
    fun `no projection without progress`() {
        val falling = slowingDown().map { it.copy(value = 300 - it.value) }
        assertEquals(ProjectionResult.NoProgress, projectOneRepMax(falling, today, PARIS))
    }

    @Test
    fun `a slowing progression is projected as a range around the curve`() {
        val result = projectOneRepMax(slowingDown(), today, PARIS) as ProjectionResult.Available
        val projection = result.projection

        // 90 jours après le 28 octobre, soit 146 jours après la première séance du 2 septembre.
        val expected = 100 + 10 * ln(1 + 146 / 30.0)
        assertEquals(LocalDate.parse("2027-01-26"), projection.horizon)
        assertTrue(projection.lowKg <= expected && expected <= projection.highKg)
        // Points exactement sur la courbe : fourchette minimale de ±1,25 kg, arrondie au demi-kilo.
        assertTrue(projection.highKg - projection.lowKg <= 3.0)
        assertEquals(0.0, projection.lowKg % 0.5, 0.0)
        assertEquals(0.0, projection.highKg % 0.5, 0.0)
    }

    @Test
    fun `the model slows down instead of extending a straight line`() {
        val points = slowingDown()
        val projection = (projectOneRepMax(points, today, PARIS) as ProjectionResult.Available).projection
        // Une droite passant par la première et la dernière séance (49 jours) donnerait bien plus à 146 jours.
        val first = points.first().value
        val last = points.last().value
        val straightLine = first + (last - first) * 146 / 49.0
        assertTrue(projection.highKg < straightLine)
    }

    @Test
    fun `very irregular sessions give no projection`() {
        val values = listOf(100.0, 130.0, 95.0, 140.0, 98.0, 150.0, 102.0, 155.0)
        val points = slowingDown().mapIndexed { i, point -> point.copy(value = values[i]) }
        assertEquals(ProjectionResult.TooIrregular, projectOneRepMax(points, today, PARIS))
    }

    @Test
    fun `the period is the start, middle or end of a month`() {
        assertEquals(MonthPart.EARLY, monthPart(LocalDate.parse("2027-01-10")))
        assertEquals(MonthPart.MID, monthPart(LocalDate.parse("2027-01-11")))
        assertEquals(MonthPart.LATE, monthPart(LocalDate.parse("2027-01-26")))
    }
}
