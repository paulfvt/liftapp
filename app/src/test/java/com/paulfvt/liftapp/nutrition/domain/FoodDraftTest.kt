package com.paulfvt.liftapp.nutrition.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodDraftTest {
    private val skyr = FoodDraft(name = "Skyr nature", kcal = "62", protein = "10,5", carbs = "4", fat = "0,2")

    @Test
    fun `a complete draft with french decimals is valid`() {
        assertTrue(skyr.errors().isEmpty())
        assertEquals(Nutrients(62.0, 10.5, 4.0, 0.2), skyr.toNutrients())
    }

    @Test
    fun `name and every value are required`() {
        assertEquals(
            setOf(
                FoodDraftError.NAME_MISSING,
                FoodDraftError.KCAL_INVALID,
                FoodDraftError.PROTEIN_INVALID,
                FoodDraftError.CARBS_INVALID,
                FoodDraftError.FAT_INVALID
            ),
            FoodDraft(name = "  ").errors()
        )
    }

    @Test
    fun `values must be possible for 100 g`() {
        assertEquals(setOf(FoodDraftError.KCAL_INVALID), skyr.copy(kcal = "950").errors())
        assertEquals(setOf(FoodDraftError.FAT_INVALID), skyr.copy(fat = "-1").errors())
        // 0 est accepté (huile : ni protéines ni glucides).
        assertTrue(skyr.copy(protein = "0", carbs = "0").errors().isEmpty())
        assertEquals(
            setOf(FoodDraftError.MACROS_OVER_100G),
            skyr.copy(protein = "60", carbs = "30", fat = "15").errors()
        )
    }

    @Test
    fun `a food turns back into the same draft`() {
        val food = Food(1, "Skyr nature", skyr.toNutrients(), isCustom = true, isFavorite = false)
        assertEquals(skyr, food.toDraft())
    }
}
