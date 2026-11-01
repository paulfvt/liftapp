package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BestWorkSetTest {
    @Test
    fun `the heaviest work set wins, then the one with more reps, warm-ups never count`() {
        val sets = listOf(
            previous(SetType.WARMUP, 100.0, 10),
            previous(SetType.WORK, 80.0, 8),
            previous(SetType.WORK, 82.5, 6),
            previous(SetType.WORK, 82.5, 7),
            previous(SetType.DROP, 60.0, 12)
        )
        assertEquals(previous(SetType.WORK, 82.5, 7), bestWorkSet(sets, TrackingType.WEIGHT_REPS))
    }

    @Test
    fun `without load the longest set wins`() {
        val pullUps = listOf(previous(SetType.WORK, null, 8), previous(SetType.WORK, -10.0, 12))
        assertEquals(previous(SetType.WORK, -10.0, 12), bestWorkSet(pullUps, TrackingType.REPS))

        val planks = listOf(
            PreviousSet(SetType.WORK, SetValues(durationSec = 60)),
            PreviousSet(SetType.WORK, SetValues(durationSec = 90))
        )
        assertEquals(90, bestWorkSet(planks, TrackingType.DURATION)?.values?.durationSec)
    }

    @Test
    fun `only warm-ups or nothing gives no best set`() {
        assertNull(bestWorkSet(listOf(previous(SetType.WARMUP, 40.0, 10)), TrackingType.WEIGHT_REPS))
        assertNull(bestWorkSet(emptyList(), TrackingType.WEIGHT_REPS))
    }
}
