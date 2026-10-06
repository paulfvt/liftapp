package com.paulfvt.liftapp.exercise.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.core.model.Equipment
import com.paulfvt.liftapp.core.model.ExerciseCategory
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.exercise.data.toDraft
import com.paulfvt.liftapp.exercise.domain.DraftError
import com.paulfvt.liftapp.exercise.domain.ExerciseDraft
import com.paulfvt.liftapp.exercise.domain.togglePrimary
import com.paulfvt.liftapp.exercise.domain.toggleSecondary
import com.paulfvt.liftapp.exercise.domain.validate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Sert à la création (pas d'identifiant dans la route) et à la modification d'un exercice personnalisé.
@HiltViewModel
class ExerciseEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository
) : ViewModel() {
    private val exerciseId: Long? = savedStateHandle.get<Long>("exerciseId")
    val isNew: Boolean = exerciseId == null

    var draft by mutableStateOf(ExerciseDraft())
        private set

    // Les erreurs ne s'affichent qu'après un premier essai d'enregistrement, pas pendant la saisie.
    var errors by mutableStateOf(emptySet<DraftError>())
        private set

    var isLoaded by mutableStateOf(isNew)
        private set

    var isSaved by mutableStateOf(false)
        private set

    private var hasTriedToSave = false

    init {
        if (exerciseId != null) {
            viewModelScope.launch {
                repository.observeExercise(exerciseId).first()?.let { draft = it.toDraft() }
                isLoaded = true
            }
        }
    }

    private fun updateDraft(transform: (ExerciseDraft) -> ExerciseDraft) {
        draft = transform(draft)
        if (hasTriedToSave) errors = draft.validate()
    }

    fun onNameChange(name: String) = updateDraft { it.copy(name = name) }

    fun onCategoryChange(category: ExerciseCategory) = updateDraft { it.copy(category = category) }

    fun onEquipmentChange(equipment: Equipment) = updateDraft { it.copy(equipment = equipment) }

    fun onTrackingTypeChange(trackingType: TrackingType) = updateDraft {
        it.copy(trackingType = trackingType)
    }

    fun onTogglePrimary(muscle: Muscle) = updateDraft { it.togglePrimary(muscle) }

    fun onToggleSecondary(muscle: Muscle) = updateDraft { it.toggleSecondary(muscle) }

    fun save() {
        hasTriedToSave = true
        errors = draft.validate()
        if (errors.isNotEmpty()) return
        viewModelScope.launch {
            if (exerciseId == null) repository.createCustom(draft) else repository.updateCustom(exerciseId, draft)
            isSaved = true
        }
    }
}
