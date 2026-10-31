package com.paulfvt.liftapp.settings.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.core.di.ApplicationScope
import com.paulfvt.liftapp.core.domain.LengthUnit
import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.progress.data.WeeklyGoalRepository
import com.paulfvt.liftapp.settings.data.AppSettings
import com.paulfvt.liftapp.settings.data.AppSettingsRepository
import com.paulfvt.liftapp.settings.data.BackupInspection
import com.paulfvt.liftapp.settings.data.BackupRepository
import com.paulfvt.liftapp.settings.data.CsvExportRepository
import com.paulfvt.liftapp.settings.data.ThemeMode
import com.paulfvt.liftapp.settings.domain.BackupInfo
import com.paulfvt.liftapp.settings.domain.BackupProblem
import com.paulfvt.liftapp.workout.data.WorkoutSettings
import com.paulfvt.liftapp.workout.data.WorkoutSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val app: AppSettings = AppSettings(),
    val workout: WorkoutSettings = WorkoutSettings(),
    // Objectif de la semaine en cours.
    val weeklyGoal: Int = 0
)

// Résultat d'une opération sur les données, affiché en bas de l'écran.
sealed interface DataMessage {
    data class HistoryExported(val workoutCount: Int) : DataMessage

    data class MeasurementsExported(val measurementCount: Int) : DataMessage

    data object ExportFailed : DataMessage

    data object BackupCreated : DataMessage

    data object BackupFailed : DataMessage

    data class RestoreRefused(val problem: BackupProblem) : DataMessage
}

// Écran Réglages. Les écritures partent dans le scope de l'application : un réglage changé juste avant de quitter
// l'écran est bien enregistré, et un fichier en cours d'écriture n'est pas abandonné à moitié.
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettingsRepository,
    private val workoutSettings: WorkoutSettingsRepository,
    private val weeklyGoals: WeeklyGoalRepository,
    private val csvExport: CsvExportRepository,
    private val backups: BackupRepository,
    private val clock: Clock,
    @param:ApplicationScope private val appScope: CoroutineScope
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        appSettings.settings,
        workoutSettings.settings,
        weeklyGoals.observeStreak().map { it.currentWeek.goal }.distinctUntilChanged()
    ) { app, workout, goal ->
        SettingsUiState(isLoading = false, app = app, workout = workout, weeklyGoal = goal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    private val _message = MutableStateFlow<DataMessage?>(null)
    val message: StateFlow<DataMessage?> = _message.asStateFlow()

    // Export, sauvegarde ou vérification d'une sauvegarde en cours : les actions sur les données attendent.
    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    // Sauvegarde vérifiée, en attente de la confirmation de l'utilisateur.
    private val _restoreCandidate = MutableStateFlow<BackupInfo?>(null)
    val restoreCandidate: StateFlow<BackupInfo?> = _restoreCandidate.asStateFlow()

    // Date du jour dans le nom proposé pour les fichiers (« liftapp-historique-2026-10-31.csv »).
    val today: LocalDate get() = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()

    fun onThemeModeChange(mode: ThemeMode) = write { appSettings.setThemeMode(mode) }

    fun onWeightUnitChange(unit: WeightUnit) = write { appSettings.setWeightUnit(unit) }

    fun onLengthUnitChange(unit: LengthUnit) = write { appSettings.setLengthUnit(unit) }

    // Comme depuis l'accueil : le nouvel objectif compte dès la semaine en cours.
    fun onWeeklyGoalChange(goal: Int) = write { weeklyGoals.changeGoal(goal) }

    fun onDefaultRestChange(seconds: Int) = write { appSettings.setDefaultRestSeconds(seconds) }

    fun onRestSoundChange(enabled: Boolean) = write { workoutSettings.setRestSound(enabled) }

    fun onRestVibrationChange(enabled: Boolean) = write { workoutSettings.setRestVibration(enabled) }

    fun onHideBodyWeightChange(hide: Boolean) = write { appSettings.setHideBodyWeight(hide) }

    fun onHidePhotosChange(hide: Boolean) = write { appSettings.setHidePhotos(hide) }

    fun exportHistory(target: Uri) = work(DataMessage.ExportFailed) {
        DataMessage.HistoryExported(csvExport.exportHistory(target))
    }

    fun exportMeasurements(target: Uri) = work(DataMessage.ExportFailed) {
        DataMessage.MeasurementsExported(csvExport.exportMeasurements(target))
    }

    fun createBackup(target: Uri, includePhotos: Boolean) = work(DataMessage.BackupFailed) {
        backups.create(target, includePhotos)
        DataMessage.BackupCreated
    }

    // Vérifie la sauvegarde choisie ; si elle est bonne, la confirmation s'affiche.
    fun inspectBackup(source: Uri) = work(DataMessage.RestoreRefused(BackupProblem.DAMAGED)) {
        when (val inspection = backups.inspect(source)) {
            is BackupInspection.Valid -> {
                _restoreCandidate.value = inspection.info
                null
            }
            is BackupInspection.Invalid -> DataMessage.RestoreRefused(inspection.problem)
        }
    }

    fun cancelRestore() {
        _restoreCandidate.value = null
        appScope.launch { backups.discardInspected() }
    }

    // Remplace toutes les données par la sauvegarde vérifiée : l'app redémarre.
    fun confirmRestore() {
        _restoreCandidate.value = null
        backups.restoreAndRestart()
    }

    fun onMessageShown() {
        _message.value = null
    }

    // Une seule opération sur les données à la fois. block renvoie le message à afficher (null : aucun).
    private fun work(failure: DataMessage, block: suspend () -> DataMessage?) {
        if (_isWorking.value) return
        _isWorking.value = true
        appScope.launch {
            try {
                _message.value = block()
            } catch (e: IOException) {
                Log.w(TAG, "Opération sur les données impossible", e)
                _message.value = failure
            } catch (e: SecurityException) {
                Log.w(TAG, "Fichier refusé par Android", e)
                _message.value = failure
            } finally {
                _isWorking.value = false
            }
        }
    }

    private fun write(block: suspend () -> Unit) {
        appScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val TAG = "Settings"
    }
}
