package com.paulfvt.liftapp.history.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.paulfvt.liftapp.exercise.navigation.ExercisePickerRoute
import com.paulfvt.liftapp.exercise.navigation.PICKED_EXERCISE_IDS_KEY
import com.paulfvt.liftapp.history.ui.WorkoutDetailScreen
import com.paulfvt.liftapp.history.ui.WorkoutEditScreen
import com.paulfvt.liftapp.history.ui.WorkoutEditViewModel
import com.paulfvt.liftapp.progress.navigation.ExerciseProgressRoute
import kotlinx.serialization.Serializable

@Serializable
data class WorkoutDetailRoute(val workoutId: Long)

// Modification d'une séance terminée, en plein écran comme la séance en cours.
@Serializable
data class WorkoutEditRoute(val workoutId: Long)

// Sous-écrans de l'onglet Historique.
fun NavGraphBuilder.historyScreens(navController: NavController) {
    composable<WorkoutDetailRoute> {
        WorkoutDetailScreen(
            // Ne dépile que le détail : un second appel (séance supprimée juste après un retour) ne fait rien.
            onBack = { navController.popBackStack<WorkoutDetailRoute>(inclusive = true) },
            onEdit = { id -> navController.navigate(WorkoutEditRoute(id)) },
            onOpenProgress = { exerciseId -> navController.navigate(ExerciseProgressRoute(exerciseId)) }
        )
    }
    composable<WorkoutEditRoute> { entry ->
        val viewModel: WorkoutEditViewModel = hiltViewModel()
        // Exercices choisis dans le catalogue, déposés dans le SavedStateHandle de cette entrée de navigation
        // (même mécanisme que pour les templates).
        val picked by entry.savedStateHandle
            .getStateFlow<LongArray?>(PICKED_EXERCISE_IDS_KEY, null)
            .collectAsStateWithLifecycle()
        LaunchedEffect(picked) {
            picked?.let { ids ->
                entry.savedStateHandle[PICKED_EXERCISE_IDS_KEY] = null
                viewModel.onExercisesPicked(ids.toList())
            }
        }
        WorkoutEditScreen(
            onClose = { navController.popBackStack<WorkoutEditRoute>(inclusive = true) },
            onAddExercises = { excludedIds -> navController.navigate(ExercisePickerRoute(excludedIds)) },
            viewModel = viewModel
        )
    }
}
