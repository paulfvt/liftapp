package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetInputTest {
    @Test
    fun `decimal input keeps one comma, two decimals and turns a dot into a comma`() {
        assertEquals("82,5", filterDecimalInput("82.5", allowNegative = false))
        assertEquals("82,25", filterDecimalInput("82,256", allowNegative = false))
        assertEquals("82,5", filterDecimalInput("82,,5", allowNegative = false))
        assertEquals("1000", filterDecimalInput("10000", allowNegative = false))
        assertEquals("80", filterDecimalInput("8a0", allowNegative = false))
    }

    @Test
    fun `a minus sign is only accepted first and when negative values are allowed`() {
        assertEquals("-10", filterDecimalInput("-10", allowNegative = true))
        assertEquals("10", filterDecimalInput("-10", allowNegative = false))
        assertEquals("10", filterDecimalInput("1-0", allowNegative = true))
    }

    @Test
    fun `decimals are parsed with a comma and formatted without useless zeros`() {
        assertEquals(82.5, parseDecimal("82,5")!!, 0.0)
        assertEquals(82.0, parseDecimal("82,")!!, 0.0)
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("-"))
        assertEquals("82,5", formatDecimal(82.5))
        assertEquals("100", formatDecimal(100.0))
        assertEquals("-10", formatDecimal(-10.0))
        assertEquals("81,25", formatDecimal(81.25))
        assertEquals("", formatDecimal(null))
    }

    @Test
    fun `reps accept zero and at most three digits`() {
        assertEquals(0, parseReps(filterRepsInput("0")))
        assertEquals(123, parseReps(filterRepsInput("1234")))
        assertNull(parseReps(filterRepsInput("")))
    }

    @Test
    fun `durations are typed as minutes and seconds or as seconds`() {
        assertEquals(90, parseDuration("1:30"))
        assertEquals(90, parseDuration("90"))
        assertEquals(45, parseDuration(":45"))
        assertNull(parseDuration("1:75"))
        assertNull(parseDuration(""))
        assertEquals("1:30", filterDurationInput("1:3:0"))
    }

    @Test
    fun `durations are displayed as minutes and seconds, with hours beyond an hour`() {
        assertEquals("0:45", formatDuration(45))
        assertEquals("1:30", formatDuration(90))
        assertEquals("1:05:00", formatDuration(3900))
        assertEquals("", formatDuration(null))
    }

    @Test
    fun `distance is in kilometers for cardio and in meters otherwise`() {
        assertEquals(DistanceUnit.KILOMETER, distanceUnitFor(ExerciseCategory.CARDIO))
        assertEquals(DistanceUnit.METER, distanceUnitFor(ExerciseCategory.OTHER))
        assertEquals(5200.0, parseDistance("5,2", DistanceUnit.KILOMETER)!!, 0.001)
        assertEquals("5,2", formatDistance(5200.0, DistanceUnit.KILOMETER))
        assertEquals("20", formatDistance(20.0, DistanceUnit.METER))
    }

    @Test
    fun `a set needs the main value of its tracking type to be completed`() {
        assertFalse(canComplete(SetValues(weightKg = 80.0), TrackingType.WEIGHT_REPS))
        assertTrue(canComplete(SetValues(reps = 0), TrackingType.WEIGHT_REPS))
        assertTrue(canComplete(SetValues(reps = 10), TrackingType.REPS))
        assertFalse(canComplete(SetValues(reps = 10), TrackingType.DURATION))
        assertTrue(canComplete(SetValues(durationSec = 60), TrackingType.DURATION))
        assertTrue(canComplete(SetValues(durationSec = 600), TrackingType.DISTANCE))
    }

    @Test
    fun `rpe goes from 6 to 10 by half points`() {
        assertEquals(9, RPE_CHOICES.size)
        assertEquals(6.0, RPE_CHOICES.first(), 0.0)
        assertEquals(10.0, RPE_CHOICES.last(), 0.0)
    }
}
