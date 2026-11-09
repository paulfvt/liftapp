package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.SetType.WARMUP
import com.paulfvt.liftapp.core.model.SetType.WORK
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestTimerTest {
    @Test
    fun `the alert ticks at three, two and one second then ends with a long vibration`() {
        val beats = restAlertBeats(Duration.ofMillis(3_400))
        assertEquals(listOf(3L, 2L, 1L, 0L), beats.map { it.beforeEnd.seconds })
        assertEquals(listOf(false, false, false, true), beats.map { it.isEnd })
        assertEquals(listOf(200L, 200L, 200L, 1_000L), beats.map { it.vibrationMs })
    }

    @Test
    fun `a late alarm keeps only the beats still to come`() {
        assertEquals(listOf(1L, 0L), restAlertBeats(Duration.ofMillis(1_500)).map { it.beforeEnd.seconds })
        // Pile à 3 s de la fin : le premier ding est gardé.
        assertEquals(4, restAlertBeats(Duration.ofSeconds(3)).size)
        // Fin déjà passée : seulement la fin, tout de suite.
        assertEquals(listOf(true), restAlertBeats(Duration.ofMillis(-400)).map { it.isEnd })
    }

    @Test
    fun `the rest alert is due from the alarm and up to one minute late`() {
        // L'alarme sonne 5 s avant la fin, pour préparer les sons avant le premier ding.
        assertEquals(Duration.ofSeconds(5), REST_ALARM_ADVANCE)
        assertTrue(isRestAlertDue(REST_ALARM_ADVANCE))
        // Alarme un peu en avance : la marge d'une seconde la garde.
        assertTrue(isRestAlertDue(Duration.ofMillis(5_900)))
        assertTrue(isRestAlertDue(Duration.ZERO))
        assertTrue(isRestAlertDue(Duration.ofSeconds(-60)))
        assertFalse(isRestAlertDue(Duration.ofSeconds(7)))
        assertFalse(isRestAlertDue(Duration.ofSeconds(-61)))
    }

    private val now = Instant.parse("2026-10-21T18:00:00Z")

    private fun workout(vararg exercises: WorkoutExercise) =
        Workout(id = 1, templateId = null, name = "Push", startedAt = now, note = null, exercises = exercises.toList())

    private fun performed(id: Long, restSeconds: Int?, vararg sets: WorkoutSet) =
        WorkoutExercise(id = id, exercise = exercise(id), restSeconds = restSeconds, sets = sets.toList())

    private fun rest(seconds: Long) = RestPeriod(now, now.plusSeconds(seconds))

    @Test
    fun `remaining time is rounded up and never negative`() {
        val rest = rest(90)
        assertEquals(90, rest.remainingSeconds(now))
        assertEquals(60, rest.remainingSeconds(now.plusSeconds(30)))
        assertEquals(1, rest.remainingSeconds(now.plusMillis(89_001)))
        assertEquals(0, rest.remainingSeconds(now.plusSeconds(90)))
        assertEquals(0, rest.remainingSeconds(now.plusSeconds(600)))
        assertEquals(90, rest.totalSeconds)
    }

    @Test
    fun `a rest is over exactly at its end time`() {
        val rest = rest(60)
        assertTrue(rest.isRunning(now.plusSeconds(59)))
        assertFalse(rest.isRunning(now.plusSeconds(60)))
    }

    @Test
    fun `validating a set starts the rest of its exercise`() {
        val workout = workout(
            performed(1, 90, set(WORK, 80.0, 8, completed = false, id = 10), set(WORK, completed = false, id = 11)),
            performed(2, 180, set(WORK, completed = false, id = 20))
        )
        assertEquals(rest(90), workout.restAfterCompleting(10, now))
        assertEquals(RestPeriod(now, now.plusSeconds(180)), workout.restAfterCompleting(20, now))
    }

    @Test
    fun `an exercise without a rest setting gets the default 2 minutes`() {
        val workout = workout(performed(1, null, set(completed = false, id = 10), set(completed = false, id = 11)))
        assertEquals(rest(120), workout.restAfterCompleting(10, now))
    }

    @Test
    fun `warm-up sets also start a rest`() {
        val workout =
            workout(performed(1, 60, set(WARMUP, completed = false, id = 10), set(completed = false, id = 11)))
        assertEquals(rest(60), workout.restAfterCompleting(10, now))
    }

    @Test
    fun `no rest after the last set of the workout or with a zero rest`() {
        val last = workout(performed(1, 90, set(completed = true, id = 10), set(completed = false, id = 11)))
        assertNull(last.restAfterCompleting(11, now))
        val noRest = workout(performed(1, 0, set(completed = false, id = 10), set(completed = false, id = 11)))
        assertNull(noRest.restAfterCompleting(10, now))
        assertNull(last.restAfterCompleting(99, now))
    }

    @Test
    fun `adding time extends a running rest and keeps its start`() {
        val adjusted = rest(90).adjusted(REST_ADD_SECONDS, now.plusSeconds(10))
        assertEquals(RestPeriod(now, now.plusSeconds(120)), adjusted)
    }

    @Test
    fun `adding time to a finished rest starts a new one from now`() {
        val later = now.plusSeconds(200)
        assertEquals(RestPeriod(later, later.plusSeconds(30)), rest(90).adjusted(REST_ADD_SECONDS, later))
        assertEquals(RestPeriod(now, now.plusSeconds(30)), null.adjusted(REST_ADD_SECONDS, now))
    }

    @Test
    fun `removing time never goes below zero and ends the rest`() {
        assertEquals(RestPeriod(now, now.plusSeconds(75)), rest(90).adjusted(-REST_REMOVE_SECONDS, now))
        assertNull(rest(90).adjusted(-REST_REMOVE_SECONDS, now.plusSeconds(80)))
        assertNull(rest(90).adjusted(-REST_REMOVE_SECONDS, now.plusSeconds(75)))
        assertNull(null.adjusted(-REST_REMOVE_SECONDS, now))
    }

    @Test
    fun `next set is the first unvalidated one in workout order`() {
        val workout = workout(
            performed(1, 90, set(completed = true, id = 10), set(completed = true, id = 11)),
            performed(
                2,
                90,
                set(completed = true, id = 20),
                set(completed = false, id = 21),
                set(completed = false, id = 22)
            )
        )
        val next = workout.nextSet()!!
        assertEquals(2L, next.exercise.id)
        assertEquals(21L, next.set.id)
        assertEquals(2, next.position)
        assertEquals(3, next.count)
    }

    @Test
    fun `a set left behind earlier comes first`() {
        val workout = workout(
            performed(1, 90, set(completed = false, id = 10)),
            performed(2, 90, set(completed = true, id = 20))
        )
        assertEquals(10L, workout.nextSet()!!.set.id)
    }

    @Test
    fun `no next set when everything is validated or the workout is empty`() {
        assertNull(workout(performed(1, 90, set(completed = true, id = 10))).nextSet())
        assertNull(workout().nextSet())
        assertNull(workout(performed(1, 90)).nextSet())
    }
}
