package com.paulfvt.liftapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.paulfvt.liftapp.exercise.navigation.ExerciseListRoute
import com.paulfvt.liftapp.exercise.navigation.exerciseScreens
import com.paulfvt.liftapp.history.navigation.WorkoutDetailRoute
import com.paulfvt.liftapp.history.navigation.historyScreens
import com.paulfvt.liftapp.history.ui.HistoryScreen
import com.paulfvt.liftapp.profile.navigation.AboutRoute
import com.paulfvt.liftapp.profile.navigation.MeasurementEntryRoute
import com.paulfvt.liftapp.profile.navigation.MeasurementRoute
import com.paulfvt.liftapp.profile.navigation.ProfileEditRoute
import com.paulfvt.liftapp.profile.navigation.profileScreens
import com.paulfvt.liftapp.profile.ui.ProfileScreen
import com.paulfvt.liftapp.progress.navigation.ExerciseProgressRoute
import com.paulfvt.liftapp.progress.navigation.progressScreens
import com.paulfvt.liftapp.progress.ui.ProgressScreen
import com.paulfvt.liftapp.template.navigation.TemplateEditRoute
import com.paulfvt.liftapp.template.navigation.templateScreens
import com.paulfvt.liftapp.template.ui.list.TemplatesScreen
import com.paulfvt.liftapp.workout.navigation.WorkoutRoute
import com.paulfvt.liftapp.workout.navigation.workoutScreen

// Graphe de navigation : un graphe imbriqué par onglet, plus la séance en cours à la racine.
@Composable
fun LiftNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = TemplatesGraph,
        modifier = modifier
    ) {
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
        navigation<ProfileGraph>(startDestination = ProfileRoute) {
            composable<ProfileRoute> {
                ProfileScreen(
                    onEditProfile = { navController.navigate(ProfileEditRoute) },
                    onOpenMeasurement = { type -> navController.navigate(MeasurementRoute(type.name)) },
                    onAddMeasurement = { navController.navigate(MeasurementEntryRoute()) },
                    onOpenAbout = { navController.navigate(AboutRoute) }
                )
            }
            profileScreens(navController)
        }
        // Séance en cours à la racine : elle s'ouvre depuis n'importe quel onglet (bandeau EN DIRECT).
        workoutScreen(navController)
    }
}
