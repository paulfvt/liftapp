package com.paulfvt.liftapp.settings.domain

import java.time.Instant
import java.util.Properties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupTest {
    private val manifest = BackupManifest(
        formatVersion = 1,
        databaseVersion = 4,
        createdAt = Instant.parse("2026-10-31T17:05:00Z"),
        includesPhotos = true,
        appVersion = "0.1.0"
    )

    @Test
    fun `a manifest is read back as written`() {
        assertEquals(manifest, backupManifestOf(manifest.toProperties()))
    }

    @Test
    fun `a file from another app or with a missing value is not a manifest`() {
        val otherApp = manifest.toProperties().apply { setProperty("app", "com.example.other") }
        val noDate = manifest.toProperties().apply { remove("createdAt") }
        val badDate = manifest.toProperties().apply { setProperty("createdAt", "hier") }
        val badFlag = manifest.toProperties().apply { setProperty("photos", "oui") }

        assertNull(backupManifestOf(otherApp))
        assertNull(backupManifestOf(noDate))
        assertNull(backupManifestOf(badDate))
        assertNull(backupManifestOf(badFlag))
        assertNull(backupManifestOf(Properties()))
    }

    @Test
    fun `a backup from a newer version is refused, an older one goes through the migrations`() {
        assertNull(backupProblem(manifest, hasDatabase = true, currentDatabaseVersion = 4))
        assertNull(backupProblem(manifest.copy(databaseVersion = 1), hasDatabase = true, currentDatabaseVersion = 4))
        assertEquals(
            BackupProblem.NEWER_VERSION,
            backupProblem(manifest.copy(databaseVersion = 5), hasDatabase = true, currentDatabaseVersion = 4)
        )
        assertEquals(
            BackupProblem.NEWER_VERSION,
            backupProblem(manifest.copy(formatVersion = 2), hasDatabase = true, currentDatabaseVersion = 4)
        )
    }

    @Test
    fun `a backup without manifest or without database cannot be restored`() {
        assertEquals(BackupProblem.NOT_A_BACKUP, backupProblem(null, hasDatabase = true, currentDatabaseVersion = 4))
        assertEquals(BackupProblem.DAMAGED, backupProblem(manifest, hasDatabase = false, currentDatabaseVersion = 4))
        assertEquals(
            BackupProblem.DAMAGED,
            backupProblem(manifest.copy(databaseVersion = 0), hasDatabase = true, currentDatabaseVersion = 4)
        )
    }

    @Test
    fun `only known entries and plain photo names are extracted`() {
        assertEquals("manifest.properties", backupEntryTarget("manifest.properties"))
        assertEquals("database/lift.db", backupEntryTarget("database/lift.db"))
        assertEquals(
            "preferences/lift_preferences.preferences_pb",
            backupEntryTarget("preferences/lift_preferences.preferences_pb")
        )
        assertEquals("photos/abc.jpg", backupEntryTarget("photos/abc.jpg"))

        assertNull(backupEntryTarget("photos/"))
        assertNull(backupEntryTarget("photos/.."))
        assertNull(backupEntryTarget("photos/../database/lift.db"))
        assertNull(backupEntryTarget("photos/sub/abc.jpg"))
        assertNull(backupEntryTarget("photos/..\\..\\files\\x"))
        assertNull(backupEntryTarget("../lift.db"))
        assertNull(backupEntryTarget("/database/lift.db"))
        assertNull(backupEntryTarget("database/lift.db-wal"))
        assertNull(backupEntryTarget("notes.txt"))
    }
}
