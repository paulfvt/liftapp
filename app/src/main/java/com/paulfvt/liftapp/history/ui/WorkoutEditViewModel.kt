package com.paulfvt.liftapp.history.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.history.data.HistoryRepository
import com.paulfvt.liftapp.history.domain.EditProblem
import com.paulfvt.liftapp.history.domain.addExercises
import com.paulfvt.liftapp.history.domain.addSet
import com.paulfvt.liftapp.history.domain.editProblem
import com.paulfvt.liftapp.history.domain.removeExercise
import com.paulfvt.liftapp.history.domain.removeSet
import com.paulfvt.liftapp.history.domain.updateSet
import com.paulfvt.liftapp.history.domain.updateSetValues
import com.paulfvt.liftapp.history.navigation.WorkoutEditRoute
import com.paulfvt.liftapp.workout.domain.Workout
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

// RPE en cours de choix pour une série : gardé ici pour que la boîte de dialogue survive à une rotation.
data class RpeChoice(val setId: Long, val current: Double?)

// Modification d'une séance terminée. Les changements restent en mémoire jusqu'à « Enregistrer », qui réécrit la
// séance et recalcule les records en une fois (HistoryRepository.save).
@HiltViewModel
class WorkoutEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val history: HistoryRepository,
    private val exercises: ExerciseRepository
) : ViewModel() {
    private val workoutId = savedStateHandle.toRoute<WorkoutEditRoute>().workoutId

    var draft by mutableStateOf<Workout?>(null)
        private set

    // Version enregistrée : sert à demander confirmation avant de quitter sans enregistrer.
    private var savedDraft by mutableStateOf<Workout?>(null)
    val hasChanges: Boolean get() = draft != savedDraft

    // Affiché seulement après un premier essai d'enregistrement, puis mis à jour à chaque changement.
    var problem by mutableStateOf<EditProblem?>(null)
        private set

    var rpeChoice by mutableStateOf<RpeChoice?>(null)
        private set

    var isSaved by mutableStateOf(false)
        private set

    private var hasTriedToSave = false
    private var isSaving = false

    // Identifiants provisoires (négatifs) des séries et exercices ajoutés : la base leur en donnera de vrais.
    private var nextTemporaryId = -1L
    private val newId = { nextTemporaryId-- }

    init {
        viewModelScope.launch {
            history.getWorkout(workoutId)?.let {
                draft = it
                savedDraft = it
            }
        }
    }

    private fun update(transform: (Workout) -> Workout) {
        val current = draft ?: return
        draft = transform(current)
        if (hasTriedToSave) problem = draft?.editProblem()
    }

    fun onNameChange(name: String) = update { it.copy(name = name) }

    fun onNoteChange(note: String) = update { it.copy(note = note) }

    fun onWeightChange(setId: Long, weightKg: Double?) = update { w ->
        w.updateSetValues(setId) { it.copy(weightKg = weightKg) }
    }

    fun onRepsChange(setId: Long, reps: Int?) = update { w -> w.updateSetValues(setId) { it.copy(reps = reps) } }

    fun onDurationChange(setId: Long, seconds: Int?) =
        update { w -> w.updateSetValues(setId) { it.copy(durationSec = seconds) } }

    fun onDistanceChange(setId: Long, meters: Double?) =
        update { w -> w.updateSetValues(setId) { it.copy(distanceM = meters) } }

    fun onSetTypeChange(setId: Long, type: SetType) = update { w -> w.updateSet(setId) { it.copy(type = type) } }

    fun onAddSet(workoutExerciseId: Long) = update { it.addSet(workoutExerciseId, newId) }

    fun onRemoveSet(setId: Long) = update { it.removeSet(setId) }

    fun onRemoveExercise(workoutExerciseId: Long) = update { it.removeExercise(workoutExerciseId) }

    // Exercices cochés dans le catalogue en mode sélection (transmis par historyScreens).
    fun onExercisesPicked(ids: List<Long>) {
        viewModelScope.launch {
            val picked = exercises.getExercises(ids)
            update { it.addExercises(picked, newId) }
        }
    }

    fun openRpe(setId: Long, current: Double?) {
        rpeChoice = RpeChoice(setId, current)
    }

    fun dismissRpe() {
        rpeChoice = null
    }

    fun onRpeSelected(setId: Long, rpe: Double?) {
        rpeChoice = null
        update { w -> w.updateSet(setId) { it.copy(rpe = rpe) } }
    }

    fun save() {
        val current = draft ?: return
        hasTriedToSave = true
        problem = current.editProblem()
        // isSaving évite un double enregistrement après deux appuis rapides.
        if (problem != null || isSaving) return
        isSaving = true
        viewModelScope.launch {
            history.save(current)
            savedDraft = current
            isSaved = true
        }
    }
}
