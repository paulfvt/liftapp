package com.paulfvt.liftapp.profile.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.profile.data.ProfileRepository
import com.paulfvt.liftapp.profile.domain.ProfileDraft
import com.paulfvt.liftapp.profile.domain.ProfileError
import com.paulfvt.liftapp.profile.domain.Sex
import com.paulfvt.liftapp.profile.domain.errors
import com.paulfvt.liftapp.profile.domain.toDraft
import com.paulfvt.liftapp.profile.domain.toProfile
import com.paulfvt.liftapp.workout.domain.filterDecimalInput
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.launch

// Modification du profil fixe : les changements restent en mémoire jusqu'à « Enregistrer », comme les templates.
@HiltViewModel
class ProfileEditViewModel @Inject constructor(private val profiles: ProfileRepository, private val clock: Clock) :
    ViewModel() {
    var draft by mutableStateOf(ProfileDraft())
        private set

    private var savedDraft by mutableStateOf(ProfileDraft())
    val hasChanges: Boolean get() = draft != savedDraft

    // Les erreurs ne s'affichent qu'après un premier essai d'enregistrement.
    var errors by mutableStateOf(emptySet<ProfileError>())
        private set

    var isLoaded by mutableStateOf(false)
        private set

    var isSaved by mutableStateOf(false)
        private set

    private var hasTriedToSave = false
    private var isSaving = false

    val today: LocalDate get() = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()

    init {
        viewModelScope.launch {
            val loaded = profiles.current().toDraft()
            draft = loaded
            savedDraft = loaded
            isLoaded = true
        }
    }

    fun onFirstNameChange(value: String) = update { copy(firstName = value) }

    fun onHeightChange(value: String) = update { copy(height = filterDecimalInput(value, allowNegative = false)) }

    fun onBirthDateChange(value: LocalDate?) = update { copy(birthDate = value) }

    fun onSexChange(value: Sex?) = update { copy(sex = value) }

    fun save() {
        hasTriedToSave = true
        errors = draft.errors(today)
        if (errors.isNotEmpty() || isSaving) return
        isSaving = true
        viewModelScope.launch {
            profiles.save(draft.toProfile())
            isSaved = true
        }
    }

    private fun update(transform: ProfileDraft.() -> ProfileDraft) {
        draft = draft.transform()
        if (hasTriedToSave) errors = draft.errors(today)
    }
}
