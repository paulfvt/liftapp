package com.paulfvt.liftapp.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.profile.data.MeasurementRepository
import com.paulfvt.liftapp.profile.data.PhotoLockRepository
import com.paulfvt.liftapp.profile.data.PhotoLockSession
import com.paulfvt.liftapp.profile.data.PhotoRepository
import com.paulfvt.liftapp.profile.data.ProfileRepository
import com.paulfvt.liftapp.profile.domain.MeasurementSummary
import com.paulfvt.liftapp.profile.domain.ProgressPhoto
import com.paulfvt.liftapp.profile.domain.UserProfile
import com.paulfvt.liftapp.profile.domain.ageOn
import com.paulfvt.liftapp.profile.domain.summaries
import com.paulfvt.liftapp.profile.domain.visibleMeasurementTypes
import com.paulfvt.liftapp.settings.data.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val age: Int? = null,
    // Dernière valeur de chaque mesure déjà saisie, sans le poids de corps s'il est masqué.
    val measurements: List<MeasurementSummary> = emptyList(),
    // Poids et masse grasse masqués dans les réglages : le texte d'invitation ne parle alors que des tours.
    val bodyWeightHidden: Boolean = false,
    // Section photos masquée dans les réglages.
    val showPhotos: Boolean = true,
    val photoCount: Int = 0,
    // Les plus récentes, en vignettes.
    val latestPhotos: List<ProgressPhoto> = emptyList(),
    // Verrouillées : ni vignettes ni aperçu tant que le code n'est pas tapé.
    val photosLocked: Boolean = false
)

// Onglet Profil : le résumé du profil fixe et la dernière valeur de chaque mesure.
@HiltViewModel
class ProfileViewModel @Inject constructor(
    profiles: ProfileRepository,
    measurements: MeasurementRepository,
    private val photos: PhotoRepository,
    lock: PhotoLockRepository,
    lockSession: PhotoLockSession,
    appSettings: AppSettingsRepository,
    private val clock: Clock
) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> = combine(
        profiles.profile,
        measurements.observeAll(),
        photos.observeAll(),
        combine(lock.settings, lockSession.isUnlocked) { settings, unlocked -> settings.isEnabled && !unlocked },
        appSettings.settings
    ) { profile, entries, allPhotos, photosLocked, settings ->
        val today = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()
        ProfileUiState(
            isLoading = false,
            profile = profile,
            age = profile.birthDate?.let { ageOn(it, today) },
            measurements = summaries(entries, visibleMeasurementTypes(settings.hideBodyWeight)),
            bodyWeightHidden = settings.hideBodyWeight,
            showPhotos = !settings.hidePhotos,
            photoCount = allPhotos.size,
            latestPhotos = if (photosLocked) emptyList() else allPhotos.take(LATEST_PHOTOS),
            photosLocked = photosLocked
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ProfileUiState())

    fun file(photo: ProgressPhoto): File = photos.file(photo)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val LATEST_PHOTOS = 3
    }
}
