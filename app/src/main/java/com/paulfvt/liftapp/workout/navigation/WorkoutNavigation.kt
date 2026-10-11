package com.paulfvt.liftapp.workout.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.paulfvt.liftapp.exercise.navigation.ExercisePickerRoute
import com.paulfvt.liftapp.exercise.navigation.PICKED_EXERCISE_IDS_KEY
import com.paulfvt.liftapp.workout.ui.WorkoutScreen
import com.paulfvt.liftapp.workout.ui.WorkoutViewModel
import kotlinx.serialization.Serializable

// Séance en cours, en plein écran (sans barre du bas). Pas d'argument : il n'y a qu'une séance en cours à la fois.
@Serializable
data object WorkoutRoute

fun NavGraphBuilder.workoutScreen(navController: NavController) {
    composable<WorkoutRoute> { entry ->
        val viewModel: WorkoutViewModel = hiltViewModel()
        // Exercices choisis dans le catalogue, déposés dans le SavedStateHandle de cette entrée de navigation
        // (différent de celui du ViewModel) : on les lit ici puis on vide la clé.
        val picked by entry.savedStateHandle
            .getStateFlow<LongArray?>(PICKED_EXERCISE_IDS_KEY, null)
            .collectAsStateWithLifecycle()
        LaunchedEffect(picked) {
            picked?.let { ids ->
                entry.savedStateHandle[PICKED_EXERCISE_IDS_KEY] = null
                viewModel.onExercisesPicked(ids.toList())
            }
        }
        WorkoutScreen(
            // Dépile jusqu'à la séance incluse : un second appel (séance abandonnée et effacée en même temps)
            // ne fait plus rien au lieu de dépiler aussi l'écran d'en dessous.
            onClose = { navController.popBackStack(WorkoutRoute, inclusive = true) },
            onPickExercises = { excludedIds, replace ->
                navController.navigate(ExercisePickerRoute(excludedIds, singleSelection = replace))
            },
            viewModel = viewModel
        )
    }
}
