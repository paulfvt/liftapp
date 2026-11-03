package com.paulfvt.liftapp.nutrition.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.nutrition.data.NutritionRepository
import com.paulfvt.liftapp.nutrition.domain.Food
import com.paulfvt.liftapp.nutrition.domain.Nutrients
import com.paulfvt.liftapp.nutrition.domain.goalOn
import com.paulfvt.liftapp.nutrition.domain.isValidGrams
import com.paulfvt.liftapp.nutrition.domain.stepGrams
import com.paulfvt.liftapp.nutrition.navigation.FoodQuantityRoute
import com.paulfvt.liftapp.workout.domain.KeypadText
import com.paulfvt.liftapp.workout.domain.erased
import com.paulfvt.liftapp.workout.domain.filterDecimalInput
import com.paulfvt.liftapp.workout.domain.parseDecimal
import com.paulfvt.liftapp.workout.domain.typed
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Calories du jour sans l'aliment en cours de saisie (ou de modification) et objectif de ce jour-là.
data class DayBudget(val eatenWithoutThis: Double, val goalKcal: Int?)

// Quantité d'un aliment : ajout à un repas, ou modification d'un aliment déjà noté (entryId).
@HiltViewModel
class FoodQuantityViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NutritionRepository
) : ViewModel() {
    private val route = savedStateHandle.toRoute<FoodQuantityRoute>()
    val meal = mealFromName(route.meal)
    val isEditing: Boolean = route.entryId != null
    private val date = LocalDate.ofEpochDay(route.date)

    val food: StateFlow<Food?> = repository.observeFood(route.foodId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val budget: StateFlow<DayBudget?> = combine(repository.observeDay(date), repository.observeGoals()) {
            entries,
            goals
        ->
        DayBudget(
            eatenWithoutThis = entries.filter { it.id != route.entryId }.sumOf { it.nutrients.kcal },
            goalKcal = goalOn(date, goals)?.target?.kcal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    // Texte du champ, sélectionné au départ : la première touche le remplace (comme le pavé des séances).
    var input by mutableStateOf(KeypadText(""))
        private set

    // Quantité notée la dernière fois pour cet aliment (portion « Dernière fois »).
    var lastGrams by mutableStateOf<Double?>(null)
        private set

    var isDone by mutableStateOf(false)
        private set

    private var isSaving = false

    init {
        viewModelScope.launch {
            val existing = route.entryId?.let { repository.getEntry(it) }
            lastGrams = repository.lastGrams(route.foodId)
            val grams = existing?.grams ?: lastGrams ?: DEFAULT_GRAMS
            input = KeypadText(decimalText(grams))
        }
    }

    val grams: Double? get() = parseDecimal(input.text)
    val isValid: Boolean get() = isValidGrams(grams)

    fun nutrientsFor(food: Food): Nutrients = food.per100g.forGrams(grams ?: 0.0)

    fun type(key: Char) {
        input = input.typed(key) { filterDecimalInput(it, allowNegative = false) }
    }

    fun erase() {
        input = input.erased()
    }

    fun step(steps: Int) {
        input = KeypadText(decimalText(stepGrams(grams, steps)))
    }

    fun choosePortion(portionGrams: Double) {
        input = KeypadText(decimalText(portionGrams))
    }

    fun toggleFavorite() {
        val food = food.value ?: return
        viewModelScope.launch { repository.setFavorite(food.id, !food.isFavorite) }
    }

    fun save() {
        val food = food.value ?: return
        val grams = grams?.takeIf { isValidGrams(it) } ?: return
        if (isSaving) return
        isSaving = true
        viewModelScope.launch {
            val entryId = route.entryId
            if (entryId !=
                null
            ) {
                repository.updateEntryGrams(entryId, grams)
            } else {
                repository.addEntry(date, meal, food, grams)
            }
            isDone = true
        }
    }

    fun delete() {
        val entryId = route.entryId ?: return
        viewModelScope.launch {
            repository.deleteEntry(entryId)
            isDone = true
        }
    }

    private companion object {
        const val DEFAULT_GRAMS = 100.0
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

// Calories restantes ce jour-là une fois cet aliment ajouté ; null sans objectif.
fun DayBudget.remainingAfter(kcal: Double): Double? = goalKcal?.let { it - eatenWithoutThis - kcal }
