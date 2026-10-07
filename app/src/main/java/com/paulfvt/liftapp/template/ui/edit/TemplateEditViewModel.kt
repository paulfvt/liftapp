package com.paulfvt.liftapp.template.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.exercise.data.ExerciseRepository
import com.paulfvt.liftapp.template.data.TemplateRepository
import com.paulfvt.liftapp.template.domain.NameError
import com.paulfvt.liftapp.template.domain.TemplateDraft
import com.paulfvt.liftapp.template.domain.addExercises
import com.paulfvt.liftapp.template.domain.moveExercise
import com.paulfvt.liftapp.template.domain.nameError
import com.paulfvt.liftapp.template.domain.parseTargetReps
import com.paulfvt.liftapp.template.domain.removeExercise
import com.paulfvt.liftapp.template.domain.updateExercise
import com.paulfvt.liftapp.template.domain.withSetCount
import com.paulfvt.liftapp.template.navigation.TemplateEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

// Création (pas de templateId dans la route) ou modification d'un template. Les changements restent
// en mémoire jusqu'à « Enregistrer », comme pour les exercices personnalisés.
@HiltViewModel
class TemplateEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val templates: TemplateRepository,
    private val exercises: ExerciseRepository
) : ViewModel() {
    private val route = savedStateHandle.toRoute<TemplateEditRoute>()
    val isNew: Boolean = route.templateId == null

    var draft by mutableStateOf(TemplateDraft())
        private set

    // Dernière version enregistrée : sert à demander confirmation avant de quitter sans enregistrer.
    private var savedDraft by mutableStateOf(TemplateDraft())
    val hasChanges: Boolean get() = draft != savedDraft

    // L'erreur de nom ne s'affiche qu'après un premier essai d'enregistrement.
    var nameError by mutableStateOf<NameError?>(null)
        private set

    var isLoaded by mutableStateOf(isNew)
        private set

    var isSaved by mutableStateOf(false)
        private set

    private var hasTriedToSave = false
    private var isSaving = false

    init {
        route.templateId?.let { id ->
            viewModelScope.launch {
                templates.getDraft(id)?.let {
                    draft = it
                    savedDraft = it
                }
                isLoaded = true
            }
        }
    }

    // Exercices cochés dans le catalogue en mode sélection (transmis par templateScreens).
    fun onExercisesPicked(ids: List<Long>) {
        viewModelScope.launch {
            val picked = exercises.getExercises(ids)
            updateDraft { it.addExercises(picked) }
        }
    }

    private fun updateDraft(transform: (TemplateDraft) -> TemplateDraft) {
        draft = transform(draft)
        if (hasTriedToSave) nameError = draft.nameError()
    }

    fun onNameChange(name: String) = updateDraft { it.copy(name = name) }

    fun onRemoveExercise(exerciseId: Long) = updateDraft { it.removeExercise(exerciseId) }

    fun onMoveExercise(fromId: Long, toId: Long) = updateDraft { it.moveExercise(fromId, toId) }

    // « Monter » (offset -1) ou « Descendre » (+1) d'un cran, pour le lecteur d'écran.
    fun onMoveExerciseBy(exerciseId: Long, offset: Int) {
        val ids = draft.exercises.map { it.exercise.id }
        val target = ids.getOrNull(ids.indexOf(exerciseId) + offset) ?: return
        onMoveExercise(exerciseId, target)
    }

    fun onSetCountChange(exerciseId: Long, count: Int) = updateDraft {
        it.updateExercise(exerciseId) { item -> item.withSetCount(count) }
    }

    fun onTargetRepsChange(exerciseId: Long, input: String) = updateDraft {
        it.updateExercise(exerciseId) { item -> item.copy(targetReps = parseTargetReps(input)) }
    }

    fun onRestChange(exerciseId: Long, seconds: Int) = updateDraft {
        it.updateExercise(exerciseId) { item -> item.copy(restSeconds = seconds) }
    }

    fun save() {
        hasTriedToSave = true
        nameError = draft.nameError()
        // isSaving évite un double enregistrement (donc un template en double) après deux appuis rapides.
        if (nameError != null || isSaving) return
        isSaving = true
        viewModelScope.launch {
            val id = route.templateId
            if (id == null) templates.createTemplate(route.folderId, draft) else templates.updateTemplate(id, draft)
            savedDraft = draft
            isSaved = true
        }
    }
}
