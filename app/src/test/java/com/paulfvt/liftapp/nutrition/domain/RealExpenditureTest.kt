package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.core.domain.DailyValue
import com.paulfvt.liftapp.core.model.NutritionObjective
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RealExpenditureTest {
    // Mardi 3 novembre : la fenêtre va du 6 octobre au 2 novembre, à cheval sur deux mois.
    private val today = LocalDate.parse("2026-11-03")
    private val windowStart = today.minusDays(28)

    private fun days(count: Int, kcal: Double, from: LocalDate = windowStart) =
        (0 until count).map { DailyValue(from.plusDays(it.toLong()), kcal) }

    // Pesées régulières : +0,3 kg par semaine depuis 75,2 kg.
    private fun weighIns(everyDays: Int = 4) = (0..28 step everyDays).map { day ->
        DailyValue(windowStart.plusDays(day.toLong()), 75.2 + 0.3 * day / 7)
    }

    @Test
    fun `weight gain is subtracted from what was eaten`() {
        val estimate = estimateExpenditure(today, days(28, 2_980.0), weighIns()) as ExpenditureEstimate.Ready
        // 0,3 kg × 7 700 / 7 = 330 kcal d'excédent par jour.
        assertEquals(2_650, estimate.kcalPerDay)
        assertEquals(2_980, estimate.averageIntake)
        assertEquals(0.3, estimate.weeklyWeightChangeKg, 1e-9)
        assertEquals(28, estimate.loggedDays)
    }

    @Test
    fun `days without anything noted are ignored, not counted as fasting`() {
        val logged = days(22, 2_980.0) + listOf(DailyValue(windowStart.plusDays(25), 0.0))
        val estimate = estimateExpenditure(today, logged, weighIns()) as ExpenditureEstimate.Ready
        assertEquals(22, estimate.loggedDays)
        assertEquals(2_980, estimate.averageIntake)
    }

    @Test
    fun `today and days before the window do not count`() {
        val logged = days(20, 2_000.0) + DailyValue(today, 9_000.0) + DailyValue(windowStart.minusDays(1), 9_000.0)
        assertEquals(
            ExpenditureEstimate.NotEnoughData(loggedDays = 20, weighIns = 8, weighInSpanDays = 28),
            estimateExpenditure(today, logged, weighIns())
        )
    }

    @Test
    fun `needs 21 logged days and 4 weigh-ins spread over 3 weeks`() {
        val enoughDays = days(21, 2_500.0)
        assertEquals(ExpenditureEstimate.Ready::class, estimateExpenditure(today, enoughDays, weighIns())::class)
        // Trois pesées seulement.
        val three = weighIns(everyDays = 10)
        assertEquals(
            ExpenditureEstimate.NotEnoughData(21, 3, 20),
            estimateExpenditure(today, enoughDays, three)
        )
        // Quatre pesées sur 20 jours.
        val shortSpan = (0..20 step 5).map { DailyValue(windowStart.plusDays(it.toLong()), 75.0) }
        assertEquals(
            ExpenditureEstimate.NotEnoughData(21, 5, 20),
            estimateExpenditure(today, enoughDays, shortSpan)
        )
    }

    @Test
    fun `a stable weight gives the average intake`() {
        val stable = (0..28 step 7).map { DailyValue(windowStart.plusDays(it.toLong()), 80.0) }
        val estimate = estimateExpenditure(today, days(28, 2_400.0), stable) as ExpenditureEstimate.Ready
        assertEquals(2_400, estimate.kcalPerDay)
    }

    @Test
    fun `a new goal is proposed only beyond 100 kcal of difference`() {
        val goal = NutritionGoal(
            today.minusDays(40),
            NutritionTarget(3_030, 153, 434, 76),
            GoalSettings(objective = NutritionObjective.BULK)
        )
        fun ready(kcal: Int) = ExpenditureEstimate.Ready(kcal, 2_980, 0.3, 28)
        // L'objectif suppose 2 730 kcal de dépense.
        assertNull(proposedGoalKcal(ready(2_650), goal))
        assertNull(proposedGoalKcal(ready(2_830), goal))
        assertEquals(2_920, proposedGoalKcal(ready(2_620), goal))
        assertEquals(3_150, proposedGoalKcal(ready(2_850), goal))
    }
}
