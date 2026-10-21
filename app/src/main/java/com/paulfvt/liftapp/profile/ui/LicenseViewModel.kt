package com.paulfvt.liftapp.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.profile.data.LicenseRepository
import com.paulfvt.liftapp.profile.data.OpenLicense
import com.paulfvt.liftapp.profile.navigation.LicenseRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

// Le texte vaut null tant qu'il n'est pas lu.
data class LicenseUiState(val license: OpenLicense, val text: String? = null)

@HiltViewModel
class LicenseViewModel @Inject constructor(savedStateHandle: SavedStateHandle, licenses: LicenseRepository) :
    ViewModel() {
    private val license = OpenLicense.fromId(savedStateHandle.toRoute<LicenseRoute>().licenseId)

    val uiState: StateFlow<LicenseUiState> = flow { emit(LicenseUiState(license, licenses.text(license))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LicenseUiState(license))

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
