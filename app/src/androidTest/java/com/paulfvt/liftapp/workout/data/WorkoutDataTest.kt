package com.paulfvt.liftapp.workout.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.migration.MIGRATION_1_2
import com.paulfvt.liftapp.core.database.migration.MIGRATION_2_3
import com.paulfvt.liftapp.core.database.migration.MIGRATION_3_4
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.exercise.domain.ExerciseDraft
import com.paulfvt.liftapp.progress.data.RecordRepository
import com.paulfvt.liftapp.template.data.TemplateRepository
import com.paulfvt.liftapp.template.domain.TemplateDraft
import com.paulfvt.liftapp.template.domain.addExercises
import com.paulfvt.liftapp.template.domain.updateExercise
import com.paulfvt.liftapp.template.domain.updateSet
import com.paulfvt.liftapp.workout.domain.RestPeriod
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.stepReps
import com.paulfvt.liftapp.workout.domain.stepWeight
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Horloge réglable : chaque séance de test peut commencer à une heure différente.
private class TestClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId?): Clock = this

    override fun instant(): Instant = now
}

@RunWith(AndroidJUnit4::class)
class WorkoutDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = TestClock(Instant.parse("2026-10-01T18:00:00Z"))
    private lateinit var database: LiftDatabase
    private lateinit var exercises: ExerciseRepository
    private lateinit var templates: TemplateRepository
    private lateinit var workouts: WorkoutRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, LiftDatabase::class.java).build()
        createRepositories()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createRepositories() {
        exercises = ExerciseRepository(database)
        templates = TemplateRepository(database)
        workouts = WorkoutRepository(database, templates, RecordRepository(database, exercises), clock)
    }

    private suspend fun newExercise(name: String): Long =
        exercises.createCustom(ExerciseDraft(name = name, primaryMuscles = setOf(Muscle.CHEST)))

    // Template d'un exercice : 1 échauffement de 12 puis 2 séries de travail à 8.
    private suspend fun template(name: String, exerciseId: Long): Long {
        val draft = TemplateDraft(name = name).addExercises(exercises.getExercises(listOf(exerciseId))).let { draft ->
            draft.updateExercise(exerciseId) {
                it.updateSet(0) { set -> set.copy(type = SetType.WARMUP, targetReps = 12) }
                    .updateSet(1) { set -> set.copy(targetReps = 8) }
                    .updateSet(2) { set -> set.copy(targetReps = 8) }
            }
        }
        return templates.createTemplate(null, draft)
    }

    private suspend fun inProgress(): Workout = requireNotNull(workouts.observeInProgress().first())

    // Fait une séance complète : chaque série reçoit le poids et les reps donnés, puis est validée.
    private suspend fun doWorkout(templateId: Long, vararg values: Pair<Double, Int>) {
        workouts.startFromTemplate(templateId)
        inProgress().exercises.single().sets.zip(values) { set, (weight, reps) ->
            workouts.updateSetWeight(set.id, weight)
            workouts.updateSetReps(set.id, reps)
            workouts.setCompleted(set.id, true)
        }
        workouts.finish(inProgress().id)
        clock.now = clock.now.plusSeconds(86_400)
    }

    @Test
    fun startingFromTemplateKeepsTypesAndTargetsAndPrefillsFromLastTime() = runTest {
        val bench = newExercise("Développé couché")
        val push = template("Push A", bench)
        doWorkout(push, 40.0 to 12, 80.0 to 8, 80.0 to 7)

        workouts.startFromTemplate(push)

        val sets = inProgress().exercises.single().sets
        assertEquals(listOf(SetType.WARMUP, SetType.WORK, SetType.WORK), sets.map { it.type })
        assertEquals(listOf(12, 8, 8), sets.map { it.targetReps })
        assertEquals(SetValues(weightKg = 80.0, reps = 7), sets[2].values)
        assertTrue(sets.none { it.isCompleted })
    }

    @Test
    fun lastTimeComesFromTheSameTemplateFirstThenFromAnyTemplate() = runTest {
        val bench = newExercise("Développé couché")
        val push = template("Push A", bench)
        val heavy = template("Push lourd", bench)
        doWorkout(push, 40.0 to 12, 80.0 to 8, 80.0 to 8)
        doWorkout(heavy, 50.0 to 10, 100.0 to 3, 100.0 to 3)

        assertEquals(80.0, workouts.previousSets(bench, push)[1].values.weightKg!!, 0.0)
        assertEquals(100.0, workouts.previousSets(bench, null)[1].values.weightKg!!, 0.0)
        val other = newExercise("Dips")
        assertTrue(workouts.previousSets(other, push).isEmpty())
    }

    @Test
    fun finishingRemovesUnvalidatedSetsAndProposesTheChangedTemplate() = runTest {
        val bench = newExercise("Développé couché")
        val dips = newExercise("Dips")
        val push = template("Push A", bench)
        workouts.startFromTemplate(push)
        val workoutId = inProgress().id
        val benchSets = inProgress().exercises.single().sets
        workouts.updateSetReps(benchSets[1].id, 8)
        workouts.updateSetWeight(benchSets[1].id, 80.0)
        workouts.setCompleted(benchSets[1].id, true)
        workouts.addExercises(workoutId, listOf(dips))

        val finished = workouts.finish(workoutId)

        assertEquals(1, finished.summary.completedSets)
        assertEquals(640.0, finished.summary.tonnageKg, 0.0)
        assertEquals(listOf(bench, dips), finished.templateUpdate?.exercises?.map { it.exercise.id })
        assertNull(workouts.observeInProgress().first())
        // Les dips, sans série validée, ont disparu de la séance terminée : ils ne servent pas de « Précédent ».
        assertTrue(workouts.previousSets(dips, null).isEmpty())
        assertEquals(1, workouts.previousSets(bench, push).size)
    }

    @Test
    fun aDiscardedWorkoutIsNeverUsedAsLastTime() = runTest {
        val bench = newExercise("Développé couché")
        val push = template("Push A", bench)
        workouts.startFromTemplate(push)
        val set = inProgress().exercises.single().sets.first()
        workouts.updateSetReps(set.id, 10)
        workouts.setCompleted(set.id, true)

        workouts.discard(inProgress().id)

        assertNull(workouts.observeInProgress().first())
        assertTrue(workouts.previousSets(bench, push).isEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun onlyOneWorkoutCanBeInProgress() = runTest {
        workouts.startEmpty("Séance libre")
        workouts.startEmpty("Séance libre")
    }

    @Test
    fun addedSetIsAnUntargetedWorkSetAndReplacingKeepsThePlannedSets() = runTest {
        val bench = newExercise("Développé couché")
        val incline = newExercise("Développé incliné")
        workouts.startFromTemplate(template("Push A", bench))
        val exercise = inProgress().exercises.single()

        workouts.addSet(exercise.id)
        workouts.replaceExercise(exercise.id, incline)

        val replaced = inProgress().exercises.single()
        assertEquals(incline, replaced.exercise.id)
        assertEquals(listOf(SetType.WARMUP, SetType.WORK, SetType.WORK, SetType.WORK), replaced.sets.map { it.type })
        assertEquals(listOf(12, 8, 8, null), replaced.sets.map { it.targetReps })
    }

    @Test
    fun validatingASetStartsTheRestOfItsExerciseAndTheLastSetEndsIt() = runTest {
        val bench = newExercise("Développé couché")
        workouts.startFromTemplate(template("Push A", bench))
        val sets = inProgress().exercises.single().sets
        sets.forEach { workouts.updateSetReps(it.id, 8) }

        assertTrue(workouts.toggleCompleted(sets[0].id))
        assertEquals(RestPeriod(clock.now, clock.now.plusSeconds(120)), inProgress().rest)

        // Décocher une série ne touche pas au repos en cours.
        workouts.toggleCompleted(sets[0].id)
        assertNotNull(inProgress().rest)

        // Après la dernière série, il n'y a plus rien à attendre.
        sets.forEach { workouts.toggleCompleted(it.id) }
        assertTrue(inProgress().exercises.single().sets.all { it.isCompleted })
        assertNull(inProgress().rest)
    }

    @Test
    fun theNotificationActionOnlyValidatesASetWithValues() = runTest {
        val bench = newExercise("Développé couché")
        workouts.startFromTemplate(template("Push A", bench))
        val set = inProgress().exercises.single().sets.first()

        assertFalse(workouts.completeSet(set.id))
        assertNull(inProgress().rest)

        workouts.updateSetReps(set.id, 12)
        assertTrue(workouts.completeSet(set.id))
        // Un second appui (notification pas encore rafraîchie) ne décoche pas la série.
        assertTrue(workouts.completeSet(set.id))
        assertTrue(inProgress().exercises.single().sets.first().isCompleted)
        assertNotNull(inProgress().rest)
    }

    @Test
    fun restCanBeExtendedShortenedSkippedAndIsClearedAtTheEnd() = runTest {
        val bench = newExercise("Développé couché")
        workouts.startFromTemplate(template("Push A", bench))
        val start = clock.now
        val set = inProgress().exercises.single().sets.first()
        workouts.updateSetReps(set.id, 12)
        workouts.toggleCompleted(set.id)

        workouts.adjustRest(30)
        assertEquals(RestPeriod(start, start.plusSeconds(150)), inProgress().rest)
        clock.now = start.plusSeconds(100)
        workouts.adjustRest(-15)
        assertEquals(RestPeriod(start, start.plusSeconds(135)), inProgress().rest)
        workouts.skipRest()
        assertNull(inProgress().rest)

        workouts.adjustRest(30)
        val workoutId = inProgress().id
        workouts.finish(workoutId)
        assertNull(database.workoutDao().getWorkout(workoutId)?.restEndsAt)
    }

    @Test
    fun gymModeStepsAddUpOnTheStoredValue() = runTest {
        val bench = newExercise("Développé couché")
        workouts.startFromTemplate(template("Push A", bench))
        val set = inProgress().exercises.single().sets.first()

        // Trois appuis rapides sur « + » : chacun part de la valeur en base, aucun n'est perdu.
        repeat(3) {
            workouts.changeSetValues(set.id) { it.copy(weightKg = stepWeight(it.weightKg, 1, allowNegative = false)) }
        }
        workouts.changeSetValues(set.id) { it.copy(reps = stepReps(it.reps, 1)) }

        assertEquals(SetValues(weightKg = 7.5, reps = 1), inProgress().exercises.single().sets.first().values)
    }

    @Test
    fun workoutInProgressIsResumedAfterTheAppIsRestarted() = runTest {
        // Base sur disque, pas en mémoire : on la ferme puis on la rouvre comme après un redémarrage.
        database.close()
        val name = "resume-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, LiftDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
        database = open()
        createRepositories()
        val bench = newExercise("Développé couché")
        workouts.startFromTemplate(template("Push A", bench))
        val before = inProgress()
        val firstSet = before.exercises.single().sets.first()
        workouts.updateSetWeight(firstSet.id, 42.5)
        workouts.updateSetReps(firstSet.id, 12)
        workouts.toggleCompleted(firstSet.id)
        workouts.updateNote(before.id, "Épaule gauche sensible")

        database.close()
        database = open()
        createRepositories()

        val after = workouts.observeInProgress().first()
        assertNotNull(after)
        assertEquals(before.id, after!!.id)
        assertEquals("Épaule gauche sensible", after.note)
        val resumedSet = after.exercises.single().sets.first()
        assertEquals(SetValues(weightKg = 42.5, reps = 12), resumedSet.values)
        assertTrue(resumedSet.isCompleted)
        // Le repos reprend avec la même heure de fin : le minuteur reste juste.
        assertEquals(RestPeriod(clock.now, clock.now.plusSeconds(120)), after.rest)
        database.close()
        context.deleteDatabase(name)
        database = Room.inMemoryDatabaseBuilder(context, LiftDatabase::class.java).build()
    }
}
