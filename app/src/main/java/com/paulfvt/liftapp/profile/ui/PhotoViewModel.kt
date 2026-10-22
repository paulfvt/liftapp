package com.paulfvt.liftapp.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.profile.data.PhotoRepository
import com.paulfvt.liftapp.profile.domain.ProgressPhoto
import com.paulfvt.liftapp.profile.navigation.PhotoRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// photo vaut null pendant le chargement et après la suppression.
data class PhotoUiState(val isLoading: Boolean = true, val photo: ProgressPhoto? = null, val file: File? = null)

// Une photo en grand, avec sa suppression.
@HiltViewModel
class PhotoViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val photos: PhotoRepository) :
    ViewModel() {
    private val id = savedStateHandle.toRoute<PhotoRoute>().id

    val uiState: StateFlow<PhotoUiState> = photos.observe(id)
        .map { photo -> PhotoUiState(isLoading = false, photo = photo, file = photo?.let(photos::file)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PhotoUiState())

    fun delete() {
        val photo = uiState.value.photo ?: return
        viewModelScope.launch { photos.delete(photo) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
