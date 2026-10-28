package com.paulfvt.liftapp.widget

import com.paulfvt.liftapp.home.data.Dashboard
import com.paulfvt.liftapp.home.data.DashboardRepository
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import com.paulfvt.liftapp.workout.domain.WorkoutNotice
import com.paulfvt.liftapp.workout.domain.toNotice
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

// Les deux états du widget : hors séance (le tableau de bord) ou en séance (ce que montre la notification).
sealed interface WidgetState {
    data class Idle(val dashboard: Dashboard) : WidgetState

    data class Live(val notice: WorkoutNotice) : WidgetState
}

@Singleton
class WidgetDataSource @Inject constructor(
    private val workouts: WorkoutRepository,
    private val dashboard: DashboardRepository
) {
    // Pendant une séance, le tableau de bord n'est pas calculé : chaque saisie toucherait les séries et relancerait
    // ce calcul pour rien. flatMapLatest passe d'une source à l'autre quand une séance commence ou se termine.
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<WidgetState> = workouts.observeInProgress()
        .map { it?.toNotice() }
        .distinctUntilChanged()
        .flatMapLatest { notice ->
            if (notice != null) flowOf(WidgetState.Live(notice)) else dashboard.observe().map { WidgetState.Idle(it) }
        }
        .distinctUntilChanged()
}

// Le widget n'est pas créé par Hilt (Android l'instancie lui-même) : il obtient ses dépendances par ce point d'entrée.
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetData(): WidgetDataSource
}
