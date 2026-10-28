package com.paulfvt.liftapp.progress.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.progress.data.MotivationRepository
import com.paulfvt.liftapp.progress.domain.ExerciseHistory
import com.paulfvt.liftapp.progress.domain.MonthlyRecap
import com.paulfvt.liftapp.progress.domain.monthlyRecap
import com.paulfvt.liftapp.progress.domain.recapMonths
import com.paulfvt.liftapp.progress.navigation.MonthlyRecapRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

// previousMonth et nextMonth : mois voisins qui ont des séances (ou le mois en cours), null au bout de la liste.
data class MonthlyRecapUiState(
    val isLoading: Boolean = true,
    val recap: MonthlyRecap? = null,
    val previousMonth: YearMonth? = null,
    val nextMonth: YearMonth? = null
)

// Récap d'un mois : séances, tonnage et son équivalence, records du mois, exercice favori.
@HiltViewModel
class MonthlyRecapViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    motivation: MotivationRepository,
    private val clock: Clock
) : ViewModel() {
    private val route = savedStateHandle.toRoute<MonthlyRecapRoute>()

    // Mois affiché, en texte (« 2026-10 »), sous une clé qui n'est pas un argument de la route.
    private val shownMonth =
        savedStateHandle.getStateFlow(SHOWN_MONTH_KEY, YearMonth.of(route.year, route.month).toString())

    val uiState: StateFlow<MonthlyRecapUiState> = combine(motivation.observeHistories(), shownMonth, ::recapState)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MonthlyRecapUiState())

    private fun recapState(histories: List<ExerciseHistory>, shown: String): MonthlyRecapUiState {
        val zone = ZoneId.systemDefault()
        val month = YearMonth.parse(shown)
        val months = recapMonths(histories, YearMonth.from(clock.instant().atZone(zone)), zone)
        return MonthlyRecapUiState(
            isLoading = false,
            recap = monthlyRecap(histories, month, zone),
            previousMonth = months.firstOrNull { it < month },
            nextMonth = months.lastOrNull { it > month }
        )
    }

    fun onMonthSelected(month: YearMonth) {
        savedStateHandle[SHOWN_MONTH_KEY] = month.toString()
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val SHOWN_MONTH_KEY = "shown_month"
    }
}
