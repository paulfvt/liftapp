package com.paulfvt.liftapp.nutrition.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.room.withTransaction
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.entity.FoodEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Format de assets/nutrition/foods.json, produit par tools/nutrition/build_foods.py (valeurs pour 100 g).
@Serializable
data class FoodsDto(val version: Int, val source: String, val foods: List<FoodDto>)

@Serializable
data class FoodDto(
    val code: Int,
    val name: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

// Importe la table Ciqual embarquée au premier lancement, puis à chaque nouvelle version, comme CatalogImporter.
// Les favoris sont gardés ; un aliment absent de la nouvelle version est masqué, pas supprimé (repas déjà notés).
@Singleton
class FoodImporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: LiftDatabase,
    private val preferences: DataStore<Preferences>
) {
    suspend fun importIfNeeded() = withContext(Dispatchers.IO) {
        // version.txt évite de relire toute la table (environ 350 Ko) à chaque lancement.
        val assetVersion = context.assets.open(VERSION_ASSET).bufferedReader().use { it.readText().trim().toInt() }
        val importedVersion = preferences.data.first()[IMPORTED_VERSION_KEY] ?: 0
        if (assetVersion <= importedVersion) return@withContext

        val table = context.assets.open(FOODS_ASSET).bufferedReader().use {
            json.decodeFromString<FoodsDto>(it.readText())
        }
        import(table)
        preferences.edit { it[IMPORTED_VERSION_KEY] = table.version }
        Log.i(TAG, "${table.source} importée (version ${table.version}) : ${table.foods.size} aliments")
    }

    // Une seule transaction : si l'import échoue au milieu, rien n'est écrit et il sera retenté au prochain lancement.
    suspend fun import(table: FoodsDto) {
        val dao = database.nutritionDao()
        database.withTransaction {
            val existingByCode = dao.getCiqualFoods().associateBy { it.ciqualCode }
            val codes = table.foods.map { it.code }.toSet()
            val (updated, added) = table.foods.partition { it.code in existingByCode }
            dao.insertFoods(added.map { it.toEntity(existing = null) })
            dao.updateFoods(updated.map { it.toEntity(existingByCode.getValue(it.code)) })
            val removed = existingByCode.values.filter { it.ciqualCode !in codes && !it.isHidden }
            dao.updateFoods(removed.map { it.copy(isHidden = true) })
        }
    }

    private fun FoodDto.toEntity(existing: FoodEntity?) = FoodEntity(
        id = existing?.id ?: 0,
        ciqualCode = code,
        name = name,
        kcal = kcal,
        protein = protein,
        carbs = carbs,
        fat = fat,
        isFavorite = existing?.isFavorite ?: false,
        isHidden = false
    )

    private companion object {
        const val TAG = "FoodImporter"
        const val FOODS_ASSET = "nutrition/foods.json"
        const val VERSION_ASSET = "nutrition/version.txt"
        val IMPORTED_VERSION_KEY = intPreferencesKey("imported_foods_version")
        val json = Json { ignoreUnknownKeys = true }
    }
}
