package com.paulfvt.liftapp.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.core.domain.DailyValue
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.profile.data.MeasurementRepository
import com.paulfvt.liftapp.profile.domain.BodyMeasurement
import com.paulfvt.liftapp.profile.domain.MeasurementSummary
import com.paulfvt.liftapp.profile.domain.dailyValues
import com.paulfvt.liftapp.profile.domain.movingAverage
import com.paulfvt.liftapp.profile.domain.summaries
import com.paulfvt.liftapp.profile.navigation.MeasurementRoute
import com.paulfvt.liftapp.progress.domain.ChartPeriod
import com.paulfvt.liftapp.progress.domain.within
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MeasurementUiState(
    val type: MeasurementType,
    val isLoading: Boolean = true,
    val summary: MeasurementSummary? = null,
    // Les plus récentes d'abord.
    val entries: List<BodyMeasurement> = emptyList(),
    val period: ChartPeriod = ChartPeriod.THREE_MONTHS,
    val chartDays: List<DailyValue> = emptyList(),
    // Moyenne mobile sur 7 jours, pour le poids seulement.
    val trend: List<DailyValue> = emptyList()
)

// Une mesure (poids, tour de taille…) : dernière valeur, courbe et historique des saisies.
@HiltViewModel
class MeasurementViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val measurements: MeasurementRepository,
    private val clock: Clock
) : ViewModel() {
    private val type = MeasurementType.entries.first {
        it.name == savedStateHandle.toRoute<MeasurementRoute>().type
    }

    // Période gardée dans le SavedStateHandle : elle survit à une rotation et à l'arrêt de l'app en arrière-plan.
    private val period = savedStateHandle.getStateFlow(PERIOD_KEY, ChartPeriod.THREE_MONTHS)

    val uiState: StateFlow<MeasurementUiState> = combine(measurements.observe(type), period) { entries, period ->
        val zone = ZoneId.systemDefault()
        val today = clock.instant().atZone(zone).toLocalDate()
        val daily = dailyValues(entries, zone)
        // La moyenne est calculée sur toute la série avant le découpage : le premier point de la période tient
        // compte des pesées des jours d'avant.
        val trend = if (type == MeasurementType.BODY_WEIGHT) movingAverage(daily) else emptyList()
        MeasurementUiState(
            type = type,
            isLoading = false,
            summary = summaries(entries).firstOrNull(),
            entries = entries,
            period = period,
            chartDays = daily.within(period, today),
            trend = trend.within(period, today)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MeasurementUiState(type))

    fun onPeriodSelected(period: ChartPeriod) {
        savedStateHandle[PERIOD_KEY] = period
    }

    fun delete(id: Long) {
        viewModelScope.launch { measurements.delete(id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val PERIOD_KEY = "period"
    }
}
