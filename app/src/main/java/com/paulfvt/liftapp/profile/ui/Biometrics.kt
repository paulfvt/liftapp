package com.paulfvt.liftapp.profile.ui

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// Visage ou empreinte disponibles et enregistrés sur le téléphone. Classe « faible » : la plupart des
// déverrouillages par le visage en font partie, et il ne s'agit ici que de cacher des photos, pas de chiffrer.
fun Context.canUseBiometric(): Boolean =
    BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

// Le téléphone a un verrouillage d'écran (code, schéma, mot de passe) : il peut confirmer l'identité si le PIN de
// l'app est oublié.
fun Context.hasScreenLock(): Boolean = getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

// Déverrouillage par le visage ou l'empreinte. Le bouton négatif ramène au pavé numérique.
fun FragmentActivity.askBiometric(title: String, negativeText: String, onSuccess: () -> Unit) {
    prompt(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText(negativeText)
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build(),
        onSuccess
    )
}

// Confirmation par le verrouillage du téléphone (ou le visage, l'empreinte), pour choisir un nouveau PIN.
fun FragmentActivity.askScreenLock(title: String, subtitle: String, onSuccess: () -> Unit) {
    prompt(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build(),
        onSuccess
    )
}

private fun FragmentActivity.prompt(info: BiometricPrompt.PromptInfo, onSuccess: () -> Unit) {
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
    }
    BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback).authenticate(info)
}
