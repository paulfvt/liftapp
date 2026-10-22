package com.paulfvt.liftapp.profile.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.core.model.PhotoPose
import com.paulfvt.liftapp.profile.data.PhotoRepository
import com.paulfvt.liftapp.profile.domain.latestOfPose
import com.paulfvt.liftapp.profile.navigation.PhotoCaptureRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.IOException
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ghost : fichier de la dernière photo de la pose choisie, montré en transparence pour garder le même cadrage.
data class PhotoCaptureUiState(val pose: PhotoPose = PhotoPose.FRONT, val ghost: File? = null)

// Prise de photo : la pose, la silhouette fantôme et l'enregistrement de la photo prise par CameraX.
@HiltViewModel
class PhotoCaptureViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val photos: PhotoRepository,
    private val clock: Clock
) : ViewModel() {
    private val pose = savedStateHandle.getStateFlow(
        POSE_KEY,
        savedStateHandle.toRoute<PhotoCaptureRoute>().pose
            ?.let { name -> PhotoPose.entries.firstOrNull { it.name == name } }
            ?: PhotoPose.FRONT
    )

    val uiState: StateFlow<PhotoCaptureUiState> = combine(photos.observeAll(), pose) { all, pose ->
        PhotoCaptureUiState(pose, all.latestOfPose(pose)?.let(photos::file))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PhotoCaptureUiState(pose.value))

    var showGhost by mutableStateOf(true)
        private set

    var isCapturing by mutableStateOf(false)
        private set

    var captureFailed by mutableStateOf(false)
        private set

    var isSaved by mutableStateOf(false)
        private set

    fun onPoseChange(pose: PhotoPose) {
        savedStateHandle[POSE_KEY] = pose
    }

    fun onGhostToggle(show: Boolean) {
        showGhost = show
    }

    fun onCaptureStarted() {
        isCapturing = true
        captureFailed = false
    }

    // Fichier brut écrit par CameraX : il est réduit et redressé comme une photo importée, puis effacé.
    fun onCaptured(file: File) {
        viewModelScope.launch {
            try {
                photos.add(Uri.fromFile(file), pose.value, clock.instant())
                isSaved = true
            } catch (_: IOException) {
                captureFailed = true
            } finally {
                file.delete()
                isCapturing = false
            }
        }
    }

    fun onCaptureFailed() {
        isCapturing = false
        captureFailed = true
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        // Pas « pose » : c'est déjà le nom de l'argument de la route, rangé sous cette clé dans le SavedStateHandle.
        const val POSE_KEY = "selected_pose"
    }
}
