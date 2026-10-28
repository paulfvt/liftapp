package com.paulfvt.liftapp.progress.domain

import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.workout.domain.SetValues
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthlyRecapTest {
    private val october = YearMonth.of(2026, 10)
    private val squat = testExercise(1, name = "Squat", primary = listOf(Muscle.QUADRICEPS))
    private val bench = testExercise(2, name = "Développé couché")
    private val pullUp = testExercise(3, TrackingType.REPS, name = "Traction")

    @Test
    fun `sessions and tonnage of the month, warm-ups and bodyweight excluded`() {
        val histories = listOf(
            ExerciseHistory(
                squat,
                listOf(
                    performed("2026-09-30", 100.0, 5),
                    performed("2026-10-05", 60.0, 10, SetType.WARMUP),
                    performed("2026-10-05", 100.0, 5),
                    performed("2026-10-12", 100.0, 5)
                )
            ),
            ExerciseHistory(bench, listOf(performed("2026-10-05", 80.0, 8))),
            ExerciseHistory(pullUp, listOf(performed("2026-10-07", 0.0, 10), performed("2026-10-07", 10.0, 5)))
        )
        val recap = monthlyRecap(histories, october, PARIS)

        assertEquals(3, recap.workoutCount)
        assertEquals(500.0 + 500.0 + 640.0 + 50.0, recap.tonnageKg, 0.0)
    }

    @Test
    fun `a session counts for the month of its start in the phone time zone`() {
        // 31 octobre à 23 h 30 à Paris : déjà le 1er novembre en UTC, mais une séance d'octobre.
        val lateSession = PerformedSet(
            setId = 99,
            workoutId = 99,
            performedAt = LocalDate.parse("2026-10-31").atTime(23, 30).atZone(PARIS).toInstant(),
            type = SetType.WORK,
            values = SetValues(weightKg = 100.0, reps = 1)
        )
        val recap = monthlyRecap(listOf(ExerciseHistory(squat, listOf(lateSession))), october, PARIS)
        assertEquals(1, recap.workoutCount)
    }

    @Test
    fun `records of the month keep one line per exercise and metric`() {
        val history = ExerciseHistory(
            squat,
            listOf(
                performed("2026-09-20", 100.0, 1),
                performed("2026-09-27", 102.5, 1),
                performed("2026-10-05", 105.0, 1),
                performed("2026-10-12", 107.5, 1)
            )
        )
        val records = monthlyRecap(listOf(history), october, PARIS).records

        // 1RM, charge max et volume battus deux fois en octobre : une ligne chacun, à 107,5 kg.
        assertEquals(3, records.size)
        assertEquals(setOf(107.5), records.map { it.value }.toSet())
        assertEquals(at("2026-10-12"), records.first().achievedAt)
    }

    @Test
    fun `the first sessions of the month on a new exercise break no record`() {
        val history = ExerciseHistory(bench, listOf(performed("2026-10-05", 80.0, 8)))
        assertEquals(emptyList<RecapRecord>(), monthlyRecap(listOf(history), october, PARIS).records)
    }

    @Test
    fun `the favorite exercise is the one done in the most sessions`() {
        val histories = listOf(
            ExerciseHistory(squat, listOf(performed("2026-10-05", 100.0, 5), performed("2026-10-12", 100.0, 5))),
            ExerciseHistory(bench, List(6) { performed("2026-10-07", 80.0, 8) })
        )
        assertEquals(FavoriteExercise(squat, 2), monthlyRecap(histories, october, PARIS).favorite)
    }

    @Test
    fun `an empty month has nothing to show`() {
        val recap = monthlyRecap(emptyList(), october, PARIS)
        assertEquals(0, recap.workoutCount)
        assertNull(recap.equivalence)
        assertNull(recap.favorite)
    }

    @Test
    fun `tonnage is compared with the heaviest object it outweighs`() {
        assertEquals(TonnageEquivalence(TonnageObject.ELEPHANT, 8.0), tonnageEquivalence(48_200.0))
        assertEquals(TonnageEquivalence(TonnageObject.CITY_CAR, 2.5), tonnageEquivalence(3_000.0))
        assertEquals(TonnageEquivalence(TonnageObject.ELEPHANT, 24.0), tonnageEquivalence(144_000.0))
        assertEquals(TonnageEquivalence(TonnageObject.BLUE_WHALE, 1.0), tonnageEquivalence(150_000.0))
        assertEquals(TonnageEquivalence(TonnageObject.CEMENT_BAG, 1.6), tonnageEquivalence(40.0))
        assertNull(tonnageEquivalence(10.0))
    }

    @Test
    fun `recap months are the months with sessions plus the current one`() {
        val history =
            ExerciseHistory(squat, listOf(performed("2026-08-10", 100.0, 5), performed("2026-10-05", 100.0, 5)))
        assertEquals(
            listOf(YearMonth.of(2026, 11), october, YearMonth.of(2026, 8)),
            recapMonths(listOf(history), YearMonth.of(2026, 11), PARIS)
        )
    }
}
