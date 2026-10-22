package com.paulfvt.liftapp.profile.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paulfvt.liftapp.core.model.PhotoPose
import com.paulfvt.liftapp.history.domain.MonthGroup
import com.paulfvt.liftapp.history.domain.groupByMonth
import com.paulfvt.liftapp.profile.data.PhotoLockSession
import com.paulfvt.liftapp.profile.data.PhotoRepository
import com.paulfvt.liftapp.profile.domain.ProgressPhoto
import com.paulfvt.liftapp.profile.domain.comparablePoses
import com.paulfvt.liftapp.profile.domain.ofPose
import com.paulfvt.liftapp.profile.domain.photoTakenAt
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PhotosUiState(
    val isLoading: Boolean = true,
    val hasPhotos: Boolean = false,
    // Au moins une pose a deux photos : la comparaison avant/après est possible.
    val canCompare: Boolean = false,
    // null : toutes les poses.
    val pose: PhotoPose? = null,
    val months: List<MonthGroup<ProgressPhoto>> = emptyList()
)

// Photo choisie dans la galerie, en attente de sa pose et de sa date. fromImage : date notée dans la photo.
data class PhotoImport(val uri: Uri, val pose: PhotoPose, val date: LocalDate, val fromImage: Instant?)

// Timeline des photos, filtrable par pose, et import depuis la galerie.
@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val photos: PhotoRepository,
    private val lockSession: PhotoLockSession,
    private val clock: Clock
) : ViewModel() {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    val today: LocalDate get() = clock.instant().atZone(zone).toLocalDate()

    private val pose = savedStateHandle.getStateFlow<PhotoPose?>(POSE_KEY, null)

    val uiState: StateFlow<PhotosUiState> = combine(photos.observeAll(), pose) { all, pose ->
        PhotosUiState(
            isLoading = false,
            hasPhotos = all.isNotEmpty(),
            canCompare = all.comparablePoses().isNotEmpty(),
            pose = pose,
            months = groupByMonth(all.ofPose(pose), zone) { it.takenAt }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PhotosUiState())

    var pendingImport by mutableStateOf<PhotoImport?>(null)
        private set

    var isImporting by mutableStateOf(false)
        private set

    var importFailed by mutableStateOf(false)
        private set

    fun file(photo: ProgressPhoto): File = photos.file(photo)

    fun beforeLeavingApp() = lockSession.keepUnlockedWhileAway()

    fun onPoseSelected(pose: PhotoPose?) {
        savedStateHandle[POSE_KEY] = pose
    }

    // La date proposée est celle notée dans la photo, sinon aujourd'hui ; la pose, celle du filtre choisi.
    fun onPicked(uri: Uri) {
        viewModelScope.launch {
            val fromImage = photos.suggestedDate(uri, zone)
            pendingImport = PhotoImport(
                uri = uri,
                pose = pose.value ?: PhotoPose.FRONT,
                date = fromImage?.atZone(zone)?.toLocalDate()?.takeIf { !it.isAfter(today) } ?: today,
                fromImage = fromImage
            )
        }
    }

    fun onImportPoseChange(pose: PhotoPose) {
        pendingImport = pendingImport?.copy(pose = pose)
    }

    fun onImportDateChange(date: LocalDate?) {
        pendingImport = pendingImport?.copy(date = date ?: today)
    }

    fun cancelImport() {
        pendingImport = null
        importFailed = false
    }

    fun confirmImport() {
        val draft = pendingImport ?: return
        if (isImporting) return
        isImporting = true
        importFailed = false
        viewModelScope.launch {
            try {
                photos.add(draft.uri, draft.pose, photoTakenAt(draft.date, draft.fromImage, clock.instant(), zone))
                pendingImport = null
            } catch (_: IOException) {
                importFailed = true
            } finally {
                isImporting = false
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val POSE_KEY = "pose"
    }
}
