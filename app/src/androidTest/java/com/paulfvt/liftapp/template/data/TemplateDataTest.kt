package com.paulfvt.liftapp.template.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.exercise.domain.ExerciseDraft
import com.paulfvt.liftapp.template.domain.TemplateDraft
import com.paulfvt.liftapp.template.domain.addExercises
import com.paulfvt.liftapp.template.domain.addSet
import com.paulfvt.liftapp.template.domain.updateExercise
import com.paulfvt.liftapp.template.domain.updateSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Tests sur le téléphone, avec une vraie base SQLite en mémoire : ils vérifient aussi les clés étrangères.
@RunWith(AndroidJUnit4::class)
class TemplateDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: LiftDatabase
    private lateinit var exercises: ExerciseRepository
    private lateinit var templates: TemplateRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, LiftDatabase::class.java).build()
        exercises = ExerciseRepository(database)
        templates = TemplateRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun draft(name: String, vararg exerciseNames: String): TemplateDraft {
        val ids = exerciseNames.map {
            exercises.createCustom(ExerciseDraft(name = it, primaryMuscles = setOf(Muscle.CHEST)))
        }
        return TemplateDraft(name = name).addExercises(exercises.getExercises(ids))
    }

    @Test
    fun savedTemplateIsReadBackWithItsSetsAndOrder() = runTest {
        // Dips : échauffement de 12, puis 10 et 8 reps, plus une série ajoutée sans cible.
        val saved = draft("Push A", "Développé couché", "Dips").let { draft ->
            draft.updateExercise(draft.exercises[1].exercise.id) {
                it.updateSet(0) { set -> set.copy(type = SetType.WARMUP, targetReps = 12) }
                    .updateSet(1) { set -> set.copy(targetReps = 10) }
                    .updateSet(2) { set -> set.copy(targetReps = 8) }
                    .addSet()
                    .copy(restSeconds = 90)
            }
        }
        val id = templates.createTemplate(folderId = null, draft = saved)

        val loaded = templates.getDraft(id)

        assertEquals(saved, loaded)
        assertEquals(
            listOf(SetType.WARMUP, SetType.WORK, SetType.WORK, SetType.WORK),
            loaded?.exercises?.get(1)?.sets?.map {
                it.type
            }
        )
        assertEquals(
            listOf("Développé couché", "Dips"),
            templates.observeLibrary().first().unfiled.single().exerciseNames
        )
    }

    @Test
    fun deletingFolderMovesItsTemplatesAfterUnfiledOnes() = runTest {
        val folder = templates.createFolder("Programme hiver")
        val unfiled = templates.createTemplate(null, draft("Full body"))
        val legs = templates.createTemplate(folder, draft("Legs"))
        val pull = templates.createTemplate(folder, draft("Pull"))

        templates.deleteFolder(folder)

        val library = templates.observeLibrary().first()
        assertTrue(library.folders.isEmpty())
        assertEquals(listOf(unfiled, legs, pull), library.unfiled.map { it.id })
    }

    @Test
    fun deletingTemplateKeepsPastWorkoutsWithoutTheLink() = runTest {
        val id = templates.createTemplate(null, draft("Push A", "Développé couché"))
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO workout (templateId, name, startedAt, endedAt, status, note) " +
                "VALUES ($id, 'Push A', 0, 3600000, 'FINISHED', NULL)"
        )

        templates.deleteTemplate(id)

        database.query("SELECT templateId, name FROM workout", null).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
            assertEquals("Push A", cursor.getString(1))
        }
        database.query("SELECT COUNT(*) FROM template_exercise", null).use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        assertNull(templates.getDraft(id))
    }

    @Test
    fun duplicateIsPlacedRightAfterTheOriginalWithTheSameExercises() = runTest {
        val folder = templates.createFolder("PPL")
        val push = templates.createTemplate(folder, draft("Push", "Développé couché", "Dips"))
        val pull = templates.createTemplate(folder, draft("Pull"))

        val copy = templates.duplicateTemplate(push, "Copie de Push")

        val section = templates.observeLibrary().first().folders.single()
        assertEquals(listOf(push, copy, pull), section.templates.map { it.id })
        assertEquals(templates.getDraft(push)?.exercises, templates.getDraft(copy)?.exercises)
        assertEquals("Copie de Push", templates.getDraft(copy)?.name)
    }

    @Test
    fun movedTemplateGoesToTheEndOfItsNewFolder() = runTest {
        val folder = templates.createFolder("PPL")
        val legs = templates.createTemplate(folder, draft("Legs"))
        val push = templates.createTemplate(null, draft("Push"))

        templates.moveTemplateToFolder(push, folder)

        val library = templates.observeLibrary().first()
        assertTrue(library.unfiled.isEmpty())
        assertEquals(listOf(legs, push), library.folders.single().templates.map { it.id })
    }
}
