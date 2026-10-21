package com.paulfvt.liftapp.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.paulfvt.liftapp.core.database.migration.MIGRATION_1_2
import com.paulfvt.liftapp.core.database.migration.MIGRATION_2_3
import com.paulfvt.liftapp.core.database.migration.MIGRATION_3_4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Chaque migration est testée sur une vraie base créée avec l'ancien schéma (app/schemas/.../1.json),
// puis comparée au nouveau schéma attendu par Room (2.json) : une colonne ou un index oublié fait échouer le test.
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), LiftDatabase::class.java)

    @Test
    fun migration1To2TurnsSetCountIntoDetailedWorkSets() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO exercise (id, catalogKey, name, equipment, category, trackingType, instructions, " +
                    "imageCount, isCustom, isHidden, note) VALUES (1, NULL, 'Dips', 'BODYWEIGHT', 'BODYWEIGHT', " +
                    "'REPS', NULL, 0, 1, 0, NULL), (2, NULL, 'Squat', 'BARBELL', 'FREE_WEIGHT', 'WEIGHT_REPS', " +
                    "NULL, 0, 1, 0, NULL)"
            )
            db.execSQL("INSERT INTO workout_template (id, folderId, name, sortOrder) VALUES (10, NULL, 'Push A', 0)")
            // Version 1 : 4 séries de 8 sur les dips, 2 séries sans cible sur le squat.
            db.execSQL(
                "INSERT INTO template_exercise (id, templateId, exerciseId, sortOrder, setCount, targetReps, " +
                    "restSeconds) VALUES (100, 10, 1, 0, 4, 8, 90), (101, 10, 2, 1, 2, NULL, 120)"
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        val exerciseSql = "SELECT id, templateId, exerciseId, sortOrder, restSeconds FROM template_exercise ORDER BY id"
        db.query(exerciseSql).use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToFirst()
            assertEquals(listOf(100L, 10L, 1L, 0L, 90L), (0..4).map { cursor.getLong(it) })
        }
        val setSql = "SELECT templateExerciseId, sortOrder, type, targetReps FROM template_set " +
            "ORDER BY templateExerciseId, sortOrder"
        val sets = db.query(setSql).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val targetReps = if (cursor.isNull(3)) null else cursor.getInt(3)
                    add(listOf(cursor.getLong(0), cursor.getInt(1), cursor.getString(2), targetReps))
                }
            }
        }
        assertEquals(
            listOf(
                listOf(100L, 0, "WORK", 8),
                listOf(100L, 1, "WORK", 8),
                listOf(100L, 2, "WORK", 8),
                listOf(100L, 3, "WORK", 8),
                listOf(101L, 0, "WORK", null),
                listOf(101L, 1, "WORK", null)
            ),
            sets
        )
        // Les anciennes colonnes ont bien disparu.
        db.query("SELECT * FROM template_exercise LIMIT 0").use { cursor ->
            assertFalse("setCount" in cursor.columnNames)
            assertFalse("targetReps" in cursor.columnNames)
        }
    }

    @Test
    fun migration2To3KeepsWorkoutsAndAddsEmptyRestAndTargets() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO exercise (id, catalogKey, name, equipment, category, trackingType, instructions, " +
                    "imageCount, isCustom, isHidden, note) VALUES (1, NULL, 'Squat', 'BARBELL', 'FREE_WEIGHT', " +
                    "'WEIGHT_REPS', NULL, 0, 1, 0, NULL)"
            )
            db.execSQL(
                "INSERT INTO workout (id, templateId, name, startedAt, endedAt, status, note) " +
                    "VALUES (5, NULL, 'Legs', 0, 3600000, 'FINISHED', NULL)"
            )
            db.execSQL(
                "INSERT INTO workout_exercise (id, workoutId, exerciseId, sortOrder, note) VALUES (50, 5, 1, 0, NULL)"
            )
            db.execSQL(
                "INSERT INTO workout_set (id, workoutExerciseId, sortOrder, type, weightKg, reps, durationSec, " +
                    "distanceM, rpe, isCompleted, completedAt) VALUES (500, 50, 0, 'WORK', 100.0, 5, NULL, NULL, 8.5, 1, 60000)"
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        db.query("SELECT restSeconds FROM workout_exercise WHERE id = 50").use { cursor ->
            cursor.moveToFirst()
            assertTrue(cursor.isNull(0))
        }
        db.query("SELECT weightKg, reps, rpe, targetReps FROM workout_set WHERE id = 500").use { cursor ->
            cursor.moveToFirst()
            assertEquals(100.0, cursor.getDouble(0), 0.0)
            assertEquals(5, cursor.getInt(1))
            assertEquals(8.5, cursor.getDouble(2), 0.0)
            assertTrue(cursor.isNull(3))
        }
    }

    @Test
    fun migration3To4KeepsTheWorkoutInProgressWithoutRest() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO workout (id, templateId, name, startedAt, endedAt, status, note) " +
                    "VALUES (7, NULL, 'Push', 1000, NULL, 'IN_PROGRESS', 'Épaule')"
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        db.query("SELECT name, startedAt, status, note, restStartedAt, restEndsAt FROM workout WHERE id = 7").use {
            it.moveToFirst()
            assertEquals("Push", it.getString(0))
            assertEquals(1000L, it.getLong(1))
            assertEquals("IN_PROGRESS", it.getString(2))
            assertEquals("Épaule", it.getString(3))
            assertTrue(it.isNull(4))
            assertTrue(it.isNull(5))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
