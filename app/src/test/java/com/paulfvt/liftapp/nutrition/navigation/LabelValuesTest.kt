package com.paulfvt.liftapp.nutrition.navigation

import com.paulfvt.liftapp.nutrition.domain.LabelReading
import org.junit.Assert.assertEquals
import org.junit.Test

class LabelValuesTest {
    @Test
    fun `scanned values survive the trip back to the food screen, missing ones included`() {
        val reading = LabelReading(kcal = 452.0, protein = 9.8, carbs = null, fat = 18.0, fiber = 5.2)
        assertEquals(reading, labelReadingOf(reading.toValues()))
        assertEquals(LabelReading(), labelReadingOf(LabelReading().toValues()))
    }
}
