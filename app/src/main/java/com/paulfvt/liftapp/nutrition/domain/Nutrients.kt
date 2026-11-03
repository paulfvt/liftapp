package com.paulfvt.liftapp.nutrition.domain

import com.paulfvt.liftapp.core.model.Meal
import java.time.LocalDate

// Calories (kcal) et macros (g), pour 100 g d'un aliment ou pour une quantité notée.
data class Nutrients(val kcal: Double, val protein: Double, val carbs: Double, val fat: Double) {
    operator fun plus(other: Nutrients) =
        Nutrients(kcal + other.kcal, protein + other.protein, carbs + other.carbs, fat + other.fat)

    // Valeurs pour 100 g ramenées à une quantité en grammes.
    fun forGrams(grams: Double) = Nutrients(
        kcal * grams / 100,
        protein * grams / 100,
        carbs * grams / 100,
        fat * grams / 100
    )

    companion object {
        val ZERO = Nutrients(0.0, 0.0, 0.0, 0.0)
    }
}

fun Iterable<Nutrients>.total(): Nutrients = fold(Nutrients.ZERO, Nutrients::plus)

// Aliment de la table Ciqual ou créé par l'utilisateur (isCustom). per100g : valeurs pour 100 g.
data class Food(val id: Long, val name: String, val per100g: Nutrients, val isCustom: Boolean, val isFavorite: Boolean)

// Aliment noté dans le journal, avec les valeurs pour 100 g copiées au moment de la saisie.
data class JournalEntry(
    val id: Long,
    val date: LocalDate,
    val meal: Meal,
    val foodId: Long,
    val foodName: String,
    val grams: Double,
    val per100g: Nutrients
) {
    val nutrients: Nutrients get() = per100g.forGrams(grams)
}

data class MealJournal(val meal: Meal, val entries: List<JournalEntry>) {
    val total: Nutrients get() = entries.map { it.nutrients }.total()
}

// Les 4 repas de la journée dans l'ordre, même vides, avec leurs aliments dans l'ordre de saisie.
fun mealsOf(entries: List<JournalEntry>): List<MealJournal> {
    val byMeal = entries.groupBy { it.meal }
    return Meal.entries.map { meal -> MealJournal(meal, byMeal[meal].orEmpty()) }
}

// Repas enregistré (« Mes repas ») : aliments et quantités, ajoutés d'un coup avec les valeurs actuelles.
data class SavedMealItem(val food: Food, val grams: Double) {
    val nutrients: Nutrients get() = food.per100g.forGrams(grams)
}

data class SavedMeal(val id: Long, val name: String, val items: List<SavedMealItem>) {
    val total: Nutrients get() = items.map { it.nutrients }.total()
}

// Quantité d'un aliment noté : plus de 0 g, 5 kg au plus (une faute de frappe plutôt qu'un repas au-delà).
const val MAX_GRAMS = 5_000.0
const val GRAMS_STEP = 10.0

fun isValidGrams(grams: Double?): Boolean = grams != null && grams > 0 && grams <= MAX_GRAMS

// Touches − et + du pavé : 10 g par appui, jamais sous 0 ni au-dessus de 5 kg.
fun stepGrams(grams: Double?, steps: Int): Double = ((grams ?: 0.0) + steps * GRAMS_STEP).coerceIn(0.0, MAX_GRAMS)
