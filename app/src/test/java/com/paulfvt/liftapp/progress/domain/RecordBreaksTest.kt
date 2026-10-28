package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.workout.domain.SetValues
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordBreaksTest {
    private var nextId = 1L

    private fun day(n: Int): Instant = Instant.parse("2026-10-01T18:00:00Z").plusSeconds(n * 86_400L)

    private fun lift(workout: Long, weight: Double, reps: Int, type: SetType = SetType.WORK) =
        PerformedSet(nextId++, workout, day(workout.toInt()), type, SetValues(weightKg = weight, reps = reps))

    @Test
    fun `the first session on an exercise breaks no record`() {
        val sets = listOf(lift(1, 100.0, 5), lift(1, 110.0, 3))
        assertTrue(recordBreaks(sets, TrackingType.WEIGHT_REPS).isEmpty())
    }

    @Test
    fun `a better session breaks each improved metric once`() {
        val sets = listOf(
            lift(1, 100.0, 5),
            // Séance 2 : charge max et 1RM battus, volume par série égal (500) donc pas battu.
            lift(2, 105.0, 3),
            lift(2, 100.0, 5)
        )
        val breaks = recordBreaks(sets, TrackingType.WEIGHT_REPS)

        assertEquals(setOf(ExerciseMetric.MAX_WEIGHT), breaks.map { it.metric }.toSet())
        val maxWeight = breaks.single()
        assertEquals(100.0, maxWeight.previous, 0.0)
        assertEquals(105.0, maxWeight.value, 0.0)
        assertEquals(2L, maxWeight.workoutId)
        assertEquals(day(2), maxWeight.achievedAt)
    }

    @Test
    fun `warm-ups and zero rep sets never break a record`() {
        val sets = listOf(lift(1, 100.0, 5), lift(2, 140.0, 5, SetType.WARMUP), lift(2, 150.0, 0))
        assertTrue(recordBreaks(sets, TrackingType.WEIGHT_REPS).isEmpty())
    }

    @Test
    fun `reps at a given weight are not celebrated`() {
        // 80 × 6 après 80 × 5 : c'est un record de reps à 80 kg, mais 1RM, charge et volume restent sous 100 × 5.
        val sets = listOf(lift(1, 100.0, 5), lift(1, 80.0, 5), lift(2, 80.0, 6))
        assertTrue(recordBreaks(sets, TrackingType.WEIGHT_REPS).isEmpty())
    }

    @Test
    fun `each session is compared with the best before it`() {
        val sets = listOf(lift(1, 100.0, 1), lift(2, 102.5, 1), lift(3, 101.0, 1), lift(4, 105.0, 1))
        val maxWeights = recordBreaks(sets, TrackingType.WEIGHT_REPS).filter { it.metric == ExerciseMetric.MAX_WEIGHT }
        assertEquals(listOf(2L, 4L), maxWeights.map { it.workoutId })
        assertEquals(listOf(100.0, 102.5), maxWeights.map { it.previous })
    }

    @Test
    fun `bodyweight exercises celebrate reps and extra load`() {
        val first = PerformedSet(1, 1, day(1), SetType.WORK, SetValues(weightKg = 0.0, reps = 10))
        val second = PerformedSet(2, 2, day(2), SetType.WORK, SetValues(weightKg = 0.0, reps = 12))
        val breaks = recordBreaks(listOf(first, second), TrackingType.REPS)
        assertEquals(listOf(ExerciseMetric.MAX_REPS), breaks.map { it.metric })
    }

    @Test
    fun `a live record needs a record from before the session`() {
        val session = listOf(lift(9, 120.0, 5))
        assertTrue(liveRecords(session, TrackingType.WEIGHT_REPS, emptyMap()).isEmpty())
    }

    @Test
    fun `the first set reaching the session best holds the live record`() {
        val session = listOf(lift(9, 100.0, 5), lift(9, 105.0, 5), lift(9, 105.0, 5))
        val previous = mapOf(ExerciseMetric.MAX_WEIGHT to 102.5, ExerciseMetric.ESTIMATED_1RM to 125.0)
        val records = liveRecords(session, TrackingType.WEIGHT_REPS, previous)

        val maxWeight = records.single { it.metric == ExerciseMetric.MAX_WEIGHT }
        assertEquals(session[1].setId, maxWeight.setId)
        assertEquals(102.5, maxWeight.previous, 0.0)
        // 105 × 5 = 122,5 de 1RM estimé, sous le record de 125 : pas de record de 1RM. Pas de record de volume connu.
        assertEquals(listOf(ExerciseMetric.MAX_WEIGHT), records.map { it.metric })
    }

    @Test
    fun `equalling a record is not beating it`() {
        val session = listOf(lift(9, 100.0, 5))
        val previous = mapOf(ExerciseMetric.MAX_WEIGHT to 100.0)
        assertTrue(liveRecords(session, TrackingType.WEIGHT_REPS, previous).isEmpty())
    }
}
