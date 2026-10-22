package com.paulfvt.liftapp.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.profile.data.PhotoLockRepository
import com.paulfvt.liftapp.profile.data.PhotoLockSession
import com.paulfvt.liftapp.profile.domain.PIN_MAX_LENGTH
import com.paulfvt.liftapp.profile.domain.isValidPin
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotoLockSettingsUiState(
    val isLoading: Boolean = true,
    val isEnabled: Boolean = false,
    val useBiometric: Boolean = false,
    // Choix d'un code en cours (activation ou changement), null sinon.
    val setup: PinEntryState? = null
)

// Réglages du verrouillage des photos. Cet écran est lui-même derrière le verrou : le modifier demande le code.
@HiltViewModel
class PhotoLockSettingsViewModel @Inject constructor(
    private val lock: PhotoLockRepository,
    private val session: PhotoLockSession
) : ViewModel() {
    private val setup = MutableStateFlow<PinEntryState?>(null)

    val uiState: StateFlow<PhotoLockSettingsUiState> = combine(lock.settings, setup) { settings, setup ->
        PhotoLockSettingsUiState(
            isLoading = false,
            isEnabled = settings.isEnabled,
            useBiometric = settings.useBiometric,
            setup = setup
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PhotoLockSettingsUiState())

    fun startSetup() {
        setup.value = PinEntryState(step = PinStep.NEW_PIN)
    }

    fun cancelSetup() {
        setup.value = null
    }

    fun onDigit(digit: Char) = setup.update { current ->
        current?.takeIf { it.pin.length < PIN_MAX_LENGTH }?.copy(pin = current.pin + digit, error = null) ?: current
    }

    fun onDelete() = setup.update { it?.copy(pin = it.pin.dropLast(1)) }

    fun onConfirm() {
        val current = setup.value ?: return
        if (current.isBusy) return
        when {
            current.step == PinStep.NEW_PIN && isValidPin(current.pin) ->
                setup.value = current.copy(step = PinStep.CONFIRM_PIN, firstPin = current.pin, pin = "")
            current.step == PinStep.CONFIRM_PIN && current.pin == current.firstPin -> {
                setup.value = current.copy(isBusy = true)
                viewModelScope.launch {
                    lock.setPin(current.pin)
                    // Celui qui vient de choisir le code ne doit pas le retaper aussitôt.
                    session.unlock()
                    setup.value = null
                }
            }
            current.step == PinStep.CONFIRM_PIN ->
                setup.value = PinEntryState(step = PinStep.NEW_PIN, error = PinError.MISMATCH)
        }
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch { lock.setBiometric(enabled) }
    }

    fun disable() {
        viewModelScope.launch { lock.disable() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
