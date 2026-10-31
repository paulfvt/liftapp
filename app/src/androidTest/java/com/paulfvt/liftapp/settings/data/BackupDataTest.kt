package com.paulfvt.liftapp.settings.data

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.entity.ProgressPhotoEntity
import com.paulfvt.liftapp.core.database.entity.WorkoutEntity
import com.paulfvt.liftapp.core.model.PhotoPose
import com.paulfvt.liftapp.core.model.WorkoutStatus
import com.paulfvt.liftapp.profile.data.PHOTO_DIRECTORY
import com.paulfvt.liftapp.settings.domain.BackupLayout
import com.paulfvt.liftapp.settings.domain.BackupProblem
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Sauvegarde d'une vraie base sur disque puis vérification du ZIP obtenu, comme avant une restauration.
@RunWith(AndroidJUnit4::class)
class BackupDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val now = Instant.parse("2026-10-31T17:05:00Z")
    private val work = File(context.cacheDir, "backup-test")
    private val photo = File(File(context.filesDir, PHOTO_DIRECTORY), "backup-test.jpg")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: LiftDatabase
    private lateinit var preferences: DataStore<Preferences>
    private lateinit var backups: BackupRepository

    @Before
    fun setUp() {
        context.deleteDatabase(DATABASE_NAME)
        work.deleteRecursively()
        work.mkdirs()
        photo.parentFile?.mkdirs()
        photo.writeBytes(byteArrayOf(1, 2, 3))
        database = LiftDatabase.build(context, DATABASE_NAME)
        preferences = PreferenceDataStoreFactory.create(scope = scope) { File(work, "test.preferences_pb") }
        backups = BackupRepository(context, database, preferences, Clock.fixed(now, ZoneOffset.UTC))
    }

    @After
    fun tearDown() {
        database.close()
        scope.cancel()
        context.deleteDatabase(DATABASE_NAME)
        work.deleteRecursively()
        photo.delete()
        PendingRestore.stagingDir(context).deleteRecursively()
    }

    private suspend fun addData() {
        database.workoutDao().insertWorkout(
            WorkoutEntity(
                templateId = null,
                name = "Push",
                startedAt = now.minusSeconds(3_600),
                endedAt = now,
                status = WorkoutStatus.FINISHED,
                note = null
            )
        )
        database.photoDao().insert(ProgressPhotoEntity(filePath = photo.name, pose = PhotoPose.FRONT, takenAt = now))
        preferences.edit {
            it[stringPreferencesKey("settings_weight_unit")] = "LB"
            it[booleanPreferencesKey("workout_permissions_asked")] = true
        }
    }

    @Test
    fun backupWithPhotosIsReadBackWithItsContent() = runTest {
        addData()
        val target = File(work, "with-photos.zip")

        val created = backups.create(Uri.fromFile(target), includePhotos = true)
        val inspection = backups.inspect(Uri.fromFile(target))

        assertEquals(1, created.workoutCount)
        assertEquals(1, created.photoCount)
        val info = (inspection as BackupInspection.Valid).info
        assertEquals(now, info.createdAt)
        assertTrue(info.includesPhotos)
        assertEquals(1, info.workoutCount)
        assertEquals(1, info.photoCount)
        ZipFile(target).use { zip ->
            assertTrue(zip.getEntry(BackupLayout.PHOTOS + photo.name) != null)
            // Les préférences propres au téléphone ne partent pas dans la sauvegarde.
            val saved = zip.getInputStream(zip.getEntry(BackupLayout.PREFERENCES)).use { String(it.readBytes()) }
            assertTrue("settings_weight_unit" in saved)
            assertFalse("workout_permissions_asked" in saved)
        }
    }

    @Test
    fun backupWithoutPhotosLeavesNoPhotoBehind() = runTest {
        addData()
        val target = File(work, "without-photos.zip")

        backups.create(Uri.fromFile(target), includePhotos = false)
        val info = (backups.inspect(Uri.fromFile(target)) as BackupInspection.Valid).info

        assertFalse(info.includesPhotos)
        assertEquals(0, info.photoCount)
        assertEquals(1, info.workoutCount)
        ZipFile(target).use { zip ->
            assertTrue(zip.entries().toList().none { it.name.startsWith(BackupLayout.PHOTOS) })
        }
        // La base de l'app, elle, garde sa photo.
        assertEquals(1, database.photoDao().observeAll().first().size)
    }

    @Test
    fun zipWithoutManifestIsNotABackup() = runTest {
        val target = File(work, "other.zip")
        ZipOutputStream(target.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("notes.txt"))
            zip.write("bonjour".toByteArray())
            zip.closeEntry()
        }

        val inspection = backups.inspect(Uri.fromFile(target))

        assertEquals(BackupInspection.Invalid(BackupProblem.NOT_A_BACKUP), inspection)
        assertFalse(PendingRestore.stagingDir(context).exists())
    }

    @Test
    fun backupFromANewerVersionIsRefused() = runTest {
        addData()
        val source = File(work, "current.zip")
        backups.create(Uri.fromFile(source), includePhotos = false)
        // Même sauvegarde, manifeste réécrit comme par une version future de l'app.
        val newer = File(work, "newer.zip")
        ZipFile(source).use { input ->
            ZipOutputStream(newer.outputStream()).use { output ->
                input.entries().toList().forEach { entry ->
                    output.putNextEntry(ZipEntry(entry.name))
                    val bytes = input.getInputStream(entry).use { it.readBytes() }
                    val content = if (entry.name == BackupLayout.MANIFEST) {
                        String(bytes, Charsets.ISO_8859_1).replace("database=4", "database=99").toByteArray()
                    } else {
                        bytes
                    }
                    output.write(content)
                    output.closeEntry()
                }
            }
        }

        assertEquals(BackupInspection.Invalid(BackupProblem.NEWER_VERSION), backups.inspect(Uri.fromFile(newer)))
    }

    private companion object {
        const val DATABASE_NAME = "backup-test.db"
    }
}
