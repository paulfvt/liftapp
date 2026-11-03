package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.core.model.Meal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutrientsTest {
    private val chicken = Nutrients(kcal = 151.0, protein = 31.0, carbs = 0.0, fat = 3.0)

    private fun entry(id: Long, meal: Meal, grams: Double, per100g: Nutrients = chicken) = JournalEntry(
        id = id,
        date = LocalDate.parse("2026-11-03"),
        meal = meal,
        foodId = 1,
        foodName = "Poulet",
        grams = grams,
        per100g = per100g
    )

    @Test
    fun `values for 100 g are scaled to the eaten quantity`() {
        val portion = chicken.forGrams(180.0)
        assertEquals(271.8, portion.kcal, 1e-9)
        assertEquals(55.8, portion.protein, 1e-9)
        assertEquals(0.0, portion.carbs, 1e-9)
        assertEquals(5.4, portion.fat, 1e-9)
    }

    @Test
    fun `a day lists the four meals in order, even the empty ones`() {
        val meals = mealsOf(listOf(entry(1, Meal.SNACKS, 30.0), entry(2, Meal.BREAKFAST, 80.0)))
        assertEquals(listOf(Meal.BREAKFAST, Meal.LUNCH, Meal.DINNER, Meal.SNACKS), meals.map { it.meal })
        assertTrue(meals[1].entries.isEmpty())
        assertEquals(Nutrients.ZERO, meals[2].total)
    }

    @Test
    fun `a meal adds up its foods in the order they were noted`() {
        val rice = Nutrients(kcal = 117.0, protein = 2.7, carbs = 24.4, fat = 0.6)
        val lunch = mealsOf(listOf(entry(1, Meal.LUNCH, 180.0), entry(2, Meal.LUNCH, 250.0, rice)))[1]
        assertEquals(listOf(1L, 2L), lunch.entries.map { it.id })
        assertEquals(271.8 + 292.5, lunch.total.kcal, 1e-9)
        assertEquals(55.8 + 6.75, lunch.total.protein, 1e-9)
    }

    @Test
    fun `minus and plus move by 10 g without going below 0 or above 5 kg`() {
        assertEquals(190.0, stepGrams(180.0, 1), 0.0)
        assertEquals(10.0, stepGrams(null, 1), 0.0)
        assertEquals(0.0, stepGrams(5.0, -1), 0.0)
        assertEquals(5_000.0, stepGrams(4_995.0, 1), 0.0)
    }

    @Test
    fun `a saved meal adds up its items`() {
        val food = Food(1, "Poulet", chicken, isCustom = false, isFavorite = false)
        val meal = SavedMeal(1, "Midi", listOf(SavedMealItem(food, 100.0), SavedMealItem(food, 50.0)))
        assertEquals(226.5, meal.total.kcal, 1e-9)
    }

    @Test
    fun `quantity must be more than 0 g and at most 5 kg`() {
        assertFalse(isValidGrams(null))
        assertFalse(isValidGrams(0.0))
        assertFalse(isValidGrams(-10.0))
        assertTrue(isValidGrams(0.5))
        assertTrue(isValidGrams(5_000.0))
        assertFalse(isValidGrams(5_000.1))
    }
}
