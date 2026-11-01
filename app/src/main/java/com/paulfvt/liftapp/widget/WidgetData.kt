package com.paulfvt.liftapp.widget

import com.paulfvt.liftapp.core.domain.Units
import com.paulfvt.liftapp.home.data.Dashboard
import com.paulfvt.liftapp.home.data.DashboardRepository
import com.paulfvt.liftapp.settings.data.AppSettingsRepository
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import com.paulfvt.liftapp.workout.domain.PreviousSet
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.WorkoutNotice
import com.paulfvt.liftapp.workout.domain.completedSetCount
import com.paulfvt.liftapp.workout.domain.matchPrevious
import com.paulfvt.liftapp.workout.domain.nextSet
import com.paulfvt.liftapp.workout.domain.summarize
import com.paulfvt.liftapp.workout.domain.toNotice
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

// Les deux états du widget : hors séance (le tableau de bord) ou en séance (ce que montre la notification).
sealed interface WidgetState {
    val units: Units

    data class Idle(val dashboard: Dashboard, override val units: Units) : WidgetState

    data class Live(val details: LiveDetails, override val units: Units) : WidgetState
}

// Séance en cours vue par le widget : la notification, plus la progression (grande taille) et ce qui a été fait la
// dernière fois pour la série à faire (« Dernière fois : 80 kg × 8 »).
data class LiveDetails(
    val notice: WorkoutNotice,
    val doneSets: Int,
    val totalSets: Int,
    val tonnageKg: Double,
    val previous: PreviousSet?
)

@Singleton
class WidgetDataSource @Inject constructor(
    private val workouts: WorkoutRepository,
    private val dashboard: DashboardRepository,
    private val appSettings: AppSettingsRepository
) {
    // Dernière fois de chaque exercice de la séance en cours : elle ne change pas pendant la séance.
    private val previousCache = mutableMapOf<Pair<Long, Long>, List<PreviousSet>>()

    // Pendant une séance, le tableau de bord n'est pas calculé : chaque saisie toucherait les séries et relancerait
    // ce calcul pour rien. flatMapLatest passe d'une source à l'autre quand une séance commence ou se termine.
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(): Flow<WidgetState> = combine(
        workouts.observeInProgress().map { it?.let { workout -> liveDetails(workout) } }.distinctUntilChanged(),
        appSettings.settings.map { it.units }.distinctUntilChanged()
    ) { details, units -> details to units }
        .flatMapLatest { (details, units) ->
            if (details != null) {
                flowOf(WidgetState.Live(details, units))
            } else {
                dashboard.observe().map { WidgetState.Idle(it, units) }
            }
        }
        .distinctUntilChanged()

    private suspend fun liveDetails(workout: Workout): LiveDetails {
        val next = workout.nextSet()
        val previous = next?.let {
            val exerciseId = it.exercise.exercise.id
            val sets = previousCache.getOrPut(workout.id to exerciseId) {
                workouts.previousSets(exerciseId, workout.templateId)
            }
            // Même correspondance que la colonne « Précédent » : la n-ième série d'un type reprend la n-ième.
            matchPrevious(it.exercise.sets.map { set -> set.type }, sets).getOrNull(it.position - 1)
        }
        return LiveDetails(
            notice = workout.toNotice(),
            doneSets = workout.completedSetCount(),
            totalSets = workout.exercises.sumOf { it.sets.size },
            tonnageKg = summarize(workout, workout.startedAt).tonnageKg,
            previous = previous
        )
    }
}

// Le widget n'est pas créé par Hilt (Android l'instancie lui-même) : il obtient ses dépendances par ce point d'entrée.
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetData(): WidgetDataSource
}
