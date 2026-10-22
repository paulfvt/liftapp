package com.paulfvt.liftapp.profile.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.paulfvt.liftapp.profile.ui.AboutScreen
import com.paulfvt.liftapp.profile.ui.LicenseScreen
import com.paulfvt.liftapp.profile.ui.MeasurementEntryScreen
import com.paulfvt.liftapp.profile.ui.MeasurementScreen
import com.paulfvt.liftapp.profile.ui.PhotoCaptureScreen
import com.paulfvt.liftapp.profile.ui.PhotoCompareScreen
import com.paulfvt.liftapp.profile.ui.PhotoLockGate
import com.paulfvt.liftapp.profile.ui.PhotoLockSettingsScreen
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

// Prise de photo en plein écran, avec la pose déjà choisie si on vient d'un filtre de la timeline.
@Serializable
data class PhotoCaptureRoute(val pose: String? = null)

// Comparaison avant/après, sur la pose du filtre de la timeline si elle en a assez.
@Serializable
data class PhotoCompareRoute(val pose: String? = null)

@Serializable
data object PhotoLockSettingsRoute

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
    // Écrans des photos : tous derrière le verrou, qui remplace l'écran tant que les photos sont verrouillées.
    composable<PhotosRoute> {
        val back = { navController.popBackStack<PhotosRoute>(inclusive = true) }
        PhotoLockGate(onBack = { back() }) {
            PhotosScreen(
                onBack = { back() },
                onOpenPhoto = { id -> navController.navigate(PhotoRoute(id)) },
                onTakePhoto = { pose -> navController.navigate(PhotoCaptureRoute(pose?.name)) },
                onCompare = { pose -> navController.navigate(PhotoCompareRoute(pose?.name)) },
                onOpenLockSettings = { navController.navigate(PhotoLockSettingsRoute) }
            )
        }
    }
    composable<PhotoCaptureRoute> {
        val close = { navController.popBackStack<PhotoCaptureRoute>(inclusive = true) }
        PhotoLockGate(onBack = { close() }) { PhotoCaptureScreen(onClose = { close() }) }
    }
    composable<PhotoCompareRoute> {
        val back = { navController.popBackStack<PhotoCompareRoute>(inclusive = true) }
        PhotoLockGate(onBack = { back() }) { PhotoCompareScreen(onBack = { back() }) }
    }
    composable<PhotoRoute> {
        val back = { navController.popBackStack<PhotoRoute>(inclusive = true) }
        PhotoLockGate(onBack = { back() }) { PhotoScreen(onBack = { back() }) }
    }
    composable<PhotoLockSettingsRoute> {
        val back = { navController.popBackStack<PhotoLockSettingsRoute>(inclusive = true) }
        PhotoLockGate(onBack = { back() }) { PhotoLockSettingsScreen(onBack = { back() }) }
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
