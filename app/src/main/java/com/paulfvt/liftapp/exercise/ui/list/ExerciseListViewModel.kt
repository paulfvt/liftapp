package com.paulfvt.liftapp.exercise.ui.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.exercise.domain.ExerciseQuery
import com.paulfvt.liftapp.exercise.domain.MuscleFilter
import com.paulfvt.liftapp.exercise.domain.matches
import com.paulfvt.liftapp.exercise.domain.normalizeForSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ExerciseFilters(
    val muscle: MuscleFilter? = null,
    val equipment: Equipment? = null,
    val showHidden: Boolean = false
)

data class ExerciseListUiState(
    val isLoading: Boolean = true,
    val exercises: List<Exercise> = emptyList(),
    val filters: ExerciseFilters = ExerciseFilters()
)

private class SearchableExercise(val exercise: Exercise, val normalizedName: String)

@HiltViewModel
class ExerciseListViewModel @Inject constructor(repository: ExerciseRepository) : ViewModel() {
    // État Compose plutôt que Flow pour le texte saisi : le champ se met à jour sans décalage du curseur.
    var searchText by mutableStateOf("")
        private set

    private val filters = MutableStateFlow(ExerciseFilters())

    // Les noms sont simplifiés une seule fois par chargement, pas à chaque lettre tapée.
    private val searchableExercises = repository.observeExercises().map { exercises ->
        exercises.map { SearchableExercise(it, normalizeForSearch(it.name)) }
    }

    val uiState: StateFlow<ExerciseListUiState> =
        combine(
            searchableExercises,
            snapshotFlow {
                searchText
            },
            filters
        ) { all, text, currentFilters ->
            val query = ExerciseQuery(
                text = text,
                muscle = currentFilters.muscle,
                equipment = currentFilters.equipment,
                showHidden = currentFilters.showHidden
            )
            ExerciseListUiState(
                // Base vide : le catalogue est encore en cours d'import au premier lancement.
                isLoading = all.isEmpty(),
                exercises = all.filter {
                    it.exercise.matches(query, it.normalizedName)
                }.map { it.exercise },
                filters = currentFilters
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            ExerciseListUiState()
        )

    fun onSearchTextChange(text: String) {
        searchText = text
    }

    fun onMuscleFilterChange(filter: MuscleFilter?) = filters.update { it.copy(muscle = filter) }

    fun onEquipmentFilterChange(equipment: Equipment?) = filters.update {
        it.copy(equipment = equipment)
    }

    fun onToggleShowHidden() = filters.update { it.copy(showHidden = !it.showHidden) }

    private companion object {
        // Garde le flux actif 5 s après la disparition de l'écran : une rotation ne relance pas tout.
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
