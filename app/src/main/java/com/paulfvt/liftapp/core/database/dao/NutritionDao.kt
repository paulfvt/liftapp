package com.paulfvt.liftapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.paulfvt.liftapp.core.database.entity.DailyNutrientsRow
import com.paulfvt.liftapp.core.database.entity.FoodEntity
import com.paulfvt.liftapp.core.database.entity.FoodEntryEntity
import com.paulfvt.liftapp.core.database.entity.FoodEntryRow
import com.paulfvt.liftapp.core.database.entity.NutritionGoalEntity
import com.paulfvt.liftapp.core.database.entity.SavedMealDetails
import com.paulfvt.liftapp.core.database.entity.SavedMealEntity
import com.paulfvt.liftapp.core.database.entity.SavedMealItemEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

// Aliments, journal, repas enregistrés et objectifs nutritionnels (phase 12, schéma version 5).
@Dao
interface NutritionDao {
    // --- Aliments ---

    @Query("SELECT * FROM food WHERE ciqualCode IS NOT NULL")
    suspend fun getCiqualFoods(): List<FoodEntity>

    // Tous les aliments visibles, gardés en mémoire pour la recherche (environ 3 100 lignes).
    @Query("SELECT * FROM food WHERE isHidden = 0")
    fun observeVisibleFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun getFood(id: Long): FoodEntity?

    @Query("SELECT * FROM food WHERE id = :id")
    fun observeFood(id: Long): Flow<FoodEntity?>

    @Query("SELECT * FROM food WHERE id IN (:ids)")
    suspend fun getFoods(ids: Collection<Long>): List<FoodEntity>

    @Insert
    suspend fun insertFood(food: FoodEntity): Long

    @Insert
    suspend fun insertFoods(foods: List<FoodEntity>)

    @Update
    suspend fun updateFood(food: FoodEntity)

    @Update
    suspend fun updateFoods(foods: List<FoodEntity>)

    @Query("UPDATE food SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE food SET isHidden = 1 WHERE id = :id")
    suspend fun hideFood(id: Long)

    @Query("DELETE FROM food WHERE id = :id")
    suspend fun deleteFood(id: Long)

    @Query(
        "SELECT EXISTS(SELECT 1 FROM food_entry WHERE foodId = :id) " +
            "OR EXISTS(SELECT 1 FROM saved_meal_item WHERE foodId = :id)"
    )
    suspend fun isFoodUsed(id: Long): Boolean

    // Aliments notés le plus récemment, un seul exemplaire chacun.
    @Query(
        "SELECT food.* FROM food JOIN (SELECT foodId, MAX(createdAt) AS lastUsed FROM food_entry GROUP BY foodId) " +
            "AS recent ON recent.foodId = food.id WHERE food.isHidden = 0 ORDER BY recent.lastUsed DESC LIMIT :limit"
    )
    fun observeRecentFoods(limit: Int): Flow<List<FoodEntity>>

    // Quantité notée la dernière fois pour cet aliment (portion « Dernière fois »).
    @Query("SELECT grams FROM food_entry WHERE foodId = :foodId ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun lastGrams(foodId: Long): Double?

    // --- Journal ---

    @Query(
        "SELECT food_entry.*, food.name AS foodName FROM food_entry JOIN food ON food.id = food_entry.foodId " +
            "WHERE food_entry.date = :date ORDER BY food_entry.createdAt, food_entry.id"
    )
    fun observeEntries(date: LocalDate): Flow<List<FoodEntryRow>>

    @Query("SELECT * FROM food_entry WHERE date = :date ORDER BY createdAt, id")
    suspend fun getEntries(date: LocalDate): List<FoodEntryEntity>

    @Query("SELECT * FROM food_entry WHERE id = :id")
    suspend fun getEntry(id: Long): FoodEntryEntity?

    @Insert
    suspend fun insertEntries(entries: List<FoodEntryEntity>)

    @Update
    suspend fun updateEntry(entry: FoodEntryEntity)

    @Query("DELETE FROM food_entry WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    // Calories et macros de chaque jour noté de la période (les jours sans rien n'apparaissent pas).
    @Query(
        "SELECT date, SUM(grams * kcal) / 100.0 AS kcal, SUM(grams * protein) / 100.0 AS protein, " +
            "SUM(grams * carbs) / 100.0 AS carbs, SUM(grams * fat) / 100.0 AS fat FROM food_entry " +
            "WHERE date BETWEEN :from AND :to GROUP BY date ORDER BY date"
    )
    fun observeDailyNutrients(from: LocalDate, to: LocalDate): Flow<List<DailyNutrientsRow>>

    // Tout le journal, pour l'export CSV.
    @Query(
        "SELECT food_entry.*, food.name AS foodName FROM food_entry JOIN food ON food.id = food_entry.foodId " +
            "ORDER BY food_entry.date, food_entry.createdAt, food_entry.id"
    )
    suspend fun getAllEntries(): List<FoodEntryRow>

    // --- Repas enregistrés ---

    @Transaction
    @Query("SELECT * FROM saved_meal ORDER BY name COLLATE NOCASE")
    fun observeSavedMeals(): Flow<List<SavedMealDetails>>

    @Transaction
    @Query("SELECT * FROM saved_meal WHERE id = :id")
    suspend fun getSavedMeal(id: Long): SavedMealDetails?

    @Insert
    suspend fun insertSavedMeal(meal: SavedMealEntity): Long

    @Insert
    suspend fun insertSavedMealItems(items: List<SavedMealItemEntity>)

    // Les aliments du repas sont supprimés avec lui (clé étrangère ON DELETE CASCADE).
    @Query("DELETE FROM saved_meal WHERE id = :id")
    suspend fun deleteSavedMeal(id: Long)

    // --- Objectifs ---

    @Query("SELECT * FROM nutrition_goal ORDER BY effectiveFrom")
    fun observeGoals(): Flow<List<NutritionGoalEntity>>

    // effectiveFrom est unique : un second changement le même jour remplace le premier.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: NutritionGoalEntity)
}
