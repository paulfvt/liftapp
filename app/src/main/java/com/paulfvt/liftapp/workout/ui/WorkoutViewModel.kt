package com.paulfvt.liftapp.workout.ui

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.di.ApplicationScope
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.workout.data.FinishedWorkout
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import com.paulfvt.liftapp.workout.data.WorkoutSettings
import com.paulfvt.liftapp.workout.data.WorkoutSettingsRepository
import com.paulfvt.liftapp.workout.domain.PreviousSet
import com.paulfvt.liftapp.workout.domain.REST_ADD_SECONDS
import com.paulfvt.liftapp.workout.domain.REST_REMOVE_SECONDS
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.completedSetCount
import com.paulfvt.liftapp.workout.domain.incompleteSetCount
import com.paulfvt.liftapp.workout.notification.AlertPermission
import com.paulfvt.liftapp.workout.notification.WorkoutSessionSync
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val workout: Workout? = null,
    // Séries de la dernière fois, par identifiant d'exercice (colonne « Précédent »).
    val previous: Map<Long, List<PreviousSet>> = emptyMap()
)

// Boîte de dialogue ouverte, gardée dans le ViewModel pour rester ouverte après une rotation.
sealed interface WorkoutDialog {
    data object Rename : WorkoutDialog

    data object Note : WorkoutDialog

    data object Discard : WorkoutDialog

    data object FinishEmpty : WorkoutDialog

    data class FinishIncomplete(val incompleteSets: Int) : WorkoutDialog

    data class Rpe(val setId: Long, val current: Double?) : WorkoutDialog

    data class ExerciseNote(val exercise: Exercise) : WorkoutDialog
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workouts: WorkoutRepository,
    private val exercises: ExerciseRepository,
    private val settingsRepository: WorkoutSettingsRepository,
    private val sessionSync: WorkoutSessionSync,
    @param:ApplicationScope private val appScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // La dernière fois ne change pas pendant la séance : chaque exercice n'est lu qu'une fois.
    private val previousCache = mutableMapOf<Long, List<PreviousSet>>()

    val uiState: StateFlow<WorkoutUiState> = workouts.observeInProgress().map { workout ->
        WorkoutUiState(
            isLoading = false,
            workout = workout,
            previous = workout?.exercises.orEmpty().associate { item ->
                item.exercise.id to previousCache.getOrPut(item.exercise.id) {
                    workouts.previousSets(item.exercise.id, workout?.templateId)
                }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WorkoutUiState())

    // Valeur de départ « déjà demandées » : la demande de permissions n'apparaît pas avant la lecture des réglages.
    val settings: StateFlow<WorkoutSettings> = settingsRepository.settings
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            WorkoutSettings(permissionsAsked = true)
        )

    // Ce qui manque pour l'alerte écran verrouillé, revérifié à chaque retour sur l'écran (réglages Android modifiés).
    var missingPermissions by mutableStateOf(emptyList<AlertPermission>())
        private set

    var dialog by mutableStateOf<WorkoutDialog?>(null)
        private set

    var finished by mutableStateOf<FinishedWorkout?>(null)
        private set

    // Vrai dès l'appui sur « Terminer » : la séance disparaît de la base avant que le résumé soit prêt,
    // l'écran ne doit pas croire qu'elle a été fermée ailleurs.
    var isFinishing by mutableStateOf(false)
        private set

    var isTemplateUpdated by mutableStateOf(false)
        private set

    // Séance abandonnée ou résumé fermé : l'écran se ferme.
    var isClosed by mutableStateOf(false)
        private set

    @get:StringRes
    var message by mutableStateOf<Int?>(null)
        private set

    // File des écritures de la séance. Elle tourne dans le scope de l'application : une saisie n'est pas perdue
    // si l'écran se ferme juste après. Une seule coroutine la vide, donc les écritures gardent l'ordre de la frappe.
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        appScope.launch {
            for (write in writes) {
                runCatching { write() }.onFailure { Log.e(TAG, "Écriture de la séance impossible", it) }
            }
        }
    }

    // Fermer la file laisse la coroutine terminer les écritures déjà reçues.
    override fun onCleared() {
        writes.close()
    }

    private fun persist(write: suspend () -> Unit) {
        writes.trySend(write)
    }

    private suspend fun onMain(block: () -> Unit) = withContext(Dispatchers.Main) { block() }

    fun onWeightChange(setId: Long, weightKg: Double?) = persist { workouts.updateSetWeight(setId, weightKg) }

    fun onRepsChange(setId: Long, reps: Int?) = persist { workouts.updateSetReps(setId, reps) }

    fun onDurationChange(setId: Long, seconds: Int?) = persist { workouts.updateSetDuration(setId, seconds) }

