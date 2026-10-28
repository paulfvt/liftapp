package com.paulfvt.liftapp.progress.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.paulfvt.liftapp.progress.ui.ExerciseProgressScreen
import com.paulfvt.liftapp.progress.ui.MonthlyRecapScreen
import kotlinx.serialization.Serializable

// Progression d'un exercice : records et courbes. Rangée dans l'onglet Progrès, même ouverte depuis l'historique
// ou la fiche d'un exercice.
@Serializable
data class ExerciseProgressRoute(val exerciseId: Long)

// Récap d'un mois (year, month : 2026, 10), ouvert depuis l'onglet Progrès.
@Serializable
data class MonthlyRecapRoute(val year: Int, val month: Int)

fun NavGraphBuilder.progressScreens(navController: NavController) {
    composable<ExerciseProgressRoute> {
        ExerciseProgressScreen(onBack = { navController.popBackStack<ExerciseProgressRoute>(inclusive = true) })
    }
    composable<MonthlyRecapRoute> {
        MonthlyRecapScreen(onBack = { navController.popBackStack<MonthlyRecapRoute>(inclusive = true) })
    }
}
