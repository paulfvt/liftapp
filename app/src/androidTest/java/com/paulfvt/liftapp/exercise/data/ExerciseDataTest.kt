package com.paulfvt.liftapp.exercise.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.exercise.domain.ExerciseDraft
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Tests sur le téléphone, avec une vraie base SQLite en mémoire (effacée après chaque test).
@RunWith(AndroidJUnit4::class)
class ExerciseDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: LiftDatabase
    private lateinit var importer: CatalogImporter
    private lateinit var repository: ExerciseRepository
    private lateinit var preferencesFile: File

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, LiftDatabase::class.java).build()
        preferencesFile = File(context.cacheDir, "test-${System.nanoTime()}.preferences_pb")
        val preferences = PreferenceDataStoreFactory.create { preferencesFile }
        importer = CatalogImporter(context, database, preferences)
        repository = ExerciseRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
        preferencesFile.delete()
    }

    private fun dto(key: String, name: String, primary: List<Muscle> = listOf(Muscle.CHEST)) = CatalogExerciseDto(
        key = key,
        name = name,
        equipment = Equipment.BARBELL,
        category = ExerciseCategory.FREE_WEIGHT,
        trackingType = TrackingType.WEIGHT_REPS,
        primaryMuscles = primary,
        secondaryMuscles = emptyList(),
        instructions = listOf("Étape 1"),
        imageCount = 2
    )

    @Test
    fun embeddedCatalogIsCompleteAndImagesExist() = runTest {
        importer.importIfNeeded()
        val exercises = repository.observeExercises().first()
        assertEquals(753, exercises.size)
        exercises.forEach { exercise ->
            assertTrue("${exercise.name} sans muscle principal", exercise.primaryMuscles.isNotEmpty())
            repeat(exercise.imageCount) { index ->
                context.assets.open("catalog/images/${exercise.catalogKey}/$index.webp").close()
            }
        }
    }

    @Test
    fun importIsSkippedWhenCatalogVersionIsAlreadyImported() = runTest {
        importer.importIfNeeded()
        val first = repository.observeExercises().first().first()
        repository.updateNote(first.id, "Ma note")
        importer.importIfNeeded()
        assertEquals("Ma note", repository.observeExercise(first.id).first()?.note)
    }

    @Test
    fun catalogUpdateKeepsNoteAndHiddenStateAndReplacesMuscles() = runTest {
        importer.import(CatalogDto(1, "test", listOf(dto("Bench", "Développé couché"))))
        val id = repository.observeExercises().first().single().id
        repository.updateNote(id, "Banc cran 2")
        repository.setHidden(id, true)

        importer.import(CatalogDto(2, "test", listOf(dto("Bench", "Développé couché barre", listOf(Muscle.TRICEPS)))))

        val updated = repository.observeExercise(id).first()!!
        assertEquals("Développé couché barre", updated.name)
        assertEquals("Banc cran 2", updated.note)
        assertTrue(updated.isHidden)
        assertEquals(listOf(Muscle.TRICEPS), updated.primaryMuscles)
    }

    @Test
    fun exercisesAreSortedInFrenchAlphabeticalOrder() = runTest {
        importer.import(
            CatalogDto(1, "test", listOf(dto("A", "Fente"), dto("B", "Écarté"), dto("C", "Développé")))
        )
        assertEquals(listOf("Développé", "Écarté", "Fente"), repository.observeExercises().first().map { it.name })
    }

    @Test
    fun customExerciseCanBeCreatedEditedAndHidden() = runTest {
        val draft = ExerciseDraft(name = "  Hip thrust machine ", primaryMuscles = setOf(Muscle.GLUTES))
        val id = repository.createCustom(draft)
        repository.updateCustom(id, draft.copy(name = "Hip thrust", secondaryMuscles = setOf(Muscle.HAMSTRINGS)))
        repository.setHidden(id, true)

        val exercise = repository.observeExercise(id).first()!!
        assertEquals("Hip thrust", exercise.name)
        assertTrue(exercise.isCustom)
        assertTrue(exercise.isHidden)
        assertEquals(listOf(Muscle.HAMSTRINGS), exercise.secondaryMuscles)
    }

    @Test
    fun blankNoteIsStoredAsNoNote() = runTest {
        val id = repository.createCustom(ExerciseDraft(name = "Test", primaryMuscles = setOf(Muscle.BICEPS)))
        repository.updateNote(id, "   ")
        assertEquals(null, repository.observeExercise(id).first()?.note)
    }
}
