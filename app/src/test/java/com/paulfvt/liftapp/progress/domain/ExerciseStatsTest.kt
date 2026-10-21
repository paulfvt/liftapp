package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.PersonalRecordType
import com.paulfvt.liftapp.core.model.PersonalRecordType.MAX_DISTANCE
import com.paulfvt.liftapp.core.model.PersonalRecordType.MAX_DURATION
import com.paulfvt.liftapp.core.model.PersonalRecordType.MAX_ESTIMATED_1RM
import com.paulfvt.liftapp.core.model.PersonalRecordType.MAX_REPS
import com.paulfvt.liftapp.core.model.PersonalRecordType.MAX_REPS_AT_WEIGHT
import com.paulfvt.liftapp.core.model.PersonalRecordType.MAX_SET_VOLUME
import com.paulfvt.liftapp.core.model.PersonalRecordType.MAX_WEIGHT
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.SetType.DROP
import com.paulfvt.liftapp.core.model.SetType.WARMUP
import com.paulfvt.liftapp.core.model.SetType.WORK
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.workout.domain.SetValues
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseStatsTest {
    private val day1 = Instant.parse("2026-10-01T18:00:00Z")
    private val day2 = Instant.parse("2026-10-08T18:00:00Z")

    private var nextId = 1L

    private fun lift(
        weight: Double?,
        reps: Int?,
        at: Instant = day1,
        type: SetType = WORK,
        workoutId: Long = if (at == day1) 1 else 2
    ) = PerformedSet(nextId++, workoutId, at, type, SetValues(weightKg = weight, reps = reps))

    private fun List<PersonalRecord>.of(type: PersonalRecordType) = filter { it.type == type }

    @Test
    fun `epley is used from 2 to 12 reps and a single rep is worth its weight`() {
        assertEquals(100.0, estimatedOneRepMax(100.0, 1)!!, 0.0)
        assertEquals(100.0 * (1 + 8 / 30.0), estimatedOneRepMax(100.0, 8)!!, 1e-9)
        assertEquals(140.0, estimatedOneRepMax(100.0, 12)!!, 1e-9)
        assertNull(estimatedOneRepMax(100.0, 13))
        assertNull(estimatedOneRepMax(100.0, 0))
        assertNull(estimatedOneRepMax(0.0, 5))
        assertNull(estimatedOneRepMax(null, 5))
    }

    @Test
    fun `the four weight records come from the best sets`() {
        val sets = listOf(lift(100.0, 5), lift(80.0, 12), lift(105.0, 1, day2))
        val records = computeRecords(sets, TrackingType.WEIGHT_REPS)

        assertEquals(105.0, records.of(MAX_WEIGHT).single().value, 0.0)
        // 100 × 5 = 116,7 ; 80 × 12 = 112 ; 105 × 1 = 105.
        assertEquals(100.0 * (1 + 5 / 30.0), records.of(MAX_ESTIMATED_1RM).single().value, 1e-9)
        assertEquals(960.0, records.of(MAX_SET_VOLUME).single().value, 0.0)
        assertEquals(
            listOf(80.0 to 12.0, 100.0 to 5.0, 105.0 to 1.0),
            records.of(MAX_REPS_AT_WEIGHT).map { it.weightKg!! to it.value }
        )
        assertEquals(day2, records.of(MAX_WEIGHT).single().achievedAt)
    }

    @Test
    fun `warm-ups are ignored and drop sets count`() {
        val sets = listOf(lift(120.0, 3, type = WARMUP), lift(100.0, 3), lift(60.0, 15, type = DROP))
        val records = computeRecords(sets, TrackingType.WEIGHT_REPS)

        assertEquals(100.0, records.of(MAX_WEIGHT).single().value, 0.0)
        assertEquals(900.0, records.of(MAX_SET_VOLUME).single().value, 0.0)
        assertEquals(15.0, records.of(MAX_REPS_AT_WEIGHT).single { it.weightKg == 60.0 }.value, 0.0)
    }

    @Test
    fun `a set with zero reps lifted nothing and sets no record`() {
        val records = computeRecords(listOf(lift(150.0, 0), lift(100.0, 3)), TrackingType.WEIGHT_REPS)
        assertEquals(100.0, records.of(MAX_WEIGHT).single().value, 0.0)
        assertTrue(records.of(MAX_REPS_AT_WEIGHT).none { it.weightKg == 150.0 })
    }

    @Test
    fun `on a tie the first time keeps the record`() {
        val first = lift(100.0, 5, day1)
        val second = lift(100.0, 5, day2)
        val records = computeRecords(listOf(second, first), TrackingType.WEIGHT_REPS)
        assertTrue(records.all { it.setId == first.setId })
    }

    @Test
    fun `no records without any valid set`() {
        assertTrue(computeRecords(emptyList(), TrackingType.WEIGHT_REPS).isEmpty())
        assertTrue(computeRecords(listOf(lift(80.0, 10, type = WARMUP)), TrackingType.WEIGHT_REPS).isEmpty())
    }

    @Test
    fun `bodyweight records are max reps without assistance and heaviest extra load`() {
        val sets = listOf(lift(null, 12), lift(-20.0, 20), lift(10.0, 6), lift(0.0, 13, day2))
        val records = computeRecords(sets, TrackingType.REPS)

        assertEquals(13.0, records.of(MAX_REPS).single().value, 0.0)
        assertEquals(10.0, records.of(MAX_WEIGHT).single().value, 0.0)
        assertTrue(records.of(MAX_ESTIMATED_1RM).isEmpty())
        assertTrue(records.of(MAX_SET_VOLUME).isEmpty())
        assertTrue(records.of(MAX_REPS_AT_WEIGHT).isEmpty())
    }

    @Test
    fun `duration and distance exercises keep their longest set`() {
        val plank = listOf(
            PerformedSet(1, 1, day1, WORK, SetValues(durationSec = 60)),
            PerformedSet(2, 2, day2, WORK, SetValues(durationSec = 90))
        )
        assertEquals(
            listOf(MAX_DURATION to 90.0),
            computeRecords(plank, TrackingType.DURATION).map {
                it.type to
                    it.value
            }
        )

        val run = listOf(PerformedSet(3, 1, day1, WORK, SetValues(distanceM = 5_000.0, durationSec = 1_500)))
        assertEquals(
            listOf(MAX_DISTANCE to 5_000.0),
            computeRecords(run, TrackingType.DISTANCE).map {
                it.type to
                    it.value
            }
        )
    }

    @Test
    fun `metrics offered depend on the tracking type`() {
        assertEquals(
            listOf(ExerciseMetric.ESTIMATED_1RM, ExerciseMetric.MAX_WEIGHT, ExerciseMetric.VOLUME),
            metricsFor(TrackingType.WEIGHT_REPS)
        )
        assertEquals(listOf(ExerciseMetric.MAX_REPS, ExerciseMetric.MAX_WEIGHT), metricsFor(TrackingType.REPS))
        assertEquals(listOf(ExerciseMetric.MAX_DURATION), metricsFor(TrackingType.DURATION))
        assertEquals(listOf(ExerciseMetric.MAX_DISTANCE), metricsFor(TrackingType.DISTANCE))
    }
}
