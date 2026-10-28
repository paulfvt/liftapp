package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyMapTest {
    private val today = LocalDate.parse("2026-10-28")

    private val benchPress =
        testExercise(1, primary = listOf(Muscle.CHEST), secondary = listOf(Muscle.TRICEPS, Muscle.SHOULDERS))

    @Test
    fun `a primary muscle counts one set and a secondary muscle half a set`() {
        val history = ExerciseHistory(benchPress, List(3) { performed("2026-10-27", 80.0, 8) })
        val sets = muscleSets(listOf(history), today, PARIS)

        assertEquals(3.0, sets[Muscle.CHEST]!!, 0.0)
        assertEquals(1.5, sets[Muscle.TRICEPS]!!, 0.0)
        assertEquals(1.5, sets[Muscle.SHOULDERS]!!, 0.0)
    }

    @Test
    fun `only the last seven days count, today included`() {
        val history = ExerciseHistory(
            benchPress,
            listOf(performed("2026-10-21", 80.0, 8), performed("2026-10-22", 80.0, 8), performed("2026-10-28", 80.0, 8))
        )
        // Le 21 octobre est à 7 jours : hors fenêtre. Le 22 et le 28 comptent.
        assertEquals(2.0, muscleSets(listOf(history), today, PARIS)[Muscle.CHEST]!!, 0.0)
    }

    @Test
    fun `warm-ups and zero rep sets are not effective`() {
        val history = ExerciseHistory(
            benchPress,
            listOf(performed("2026-10-27", 40.0, 10, SetType.WARMUP), performed("2026-10-27", 100.0, 0))
        )
        assertTrue(muscleSets(listOf(history), today, PARIS).isEmpty())
    }

    @Test
    fun `drop sets count and several exercises add up`() {
        val dips =
            testExercise(2, TrackingType.REPS, primary = listOf(Muscle.TRICEPS), secondary = listOf(Muscle.CHEST))
        val sets = muscleSets(
            listOf(
                ExerciseHistory(benchPress, listOf(performed("2026-10-27", 60.0, 12, SetType.DROP))),
                ExerciseHistory(dips, List(2) { performed("2026-10-27", 0.0, 10) })
            ),
            today,
            PARIS
        )
        assertEquals(2.0, sets[Muscle.CHEST]!!, 0.0)
        assertEquals(2.5, sets[Muscle.TRICEPS]!!, 0.0)
    }

    @Test
    fun `cardio is left out`() {
        val running =
            testExercise(
                3,
                TrackingType.DISTANCE,
                primary = listOf(Muscle.QUADRICEPS),
                category = ExerciseCategory.CARDIO
            )
        val history =
            ExerciseHistory(running, listOf(performed("2026-10-27", distanceM = 5_000.0, durationSec = 1_500)))
        assertNull(muscleSets(listOf(history), today, PARIS)[Muscle.QUADRICEPS])
    }

    @Test
    fun `a plank counts when it lasted`() {
        val plank = testExercise(4, TrackingType.DURATION, primary = listOf(Muscle.ABDOMINALS))
        val history =
            ExerciseHistory(
                plank,
                listOf(performed("2026-10-27", durationSec = 60), performed("2026-10-27", durationSec = 0))
            )
        assertEquals(1.0, muscleSets(listOf(history), today, PARIS)[Muscle.ABDOMINALS]!!, 0.0)
    }

    @Test
    fun `heat levels follow the weekly set count`() {
        assertEquals(HeatLevel.NONE, heatLevel(0.0))
        assertEquals(HeatLevel.LOW, heatLevel(0.5))
        assertEquals(HeatLevel.LOW, heatLevel(2.5))
        assertEquals(HeatLevel.MEDIUM, heatLevel(3.0))
        assertEquals(HeatLevel.HIGH, heatLevel(6.0))
        assertEquals(HeatLevel.VERY_HIGH, heatLevel(14.5))
    }
}
