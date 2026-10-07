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
import com.paulfvt.liftapp.template.navigation.TemplateEditRoute
import com.paulfvt.liftapp.template.navigation.templateScreens
import com.paulfvt.liftapp.template.ui.list.TemplatesScreen

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
                    onOpenCatalog = { navController.navigate(ExerciseListRoute) }
                )
            }
            templateScreens(navController)
            exerciseScreens(navController)
        }
        composable<HistoryRoute> { ComingSoonScreen(R.string.nav_history) }
        composable<ProgressRoute> { ComingSoonScreen(R.string.nav_progress) }
        composable<ProfileRoute> { ComingSoonScreen(R.string.nav_profile) }
    }
}
