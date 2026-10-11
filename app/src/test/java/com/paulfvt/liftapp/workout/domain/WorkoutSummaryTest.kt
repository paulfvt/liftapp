package com.paulfvt.liftapp.workout.domain

import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.SetType.DROP
import com.paulfvt.liftapp.core.model.SetType.WARMUP
import com.paulfvt.liftapp.core.model.SetType.WORK
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.template.domain.TemplateDraft
import com.paulfvt.liftapp.template.domain.TemplateExerciseDraft
import com.paulfvt.liftapp.template.domain.TemplateSetDraft
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSummaryTest {
    private val start = Instant.parse("2026-10-11T18:00:00Z")

    private fun workout(vararg exercises: WorkoutExercise) =
        Workout(id = 1, templateId = 7, name = "Push A", startedAt = start, note = null, exercises = exercises.toList())

    private fun performed(id: Long, vararg sets: WorkoutSet, tracking: TrackingType = TrackingType.WEIGHT_REPS) =
        WorkoutExercise(id = id, exercise = exercise(id, tracking), restSeconds = 120, sets = sets.toList())

    @Test
    fun `warm-ups are excluded from tonnage and set count, drop sets are included`() {
        val summary = summarize(
            workout(performed(1, set(WARMUP, 40.0, 10), set(WORK, 80.0, 8), set(DROP, 60.0, 10))),
            start.plusSeconds(3600)
        )
        assertEquals(80.0 * 8 + 60.0 * 10, summary.tonnageKg, 0.0)
        assertEquals(2, summary.completedSets)
        assertEquals(Duration.ofHours(1), summary.duration)
    }

    @Test
    fun `a set with zero reps counts as a set but adds no tonnage`() {
        val summary = summarize(workout(performed(1, set(WORK, 100.0, 0))), start)
        assertEquals(0.0, summary.tonnageKg, 0.0)
        assertEquals(1, summary.completedSets)
    }

    @Test
    fun `sets that were not validated are ignored`() {
        val summary = summarize(workout(performed(1, set(WORK, 80.0, 8, completed = false))), start)
        assertEquals(0, summary.completedSets)
        assertEquals(0, summary.exerciseCount)
    }

    @Test
    fun `bodyweight counts only the added load, never an assistance`() {
        val summary = summarize(
            workout(
                performed(1, set(WORK, 10.0, 8), tracking = TrackingType.REPS),
                performed(2, set(WORK, -20.0, 8), set(WORK, null, 12), tracking = TrackingType.REPS)
            ),
            start
        )
        assertEquals(80.0, summary.tonnageKg, 0.0)
        assertEquals(3, summary.completedSets)
        assertEquals(2, summary.exerciseCount)
    }

    @Test
    fun `incomplete and completed sets are counted across exercises`() {
        val workout =
            workout(performed(1, set(completed = true), set(completed = false)), performed(2, set(completed = false)))
        assertEquals(2, workout.incompleteSetCount())
        assertEquals(1, workout.completedSetCount())
    }

    private fun template(vararg exercises: Pair<Long, List<SetType>>) = TemplateDraft(
        name = "Push A",
        exercises = exercises.map { (id, types) ->
            TemplateExerciseDraft(exercise(id), types.map { TemplateSetDraft(it, targetReps = 8) })
        }
    )

    @Test
    fun `the template is unchanged when exercises and set types are the same, whatever the values`() {
        val workout = workout(performed(1, set(WARMUP, 40.0, 12), set(WORK, 85.0, 6, completed = false)))
        assertFalse(workout.differsFrom(template(1L to listOf(WARMUP, WORK))))
    }

    @Test
    fun `an added set, an added exercise or a new order changes the template`() {
        val template = template(1L to listOf(WORK), 2L to listOf(WORK))
        assertTrue(workout(performed(1, set(), set()), performed(2, set())).differsFrom(template))
        assertTrue(workout(performed(1, set()), performed(2, set()), performed(3, set())).differsFrom(template))
        assertTrue(workout(performed(2, set()), performed(1, set())).differsFrom(template))
    }

    @Test
    fun `the updated template takes the workout structure, its targets and its rest`() {
        val draft = workout(performed(1, set(WARMUP, targetReps = 12), set(WORK))).toTemplateDraft("Push A")
        assertEquals("Push A", draft.name)
        assertEquals(listOf(TemplateSetDraft(WARMUP, 12), TemplateSetDraft(WORK, null)), draft.exercises.single().sets)
        assertEquals(120, draft.exercises.single().restSeconds)
    }
}
