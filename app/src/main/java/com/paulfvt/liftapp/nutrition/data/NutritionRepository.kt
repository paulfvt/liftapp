package com.paulfvt.liftapp.nutrition.data

import androidx.room.withTransaction
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.database.entity.FoodEntity
import com.paulfvt.liftapp.core.database.entity.FoodEntryEntity
import com.paulfvt.liftapp.core.database.entity.FoodEntryRow
import com.paulfvt.liftapp.core.database.entity.NutritionGoalEntity
import com.paulfvt.liftapp.core.database.entity.SavedMealDetails
import com.paulfvt.liftapp.core.database.entity.SavedMealEntity
import com.paulfvt.liftapp.core.database.entity.SavedMealItemEntity
import com.paulfvt.liftapp.core.domain.DailyValue
import com.paulfvt.liftapp.core.model.Meal
import com.paulfvt.liftapp.nutrition.domain.Food
import com.paulfvt.liftapp.nutrition.domain.FoodDraft
import com.paulfvt.liftapp.nutrition.domain.GoalSettings
import com.paulfvt.liftapp.nutrition.domain.JournalEntry
import com.paulfvt.liftapp.nutrition.domain.Nutrients
import com.paulfvt.liftapp.nutrition.domain.NutritionGoal
import com.paulfvt.liftapp.nutrition.domain.NutritionTarget
import com.paulfvt.liftapp.nutrition.domain.SavedMeal
import com.paulfvt.liftapp.nutrition.domain.SavedMealItem
import com.paulfvt.liftapp.nutrition.domain.SearchableFood
import com.paulfvt.liftapp.nutrition.domain.toNutrients
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

// Unique accès aux données de la nutrition : aliments, journal, repas enregistrés et objectifs.
@Singleton
class NutritionRepository @Inject constructor(private val database: LiftDatabase, private val clock: Clock) {
    private val dao = database.nutritionDao()

    // --- Aliments ---

    // Tous les aliments visibles, mots déjà préparés pour la recherche (hors du fil principal : ~3 100 noms).
    fun observeSearchableFoods(): Flow<List<SearchableFood>> =
        dao.observeVisibleFoods().map { rows -> rows.map { SearchableFood(it.toDomain()) } }.flowOn(Dispatchers.Default)

    fun observeRecentFoods(): Flow<List<Food>> =
        dao.observeRecentFoods(RECENT_FOODS).map { rows -> rows.map { it.toDomain() } }

    fun observeFood(id: Long): Flow<Food?> = dao.observeFood(id).map { it?.toDomain() }

    suspend fun lastGrams(foodId: Long): Double? = dao.lastGrams(foodId)

    suspend fun setFavorite(foodId: Long, favorite: Boolean) = dao.setFavorite(foodId, favorite)

    suspend fun createFood(draft: FoodDraft): Long {
        val values = draft.toNutrients()
        return dao.insertFood(
            FoodEntity(
                ciqualCode = null,
                name = draft.name.trim(),
                kcal = values.kcal,
                protein = values.protein,
                carbs = values.carbs,
                fat = values.fat
            )
        )
    }

    // Les repas déjà notés gardent les valeurs d'avant (copiées à la saisie) ; seul le nom suit.
    suspend fun updateFood(id: Long, draft: FoodDraft) {
        val food = dao.getFood(id) ?: return
        val values = draft.toNutrients()
        dao.updateFood(
            food.copy(
                name = draft.name.trim(),
                kcal = values.kcal,
                protein = values.protein,
                carbs = values.carbs,
                fat = values.fat
            )
        )
    }

    // Un aliment personnel déjà noté ou dans un repas enregistré est masqué, sinon supprimé.
    suspend fun removeFood(id: Long) = database.withTransaction {
        if (dao.isFoodUsed(id)) dao.hideFood(id) else dao.deleteFood(id)
    }

    // --- Journal ---

    fun observeDay(date: LocalDate): Flow<List<JournalEntry>> =
        dao.observeEntries(date).map { rows -> rows.map { it.toDomain() } }

    suspend fun getEntry(id: Long): JournalEntry? {
        val entry = dao.getEntry(id) ?: return null
        val food = dao.getFood(entry.foodId) ?: return null
        return FoodEntryRow(entry, food.name).toDomain()
    }

    // Les valeurs pour 100 g de l'aliment sont copiées : les jours passés ne bougent plus ensuite.
    suspend fun addEntry(date: LocalDate, meal: Meal, food: Food, grams: Double) {
        dao.insertEntries(listOf(newEntry(date, meal, food, grams, clock.instant())))
    }

    suspend fun updateEntryGrams(id: Long, grams: Double) {
        val entry = dao.getEntry(id) ?: return
        dao.updateEntry(entry.copy(grams = grams))
    }

    suspend fun deleteEntry(id: Long) = dao.deleteEntry(id)

