package com.paulfvt.liftapp.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.paulfvt.liftapp.R
import kotlinx.serialization.Serializable

// Routes typées : chaque destination est un objet sérialisable plutôt qu'une chaîne de caractères.
// Le compilateur vérifie ainsi les routes et leurs arguments.
// Graphe imbriqué de l'onglet Séances : l'onglet reste sélectionné sur tous ses sous-écrans (catalogue…).
@Serializable
data object TemplatesGraph

@Serializable
data object TemplatesRoute

// Graphes imbriqués des onglets Historique, Progrès et Profil, pour la même raison que TemplatesGraph.
@Serializable
data object HistoryGraph

@Serializable
data object HistoryRoute

@Serializable
data object ProgressGraph

@Serializable
data object ProgressRoute

@Serializable
data object ProfileGraph

@Serializable
data object ProfileRoute

// Les onglets de la barre du bas, dans l'ordre d'affichage. L'onglet Accueil (tableau de bord) est masqué pendant
// le test fermé et revient en phase 9 : l'app s'ouvre en attendant sur Séances.
enum class TopLevelDestination(
    val route: Any,
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int
) {
    TEMPLATES(TemplatesGraph, R.drawable.ic_fitness_center, R.string.nav_templates),
    HISTORY(HistoryGraph, R.drawable.ic_history, R.string.nav_history),
    PROGRESS(ProgressGraph, R.drawable.ic_trending_up, R.string.nav_progress),
    PROFILE(ProfileGraph, R.drawable.ic_person, R.string.nav_profile)
}
