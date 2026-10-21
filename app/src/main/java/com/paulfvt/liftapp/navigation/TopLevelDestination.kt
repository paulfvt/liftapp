package com.paulfvt.liftapp.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.paulfvt.liftapp.R
import kotlinx.serialization.Serializable

// Routes typées : chaque destination est un objet sérialisable plutôt qu'une chaîne de caractères.
// Le compilateur vérifie ainsi les routes et leurs arguments.
@Serializable
data object HomeRoute

// Graphe imbriqué de l'onglet Séances : l'onglet reste sélectionné sur tous ses sous-écrans (catalogue…).
@Serializable
data object TemplatesGraph

@Serializable
data object TemplatesRoute

// Graphes imbriqués des onglets Historique et Progrès, pour la même raison que TemplatesGraph.
@Serializable
data object HistoryGraph

@Serializable
data object HistoryRoute

@Serializable
data object ProgressGraph

@Serializable
data object ProgressRoute

@Serializable
data object ProfileRoute

// Les cinq onglets de la barre du bas, dans l'ordre d'affichage.
enum class TopLevelDestination(
    val route: Any,
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int
) {
    HOME(HomeRoute, R.drawable.ic_home, R.string.nav_home),
    TEMPLATES(TemplatesGraph, R.drawable.ic_fitness_center, R.string.nav_templates),
    HISTORY(HistoryGraph, R.drawable.ic_history, R.string.nav_history),
    PROGRESS(ProgressGraph, R.drawable.ic_trending_up, R.string.nav_progress),
    PROFILE(ProfileRoute, R.drawable.ic_person, R.string.nav_profile)
}