    // « Copier hier » : les aliments de la veille s'ajoutent à la suite de ceux du jour, dans le même ordre.
    // Renvoie le nombre d'aliments copiés.
    suspend fun copyPreviousDay(date: LocalDate): Int = database.withTransaction {
        val previous = dao.getEntries(date.minusDays(1))
        val now = clock.instant()
        dao.insertEntries(
            previous.mapIndexed { index, entry ->
                entry.copy(id = 0, date = date, createdAt = now.plusMillis(index.toLong()))
            }
        )
        previous.size
    }

    // Calories de chaque jour noté de la période, pour la tendance et la dépense réelle.
    fun observeDailyKcal(from: LocalDate, to: LocalDate): Flow<List<DailyValue>> =
        dao.observeDailyNutrients(from, to).map { rows -> rows.map { DailyValue(it.date, it.kcal) } }

    // --- Repas enregistrés ---

    fun observeSavedMeals(): Flow<List<SavedMeal>> =
        combine(dao.observeSavedMeals(), dao.observeSavedMealFoods()) { meals, foods ->
            val byId = foods.associateBy { it.id }
            meals.mapNotNull { it.toDomain(byId) }
        }

    // Enregistre les aliments d'un repas du journal sous un nom, avec leurs quantités.
    suspend fun saveMeal(name: String, entries: List<JournalEntry>) = database.withTransaction {
        val id = dao.insertSavedMeal(SavedMealEntity(name = name.trim()))
        dao.insertSavedMealItems(
            entries.mapIndexed { index, entry ->
                SavedMealItemEntity(savedMealId = id, foodId = entry.foodId, grams = entry.grams, sortOrder = index)
            }
        )
    }

    // Ajoute d'un coup tous les aliments d'un repas enregistré, avec les valeurs actuelles des aliments.
    suspend fun addSavedMeal(savedMealId: Long, date: LocalDate, meal: Meal) = database.withTransaction {
        val details = dao.getSavedMeal(savedMealId) ?: return@withTransaction
        val foods = dao.getFoods(details.items.map { it.foodId }).associateBy { it.id }
        val now = clock.instant()
        dao.insertEntries(
            details.items.sortedBy { it.sortOrder }.mapIndexedNotNull { index, item ->
                foods[item.foodId]?.let { food ->
                    newEntry(date, meal, food.toDomain(), item.grams, now.plusMillis(index.toLong()))
                }
            }
        )
    }

    suspend fun deleteSavedMeal(id: Long) = dao.deleteSavedMeal(id)

    // --- Objectifs ---

    fun observeGoals(): Flow<List<NutritionGoal>> = dao.observeGoals().map { rows -> rows.map { it.toDomain() } }

    suspend fun saveGoal(effectiveFrom: LocalDate, target: NutritionTarget, settings: GoalSettings) = dao.upsertGoal(
        NutritionGoalEntity(
            effectiveFrom = effectiveFrom,
            kcal = target.kcal,
            proteinGrams = target.proteinGrams,
            carbsGrams = target.carbsGrams,
            fatGrams = target.fatGrams,
            activityLevel = settings.activityLevel,
            objective = settings.objective,
            proteinPerKg = settings.proteinPerKg,
            fatPerKg = settings.fatPerKg,
            isKcalManual = settings.manualKcal != null
        )
    )

    private fun newEntry(date: LocalDate, meal: Meal, food: Food, grams: Double, createdAt: Instant) = FoodEntryEntity(
        date = date,
        meal = meal,
        foodId = food.id,
        grams = grams,
        kcal = food.per100g.kcal,
        protein = food.per100g.protein,
        carbs = food.per100g.carbs,
        fat = food.per100g.fat,
        createdAt = createdAt
    )

    private companion object {
        const val RECENT_FOODS = 30
    }
}

private fun FoodEntity.toDomain() = Food(
    id = id,
    name = name,
    per100g = Nutrients(kcal, protein, carbs, fat),
    isCustom = ciqualCode == null,
    isFavorite = isFavorite
)

private fun FoodEntryRow.toDomain() = JournalEntry(
    id = entry.id,
    date = entry.date,
    meal = entry.meal,
    foodId = entry.foodId,
    foodName = foodName,
    grams = entry.grams,
    per100g = Nutrients(entry.kcal, entry.protein, entry.carbs, entry.fat)
)

private fun SavedMealDetails.toDomain(foods: Map<Long, FoodEntity>): SavedMeal? {
    val items = items.sortedBy { it.sortOrder }.mapNotNull { item ->
        foods[item.foodId]?.let { SavedMealItem(it.toDomain(), item.grams) }
    }
    return SavedMeal(meal.id, meal.name, items).takeIf { items.isNotEmpty() }
}

private fun NutritionGoalEntity.toDomain() = NutritionGoal(
    effectiveFrom = effectiveFrom,
    target = NutritionTarget(kcal, proteinGrams, carbsGrams, fatGrams),
    settings = GoalSettings(
        activityLevel = activityLevel,
        objective = objective,
        proteinPerKg = proteinPerKg,
        fatPerKg = fatPerKg,
        manualKcal = kcal.takeIf { isKcalManual }
    )
)
