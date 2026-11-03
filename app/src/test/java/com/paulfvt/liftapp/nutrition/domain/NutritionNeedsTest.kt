package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.core.model.ActivityLevel
import com.paulfvt.liftapp.core.model.NutritionObjective
import com.paulfvt.liftapp.profile.domain.Sex
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionNeedsTest {
    private val man = BodyProfile(sex = Sex.MALE, age = 24, heightCm = 178.0, weightKg = 76.4)

    @Test
    fun `mifflin st jeor differs by sex only by its constant`() {
        // 10 × 76,4 + 6,25 × 178 − 5 × 24 = 1 756,5
        assertEquals(1_761.5, basalMetabolicRate(man), 1e-9)
        assertEquals(1_595.5, basalMetabolicRate(man.copy(sex = Sex.FEMALE)), 1e-9)
    }

    @Test
    fun `breakdown is rounded to 10 kcal and adds the objective offset`() {
        val bulk = needsBreakdown(man, ActivityLevel.ACTIVE, NutritionObjective.BULK)
        assertEquals(NeedsBreakdown(basalMetabolism = 1_760, expenditure = 2_730, offset = 300, goalKcal = 3_030), bulk)
        assertEquals(2_230, needsBreakdown(man, ActivityLevel.ACTIVE, NutritionObjective.CUT).goalKcal)
        assertEquals(2_730, needsBreakdown(man, ActivityLevel.ACTIVE, NutritionObjective.MAINTAIN).goalKcal)
        assertEquals(2_110, needsBreakdown(man, ActivityLevel.SEDENTARY, NutritionObjective.MAINTAIN).goalKcal)
    }

    @Test
    fun `protein and fat follow the weight, carbs take the rest`() {
        val target = macroTarget(kcal = 3_030, weightKg = 76.4, proteinPerKg = 2.0, fatPerKg = 1.0)
        // 153 g × 4 + 76 g × 9 = 1 296 kcal, reste 1 734 kcal = 434 g de glucides (433,5 arrondi).
        assertEquals(NutritionTarget(kcal = 3_030, proteinGrams = 153, carbsGrams = 434, fatGrams = 76), target)
        assertTrue(target.isConsistent())
    }

    @Test
    fun `carbs never go below zero when protein and fat exceed the calories`() {
        val target = macroTarget(kcal = 1_000, weightKg = 120.0, proteinPerKg = 2.5, fatPerKg = 1.0)
        assertEquals(0, target.carbsGrams)
        assertFalse(target.isConsistent())
    }

    @Test
    fun `calories typed by hand replace the calculation`() {
        val settings = GoalSettings(objective = NutritionObjective.BULK, manualKcal = 2_800)
        assertEquals(2_800, nutritionTarget(man, settings).kcal)
        assertEquals(3_030, nutritionTarget(man, settings.copy(manualKcal = null)).kcal)
    }

    @Test
    fun `a goal change applies from its day and keeps the previous one before`() {
        val target = NutritionTarget(2_730, 153, 400, 76)
        val first = NutritionGoal(LocalDate.parse("2026-10-01"), target, GoalSettings())
        // Changement un mercredi, au milieu de la semaine et du mois suivant.
        val second = NutritionGoal(LocalDate.parse("2026-11-04"), target.copy(kcal = 3_030), GoalSettings())
        val goals = listOf(first, second)
        assertNull(goalOn(LocalDate.parse("2026-09-30"), goals))
        assertEquals(first, goalOn(LocalDate.parse("2026-10-31"), goals))
        assertEquals(first, goalOn(LocalDate.parse("2026-11-03"), goals))
        assertEquals(second, goalOn(LocalDate.parse("2026-11-04"), goals))
        assertEquals(second, goalOn(LocalDate.parse("2026-12-25"), goals))
    }

    @Test
    fun `implied expenditure removes the objective offset`() {
        val goal = NutritionGoal(
            LocalDate.parse("2026-11-01"),
            NutritionTarget(3_030, 153, 434, 76),
            GoalSettings(objective = NutritionObjective.BULK)
        )
        assertEquals(2_730, goal.impliedExpenditure)
    }

    @Test
    fun `grams per kg move by exact tenths within their range`() {
        assertEquals(2.0, stepPerKg(1.9, 1, PROTEIN_PER_KG_RANGE), 0.0)
        assertEquals(1.7, stepPerKg(2.0, -3, PROTEIN_PER_KG_RANGE), 0.0)
        assertEquals(4.0, stepPerKg(4.0, 1, PROTEIN_PER_KG_RANGE), 0.0)
        assertEquals(0.3, stepPerKg(0.3, -1, FAT_PER_KG_RANGE), 0.0)
    }
}
