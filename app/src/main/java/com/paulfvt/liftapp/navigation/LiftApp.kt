package com.paulfvt.liftapp.navigation

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// Squelette de l'app : barre de navigation du bas et zone de contenu.
@Composable
fun LiftApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    // Les écrans gèrent eux-mêmes la barre d'état (via leur TopAppBar) : ce Scaffold ne réserve que la place
    // de la barre du bas, sinon la marge du haut serait appliquée deux fois.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                // Onglet actif en pastille rouge signal (primaryContainer) au lieu du secondaryContainer par défaut.
                val itemColors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination.isInHierarchyOf(destination),
                        onClick = { navController.navigateToTopLevel(destination) },
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
    ) { innerPadding ->
        LiftNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        )
    }
}

private fun NavDestination?.isInHierarchyOf(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

// Navigation entre onglets : on revient à la racine du graphe pour ne pas empiler les onglets,
// et on sauvegarde puis restaure l'état de chaque onglet (position de défilement, sous-écran ouvert).
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
