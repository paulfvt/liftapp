package com.paulfvt.liftapp.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.profile.data.PhotoLockRepository
import com.paulfvt.liftapp.profile.data.PhotoLockSession
import com.paulfvt.liftapp.profile.data.PinCheck
import com.paulfvt.liftapp.profile.domain.PIN_MAX_LENGTH
import com.paulfvt.liftapp.profile.domain.isValidPin
import com.paulfvt.liftapp.profile.domain.lockoutEndsAt
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Étapes du pavé : déverrouiller, ou choisir puis confirmer un nouveau code (après « Code oublié ? »).
enum class PinStep {
    UNLOCK,
    NEW_PIN,
    CONFIRM_PIN
}

enum class PinError {
    WRONG_PIN,
    MISMATCH
}

data class PinEntryState(
    val step: PinStep = PinStep.UNLOCK,
    val pin: String = "",
    // Premier code tapé, en attente de confirmation.
    val firstPin: String = "",
    val error: PinError? = null,
    val isBusy: Boolean = false
)

data class PhotoLockUiState(
    val isLoading: Boolean = true,
    val isLocked: Boolean = false,
    val useBiometric: Boolean = false,
    val entry: PinEntryState = PinEntryState(),
    // Attente imposée après trop d'erreurs.
    val lockedUntil: Instant? = null
)

// Verrou devant les écrans de photos : PIN, visage ou empreinte, ou nouveau code après confirmation par le
// verrouillage du téléphone.
@HiltViewModel
class PhotoLockViewModel @Inject constructor(
    private val lock: PhotoLockRepository,
    private val session: PhotoLockSession,
    private val clock: Clock
) : ViewModel() {
    private val entry = MutableStateFlow(PinEntryState())

    val uiState: StateFlow<PhotoLockUiState> = combine(lock.settings, session.isUnlocked, entry) {
            settings,
            unlocked,
            entry
        ->
        PhotoLockUiState(
            isLoading = false,
            isLocked = settings.isEnabled && !unlocked,
            useBiometric = settings.useBiometric,
            entry = entry,
            lockedUntil = lockoutEndsAt(settings.failures, settings.lastFailureAt, clock.instant())
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PhotoLockUiState())

    fun onDigit(digit: Char) = entry.update {
        if (it.pin.length < PIN_MAX_LENGTH) it.copy(pin = it.pin + digit, error = null) else it
    }

    fun onDelete() = entry.update { it.copy(pin = it.pin.dropLast(1)) }

    fun onConfirm() {
        val current = entry.value
        if (current.isBusy) return
        when (current.step) {
            PinStep.UNLOCK -> checkPin(current.pin)
            PinStep.NEW_PIN -> if (isValidPin(current.pin)) {
                entry.value = current.copy(step = PinStep.CONFIRM_PIN, firstPin = current.pin, pin = "")
            }
            PinStep.CONFIRM_PIN -> if (current.pin == current.firstPin) {
                saveNewPin(current.pin)
            } else {
                entry.value = PinEntryState(step = PinStep.NEW_PIN, error = PinError.MISMATCH)
            }
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            lock.clearFailures()
            unlock()
        }
    }

    // Identité confirmée par le verrouillage du téléphone : on choisit un nouveau code, les photos sont gardées.
    fun onScreenLockConfirmed() {
        viewModelScope.launch {
            lock.clearFailures()
            entry.value = PinEntryState(step = PinStep.NEW_PIN)
        }
    }

    private fun checkPin(pin: String) {
        entry.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            when (lock.check(pin)) {
                PinCheck.Correct -> unlock()
                is PinCheck.Wrong -> entry.value = PinEntryState(error = PinError.WRONG_PIN)
                is PinCheck.LockedOut -> entry.value = PinEntryState()
            }
        }
    }

    private fun saveNewPin(pin: String) {
        entry.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            lock.setPin(pin)
            unlock()
        }
    }

    private fun unlock() {
        session.unlock()
        entry.value = PinEntryState()
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
