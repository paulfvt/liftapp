package com.paulfvt.liftapp.profile.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Déverrouillage des photos pour la session en cours (décision du 21 octobre 2026) : une fois déverrouillées, elles
// le restent tant que l'app est au premier plan, et se reverrouillent dès qu'elle passe en arrière-plan.
// Gardé en mémoire seulement : après un redémarrage de l'app, les photos sont verrouillées.
@Singleton
class PhotoLockSession @Inject constructor() {
    private val unlockedState = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = unlockedState.asStateFlow()

    // Vrai juste avant d'ouvrir le sélecteur de photos ou les réglages d'Android : l'app passe alors en arrière-plan
    // à la demande de l'utilisateur, sans le quitter.
    private var keepUnlockedOnce = false

    fun unlock() {
        unlockedState.value = true
    }

    fun lock() {
        unlockedState.value = false
    }

    fun keepUnlockedWhileAway() {
        keepUnlockedOnce = true
    }

    // Appelé par MainActivity quand l'app n'est plus visible (hors rotation de l'écran).
    fun onAppStopped() {
        if (keepUnlockedOnce) keepUnlockedOnce = false else lock()
    }

    // Retour au premier plan : la tolérance ne sert qu'une fois, même si l'app n'est pas passée en arrière-plan.
    fun onAppResumed() {
        keepUnlockedOnce = false
    }
}