    fun onDistanceChange(setId: Long, meters: Double?) = persist { workouts.updateSetDistance(setId, meters) }

    fun onSetTypeChange(setId: Long, type: SetType) = persist { workouts.updateSetType(setId, type) }

    fun onToggleCompleted(setId: Long) = persist {
        if (!workouts.toggleCompleted(setId)) onMain { message = R.string.workout_cannot_complete }
    }

    fun onAddSet(workoutExerciseId: Long) = persist { workouts.addSet(workoutExerciseId) }

    fun onAddRest() = persist { workouts.adjustRest(REST_ADD_SECONDS) }

    fun onRemoveRest() = persist { workouts.adjustRest(-REST_REMOVE_SECONDS) }

    fun onSkipRest() = persist { workouts.skipRest() }

    fun onRestSoundChange(enabled: Boolean) {
        appScope.launch { settingsRepository.setRestSound(enabled) }
    }

    fun onRestVibrationChange(enabled: Boolean) {
        appScope.launch { settingsRepository.setRestVibration(enabled) }
    }

    fun currentMissingPermissions(): List<AlertPermission> = sessionSync.missingPermissions()

    // Retour sur l'écran, par exemple depuis les réglages Android : la notification apparaît si elle vient d'être
    // autorisée, et l'alarme repasse en exacte si « Alarmes et rappels » vient d'être accordé.
    fun onResume() {
        missingPermissions = sessionSync.missingPermissions()
        appScope.launch { sessionSync.refresh() }
    }

    fun onPermissionsAsked() {
        appScope.launch { settingsRepository.markPermissionsAsked() }
        onResume()
    }

    fun onRemoveSet(setId: Long) = persist { workouts.removeSet(setId) }

    fun onRemoveExercise(workoutExerciseId: Long) = persist { workouts.removeExercise(workoutExerciseId) }

    fun onReorderExercises(orderedIds: List<Long>) = persist { workouts.reorderExercises(orderedIds) }

    // L'exercice à remplacer est retenu le temps de choisir son remplaçant dans le catalogue.
    fun onReplaceExercise(workoutExerciseId: Long) {
        savedStateHandle[PENDING_REPLACE_KEY] = workoutExerciseId
    }

    fun onExercisesPicked(exerciseIds: List<Long>) {
        val replaced = savedStateHandle.remove<Long>(PENDING_REPLACE_KEY)
        persist {
            if (replaced != null) {
                workouts.replaceExercise(replaced, exerciseIds.first())
            } else {
                workouts.inProgressId()?.let { workouts.addExercises(it, exerciseIds) }
            }
        }
    }

    fun openDialog(dialog: WorkoutDialog) {
        this.dialog = dialog
    }

    fun dismissDialog() {
        dialog = null
    }

    fun messageShown() {
        message = null
    }

    fun onRename(name: String) = closeDialogThen { workouts.inProgressId()?.let { workouts.rename(it, name) } }

    fun onNoteSave(note: String) = closeDialogThen { workouts.inProgressId()?.let { workouts.updateNote(it, note) } }

    fun onExerciseNoteSave(exerciseId: Long, note: String) = closeDialogThen { exercises.updateNote(exerciseId, note) }

    fun onRpeSelected(setId: Long, rpe: Double?) = closeDialogThen { workouts.updateSetRpe(setId, rpe) }

    // Avant de terminer : confirmation s'il reste des séries non validées, ou si aucune ne l'est.
    fun onFinishClick() {
        val workout = uiState.value.workout ?: return
        val incomplete = workout.incompleteSetCount()
        dialog = when {
            workout.completedSetCount() == 0 -> WorkoutDialog.FinishEmpty
            incomplete > 0 -> WorkoutDialog.FinishIncomplete(incomplete)
            else -> null
        }
        if (dialog == null) finish()
    }

    fun finish() {
        isFinishing = true
        closeDialogThen {
            val id = workouts.inProgressId() ?: return@closeDialogThen
            val result = workouts.finish(id)
            onMain { finished = result }
        }
    }

    fun discard() = closeDialogThen {
        workouts.inProgressId()?.let { workouts.discard(it) }
        onMain { isClosed = true }
    }

    fun onUpdateTemplate() {
        val result = finished ?: return
        val templateId = result.templateId ?: return
        val draft = result.templateUpdate ?: return
        persist {
            workouts.updateTemplate(templateId, draft)
            onMain { isTemplateUpdated = true }
        }
    }

    fun onKeepTemplate() {
        finished = finished?.copy(templateUpdate = null)
    }

    fun onCloseSummary() {
        isClosed = true
    }

    private fun closeDialogThen(write: suspend () -> Unit) {
        dialog = null
        persist(write)
    }

    private companion object {
        const val TAG = "WorkoutViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
        const val PENDING_REPLACE_KEY = "pending_replace"
    }
}
