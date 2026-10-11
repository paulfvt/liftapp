package com.paulfvt.liftapp.template.ui.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.core.domain.moved
import com.paulfvt.liftapp.template.data.TemplateRepository
import com.paulfvt.liftapp.template.domain.TemplateDraft
import com.paulfvt.liftapp.template.domain.TemplateFolder
import com.paulfvt.liftapp.template.domain.TemplateLibrary
import com.paulfvt.liftapp.template.domain.TemplateSummary
import com.paulfvt.liftapp.template.domain.copyName
import com.paulfvt.liftapp.template.domain.sectionContaining
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TemplatesUiState(
    val isLoading: Boolean = true,
    val library: TemplateLibrary = TemplateLibrary(unfiled = emptyList(), folders = emptyList())
) {
    val isEmpty: Boolean get() = library.unfiled.isEmpty() && library.folders.isEmpty()
}

// Boîte de dialogue ouverte. Gardée dans le ViewModel pour rester ouverte après une rotation de l'écran.
sealed interface TemplatesDialog {
    data object CreateFolder : TemplatesDialog

    data class RenameFolder(val folder: TemplateFolder) : TemplatesDialog

    data class DeleteFolder(val folder: TemplateFolder) : TemplatesDialog

    data class RenameTemplate(val template: TemplateSummary) : TemplatesDialog

    data class MoveTemplate(val template: TemplateSummary) : TemplatesDialog

    data class DeleteTemplate(val template: TemplateSummary) : TemplatesDialog

    data object WorkoutAlreadyRunning : TemplatesDialog
}

// Aperçu d'un template avant de démarrer la séance : ses exercices et leurs séries.
data class TemplatePreview(val templateId: Long, val draft: TemplateDraft)

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val repository: TemplateRepository,
    private val workouts: WorkoutRepository
) : ViewModel() {
    val uiState: StateFlow<TemplatesUiState> = repository.observeLibrary()
        .map { TemplatesUiState(isLoading = false, library = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TemplatesUiState())

    var dialog by mutableStateOf<TemplatesDialog?>(null)
        private set

    var preview by mutableStateOf<TemplatePreview?>(null)
        private set

    // Vrai quand une séance vient de démarrer : l'écran ouvre alors la séance puis le remet à faux.
    var isWorkoutStarted by mutableStateOf(false)
        private set

    // Évite de démarrer deux séances sur un double appui.
    private var isStarting = false

    fun openPreview(templateId: Long) {
        viewModelScope.launch {
            repository.getDraft(templateId)?.let { preview = TemplatePreview(templateId, it) }
        }
    }

    fun closePreview() {
        preview = null
    }

    fun startFromTemplate(templateId: Long) = start { workouts.startFromTemplate(templateId) }

    // emptyName est le nom « Séance libre » déjà traduit par l'écran.
    fun startEmpty(emptyName: String) = start { workouts.startEmpty(emptyName) }

    fun onWorkoutOpened() {
        isWorkoutStarted = false
    }

    // Une seule séance à la fois : si une autre est en cours, on propose de la reprendre.
    private fun start(startWorkout: suspend () -> Unit) {
        if (isStarting) return
        isStarting = true
        preview = null
        viewModelScope.launch {
            if (workouts.inProgressId() != null) {
                dialog = TemplatesDialog.WorkoutAlreadyRunning
            } else {
                startWorkout()
                isWorkoutStarted = true
            }
            isStarting = false
        }
    }

    fun openDialog(dialog: TemplatesDialog) {
        this.dialog = dialog
    }

    fun dismissDialog() {
        dialog = null
    }

    fun createFolder(name: String) = closeDialogThen { repository.createFolder(name) }

    fun renameFolder(folderId: Long, name: String) = closeDialogThen { repository.renameFolder(folderId, name) }

    fun deleteFolder(folderId: Long) = closeDialogThen { repository.deleteFolder(folderId) }

    fun renameTemplate(templateId: Long, name: String) = closeDialogThen { repository.renameTemplate(templateId, name) }

    fun moveTemplateToFolder(template: TemplateSummary, folderId: Long?) = closeDialogThen {
        if (folderId != template.folderId) repository.moveTemplateToFolder(template.id, folderId)
    }

    fun deleteTemplate(templateId: Long) = closeDialogThen { repository.deleteTemplate(templateId) }

    // formattedCopyName est le texte « Copie de … » déjà traduit par l'écran.
    fun duplicateTemplate(templateId: Long, formattedCopyName: String) {
        viewModelScope.launch { repository.duplicateTemplate(templateId, copyName(formattedCopyName)) }
    }

    // Ordre complet d'une section, à la fin d'un glisser-déposer.
    fun reorderTemplates(orderedIds: List<Long>) {
        viewModelScope.launch { repository.reorderTemplates(orderedIds) }
    }

    // « Monter » (offset -1) ou « Descendre » (+1) d'un cran, pour le lecteur d'écran.
    fun moveTemplateBy(templateId: Long, offset: Int) {
        val ids = uiState.value.library.sectionContaining(templateId)?.map { it.id } ?: return
        val from = ids.indexOf(templateId)
        if (from + offset in ids.indices) reorderTemplates(ids.moved(from, from + offset))
    }

    fun moveFolderBy(folderId: Long, offset: Int) {
        val ids = uiState.value.library.folders.map { it.folder.id }
        val from = ids.indexOf(folderId)
        if (from == -1 || from + offset !in ids.indices) return
        viewModelScope.launch { repository.reorderFolders(ids.moved(from, from + offset)) }
    }

    private fun closeDialogThen(action: suspend () -> Unit) {
        dialog = null
        viewModelScope.launch { action() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
