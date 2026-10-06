package com.paulfvt.liftapp.exercise.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.paulfvt.liftapp.exercise.ui.detail.ExerciseDetailScreen
import com.paulfvt.liftapp.exercise.ui.edit.ExerciseEditScreen
import com.paulfvt.liftapp.exercise.ui.list.ExerciseListScreen
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
}
