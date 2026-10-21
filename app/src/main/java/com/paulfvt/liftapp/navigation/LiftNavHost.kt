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
import com.paulfvt.liftapp.history.navigation.WorkoutDetailRoute
import com.paulfvt.liftapp.history.navigation.historyScreens
import com.paulfvt.liftapp.history.ui.HistoryScreen
import com.paulfvt.liftapp.progress.navigation.ExerciseProgressRoute
import com.paulfvt.liftapp.progress.navigation.progressScreens
import com.paulfvt.liftapp.progress.ui.ProgressScreen
import com.paulfvt.liftapp.template.navigation.TemplateEditRoute
import com.paulfvt.liftapp.template.navigation.templateScreens
import com.paulfvt.liftapp.template.ui.list.TemplatesScreen
import com.paulfvt.liftapp.workout.navigation.WorkoutRoute
import com.paulfvt.liftapp.workout.navigation.workoutScreen

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
                TemplatesScreen(
                    onOpenTemplate = { id -> navController.navigate(TemplateEditRoute(templateId = id)) },
                    onCreateTemplate = { folderId -> navController.navigate(TemplateEditRoute(folderId = folderId)) },
                    onOpenCatalog = { navController.navigate(ExerciseListRoute) },
                    onOpenWorkout = { navController.navigate(WorkoutRoute) { launchSingleTop = true } }
                )
            }
            templateScreens(navController)
            exerciseScreens(navController)
        }
        navigation<HistoryGraph>(startDestination = HistoryRoute) {
            composable<HistoryRoute> {
                HistoryScreen(onOpenWorkout = { id -> navController.navigate(WorkoutDetailRoute(id)) })
            }
            historyScreens(navController)
        }
        navigation<ProgressGraph>(startDestination = ProgressRoute) {
            composable<ProgressRoute> {
                ProgressScreen(onOpenExercise = { id -> navController.navigate(ExerciseProgressRoute(id)) })
            }
            progressScreens(navController)
        }
        composable<ProfileRoute> { ComingSoonScreen(R.string.nav_profile) }
        // Séance en cours à la racine : elle s'ouvre depuis n'importe quel onglet (bandeau EN DIRECT).
        workoutScreen(navController)
    }
}
