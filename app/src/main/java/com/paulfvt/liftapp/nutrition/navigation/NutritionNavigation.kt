package com.paulfvt.liftapp.nutrition.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.nutrition.ui.FoodEditScreen
import com.paulfvt.liftapp.nutrition.ui.FoodQuantityScreen
import com.paulfvt.liftapp.nutrition.ui.FoodSearchScreen
import com.paulfvt.liftapp.nutrition.ui.NeedsScreen
import com.paulfvt.liftapp.profile.navigation.MeasurementEntryRoute
import com.paulfvt.liftapp.profile.navigation.ProfileEditRoute
import kotlinx.serialization.Serializable

// Les dates passent en nombre de jours depuis 1970 (LocalDate.toEpochDay) et les repas par leur nom (Meal) :
// une route ne prend que des types simples.

// Recherche d'un aliment à ajouter à un repas d'un jour.
@Serializable
data class FoodSearchRoute(val date: Long, val meal: String)

// Quantité d'un aliment : ajout au repas, ou modification d'un aliment déjà noté (entryId).
@Serializable
data class FoodQuantityRoute(val foodId: Long, val date: Long, val meal: String, val entryId: Long? = null)

// Création d'un aliment personnel (sans foodId), à ajouter ensuite au repas s'il est donné, ou modification.
@Serializable
data class FoodEditRoute(val foodId: Long? = null, val date: Long? = null, val meal: String? = null)

// « Mes besoins » : objectif de calories et de macros.
@Serializable
data object NutritionNeedsRoute

// Sous-écrans de l'onglet Nutrition. backToJournal : revient au journal en fermant la recherche et la quantité.
fun NavGraphBuilder.nutritionScreens(navController: NavController, backToJournal: () -> Unit) {
    composable<FoodSearchRoute> { entry ->
        val route = entry.toRoute<FoodSearchRoute>()
        FoodSearchScreen(
            onBack = navController::popBackStack,
            onOpenFood = { foodId -> navController.navigate(FoodQuantityRoute(foodId, route.date, route.meal)) },
            onCreateFood = { navController.navigate(FoodEditRoute(date = route.date, meal = route.meal)) },
            onDone = backToJournal
        )
    }
    composable<NutritionNeedsRoute> {
        NeedsScreen(
            onBack = navController::popBackStack,
            onEditProfile = { navController.navigate(ProfileEditRoute) },
            onAddWeight = { navController.navigate(MeasurementEntryRoute(MeasurementType.BODY_WEIGHT.name)) }
        )
    }
    composable<FoodQuantityRoute> {
        FoodQuantityScreen(
            onBack = navController::popBackStack,
            onDone = backToJournal,
            onEditFood = { foodId -> navController.navigate(FoodEditRoute(foodId = foodId)) }
        )
    }
    composable<FoodEditRoute> { entry ->
        val route = entry.toRoute<FoodEditRoute>()
        FoodEditScreen(
            onBack = navController::popBackStack,
            onCreated = { foodId ->
                // Créé depuis la recherche d'un repas : on enchaîne sur sa quantité, la création quitte la pile.
                if (route.date != null && route.meal != null) {
                    navController.navigate(FoodQuantityRoute(foodId, route.date, route.meal)) {
                        popUpTo<FoodEditRoute> { inclusive = true }
                    }
                } else {
                    navController.popBackStack()
                }
            },
            onSaved = navController::popBackStack,
            // Aliment supprimé : sa quantité, ouverte en dessous, n'a plus de sens.
            onDeleted = { if (!navController.popBackStack<FoodSearchRoute>(inclusive = false)) backToJournal() }
        )
    }
}
