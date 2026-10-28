package com.paulfvt.liftapp.progress.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.exercise.domain.ExerciseDraft
import com.paulfvt.liftapp.history.data.HistoryRepository
import com.paulfvt.liftapp.history.domain.updateSetValues
import com.paulfvt.liftapp.progress.domain.Badge
import com.paulfvt.liftapp.progress.domain.ExerciseMetric
import com.paulfvt.liftapp.progress.domain.GOAL_SINCE_ALWAYS
import com.paulfvt.liftapp.progress.domain.GoalTier
import com.paulfvt.liftapp.progress.domain.GoalTiers
import com.paulfvt.liftapp.progress.domain.weekOf
import com.paulfvt.liftapp.template.data.TemplateRepository
import com.paulfvt.liftapp.workout.data.FinishedWorkout
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private class TestClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = now
}

// Motivation de bout en bout sur une vraie base Room (en mémoire) : fin de séance, paliers, badges, objectif hebdo.
@RunWith(AndroidJUnit4::class)
class MotivationDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = TestClock(Instant.parse("2026-10-05T18:00:00Z"))
    private lateinit var database: LiftDatabase
    private lateinit var exercises: ExerciseRepository
    private lateinit var workouts: WorkoutRepository
    private lateinit var history: HistoryRepository
    private lateinit var goals: GoalRepository
    private lateinit var badges: BadgeRepository
    private lateinit var weeklyGoals: WeeklyGoalRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, LiftDatabase::class.java).build()
        exercises = ExerciseRepository(database)
        val records = RecordRepository(database, exercises)
        goals = GoalRepository(database, exercises, clock)
        badges = BadgeRepository(database, exercises)
        val achievements = AchievementRepository(goals, badges)
        workouts = WorkoutRepository(database, TemplateRepository(database), records, achievements, clock)
        history = HistoryRepository(database, records, achievements)
        weeklyGoals = WeeklyGoalRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun newExercise(name: String): Long =
        exercises.createCustom(ExerciseDraft(name = name, primaryMuscles = setOf(Muscle.CHEST)))

    // Séance libre d'un exercice, une série par couple (poids, reps), terminée le jour de l'horloge.
    private suspend fun doWorkout(exerciseId: Long, vararg values: Pair<Double, Int>): Pair<Long, FinishedWorkout> {
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
        val finished = workouts.finish(workoutId)
        clock.now = clock.now.plusSeconds(86_400)
        return workoutId to finished
    }

    @Test
    fun finishingAWorkoutReportsBrokenRecordsAndTheFirstBadge() = runTest {
        val bench = newExercise("Développé couché")
        val (_, first) = doWorkout(bench, 80.0 to 8)
        val (_, second) = doWorkout(bench, 85.0 to 8)

        assertTrue(first.records.isEmpty())
        assertEquals(listOf(Badge.WORKOUTS_1), first.achievements.badges)
        assertEquals(
            setOf(ExerciseMetric.MAX_WEIGHT, ExerciseMetric.ESTIMATED_1RM, ExerciseMetric.VOLUME),
            second.records.map { it.recordBreak.metric }.toSet()
        )
        assertTrue(second.achievements.badges.isEmpty())
    }

    @Test
    fun aReachedTierStaysReachedAfterTheWorkoutIsCorrected() = runTest {
        val squat = newExercise("Squat")
        doWorkout(squat, 100.0 to 1)
        goals.create(squat, GoalTiers(bronzeKg = 102.5, silverKg = 105.0, goldKg = 110.0))

        val (workoutId, finished) = doWorkout(squat, 103.0 to 1)
        assertEquals(listOf(GoalTier.BRONZE), finished.achievements.tiers.map { it.tier })

        // Faute de frappe corrigée : 93 kg et non 103. Le bronze reste acquis.
        val edited = requireNotNull(history.getWorkout(workoutId))
        val setId = edited.exercises.single().sets.single().id
        history.save(edited.updateSetValues(setId) { it.copy(weightKg = 93.0) })

        val goal = goals.observeGoals().first().single()
        assertEquals(setOf(GoalTier.BRONZE), goal.reachedAt.keys)
    }

    @Test
    fun badgesAreUnlockedOnceWithTheirHistoricalDate() = runTest {
        val row = newExercise("Rowing")
        val firstStart = clock.now
        doWorkout(row, 60.0 to 10)

        // Déjà débloqué en fin de séance : une nouvelle vérification ne trouve rien de neuf.
        assertTrue(badges.refresh().isEmpty())
        assertEquals(firstStart, badges.observeUnlocked().first()[Badge.WORKOUTS_1])
    }

    @Test
    fun aSecondGoalChangeInTheSameWeekReplacesTheFirst() = runTest {
        weeklyGoals.setInitialGoal(3)
        weeklyGoals.changeGoal(4)
        weeklyGoals.changeGoal(2)

        val saved = weeklyGoals.getGoals()
        assertEquals(listOf(3, 2), saved.map { it.goal })
        assertEquals(GOAL_SINCE_ALWAYS, saved.first().effectiveFrom)
        assertEquals(weekOf(clock.now, ZoneId.systemDefault()), saved.last().effectiveFrom)
    }
}
