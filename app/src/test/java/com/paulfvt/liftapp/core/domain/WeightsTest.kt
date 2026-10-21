package com.paulfvt.liftapp.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeightsTest {
    @Test
    fun `display weights are rounded to the nearest half kilo`() {
        assertEquals(116.5, roundForDisplay(116.67), 0.0)
        assertEquals(116.5, roundForDisplay(116.3), 0.0)
        assertEquals(116.0, roundForDisplay(116.2), 0.0)
        assertEquals(117.0, roundForDisplay(116.75), 0.0)
        assertEquals(82.5, roundForDisplay(82.5), 0.0)
        assertEquals(0.0, roundForDisplay(0.1), 0.0)
    }
}
