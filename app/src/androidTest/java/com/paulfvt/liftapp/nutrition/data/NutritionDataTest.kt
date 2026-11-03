package com.paulfvt.liftapp.nutrition.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paulfvt.liftapp.core.database.LiftDatabase
import com.paulfvt.liftapp.core.model.Meal
import com.paulfvt.liftapp.nutrition.domain.FoodDraft
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Tests sur le téléphone, avec une vraie base SQLite en mémoire (effacée après chaque test).
@RunWith(AndroidJUnit4::class)
class NutritionDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = Clock.fixed(Instant.parse("2026-11-03T18:00:00Z"), ZoneOffset.UTC)
    private val today = LocalDate.parse("2026-11-03")
    private lateinit var database: LiftDatabase
    private lateinit var importer: FoodImporter
    private lateinit var repository: NutritionRepository
    private lateinit var preferencesFile: File

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, LiftDatabase::class.java).build()
        preferencesFile = File(context.cacheDir, "test-${System.nanoTime()}.preferences_pb")
        importer = FoodImporter(context, database, PreferenceDataStoreFactory.create { preferencesFile })
        repository = NutritionRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
        preferencesFile.delete()
    }

    private fun dto(code: Int, name: String, kcal: Double = 100.0) = FoodDto(code, name, kcal, 10.0, 10.0, 1.0)

    private suspend fun foodNamed(name: String) =
        repository.observeSearchableFoods().first().map { it.food }.first { it.name == name }

    @Test
    fun embeddedTableIsImportedOnceWithItsValues() = runTest {
        importer.importIfNeeded()
        importer.importIfNeeded()
        val foods = repository.observeSearchableFoods().first()
        assertEquals(3_092, foods.size)
        val egg = foods.first { it.food.name == "Oeuf, cru" }.food
        assertFalse(egg.isCustom)
        assertTrue(egg.per100g.kcal > 0)
    }

    @Test
    fun newTableVersionKeepsFavoritesAndHidesRemovedFoods() = runTest {
        importer.import(FoodsDto(1, "test", listOf(dto(1, "Riz"), dto(2, "Pâtes"))))
        repository.setFavorite(foodNamed("Riz").id, true)
        repository.addEntry(today, Meal.LUNCH, foodNamed("Pâtes"), 120.0)

        importer.import(FoodsDto(2, "test", listOf(dto(1, "Riz, cuit", kcal = 130.0), dto(3, "Quinoa"))))

        val names = repository.observeSearchableFoods().first().map { it.food.name }.toSet()
        assertEquals(setOf("Riz, cuit", "Quinoa"), names)
        assertTrue(foodNamed("Riz, cuit").isFavorite)
        // Les pâtes, retirées de la table, restent dans le repas déjà noté.
        assertEquals("Pâtes", repository.observeDay(today).first().single().foodName)
    }

    @Test
    fun entryKeepsTheValuesOfTheMomentItWasNoted() = runTest {
        val id = repository.createFood(FoodDraft("Skyr", "60", "10", "4", "0,2"))
        val skyr = repository.observeFood(id).first()!!
        repository.addEntry(today, Meal.SNACKS, skyr, 150.0)

        repository.updateFood(id, FoodDraft("Skyr nature", "70", "11", "4", "0,2"))

        val entry = repository.observeDay(today).first().single()
        assertEquals("Skyr nature", entry.foodName)
        assertEquals(90.0, entry.nutrients.kcal, 1e-9)
        assertEquals(150.0, repository.lastGrams(id)!!, 0.0)
    }

    @Test
    fun copyPreviousDayAppendsInTheSameOrder() = runTest {
        importer.import(FoodsDto(1, "test", listOf(dto(1, "Avoine"), dto(2, "Lait"), dto(3, "Banane"))))
        val yesterday = today.minusDays(1)
        repository.addEntry(yesterday, Meal.BREAKFAST, foodNamed("Avoine"), 80.0)
        repository.addEntry(yesterday, Meal.BREAKFAST, foodNamed("Lait"), 250.0)
        repository.addEntry(today, Meal.SNACKS, foodNamed("Banane"), 120.0)

        assertEquals(2, repository.copyPreviousDay(today))

        val day = repository.observeDay(today).first()
        assertEquals(listOf("Banane", "Avoine", "Lait"), day.map { it.foodName })
        assertEquals(2, repository.observeDay(yesterday).first().size)
        assertEquals(0, repository.copyPreviousDay(today.minusDays(5)))
    }

    @Test
    fun savedMealIsAddedInOneGo() = runTest {
        importer.import(FoodsDto(1, "test", listOf(dto(1, "Poulet", kcal = 151.0), dto(2, "Riz", kcal = 117.0))))
        repository.addEntry(today, Meal.LUNCH, foodNamed("Poulet"), 180.0)
        repository.addEntry(today, Meal.LUNCH, foodNamed("Riz"), 250.0)
        repository.saveMeal("Poulet riz", repository.observeDay(today).first())

        val saved = repository.observeSavedMeals().first().single()
        assertEquals(151.0 * 1.8 + 117.0 * 2.5, saved.total.kcal, 1e-9)
        repository.addSavedMeal(saved.id, today.plusDays(1), Meal.DINNER)
        assertEquals(listOf("Poulet", "Riz"), repository.observeDay(today.plusDays(1)).first().map { it.foodName })

        repository.deleteSavedMeal(saved.id)
        assertTrue(repository.observeSavedMeals().first().isEmpty())
        assertEquals(2, repository.observeDay(today.plusDays(1)).first().size)
    }

    @Test
    fun usedCustomFoodIsHiddenUnusedOneIsDeleted() = runTest {
        val used = repository.createFood(FoodDraft("Whey", "380", "78", "6", "5"))
        val unused = repository.createFood(FoodDraft("Barre", "400", "20", "40", "15"))
        repository.addEntry(today, Meal.SNACKS, repository.observeFood(used).first()!!, 30.0)

        repository.removeFood(used)
        repository.removeFood(unused)

        assertTrue(repository.observeSearchableFoods().first().isEmpty())
        assertNull(repository.observeFood(unused).first())
        assertEquals("Whey", repository.observeDay(today).first().single().foodName)
    }
}
