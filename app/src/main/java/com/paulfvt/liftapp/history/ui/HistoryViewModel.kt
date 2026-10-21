package com.paulfvt.liftapp.history.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.history.data.HistoryRepository
import com.paulfvt.liftapp.history.domain.HistoryItem
import com.paulfvt.liftapp.history.domain.MonthGroup
import com.paulfvt.liftapp.history.domain.groupByMonth
import com.paulfvt.liftapp.history.domain.toHistoryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(val isLoading: Boolean = true, val months: List<MonthGroup<HistoryItem>> = emptyList())

@HiltViewModel
class HistoryViewModel @Inject constructor(history: HistoryRepository) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = history.observeFinished().map { workouts ->
        HistoryUiState(
            isLoading = false,
            months = groupByMonth(workouts.map { it.toHistoryItem() }, ZoneId.systemDefault()) { it.startedAt }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
