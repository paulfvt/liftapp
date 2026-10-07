package com.paulfvt.liftapp.exercise.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.paulfvt.liftapp.exercise.ui.detail.ExerciseDetailScreen
import com.paulfvt.liftapp.exercise.ui.edit.ExerciseEditScreen
import com.paulfvt.liftapp.exercise.ui.list.ExerciseListScreen
import com.paulfvt.liftapp.exercise.ui.list.ExercisePickerScreen
import kotlinx.serialization.Serializable

@Serializable
data object ExerciseListRoute

@Serializable
data class ExerciseDetailRoute(val exerciseId: Long)

@Serializable
data object ExerciseCreateRoute

// Même écran que la création ; le nom du paramètre est lu par ExerciseEditViewModel.
@Serializable
data class ExerciseEditRoute(val exerciseId: Long)

// Catalogue en mode sélection multiple. Les exercices cochés sont renvoyés à l'écran précédent,
// dans son SavedStateHandle sous la clé PICKED_EXERCISE_IDS_KEY.
@Serializable
data class ExercisePickerRoute(val excludedIds: List<Long>)

const val PICKED_EXERCISE_IDS_KEY = "picked_exercise_ids"

// Écrans du catalogue, ajoutés au graphe de l'onglet qui les ouvre (Séances pour l'instant).
fun NavGraphBuilder.exerciseScreens(navController: NavController) {
    composable<ExerciseListRoute> {
        ExerciseListScreen(
            onBack = navController::popBackStack,
            onExerciseClick = { id -> navController.navigate(ExerciseDetailRoute(id)) },
            onCreateClick = { navController.navigate(ExerciseCreateRoute) }
        )
    }
    composable<ExerciseDetailRoute> {
        ExerciseDetailScreen(
            onBack = navController::popBackStack,
            onEdit = { id -> navController.navigate(ExerciseEditRoute(id)) }
        )
    }
    composable<ExerciseCreateRoute> {
        ExerciseEditScreen(
            onBack = navController::popBackStack,
            onSaved = navController::popBackStack
        )
    }
    composable<ExerciseEditRoute> {
        ExerciseEditScreen(
            onBack = navController::popBackStack,
            onSaved = navController::popBackStack
        )
    }
    composable<ExercisePickerRoute> { entry ->
        ExercisePickerScreen(
            excludedIds = entry.toRoute<ExercisePickerRoute>().excludedIds.toSet(),
            onCancel = navController::popBackStack,
            onConfirm = { ids ->
                navController.previousBackStackEntry?.savedStateHandle?.set(PICKED_EXERCISE_IDS_KEY, ids.toLongArray())
                navController.popBackStack()
            }
        )
    }
}
