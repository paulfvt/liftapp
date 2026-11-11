package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.core.domain.DailyValue
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NutritionTrendTest {
    private fun day(date: String, kcal: Double, protein: Double = 0.0) =
        DailyNutrients(LocalDate.parse(date), Nutrients(kcal, protein, 0.0, 0.0))

    private fun goal(from: String, kcal: Int, protein: Int) =
        NutritionGoal(LocalDate.parse(from), NutritionTarget(kcal, protein, 0, 0), GoalSettings())

    private val today = LocalDate.parse("2026-11-02")

    @Test
    fun `the week spans two months and ends today`() {
        val week = calorieWeek(today, emptyList(), emptyList())
        assertEquals(LocalDate.parse("2026-10-27"), week.days.first().date)
        assertEquals(today, week.days.last().date)
        assertEquals(listOf(false, false, false, false, false, false, true), week.days.map { it.isToday })
        assertNull(week.averageKcal)
        assertNull(week.goalKcal)
    }

    @Test
    fun `the average ignores today and days left empty`() {
        val week = calorieWeek(
            today,
            listOf(day("2026-10-30", 2000.0), day("2026-10-31", 3000.0), day("2026-11-02", 500.0)),
            emptyList()
        )
        assertEquals(2500, week.averageKcal)
        assertEquals(2, week.loggedDays)
        // Le jour en cours est dessiné avec ce qui est déjà noté.
        assertEquals(500.0, week.days.last().kcal, 0.0)
    }

    @Test
    fun `protein follows the goal of each day after a goal change`() {
        val goals = listOf(goal("2026-10-01", 3000, 150), goal("2026-10-31", 2800, 100))
        val week = calorieWeek(
            today,
            listOf(day("2026-10-30", 2900.0, protein = 120.0), day("2026-10-31", 2700.0, protein = 120.0)),
            goals
        )
        // 120 g ne tient pas l'objectif de 150 g du 30, mais tient celui de 100 g du 31.
        assertEquals(1, week.proteinReachedDays)
        assertEquals(2, week.loggedDays)
        assertEquals(2800, week.goalKcal)
    }

    @Test
    fun `the weight trend is the slope over the last four weeks`() {
        // +0,3 kg par semaine, plus une pesée trop ancienne qui ne compte pas.
        val weights = listOf(
            DailyValue(LocalDate.parse("2026-09-01"), 90.0),
            DailyValue(LocalDate.parse("2026-10-06"), 75.0),
            DailyValue(LocalDate.parse("2026-10-13"), 75.3),
            DailyValue(LocalDate.parse("2026-10-20"), 75.6),
            DailyValue(LocalDate.parse("2026-10-27"), 75.9)
        )
        val trend = weightTrend(today, weights)!!
        assertEquals(75.9, trend.latestKg, 0.0)
        assertEquals(0.3, trend.weeklyChangeKg, 1e-9)
        assertEquals(0.9, trend.changeKg, 1e-9)
        assertEquals(4, trend.days.size)
    }

    @Test
    fun `one weigh-in gives no trend`() {
        assertNull(weightTrend(today, listOf(DailyValue(today, 75.0))))
    }
}
