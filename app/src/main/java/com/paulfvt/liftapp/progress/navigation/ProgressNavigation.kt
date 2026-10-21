package com.paulfvt.liftapp.progress.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.paulfvt.liftapp.progress.ui.ExerciseProgressScreen
import kotlinx.serialization.Serializable

// Progression d'un exercice : records et courbes. Rangée dans l'onglet Progrès, même ouverte depuis l'historique
// ou la fiche d'un exercice.
@Serializable
data class ExerciseProgressRoute(val exerciseId: Long)

fun NavGraphBuilder.progressScreens(navController: NavController) {
    composable<ExerciseProgressRoute> {
        ExerciseProgressScreen(onBack = { navController.popBackStack<ExerciseProgressRoute>(inclusive = true) })
    }
}
