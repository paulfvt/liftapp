package com.paulfvt.liftapp.settings.domain

import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Properties

// Version du format de sauvegarde, à augmenter si le contenu du ZIP change d'une façon que les versions précédentes
// de l'app ne sauraient pas lire.
const val BACKUP_FORMAT_VERSION = 1

// Contenu du ZIP : un manifeste, la base Room, les préférences (réglages et profil fixe) et, au choix, les photos.
// Ces chemins servent aussi dans le dossier de préparation de la restauration.
object BackupLayout {
    const val MANIFEST = "manifest.properties"
    const val DATABASE = "database/lift.db"
    const val PREFERENCES = "preferences/lift_preferences.preferences_pb"
    const val PHOTOS = "photos/"
}

// Le manifeste est un fichier .properties (clé=valeur) : lisible à l'œil et lu par la bibliothèque standard de Java.
data class BackupManifest(
    val formatVersion: Int,
    // Version du schéma Room de la base sauvegardée.
    val databaseVersion: Int,
    val createdAt: Instant,
    val includesPhotos: Boolean,
    val appVersion: String
)

private const val APP_ID = "com.paulfvt.liftapp"
private const val KEY_APP = "app"
private const val KEY_FORMAT = "format"
private const val KEY_DATABASE = "database"
private const val KEY_CREATED_AT = "createdAt"
private const val KEY_PHOTOS = "photos"
private const val KEY_APP_VERSION = "appVersion"

fun BackupManifest.toProperties(): Properties = Properties().apply {
    setProperty(KEY_APP, APP_ID)
    setProperty(KEY_FORMAT, formatVersion.toString())
    setProperty(KEY_DATABASE, databaseVersion.toString())
    setProperty(KEY_CREATED_AT, createdAt.toString())
    setProperty(KEY_PHOTOS, includesPhotos.toString())
    setProperty(KEY_APP_VERSION, appVersion)
}

// Null si le fichier ne vient pas de LiftApp ou s'il lui manque une valeur.
fun backupManifestOf(properties: Properties): BackupManifest? {
    if (properties.getProperty(KEY_APP) != APP_ID) return null
    val createdAt = try {
        Instant.parse(properties.getProperty(KEY_CREATED_AT) ?: return null)
    } catch (_: DateTimeParseException) {
        return null
    }
    return BackupManifest(
        formatVersion = properties.getProperty(KEY_FORMAT)?.toIntOrNull() ?: return null,
        databaseVersion = properties.getProperty(KEY_DATABASE)?.toIntOrNull() ?: return null,
        createdAt = createdAt,
        includesPhotos = properties.getProperty(KEY_PHOTOS)?.toBooleanStrictOrNull() ?: return null,
        appVersion = properties.getProperty(KEY_APP_VERSION).orEmpty()
    )
}

enum class BackupProblem {
    NOT_A_BACKUP,
    NEWER_VERSION,
    DAMAGED
}

// Une sauvegarde faite par une version plus récente de l'app peut avoir un schéma que celle-ci ne connaît pas : elle
// est refusée. Une plus ancienne est acceptée, la base passera par les migrations Room.
fun backupProblem(manifest: BackupManifest?, hasDatabase: Boolean, currentDatabaseVersion: Int): BackupProblem? = when {
    manifest == null -> BackupProblem.NOT_A_BACKUP
    manifest.formatVersion > BACKUP_FORMAT_VERSION || manifest.databaseVersion > currentDatabaseVersion ->
        BackupProblem.NEWER_VERSION
    !hasDatabase || manifest.formatVersion < 1 || manifest.databaseVersion < 1 -> BackupProblem.DAMAGED
    else -> null
}

// Chemin où extraire une entrée du ZIP, relatif au dossier de préparation. Null pour une entrée inconnue (ignorée) ou
// dangereuse : « ../ » ou un sous-dossier permettraient d'écrire en dehors du dossier prévu.
fun backupEntryTarget(name: String): String? = when {
    name == BackupLayout.MANIFEST || name == BackupLayout.DATABASE || name == BackupLayout.PREFERENCES -> name
    name.startsWith(BackupLayout.PHOTOS) -> name.removePrefix(BackupLayout.PHOTOS)
        .takeIf(::isPlainFileName)
        ?.let { BackupLayout.PHOTOS + it }
    else -> null
}

private fun isPlainFileName(name: String): Boolean =
    name.isNotEmpty() && name != "." && name != ".." && name.none { it == '/' || it == '\\' || it.code == 0 }

// Ce que contient une sauvegarde, montré avant de confirmer la restauration.
data class BackupInfo(val createdAt: Instant, val includesPhotos: Boolean, val workoutCount: Int, val photoCount: Int)
