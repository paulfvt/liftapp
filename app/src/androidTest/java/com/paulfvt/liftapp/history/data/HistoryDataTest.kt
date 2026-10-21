package com.paulfvt.liftapp.history.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.PersonalRecordType
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.exercise.domain.ExerciseDraft
import com.paulfvt.liftapp.history.domain.removeExercise
import com.paulfvt.liftapp.history.domain.updateSetValues
import com.paulfvt.liftapp.progress.data.RecordRepository
import com.paulfvt.liftapp.progress.domain.PersonalRecord
import com.paulfvt.liftapp.template.data.TemplateRepository
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private class TestClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = now
}

@RunWith(AndroidJUnit4::class)
class HistoryDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = TestClock(Instant.parse("2026-10-01T18:00:00Z"))
    private lateinit var database: LiftDatabase
    private lateinit var exercises: ExerciseRepository
    private lateinit var records: RecordRepository
    private lateinit var workouts: WorkoutRepository
    private lateinit var history: HistoryRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, LiftDatabase::class.java).build()
        exercises = ExerciseRepository(database)
        records = RecordRepository(database, exercises)
        workouts = WorkoutRepository(database, TemplateRepository(database), records, clock)
        history = HistoryRepository(database, records)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun newExercise(name: String): Long =
        exercises.createCustom(ExerciseDraft(name = name, primaryMuscles = setOf(Muscle.CHEST)))

    // Séance libre d'un exercice, une série par couple (poids, reps), terminée ; renvoie son identifiant.
    private suspend fun doWorkout(exerciseId: Long, vararg values: Pair<Double, Int>): Long {
        val workoutId = workouts.startEmpty("Séance")
        workouts.addExercises(workoutId, listOf(exerciseId))
        val exercise = requireNotNull(workouts.observeInProgress().first()).exercises.single()
        repeat(values.size - exercise.sets.size) { workouts.addSet(exercise.id) }
        val sets = requireNotNull(workouts.observeInProgress().first()).exercises.single().sets
        sets.zip(values) { set, (weight, reps) ->
            workouts.updateSetWeight(set.id, weight)
            workouts.updateSetReps(set.id, reps)
            workouts.setCompleted(set.id, true)
        }
        workouts.finish(workoutId)
        clock.now = clock.now.plusSeconds(86_400)
        return workoutId
    }

    private suspend fun recordsOf(exerciseId: Long): List<PersonalRecord> = records.observeRecords(exerciseId).first()

    private fun List<PersonalRecord>.value(type: PersonalRecordType): Double? = firstOrNull { it.type == type }?.value

    @Test
    fun recordsAreComputedWhenAWorkoutFinishes() = runTest {
        val bench = newExercise("Développé couché")
        doWorkout(bench, 80.0 to 8, 85.0 to 5)

        val recorded = recordsOf(bench)
        assertEquals(85.0, recorded.value(PersonalRecordType.MAX_WEIGHT)!!, 0.0)
        assertEquals(640.0, recorded.value(PersonalRecordType.MAX_SET_VOLUME)!!, 0.0)
        assertEquals(2, recorded.count { it.type == PersonalRecordType.MAX_REPS_AT_WEIGHT })
    }

    @Test
    fun editingAnOldWorkoutRecomputesTheRecords() = runTest {
        val bench = newExercise("Développé couché")
        val old = doWorkout(bench, 100.0 to 5)
        doWorkout(bench, 90.0 to 5)
        assertEquals(100.0, recordsOf(bench).value(PersonalRecordType.MAX_WEIGHT)!!, 0.0)

        // Faute de frappe corrigée dans l'ancienne séance : 100 kg étaient en fait 80 kg.
        val edited = requireNotNull(history.getWorkout(old))
        val setId = edited.exercises.single().sets.single().id
        history.save(edited.updateSetValues(setId) { it.copy(weightKg = 80.0) })

        val recorded = recordsOf(bench)
        assertEquals(90.0, recorded.value(PersonalRecordType.MAX_WEIGHT)!!, 0.0)
        assertTrue(recorded.none { it.type == PersonalRecordType.MAX_REPS_AT_WEIGHT && it.weightKg == 100.0 })
        assertEquals(80.0, history.getWorkout(old)!!.exercises.single().sets.single().values.weightKg!!, 0.0)
    }

    @Test
    fun removingAnExerciseFromAnOldWorkoutRemovesItsRecords() = runTest {
        val bench = newExercise("Développé couché")
        val dips = newExercise("Dips")
        val workoutId = doWorkout(bench, 80.0 to 8)
        val withDips = requireNotNull(history.getWorkout(workoutId))
        history.save(
            withDips.copy(
                exercises = withDips.exercises + withDips.exercises.single().copy(
                    id = -1,
                    exercise = exercises.getExercises(listOf(dips)).single()
                )
            )
        )
        assertTrue(recordsOf(dips).isNotEmpty())

        val saved = requireNotNull(history.getWorkout(workoutId))
        history.save(saved.removeExercise(saved.exercises.last().id))

        assertTrue(recordsOf(dips).isEmpty())
        assertTrue(recordsOf(bench).isNotEmpty())
    }

    @Test
    fun deletingAWorkoutRecomputesTheRecordsAndRemovesItFromHistory() = runTest {
        val bench = newExercise("Développé couché")
        val best = doWorkout(bench, 100.0 to 5)
        doWorkout(bench, 90.0 to 5)

        history.delete(best)

        assertEquals(90.0, recordsOf(bench).value(PersonalRecordType.MAX_WEIGHT)!!, 0.0)
        assertNull(history.getWorkout(best))
        assertEquals(1, history.observeFinished().first().size)
    }

    @Test
    fun historyListsFinishedWorkoutsNewestFirstWithoutDiscardedOnes() = runTest {
        val bench = newExercise("Développé couché")
        val first = doWorkout(bench, 80.0 to 8)
        val second = doWorkout(bench, 82.5 to 8)
        val discarded = workouts.startEmpty("Abandonnée")
        workouts.discard(discarded)

        assertEquals(listOf(second, first), history.observeFinished().first().map { it.id })
    }

    @Test
    fun performedExercisesShowTheirLastDateAndBestRecord() = runTest {
        val bench = newExercise("Développé couché")
        val squat = newExercise("Squat")
        doWorkout(bench, 100.0 to 1)
        doWorkout(squat, 120.0 to 5)

        val performed = records.observePerformedExercises().first()

        assertEquals(listOf(squat, bench), performed.map { it.exercise.id })
        assertEquals(100.0, performed.last().headline!!.value, 0.0)
        assertEquals(1, performed.first().workoutCount)
    }
}
