package com.paulfvt.liftapp.nutrition.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.exercise.domain.normalizeForSearch
import com.paulfvt.liftapp.nutrition.data.NutritionRepository
import com.paulfvt.liftapp.nutrition.domain.Food
import com.paulfvt.liftapp.nutrition.domain.SavedMeal
import com.paulfvt.liftapp.nutrition.domain.SearchableFood
import com.paulfvt.liftapp.nutrition.domain.searchFoods
import com.paulfvt.liftapp.nutrition.navigation.FoodSearchRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FoodFilter {
    ALL,
    RECENT,
    FAVORITES,
    SAVED_MEALS,
    CUSTOM
}

// Résultat affiché : des aliments, ou des repas enregistrés pour le filtre « Mes repas ». showsRecents : liste des
// récents montrée faute de recherche tapée (titre « Récents »).
data class FoodSearchUiState(
    val query: String = "",
    val filter: FoodFilter = FoodFilter.ALL,
    val foods: List<Food> = emptyList(),
    val savedMeals: List<SavedMeal> = emptyList(),
    val showsRecents: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: NutritionRepository
) : ViewModel() {
    private val route = savedStateHandle.toRoute<FoodSearchRoute>()
    val meal = mealFromName(route.meal)
    private val date = LocalDate.ofEpochDay(route.date)

    // Texte du champ, mis à jour à chaque frappe sans attendre les résultats : s'il suivait le calcul de la
    // recherche (hors du fil principal), des lettres tapées vite se perdraient.
    var queryText by mutableStateOf(savedStateHandle[QUERY_KEY] ?: "")
        private set

    private val query = savedStateHandle.getStateFlow(QUERY_KEY, "")
    private val filter = savedStateHandle.getStateFlow(FILTER_KEY, FoodFilter.ALL)

    val uiState: StateFlow<FoodSearchUiState> = combine(
        query,
        filter,
        repository.observeSearchableFoods(),
        repository.observeRecentFoods(),
        repository.observeSavedMeals()
    ) { query, filter, foods, recents, savedMeals ->
        results(query, filter, foods, recents, savedMeals)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FoodSearchUiState())

    // Vrai une fois un repas enregistré ajouté : l'écran revient au journal.
    var isDone by mutableStateOf(false)
        private set

    fun onQueryChange(text: String) {
        queryText = text
        savedStateHandle[QUERY_KEY] = text
    }

    fun onFilterChange(value: FoodFilter) {
        savedStateHandle[FILTER_KEY] = value
    }

    fun addSavedMeal(id: Long) {
        viewModelScope.launch {
            repository.addSavedMeal(id, date, meal)
            isDone = true
        }
    }

    fun deleteSavedMeal(id: Long) {
        viewModelScope.launch { repository.deleteSavedMeal(id) }
    }

    private fun results(
        query: String,
        filter: FoodFilter,
        foods: List<SearchableFood>,
        recents: List<Food>,
        savedMeals: List<SavedMeal>
    ): FoodSearchUiState {
        val base = FoodSearchUiState(query = query, filter = filter, isLoading = false)
        val hasQuery = query.isNotBlank()
        return when (filter) {
            FoodFilter.ALL ->
                if (hasQuery) {
                    base.copy(
                        foods = searchFoods(query, foods)
                    )
                } else {
                    base.copy(foods = recents, showsRecents = true)
                }
            FoodFilter.RECENT ->
                base.copy(foods = if (hasQuery) searchFoods(query, recents.map(::SearchableFood)) else recents)
            FoodFilter.FAVORITES -> base.copy(foods = searchOrAll(query, foods.filter { it.food.isFavorite }))
            FoodFilter.CUSTOM -> base.copy(foods = searchOrAll(query, foods.filter { it.food.isCustom }))
            FoodFilter.SAVED_MEALS -> {
                val words = normalizeForSearch(query)
                base.copy(savedMeals = savedMeals.filter { normalizeForSearch(it.name).contains(words) })
            }
        }
    }

    private fun searchOrAll(query: String, candidates: List<SearchableFood>): List<Food> =
        if (query.isBlank()) candidates.map { it.food }.sortedBy { it.name } else searchFoods(query, candidates)

    private companion object {
        const val QUERY_KEY = "food_query"
        const val FILTER_KEY = "food_filter"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
