package com.paulfvt.liftapp.exercise.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.room.withTransaction
import com.paulfvt.liftapp.core.database.LiftDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

// Importe le catalogue embarqué dans la base au premier lancement, puis à chaque nouvelle version du catalogue.
// Les exercices du catalogue sont mis à jour sans toucher à la note ni au masquage choisis par l'utilisateur.
@Singleton
class CatalogImporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: LiftDatabase,
    private val preferences: DataStore<Preferences>
) {
    suspend fun importIfNeeded() = withContext(Dispatchers.IO) {
        // version.txt évite de relire tout le catalogue (environ 1 Mo) à chaque lancement.
        val assetVersion = context.assets.open(VERSION_ASSET).bufferedReader().use {
            it.readText().trim().toInt()
        }
        val importedVersion = preferences.data.first()[IMPORTED_VERSION_KEY] ?: 0
        if (assetVersion <= importedVersion) return@withContext

        val catalog = context.assets.open(CATALOG_ASSET).bufferedReader().use {
            json.decodeFromString<CatalogDto>(it.readText())
        }
        import(catalog)
        preferences.edit { it[IMPORTED_VERSION_KEY] = catalog.version }
        Log.i(TAG, "Catalogue v${catalog.version} importé : ${catalog.exercises.size} exercices")
    }

    // Une seule transaction : si l'import échoue au milieu, rien n'est écrit et il sera retenté au prochain lancement.
    suspend fun import(catalog: CatalogDto) {
        val dao = database.exerciseDao()
        database.withTransaction {
            val existingByKey = dao.getCatalogExercises().associateBy { it.catalogKey }
            catalog.exercises.forEach { dto ->
                val existing = existingByKey[dto.key]
                val id = if (existing == null) {
                    dao.insert(dto.toEntity(existing = null))
                } else {
                    dao.update(dto.toEntity(existing))
                    dao.deleteMuscles(existing.id)
                    existing.id
                }
                dao.insertMuscles(dto.toMuscleEntities(id))
            }
        }
    }

    private companion object {
        const val TAG = "CatalogImporter"
        const val CATALOG_ASSET = "catalog/exercises.json"
        const val VERSION_ASSET = "catalog/version.txt"
        val IMPORTED_VERSION_KEY = intPreferencesKey("imported_catalog_version")
        val json = Json { ignoreUnknownKeys = true }
    }
}
