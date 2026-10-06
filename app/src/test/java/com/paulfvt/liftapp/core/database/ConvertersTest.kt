package com.paulfvt.liftapp.core.database

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `instant survives a round trip at millisecond precision`() {
        val instant = Instant.parse("2026-10-06T18:30:15.123Z")
        assertEquals(instant, converters.epochMilliToInstant(converters.instantToEpochMilli(instant)))
    }

    @Test
    fun `local date survives a round trip`() {
        val monday = LocalDate.of(2026, 10, 5)
        assertEquals(monday, converters.epochDayToLocalDate(converters.localDateToEpochDay(monday)))
    }

    @Test
    fun `null stays null`() {
        assertNull(converters.instantToEpochMilli(null))
        assertNull(converters.epochMilliToInstant(null))
        assertNull(converters.localDateToEpochDay(null))
        assertNull(converters.epochDayToLocalDate(null))
    }
}
