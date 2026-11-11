package com.paulfvt.liftapp.nutrition.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnergyCheckTest {
    @Test
    fun `calories matching the macros raise nothing`() {
        // Granola : 452 kcal, 9,8 g de protéines, 61 g de glucides, 18 g de lipides, 5,2 g de fibres.
        assertNull(energyMismatch(452.0, 9.8, 61.0, 18.0, fiber = 5.2))
        // Rounding of a light food stays within the 25 kcal margin.
        assertNull(energyMismatch(20.0, 1.0, 3.0, 0.2))
    }

    @Test
    fun `a lost decimal comma is pointed out with its fix`() {
        val mismatch = energyMismatch(452.0, 9.8, 61.0, 1.8, fiber = 5.2)!!
        assertEquals(LabelField.FAT, mismatch.suspect)
        assertEquals(18.0, mismatch.suggestion!!, 0.0)
        assertEquals(309.8, mismatch.expectedKcal, 0.01)
    }

    @Test
    fun `a final g read as 9 is fixed with the closest value`() {
        // « 13g » lu « 139 » : 13,9 et 13 collent tous deux, 13 colle le mieux.
        val mismatch = energyMismatch(362.0, 139.0, 58.0, 6.5, fiber = 9.0)!!
        assertEquals(LabelField.PROTEIN, mismatch.suspect)
        assertEquals(13.0, mismatch.suggestion!!, 0.0)
    }

    @Test
    fun `kJ taken for kcal are converted`() {
        val mismatch = energyMismatch(1520.0, 13.0, 58.0, 6.5, fiber = 9.0)!!
        assertEquals(LabelField.ENERGY, mismatch.suspect)
        assertEquals(363.0, mismatch.suggestion!!, 0.0)
    }

    @Test
    fun `no guess when several values or none could explain the gap`() {
        // Protéines ou glucides multipliés par 10 collent aussi bien.
        val ambiguous = energyMismatch(400.0, 9.0, 9.0, 0.0)!!
        assertNull(ambiguous.suspect)
        assertNull(ambiguous.suggestion)
        assertEquals(72.0, ambiguous.expectedKcal, 0.0)
        assertNull(energyMismatch(250.0, 2.0, 3.0, 1.0)!!.suspect)
    }

    @Test
    fun `a draft is checked only once its four values are numbers`() {
        val draft = FoodDraft(name = "Granola", kcal = "452", protein = "9,8", carbs = "61", fat = "1,8")
        assertEquals(LabelField.FAT, draft.energyMismatch(fiber = 5.2)!!.suspect)
        assertNull(draft.copy(fat = "").energyMismatch())
    }
}
