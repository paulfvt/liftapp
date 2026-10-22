package com.paulfvt.liftapp.profile.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.paulfvt.liftapp.profile.ui.AboutScreen
import com.paulfvt.liftapp.profile.ui.LicenseScreen
import com.paulfvt.liftapp.profile.ui.MeasurementEntryScreen
import com.paulfvt.liftapp.profile.ui.MeasurementScreen
import com.paulfvt.liftapp.profile.ui.PhotoScreen
import com.paulfvt.liftapp.profile.ui.PhotosScreen
import com.paulfvt.liftapp.profile.ui.ProfileEditScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProfileEditRoute

// Une mesure (poids, tour de taille…), désignée par le nom de son MeasurementType.
@Serializable
data class MeasurementRoute(val type: String)

// Saisie d'une mesure, avec son type déjà choisi si on vient de l'écran de cette mesure.
@Serializable
data class MeasurementEntryRoute(val type: String? = null)

@Serializable
data object PhotosRoute

@Serializable
data class PhotoRoute(val id: Long)

@Serializable
data object AboutRoute

// Texte complet d'une licence, désignée par OpenLicense.id.
@Serializable
data class LicenseRoute(val licenseId: String)

fun NavGraphBuilder.profileScreens(navController: NavController) {
    composable<ProfileEditRoute> {
        ProfileEditScreen(onClose = { navController.popBackStack<ProfileEditRoute>(inclusive = true) })
    }
    composable<MeasurementRoute> {
        MeasurementScreen(
            onBack = { navController.popBackStack<MeasurementRoute>(inclusive = true) },
            onAdd = { type -> navController.navigate(MeasurementEntryRoute(type.name)) }
        )
    }
    composable<MeasurementEntryRoute> {
        MeasurementEntryScreen(onClose = { navController.popBackStack<MeasurementEntryRoute>(inclusive = true) })
    }
    composable<PhotosRoute> {
        PhotosScreen(
            onBack = { navController.popBackStack<PhotosRoute>(inclusive = true) },
            onOpenPhoto = { id -> navController.navigate(PhotoRoute(id)) }
        )
    }
    composable<PhotoRoute> {
        PhotoScreen(onBack = { navController.popBackStack<PhotoRoute>(inclusive = true) })
    }
    composable<AboutRoute> {
        AboutScreen(
            onBack = { navController.popBackStack<AboutRoute>(inclusive = true) },
            onOpenLicense = { license -> navController.navigate(LicenseRoute(license.id)) }
        )
    }
    composable<LicenseRoute> {
        LicenseScreen(onBack = { navController.popBackStack<LicenseRoute>(inclusive = true) })
    }
}
