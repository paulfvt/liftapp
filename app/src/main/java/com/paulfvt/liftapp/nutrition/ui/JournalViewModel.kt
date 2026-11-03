package com.paulfvt.liftapp.nutrition.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.core.model.Meal
import com.paulfvt.liftapp.nutrition.data.NutritionRepository
import com.paulfvt.liftapp.nutrition.domain.JournalEntry
import com.paulfvt.liftapp.nutrition.domain.MealJournal
import com.paulfvt.liftapp.nutrition.domain.Nutrients
import com.paulfvt.liftapp.nutrition.domain.NutritionTarget
import com.paulfvt.liftapp.nutrition.domain.goalOn
import com.paulfvt.liftapp.nutrition.domain.mealsOf
import com.paulfvt.liftapp.nutrition.domain.total
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JournalUiState(
    val date: LocalDate,
    val today: LocalDate,
    val meals: List<MealJournal> = mealsOf(emptyList()),
    // Objectif en vigueur ce jour-là ; nul tant qu'aucun n'a été défini.
    val goal: NutritionTarget? = null,
    val isLoading: Boolean = true
) {
    val total: Nutrients get() = meals.map { it.total }.total()
    val isToday: Boolean get() = date >= today
}

// Message bref affiché en bas de l'écran après « Copier hier » ou l'enregistrement d'un repas.
sealed interface JournalMessage {
    data class Copied(val count: Int) : JournalMessage

    data object NothingToCopy : JournalMessage

    data object MealSaved : JournalMessage
}

// Journal d'un jour : aujourd'hui à l'ouverture, les jours passés avec les flèches (jamais dans le futur).
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: NutritionRepository,
    private val clock: Clock
) : ViewModel() {
    private val today: LocalDate get() = LocalDate.now(clock.withZone(ZoneId.systemDefault()))

    // Jour affiché, gardé après une rotation ; null = aujourd'hui (suit le changement de jour si l'app reste ouverte).
    private val shownDay = savedStateHandle.getStateFlow<Long?>(DATE_KEY, null)

    val uiState: StateFlow<JournalUiState> = shownDay.flatMapLatest { epochDay ->
        val date = epochDay?.let(LocalDate::ofEpochDay) ?: today
        combine(repository.observeDay(date), repository.observeGoals()) { entries, goals ->
            JournalUiState(
                date = date,
                today = today,
                meals = mealsOf(entries),
                goal = goalOn(date, goals)?.target,
                isLoading = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), JournalUiState(today, today))

    var message by mutableStateOf<JournalMessage?>(null)
        private set

    fun showPreviousDay() = moveBy(-1)

    fun showNextDay() = moveBy(1)

    private fun moveBy(days: Long) {
        val next = uiState.value.date.plusDays(days)
        savedStateHandle[DATE_KEY] = if (next >= today) null else next.toEpochDay()
    }

    fun copyPreviousDay() {
        val date = uiState.value.date
        viewModelScope.launch {
            val count = repository.copyPreviousDay(date)
            message = if (count == 0) JournalMessage.NothingToCopy else JournalMessage.Copied(count)
        }
    }

    fun saveMeal(name: String, entries: List<JournalEntry>) {
        viewModelScope.launch {
            repository.saveMeal(name, entries)
            message = JournalMessage.MealSaved
        }
    }

    fun onMessageShown() {
        message = null
    }

    // Repas dont l'utilisateur choisit le nom pour l'enregistrer dans « Mes repas ».
    var mealToSave by mutableStateOf<Meal?>(null)
        private set

    fun askMealName(meal: Meal) {
        mealToSave = meal
    }

    fun dismissMealName() {
        mealToSave = null
    }

    private companion object {
        const val DATE_KEY = "journal_date"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
