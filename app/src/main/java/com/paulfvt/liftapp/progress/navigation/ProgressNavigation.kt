package com.paulfvt.liftapp.progress.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.ui.ComingSoonScreen
import kotlinx.serialization.Serializable

// Progression d'un exercice : records et courbes. Rangée dans l'onglet Progrès, même ouverte depuis l'historique
// ou la fiche d'un exercice.
@Serializable
data class ExerciseProgressRoute(val exerciseId: Long)

fun NavGraphBuilder.progressScreens(@Suppress("UNUSED_PARAMETER") navController: NavController) {
    composable<ExerciseProgressRoute> { ComingSoonScreen(R.string.nav_progress) }
}
