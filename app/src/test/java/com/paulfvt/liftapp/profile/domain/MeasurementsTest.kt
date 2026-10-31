package com.paulfvt.liftapp.profile.domain

import com.paulfvt.liftapp.core.domain.DailyValue
import com.paulfvt.liftapp.core.domain.LengthUnit
import com.paulfvt.liftapp.core.domain.Units
import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.core.model.BodyFatMethod
import com.paulfvt.liftapp.core.model.MeasurementType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementsTest {
    private val paris = ZoneId.of("Europe/Paris")

    private fun weight(at: String, kg: Double, id: Long = 0) =
        BodyMeasurement(id, MeasurementType.BODY_WEIGHT, kg, null, Instant.parse(at))

    private fun day(date: String, value: Double) = DailyValue(LocalDate.parse(date), value)

    @Test
    fun `two weigh-ins on the same day give their average`() {
        val daily = dailyValues(
            listOf(weight("2026-10-20T06:00:00Z", 80.0), weight("2026-10-20T19:00:00Z", 81.0)),
            paris
        )
        assertEquals(listOf(day("2026-10-20", 80.5)), daily)
    }

    @Test
    fun `days follow the phone time zone`() {
        // 23:30 UTC le 20 octobre, c'est déjà le 21 à Paris.
        val daily = dailyValues(listOf(weight("2026-10-20T23:30:00Z", 80.0)), paris)
        assertEquals(LocalDate.parse("2026-10-21"), daily.single().date)
    }

    @Test
    fun `moving average covers the day and the six days before`() {
        val daily = listOf(
            day("2026-10-01", 80.0),
            day("2026-10-04", 82.0),
            day("2026-10-07", 84.0),
            day("2026-10-08", 86.0)
        )
        val average = movingAverage(daily)
        assertEquals(80.0, average[0].value, 1e-9)
        assertEquals(81.0, average[1].value, 1e-9)
        // Le 7 : du 1er au 7, trois pesées.
        assertEquals(82.0, average[2].value, 1e-9)
        // Le 8 : le 1er sort de la fenêtre.
        assertEquals(84.0, average[3].value, 1e-9)
    }

    @Test
    fun `moving average window crosses the end of a month`() {
        val daily = listOf(day("2026-09-28", 70.0), day("2026-10-02", 72.0), day("2026-10-05", 74.0))
        val average = movingAverage(daily)
        assertEquals(71.0, average[1].value, 1e-9)
        // Le 5 octobre : la fenêtre part du 29 septembre, la pesée du 28 n'y est plus.
        assertEquals(73.0, average[2].value, 1e-9)
    }

    @Test
    fun `a single measure is its own average and an empty list stays empty`() {
        assertEquals(listOf(day("2026-10-01", 80.0)), movingAverage(listOf(day("2026-10-01", 80.0))))
        assertTrue(movingAverage(emptyList()).isEmpty())
    }

    @Test
    fun `summary gives the latest value of each type and the change since the previous one`() {
        val waist = BodyMeasurement(9, MeasurementType.WAIST, 82.0, null, Instant.parse("2026-10-01T08:00:00Z"))
        val result = summaries(
            listOf(
                weight("2026-10-01T06:00:00Z", 81.0, id = 1),
                weight("2026-10-15T06:00:00Z", 79.5, id = 2),
                waist
            )
        )
        assertEquals(listOf(MeasurementType.BODY_WEIGHT, MeasurementType.WAIST), result.map { it.latest.type })
        assertEquals(2L, result[0].latest.id)
        assertEquals(-1.5, result[0].change!!, 1e-9)
        assertNull(result[1].change)
    }

    @Test
    fun `a draft needs a value within the type range`() {
        val today = LocalDate.parse("2026-10-21")
        fun draft(type: MeasurementType, value: String) = MeasurementDraft(type, value, date = today)
        assertEquals(setOf(MeasurementError.VALUE_MISSING), draft(MeasurementType.BODY_WEIGHT, "").errors(today))
        assertEquals(
            setOf(MeasurementError.VALUE_OUT_OF_RANGE),
            draft(MeasurementType.BODY_WEIGHT, "805").errors(today)
        )
        assertTrue(draft(MeasurementType.BODY_WEIGHT, "80,5").errors(today).isEmpty())
        assertTrue(draft(MeasurementType.ARM, "38").errors(today).isEmpty())
        assertEquals(setOf(MeasurementError.VALUE_OUT_OF_RANGE), draft(MeasurementType.ARM, "5").errors(today))
    }

    @Test
    fun `body fat needs its method, other types ignore it`() {
        val today = LocalDate.parse("2026-10-21")
        val fat = MeasurementDraft(MeasurementType.BODY_FAT, "15", date = today)
        assertEquals(setOf(MeasurementError.METHOD_MISSING), fat.errors(today))
        assertTrue(fat.copy(method = BodyFatMethod.CALIPER).errors(today).isEmpty())
        val weight = MeasurementDraft(MeasurementType.BODY_WEIGHT, "80", BodyFatMethod.SCALE, today)
        assertNull(weight.toMeasurement(Instant.parse("2026-10-21T08:00:00Z"), paris).method)
    }

    @Test
    fun `a measure cannot be dated in the future`() {
        val today = LocalDate.parse("2026-10-21")
        val draft = MeasurementDraft(MeasurementType.BODY_WEIGHT, "80", date = today.plusDays(1))
        assertEquals(setOf(MeasurementError.DATE_IN_FUTURE), draft.errors(today))
    }

    @Test
    fun `today keeps the current time, a past day is stored at noon`() {
        val now = Instant.parse("2026-10-21T06:12:00Z")
        assertEquals(now, measuredAtFor(LocalDate.parse("2026-10-21"), now, paris))
        assertEquals(
            Instant.parse("2026-10-18T10:00:00Z"),
            measuredAtFor(LocalDate.parse("2026-10-18"), now, paris)
        )
    }

    @Test
    fun `measurements are typed and shown in the chosen units and stored in metric`() {
        val imperial = Units(
            WeightUnit.LB,
            LengthUnit.IN
        )
        assertEquals(80.0, MeasurementType.BODY_WEIGHT.toStorage(176.37, imperial), 0.01)
        assertEquals(38.1, MeasurementType.ARM.toStorage(15.0, imperial), 1e-9)
        assertEquals(15.0, MeasurementType.BODY_FAT.toStorage(15.0, imperial), 0.0)
        assertEquals(15.0, MeasurementType.ARM.toDisplay(38.1, imperial), 1e-9)
    }
}
