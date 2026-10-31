package com.paulfvt.liftapp.settings.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.preferencesDataStoreFile
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.di.DataModule
import com.paulfvt.liftapp.profile.data.PHOTO_DIRECTORY
import com.paulfvt.liftapp.settings.domain.BackupLayout
import java.io.File
import java.io.IOException

// Restauration en attente. La sauvegarde, extraite et vérifiée dans no_backup/restore/, remplace les données au
// démarrage suivant de l'app, avant que la base et les préférences ne soient ouvertes : les remplacer pendant que
// l'app s'en sert les abîmerait.
object PendingRestore {
    private const val DIRECTORY = "restore"
    private const val READY_MARKER = "ready"
    private const val TAG = "PendingRestore"

    // Fichiers d'une base SQLite : la base, son journal WAL et son index, l'ancien journal.
    private val DATABASE_SUFFIXES = listOf("", "-wal", "-shm", "-journal")

    // Dans no_backup/ : jamais envoyé dans la sauvegarde cloud d'Android.
    fun stagingDir(context: Context): File = File(context.noBackupFilesDir, DIRECTORY)

    // La sauvegarde vérifiée remplacera les données au prochain démarrage.
    fun markReady(context: Context) {
        File(stagingDir(context), READY_MARKER).createNewFile()
    }

    // Appelé par LiftApplication. Un dossier sans marque (restauration annulée, app arrêtée pendant la vérification)
    // est seulement supprimé.
    fun applyIfReady(context: Context) {
        val staging = stagingDir(context)
        if (!staging.exists()) return
        try {
            if (File(staging, READY_MARKER).isFile) replaceData(context, staging)
        } catch (e: IOException) {
            Log.e(TAG, "Restauration interrompue", e)
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun replaceData(context: Context, staging: File) {
        val database = context.getDatabasePath(LiftDatabase.NAME)
        DATABASE_SUFFIXES.forEach { File(database.path + it).delete() }
        move(File(staging, BackupLayout.DATABASE), database)
        // Journal laissé par la vérification, s'il n'a pas été reporté dans la base.
        val stagedWal = File(staging, BackupLayout.DATABASE + "-wal")
        if (stagedWal.isFile) move(stagedWal, File(database.path + "-wal"))

        val preferences = context.preferencesDataStoreFile(DataModule.PREFERENCES_NAME)
        preferences.delete()
        File(staging, BackupLayout.PREFERENCES).takeIf { it.isFile }?.let { move(it, preferences) }

        // Remplacement complet : sans photos dans la sauvegarde, les photos actuelles disparaissent aussi.
        val photos = File(context.filesDir, PHOTO_DIRECTORY)
        photos.deleteRecursively()
        File(staging, BackupLayout.PHOTOS).takeIf { it.isDirectory }?.let { move(it, photos) }
    }

    // Un renommage suffit (même partition, instantané) ; la copie n'est qu'un secours.
    private fun move(source: File, target: File) {
        target.parentFile?.mkdirs()
        if (source.renameTo(target)) return
        if (source.isDirectory) {
            source.copyRecursively(target, overwrite = true)
        } else {
            source.copyTo(target, overwrite = true)
        }
    }
}
