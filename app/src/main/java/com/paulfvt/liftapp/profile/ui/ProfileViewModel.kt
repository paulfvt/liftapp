package com.paulfvt.liftapp.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.profile.data.ProfileRepository
import com.paulfvt.liftapp.profile.domain.UserProfile
import com.paulfvt.liftapp.profile.domain.ageOn
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(val isLoading: Boolean = true, val profile: UserProfile = UserProfile(), val age: Int? = null)

// Onglet Profil : le résumé du profil fixe.
@HiltViewModel
class ProfileViewModel @Inject constructor(profiles: ProfileRepository, private val clock: Clock) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> = profiles.profile
        .map { profile ->
            val today = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()
            ProfileUiState(isLoading = false, profile = profile, age = profile.birthDate?.let { ageOn(it, today) })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProfileUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
