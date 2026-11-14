package com.paulfvt.liftapp.nutrition.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.nutrition.domain.LabelReading
import com.paulfvt.liftapp.nutrition.ui.FoodEditScreen
import com.paulfvt.liftapp.nutrition.ui.FoodEditViewModel
import com.paulfvt.liftapp.nutrition.ui.FoodQuantityScreen
import com.paulfvt.liftapp.nutrition.ui.FoodSearchScreen
import com.paulfvt.liftapp.nutrition.ui.LabelScanScreen
import com.paulfvt.liftapp.nutrition.ui.NeedsScreen
import com.paulfvt.liftapp.nutrition.ui.TrendScreen
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

// Tendance : semaine de calories, poids, dépense réelle estimée.
@Serializable
data object NutritionTrendRoute

// Scan en direct de l'étiquette, ouvert depuis la création d'un aliment.
@Serializable
data object LabelScanRoute

// Ce que le scan rend à l'écran de l'aliment, dans le SavedStateHandle de son entrée de navigation (même mécanisme que
// le choix d'exercices) : les valeurs lues, ou l'adresse de l'image choisie à la place.
const val LABEL_VALUES_KEY = "labelValues"
const val LABEL_IMAGE_KEY = "labelImage"

// Le SavedStateHandle ne garde que des types simples : les valeurs passent en tableau, NaN pour une valeur non lue.
fun LabelReading.toValues(): DoubleArray =
    listOf(kcal, protein, carbs, fat, fiber).map { it ?: Double.NaN }.toDoubleArray()

fun labelReadingOf(values: DoubleArray): LabelReading {
    val (kcal, protein, carbs, fat, fiber) = values.map { value -> value.takeUnless { it.isNaN() } }
    return LabelReading(kcal = kcal, protein = protein, carbs = carbs, fat = fat, fiber = fiber)
}

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
    composable<NutritionTrendRoute> {
        TrendScreen(
            onBack = navController::popBackStack,
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
        val viewModel: FoodEditViewModel = hiltViewModel()
        // Ce que le scan rend en se fermant arrive dans le SavedStateHandle de cette entrée de navigation, qui n'est pas
        // celui du ViewModel : on le relève ici, puis on l'efface pour ne pas le resservir (même mécanisme que le choix
        // d'exercices). Le relever dans le ViewModel laissait les cases vides (11 novembre 2026).
        val scannedValues by entry.savedStateHandle
            .getStateFlow<DoubleArray?>(LABEL_VALUES_KEY, null)
            .collectAsStateWithLifecycle()
        val pickedImage by entry.savedStateHandle
            .getStateFlow<String?>(LABEL_IMAGE_KEY, null)
            .collectAsStateWithLifecycle()
        LaunchedEffect(scannedValues) {
            scannedValues?.let { values ->
                entry.savedStateHandle[LABEL_VALUES_KEY] = null
                viewModel.onLabelRead(labelReadingOf(values))
            }
        }
        LaunchedEffect(pickedImage) {
            pickedImage?.let { uri ->
                entry.savedStateHandle[LABEL_IMAGE_KEY] = null
                viewModel.onLabelImagePicked(uri.toUri())
            }
        }
        FoodEditScreen(
            viewModel = viewModel,
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
            onDeleted = { if (!navController.popBackStack<FoodSearchRoute>(inclusive = false)) backToJournal() },
            onScanLabel = { navController.navigate(LabelScanRoute) }
        )
    }
    composable<LabelScanRoute> {
        LabelScanScreen(
            onClose = navController::popBackStack,
            onUse = { reading ->
                navController.previousBackStackEntry?.savedStateHandle?.set(LABEL_VALUES_KEY, reading.toValues())
                navController.popBackStack()
            },
            onPickImage = { uri ->
                navController.previousBackStackEntry?.savedStateHandle?.set(LABEL_IMAGE_KEY, uri.toString())
                navController.popBackStack()
            }
        )
    }
}
