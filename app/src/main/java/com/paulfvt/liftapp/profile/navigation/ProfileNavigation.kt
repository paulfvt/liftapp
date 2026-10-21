package com.paulfvt.liftapp.profile.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.paulfvt.liftapp.profile.ui.AboutScreen
import com.paulfvt.liftapp.profile.ui.LicenseScreen
import com.paulfvt.liftapp.profile.ui.ProfileEditScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProfileEditRoute

@Serializable
data object AboutRoute

// Texte complet d'une licence, désignée par OpenLicense.id.
@Serializable
data class LicenseRoute(val licenseId: String)

fun NavGraphBuilder.profileScreens(navController: NavController) {
    composable<ProfileEditRoute> {
        ProfileEditScreen(onClose = { navController.popBackStack<ProfileEditRoute>(inclusive = true) })
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
