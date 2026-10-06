package com.paulfvt.liftapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.ui.ComingSoonScreen
import com.paulfvt.liftapp.exercise.navigation.ExerciseListRoute
import com.paulfvt.liftapp.exercise.navigation.exerciseScreens
import com.paulfvt.liftapp.template.TemplatesScreen

// Graphe de navigation : chaque phase remplacera un ComingSoonScreen par le vrai écran.
@Composable
fun LiftNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> { ComingSoonScreen(R.string.nav_home) }
        navigation<TemplatesGraph>(startDestination = TemplatesRoute) {
            composable<TemplatesRoute> {
                TemplatesScreen(onOpenExercises = { navController.navigate(ExerciseListRoute) })
            }
            exerciseScreens(navController)
        }
        composable<HistoryRoute> { ComingSoonScreen(R.string.nav_history) }
        composable<ProgressRoute> { ComingSoonScreen(R.string.nav_progress) }
        composable<ProfileRoute> { ComingSoonScreen(R.string.nav_profile) }
    }
}
