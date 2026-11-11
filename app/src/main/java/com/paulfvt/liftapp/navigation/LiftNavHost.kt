package com.paulfvt.liftapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.paulfvt.liftapp.exercise.navigation.ExerciseListRoute
import com.paulfvt.liftapp.exercise.navigation.exerciseScreens
import com.paulfvt.liftapp.history.navigation.WorkoutDetailRoute
import com.paulfvt.liftapp.history.navigation.historyScreens
import com.paulfvt.liftapp.home.ui.HomeScreen
import com.paulfvt.liftapp.nutrition.navigation.FoodQuantityRoute
import com.paulfvt.liftapp.nutrition.navigation.FoodSearchRoute
import com.paulfvt.liftapp.nutrition.navigation.NutritionNeedsRoute
import com.paulfvt.liftapp.nutrition.navigation.NutritionTrendRoute
import com.paulfvt.liftapp.nutrition.navigation.nutritionScreens
import com.paulfvt.liftapp.nutrition.ui.JournalScreen
import com.paulfvt.liftapp.profile.navigation.AboutRoute
import com.paulfvt.liftapp.profile.navigation.MeasurementEntryRoute
import com.paulfvt.liftapp.profile.navigation.MeasurementRoute
import com.paulfvt.liftapp.profile.navigation.PhotosRoute
import com.paulfvt.liftapp.profile.navigation.ProfileEditRoute
import com.paulfvt.liftapp.profile.navigation.SettingsRoute
import com.paulfvt.liftapp.profile.navigation.profileScreens
import com.paulfvt.liftapp.profile.ui.ProfileScreen
import com.paulfvt.liftapp.progress.navigation.ExerciseProgressRoute
import com.paulfvt.liftapp.progress.navigation.MonthlyRecapRoute
import com.paulfvt.liftapp.progress.navigation.progressScreens
import com.paulfvt.liftapp.progress.ui.ProgressScreen
import com.paulfvt.liftapp.template.navigation.TemplateEditRoute
import com.paulfvt.liftapp.template.navigation.templateScreens
import com.paulfvt.liftapp.template.ui.list.TemplatesScreen
import com.paulfvt.liftapp.workout.navigation.WorkoutRoute
import com.paulfvt.liftapp.workout.navigation.workoutScreen

// Clé du SavedStateHandle de TemplatesRoute : vrai tant que la liste doit descendre jusqu'à l'historique.
private const val SCROLL_TO_HISTORY_KEY = "scroll_to_history"

// « Tout voir » de l'accueil : ouvre l'onglet Séances et descend jusqu'à l'historique, sous les templates.
private fun NavHostController.navigateToHistory() {
    navigateToTopLevel(TopLevelDestination.TEMPLATES)
    getBackStackEntry<TemplatesRoute>().savedStateHandle[SCROLL_TO_HISTORY_KEY] = true
}

// Graphe de navigation : un graphe imbriqué par onglet, plus la séance en cours à la racine.
@Composable
fun LiftNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = HomeGraph,
        modifier = modifier
    ) {
        navigation<HomeGraph>(startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(
                    onOpenWorkout = { navController.navigate(WorkoutRoute) { launchSingleTop = true } },
                    onOpenExercise = { id -> navController.navigate(ExerciseProgressRoute(id)) },
                    onOpenHistory = navController::navigateToHistory,
                    onOpenNutrition = { navController.navigateToTopLevel(TopLevelDestination.NUTRITION) }
                )
            }
        }
        navigation<NutritionGraph>(startDestination = NutritionRoute) {
            composable<NutritionRoute> {
                JournalScreen(
                    onAddFood = { date, meal -> navController.navigate(FoodSearchRoute(date.toEpochDay(), meal.name)) },
                    onOpenEntry = { entry ->
                        navController.navigate(
                            FoodQuantityRoute(entry.foodId, entry.date.toEpochDay(), entry.meal.name, entry.id)
                        )
                    },
                    onOpenNeeds = { navController.navigate(NutritionNeedsRoute) },
                    onOpenTrend = { navController.navigate(NutritionTrendRoute) }
                )
            }
            nutritionScreens(navController, backToJournal = {
                navController.popBackStack<NutritionRoute>(inclusive = false)
            })
        }
        navigation<TemplatesGraph>(startDestination = TemplatesRoute) {
            composable<TemplatesRoute> { entry ->
                // Demande de « Tout voir » sur l'accueil, déposée par navigateToHistory dans cette entrée.
                val scrollToHistory by entry.savedStateHandle
                    .getStateFlow(SCROLL_TO_HISTORY_KEY, false)
                    .collectAsStateWithLifecycle()
                TemplatesScreen(
                    onOpenTemplate = { id -> navController.navigate(TemplateEditRoute(templateId = id)) },
                    onCreateTemplate = { folderId -> navController.navigate(TemplateEditRoute(folderId = folderId)) },
                    onOpenCatalog = { navController.navigate(ExerciseListRoute) },
                    onOpenWorkout = { navController.navigate(WorkoutRoute) { launchSingleTop = true } },
                    onOpenWorkoutDetail = { id -> navController.navigate(WorkoutDetailRoute(id)) },
                    scrollToHistory = scrollToHistory,
                    onScrolledToHistory = { entry.savedStateHandle[SCROLL_TO_HISTORY_KEY] = false }
                )
            }
            templateScreens(navController)
            exerciseScreens(navController)
            historyScreens(navController)
        }
        navigation<ProgressGraph>(startDestination = ProgressRoute) {
            composable<ProgressRoute> {
                ProgressScreen(
                    onOpenExercise = { id -> navController.navigate(ExerciseProgressRoute(id)) },
                    onOpenRecap = { month -> navController.navigate(MonthlyRecapRoute(month.year, month.monthValue)) }
                )
            }
            progressScreens(navController)
        }
        navigation<ProfileGraph>(startDestination = ProfileRoute) {
            composable<ProfileRoute> {
                ProfileScreen(
                    onEditProfile = { navController.navigate(ProfileEditRoute) },
                    onOpenMeasurement = { type -> navController.navigate(MeasurementRoute(type.name)) },
                    onAddMeasurement = { navController.navigate(MeasurementEntryRoute()) },
                    onOpenPhotos = { navController.navigate(PhotosRoute) },
                    onOpenAbout = { navController.navigate(AboutRoute) },
                    onOpenSettings = { navController.navigate(SettingsRoute) }
                )
            }
            profileScreens(navController)
        }
        // Séance en cours à la racine : elle s'ouvre depuis n'importe quel onglet (bandeau EN DIRECT).
        workoutScreen(navController)
    }
}
