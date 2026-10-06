package com.paulfvt.liftapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.ui.ComingSoonScreen

// Graphe de navigation : chaque phase remplacera un ComingSoonScreen par le vrai écran.
@Composable
fun LiftNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> { ComingSoonScreen(R.string.nav_home) }
        composable<TemplatesRoute> { ComingSoonScreen(R.string.nav_templates) }
        composable<HistoryRoute> { ComingSoonScreen(R.string.nav_history) }
        composable<ProgressRoute> { ComingSoonScreen(R.string.nav_progress) }
        composable<ProfileRoute> { ComingSoonScreen(R.string.nav_profile) }
    }
}
