package com.paulfvt.liftapp.profile.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.paulfvt.liftapp.core.model.PhotoPose
import com.paulfvt.liftapp.profile.data.PhotoRepository
import com.paulfvt.liftapp.profile.domain.PhotoComparison
import com.paulfvt.liftapp.profile.domain.ProgressPhoto
import com.paulfvt.liftapp.profile.domain.chronologicalOfPose
import com.paulfvt.liftapp.profile.domain.comparablePoses
import com.paulfvt.liftapp.profile.domain.comparison
import com.paulfvt.liftapp.profile.domain.daysBetween
import com.paulfvt.liftapp.profile.navigation.PhotoCompareRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PhotoCompareUiState(
    val isLoading: Boolean = true,
    val pose: PhotoPose = PhotoPose.FRONT,
    // Poses qui ont au moins deux photos.
    val poses: List<PhotoPose> = emptyList(),
    // Photos de la pose, de la plus ancienne à la plus récente.
    val photos: List<ProgressPhoto> = emptyList(),
    val comparison: PhotoComparison? = null,
    val daysBetween: Long = 0
)

// Comparaison avant/après de deux photos de la même pose.
@HiltViewModel
class PhotoCompareViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val photos: PhotoRepository
) : ViewModel() {
    // Clés distinctes de « pose », nom de l'argument de la route déjà rangé dans le SavedStateHandle.
    private val requestedPose = savedStateHandle.getStateFlow(
        POSE_KEY,
        poseNamed(savedStateHandle.toRoute<PhotoCompareRoute>().pose)
    )
    private val beforeId = savedStateHandle.getStateFlow<Long?>(BEFORE_KEY, null)
    private val afterId = savedStateHandle.getStateFlow<Long?>(AFTER_KEY, null)

    val uiState: StateFlow<PhotoCompareUiState> = combine(
        photos.observeAll(),
        requestedPose,
        beforeId,
        afterId
    ) { all, requested, before, after ->
        val poses = all.comparablePoses()
        // La pose demandée si on peut la comparer, sinon la première qui le permet.
        val pose = requested?.takeIf { it in poses } ?: poses.firstOrNull() ?: requested ?: PhotoPose.FRONT
        val comparison = all.comparison(pose, before, after)
        PhotoCompareUiState(
            isLoading = false,
            pose = pose,
            poses = poses,
            photos = all.chronologicalOfPose(pose),
            comparison = comparison,
            daysBetween = comparison?.daysBetween(ZoneId.systemDefault()) ?: 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PhotoCompareUiState())

    fun file(photo: ProgressPhoto): File = photos.file(photo)

    // Changer de pose repart de sa première et de sa dernière photo.
    fun onPoseChange(pose: PhotoPose) {
        savedStateHandle[POSE_KEY] = pose
        savedStateHandle[BEFORE_KEY] = null
        savedStateHandle[AFTER_KEY] = null
    }

    fun onBeforeChange(photo: ProgressPhoto) {
        savedStateHandle[BEFORE_KEY] = photo.id
        savedStateHandle[AFTER_KEY] = uiState.value.comparison?.after?.id
    }

    fun onAfterChange(photo: ProgressPhoto) {
        savedStateHandle[AFTER_KEY] = photo.id
        savedStateHandle[BEFORE_KEY] = uiState.value.comparison?.before?.id
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val POSE_KEY = "selected_pose"
        const val BEFORE_KEY = "before_id"
        const val AFTER_KEY = "after_id"
    }
}

private fun poseNamed(name: String?): PhotoPose? = PhotoPose.entries.firstOrNull { it.name == name }
