package com.paulfvt.liftapp.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {
    @Test
    fun `kilos are stored as typed`() {
        assertEquals(100.0, WeightUnit.KG.toKg(100.0), 0.0)
        assertEquals(81.25, WeightUnit.KG.inputValue(81.25), 0.0)
    }

    @Test
    fun `pounds are converted to kilos and read back as typed`() {
        val stored = WeightUnit.LB.toKg(225.0)
        assertEquals(102.058, stored, 1e-3)
        assertEquals(225.0, WeightUnit.LB.inputValue(stored), 0.0)
        assertEquals(220.5, WeightUnit.LB.inputValue(100.0), 0.0)
    }

    @Test
    fun `computed values are rounded to half a kilo or a pound and a quarter`() {
        assertEquals(116.5, WeightUnit.KG.displayValue(116.67), 0.0)
        // 116,67 kg = 257,22 lb → 257,5 lb (pas de 1,25 lb).
        assertEquals(257.5, WeightUnit.LB.displayValue(116.67), 0.0)
        assertEquals(220.0, WeightUnit.LB.displayValue(100.0), 0.0)
    }

    @Test
    fun `lengths are converted with inches at a tenth`() {
        assertEquals(181.0, LengthUnit.CM.displayValue(181.0), 0.0)
        assertEquals(71.3, LengthUnit.IN.displayValue(181.0), 0.0)
        assertEquals(15.0 * 2.54, LengthUnit.IN.toCm(15.0), 1e-9)
    }
}
