package com.paulfvt.liftapp.template.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.paulfvt.liftapp.exercise.navigation.ExercisePickerRoute
import com.paulfvt.liftapp.exercise.navigation.PICKED_EXERCISE_IDS_KEY
import com.paulfvt.liftapp.template.ui.edit.TemplateEditScreen
import com.paulfvt.liftapp.template.ui.edit.TemplateEditViewModel
import kotlinx.serialization.Serializable

// Sans templateId : création, dans le dossier folderId s'il est donné. Avec templateId : modification.
@Serializable
data class TemplateEditRoute(val templateId: Long? = null, val folderId: Long? = null)

fun NavGraphBuilder.templateScreens(navController: NavController) {
    composable<TemplateEditRoute> { entry ->
        val viewModel: TemplateEditViewModel = hiltViewModel()
        // Le catalogue en mode sélection dépose les exercices cochés dans le SavedStateHandle de cette entrée
        // de navigation, qui n'est pas celui du ViewModel : on les lit ici puis on vide la clé.
        val picked by entry.savedStateHandle
            .getStateFlow<LongArray?>(PICKED_EXERCISE_IDS_KEY, null)
            .collectAsStateWithLifecycle()
        LaunchedEffect(picked) {
            picked?.let { ids ->
                entry.savedStateHandle[PICKED_EXERCISE_IDS_KEY] = null
                viewModel.onExercisesPicked(ids.toList())
            }
        }
        TemplateEditScreen(
            onClose = navController::popBackStack,
            onAddExercises = { excludedIds -> navController.navigate(ExercisePickerRoute(excludedIds)) },
            viewModel = viewModel
        )
    }
}
