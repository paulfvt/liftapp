package com.paulfvt.liftapp.template.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.paulfvt.liftapp.exercise.navigation.ExercisePickerRoute
import com.paulfvt.liftapp.template.ui.edit.TemplateEditScreen
import kotlinx.serialization.Serializable

// Sans templateId : création, dans le dossier folderId s'il est donné. Avec templateId : modification.
@Serializable
data class TemplateEditRoute(val templateId: Long? = null, val folderId: Long? = null)

fun NavGraphBuilder.templateScreens(navController: NavController) {
    composable<TemplateEditRoute> {
        TemplateEditScreen(
            onClose = navController::popBackStack,
            onAddExercises = { excludedIds -> navController.navigate(ExercisePickerRoute(excludedIds)) }
        )
    }
}
