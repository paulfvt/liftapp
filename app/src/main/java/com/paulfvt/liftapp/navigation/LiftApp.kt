package com.paulfvt.liftapp.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.paulfvt.liftapp.history.navigation.WorkoutEditRoute
import com.paulfvt.liftapp.onboarding.ui.WelcomeScreen
import com.paulfvt.liftapp.profile.navigation.PhotoCaptureRoute
import com.paulfvt.liftapp.workout.navigation.WorkoutRoute
import com.paulfvt.liftapp.workout.ui.LiveWorkoutBar

// Racine de l'app : l'écran de bienvenue au premier lancement, puis le squelette avec ses onglets.
@Composable
fun LiftApp(openWorkoutRequest: Int = 0, viewModel: LiftAppViewModel = hiltViewModel()) {
    val showWelcome by viewModel.showWelcome.collectAsStateWithLifecycle()
    when (showWelcome) {
        // Préférence pas encore lue : le fond de la fenêtre, de la couleur du thème, reste affiché.
        null -> Unit
        true -> WelcomeScreen(onStart = viewModel::finishWelcome)
        false -> MainContent(openWorkoutRequest, viewModel)
    }
}

// Squelette de l'app : barre de navigation du bas, bandeau de la séance en cours et zone de contenu.
@Composable
private fun MainContent(openWorkoutRequest: Int, viewModel: LiftAppViewModel) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val liveWorkout by viewModel.liveWorkout.collectAsStateWithLifecycle()

    // Une séance encore en cours au lancement (app tuée, téléphone redémarré) est rouverte telle quelle.
    // Même chose à chaque appui sur la notification de séance (openWorkoutRequest augmente).
    LaunchedEffect(openWorkoutRequest) {
        if (viewModel.hasWorkoutInProgress()) {
            // Déjà dans la pile (catalogue ouvert par-dessus, par exemple) : on y revient au lieu d'en empiler une autre.
            if (!navController.popBackStack(WorkoutRoute, inclusive = false)) {
                navController.navigate(WorkoutRoute) { launchSingleTop = true }
            }
        }
    }

    // La séance en cours, la modification d'une séance passée et la prise de photo sont en plein écran : pas de
    // barre du bas sur elles, ni sur le catalogue ouvert depuis une séance.
    val previousDestination = navController.previousBackStackEntry?.destination
    val isOnWorkout = listOf(WorkoutRoute::class, WorkoutEditRoute::class).any { route ->
        currentDestination?.hasRoute(route) == true || previousDestination?.hasRoute(route) == true
    } ||
        currentDestination?.hasRoute(PhotoCaptureRoute::class) == true

    // Les écrans gèrent eux-mêmes la barre d'état (via leur TopAppBar) : ce Scaffold ne réserve que la place
    // de la barre du bas, sinon la marge du haut serait appliquée deux fois.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isOnWorkout) {
                Column {
                    liveWorkout?.let { live ->
                        LiveWorkoutBar(
                            name = live.name,
                            startedAt = live.startedAt,
                            onClick = { navController.navigate(WorkoutRoute) { launchSingleTop = true } }
                        )
                    }
                    LiftNavigationBar(currentDestination, onSelect = navController::navigateToTopLevel)
                }
            }
        }
    ) { innerPadding ->
        LiftNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        )
    }
}

@Composable
private fun LiftNavigationBar(currentDestination: NavDestination?, onSelect: (TopLevelDestination) -> Unit) {
    NavigationBar {
        // Onglet actif en pastille rouge signal (primaryContainer) au lieu du secondaryContainer par défaut.
        val itemColors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentDestination.isInHierarchyOf(destination),
                onClick = { onSelect(destination) },
                // Le libellé est déjà lu par le lecteur d'écran : l'icône n'a pas besoin de description.
                icon = {
                    Icon(painterResource(destination.iconRes), contentDescription = null)
                },
                label = { Text(stringResource(destination.labelRes)) },
                colors = itemColors
            )
        }
    }
}

private fun NavDestination?.isInHierarchyOf(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

// Navigation entre onglets : on revient à la racine du graphe pour ne pas empiler les onglets,
// et on sauvegarde puis restaure l'état de chaque onglet (position de défilement, sous-écran ouvert).
internal fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
