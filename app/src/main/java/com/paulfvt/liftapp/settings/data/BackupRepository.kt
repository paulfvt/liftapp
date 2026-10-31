package com.paulfvt.liftapp.settings.data

import android.content.Context
import android.content.Intent
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.room.withTransaction
import com.paulfvt.liftapp.BuildConfig
import com.paulfvt.liftapp.core.database.DATABASE_VERSION
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.profile.data.PHOTO_DIRECTORY
import com.paulfvt.liftapp.settings.domain.BACKUP_FORMAT_VERSION
import com.paulfvt.liftapp.settings.domain.BackupInfo
import com.paulfvt.liftapp.settings.domain.BackupLayout
import com.paulfvt.liftapp.settings.domain.BackupManifest
import com.paulfvt.liftapp.settings.domain.BackupProblem
import com.paulfvt.liftapp.settings.domain.backupEntryTarget
import com.paulfvt.liftapp.settings.domain.backupManifestOf
import com.paulfvt.liftapp.settings.domain.backupProblem
import com.paulfvt.liftapp.settings.domain.toProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed interface BackupInspection {
    data class Valid(val info: BackupInfo) : BackupInspection

    data class Invalid(val problem: BackupProblem) : BackupInspection
}

// Sauvegarde complète dans un fichier ZIP choisi par l'utilisateur (cahier § 4.11), et restauration qui remplace
// toutes les données (décision du 28 octobre 2026). Le ZIP n'est pas chiffré : l'écran de sauvegarde le dit.
@Singleton
class BackupRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: LiftDatabase,
    private val preferences: DataStore<Preferences>,
    private val clock: Clock
) {
    // Crée la sauvegarde et renvoie ce qu'elle contient.
    suspend fun create(target: Uri, includePhotos: Boolean): BackupInfo = withContext(Dispatchers.IO) {
        val work = File(context.cacheDir, WORK_DIRECTORY).apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            val databaseCopy = File(work, LiftDatabase.NAME)
            snapshotDatabase(databaseCopy)
            val content = prepareDatabaseCopy(databaseCopy, includePhotos)
            val preferencesCopy = File(work, PREFERENCES_COPY)
            snapshotPreferences(preferencesCopy)
            val photos = if (includePhotos) photoFiles() else emptyList()
            val manifest = BackupManifest(
                formatVersion = BACKUP_FORMAT_VERSION,
                databaseVersion = content.version,
                createdAt = clock.instant(),
                includesPhotos = includePhotos,
                appVersion = BuildConfig.VERSION_NAME
            )
            val output = context.contentResolver.openOutputStream(target, "wt")
                ?: throw FileNotFoundException("Fichier inaccessible : $target")
            writeZip(output, manifest, databaseCopy, preferencesCopy, photos)
            BackupInfo(manifest.createdAt, includePhotos, content.workoutCount, content.photoCount)
        } finally {
            work.deleteRecursively()
        }
    }

    // Extrait la sauvegarde dans le dossier de préparation et la vérifie, sans toucher aux données actuelles. La base
    // y est ouverte par Room : une sauvegarde plus ancienne passe par les migrations, une base abîmée est repérée
    // avant tout remplacement.
    suspend fun inspect(source: Uri): BackupInspection = withContext(Dispatchers.IO) {
        val staging = PendingRestore.stagingDir(context).apply {
            deleteRecursively()
            mkdirs()
        }
        val result = try {
            val input = context.contentResolver.openInputStream(source)
                ?: throw FileNotFoundException("Fichier inaccessible : $source")
            extract(input, staging)
            val manifest = File(staging, BackupLayout.MANIFEST).takeIf { it.isFile }?.let(::readManifest)
            val databaseFile = File(staging, BackupLayout.DATABASE)
            val problem = backupProblem(manifest, databaseFile.isFile, DATABASE_VERSION)
            if (problem != null || manifest == null) {
                BackupInspection.Invalid(problem ?: BackupProblem.NOT_A_BACKUP)
            } else {
                BackupInspection.Valid(verify(databaseFile, manifest))
            }
        } catch (e: ZipException) {
            Log.w(TAG, "Pas une sauvegarde", e)
            BackupInspection.Invalid(BackupProblem.NOT_A_BACKUP)
        } catch (e: IOException) {
            Log.w(TAG, "Sauvegarde illisible", e)
            BackupInspection.Invalid(BackupProblem.DAMAGED)
        } catch (e: SQLiteException) {
            Log.w(TAG, "Base de la sauvegarde illisible", e)
            BackupInspection.Invalid(BackupProblem.DAMAGED)
        } catch (e: IllegalStateException) {
            // Room : schéma inattendu ou migration impossible.
            Log.w(TAG, "Base de la sauvegarde refusée par Room", e)
            BackupInspection.Invalid(BackupProblem.DAMAGED)
        } catch (e: SecurityException) {
            Log.w(TAG, "Fichier refusé par Android", e)
            BackupInspection.Invalid(BackupProblem.DAMAGED)
        }
        if (result is BackupInspection.Invalid) staging.deleteRecursively()
        result
    }

    // Restauration annulée : la sauvegarde extraite est supprimée.
    suspend fun discardInspected() = withContext(Dispatchers.IO) {
        PendingRestore.stagingDir(context).deleteRecursively()
    }

    // Marque la sauvegarde vérifiée comme prête, puis redémarre l'app : le remplacement se fait au démarrage, avant
    // que la base et les préférences ne soient ouvertes (PendingRestore). La notification et l'alarme de séance sont
    // remises en accord avec les données restaurées par WorkoutSessionSync au démarrage.
    fun restoreAndRestart() {
        PendingRestore.markReady(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.component
        if (launch != null) context.startActivity(Intent.makeRestartActivityTask(launch))
        // Android relance l'app dans un nouveau processus pour l'activité demandée.
        Runtime.getRuntime().exit(0)
    }

    // Copie cohérente de la base : pendant la transaction, aucune autre écriture ne touche ni le fichier ni son journal
    // WAL (où attendent les dernières écritures), qui sont copiés ensemble.
    private suspend fun snapshotDatabase(copy: File) {
        val file = context.getDatabasePath(database.openHelper.databaseName ?: LiftDatabase.NAME)
        database.withTransaction {
            file.copyTo(copy, overwrite = true)
            val wal = File(file.path + WAL_SUFFIX)
            if (wal.isFile) wal.copyTo(File(copy.path + WAL_SUFFIX), overwrite = true)
        }
    }

    private data class DatabaseContent(val version: Int, val workoutCount: Int, val photoCount: Int)

    // Sans les photos, leurs lignes sont retirées de la copie : la base restaurée ne pointera pas vers des fichiers
    // absents. Le journal WAL est reporté dans le fichier, qui part seul dans le ZIP.
    private fun prepareDatabaseCopy(copy: File, includePhotos: Boolean): DatabaseContent {
        val db = SQLiteDatabase.openDatabase(copy.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            if (!includePhotos) db.execSQL("DELETE FROM progress_photo")
            val content = DatabaseContent(
                version = db.version,
                workoutCount = DatabaseUtils.longForQuery(db, COUNT_FINISHED_WORKOUTS, null).toInt(),
                photoCount = DatabaseUtils.longForQuery(db, COUNT_PHOTOS, null).toInt()
            )
            db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
            return content
        } finally {
            db.close()
        }
    }

    // Préférences sans celles propres au téléphone, écrites au même format par un DataStore temporaire.
    private suspend fun snapshotPreferences(copy: File) {
        val kept = preferences.data.first().toMutablePreferences().apply { DEVICE_KEYS.forEach { remove(it) } }
        val job = SupervisorJob()
        try {
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { copy }
                .updateData { kept }
        } finally {
            // Arrêté et attendu : un autre DataStore pourra rouvrir ce fichier à la sauvegarde suivante.
            job.cancelAndJoin()
        }
    }

    private fun photoFiles(): List<File> =
        File(context.filesDir, PHOTO_DIRECTORY).listFiles()?.filter { it.isFile }.orEmpty()

    private fun writeZip(
        output: OutputStream,
        manifest: BackupManifest,
        databaseCopy: File,
        preferencesCopy: File,
        photos: List<File>
    ) {
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(BackupLayout.MANIFEST))
            manifest.toProperties().store(zip, null)
            zip.closeEntry()
            zip.addFile(BackupLayout.DATABASE, databaseCopy)
            zip.addFile(BackupLayout.PREFERENCES, preferencesCopy)
            photos.forEach { zip.addFile(BackupLayout.PHOTOS + it.name, it) }
        }
    }

    private fun ZipOutputStream.addFile(name: String, file: File) {
        putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(this) }
        closeEntry()
    }

    // Seules les entrées connues sont extraites, chacune à sa place (voir backupEntryTarget). La taille totale est
    // bornée : un ZIP piégé pourrait sinon remplir le téléphone.
    private fun extract(input: InputStream, staging: File) {
        var total = 0L
        ZipInputStream(input.buffered()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry ->
                val target = backupEntryTarget(entry.name)?.takeUnless { entry.isDirectory } ?: return@forEach
                val file = File(staging, target).apply { parentFile?.mkdirs() }
                file.outputStream().use { output -> total += zip.copyAtMost(output, MAX_EXTRACTED_BYTES - total) }
            }
        }
    }

    private fun InputStream.copyAtMost(output: OutputStream, limit: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) return copied
            copied += read
            if (copied > limit) throw IOException("Sauvegarde trop volumineuse")
            output.write(buffer, 0, read)
        }
    }

    private fun readManifest(file: File): BackupManifest? =
        backupManifestOf(Properties().apply { file.inputStream().use(::load) })

    private fun verify(databaseFile: File, manifest: BackupManifest): BackupInfo {
        val staged = LiftDatabase.build(context, databaseFile.path)
        try {
            // Ouvre la base : Room applique les migrations et vérifie le schéma.
            val db = staged.openHelper.writableDatabase
            fun count(sql: String) = db.query(sql).use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
            return BackupInfo(
                manifest.createdAt,
                manifest.includesPhotos,
                count(COUNT_FINISHED_WORKOUTS),
                count(COUNT_PHOTOS)
            )
        } finally {
            staged.close()
        }
    }

    private companion object {
        const val TAG = "Backup"
        const val WORK_DIRECTORY = "backup"
        const val WAL_SUFFIX = "-wal"

        // DataStore exige l'extension .preferences_pb.
        const val PREFERENCES_COPY = "lift_preferences.preferences_pb"
        const val MAX_EXTRACTED_BYTES = 4L * 1024 * 1024 * 1024
        const val COUNT_FINISHED_WORKOUTS = "SELECT COUNT(*) FROM workout WHERE status = 'FINISHED'"
        const val COUNT_PHOTOS = "SELECT COUNT(*) FROM progress_photo"

        // Propres au téléphone, donc pas sauvegardées : la demande de notifications doit être refaite sur un nouveau
        // téléphone, et les erreurs de code du verrou des photos repartent de zéro. Mêmes noms que dans
        // WorkoutSettingsRepository et PhotoLockRepository.
        val DEVICE_KEYS = listOf(
            booleanPreferencesKey("workout_permissions_asked"),
            intPreferencesKey("photo_lock_failures"),
            longPreferencesKey("photo_lock_last_failure")
        )
    }
}
