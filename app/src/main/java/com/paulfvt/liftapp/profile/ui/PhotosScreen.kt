package com.paulfvt.liftapp.profile.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.ButtonLabel
import com.paulfvt.liftapp.core.designsystem.component.EmptyState
import com.paulfvt.liftapp.core.designsystem.component.LiftButtonSize
import com.paulfvt.liftapp.core.designsystem.component.LiftOutlinedButton
import com.paulfvt.liftapp.core.designsystem.component.PastDateField
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.SignalButton
import com.paulfvt.liftapp.core.model.PhotoPose
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.monthTitle
import java.time.LocalDate

private const val GRID_COLUMNS = 3

// Timeline des photos par mois, la plus récente d'abord, filtrable par pose.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    onBack: () -> Unit,
    onOpenPhoto: (Long) -> Unit,
    onTakePhoto: (PhotoPose?) -> Unit,
    onCompare: (PhotoPose?) -> Unit,
    onOpenLockSettings: () -> Unit,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Sélecteur de photos d'Android : aucune permission nécessaire, l'app ne voit que l'image choisie.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onPicked)
    }
    val pickImage = {
        // Le sélecteur passe l'app en arrière-plan à ta demande : les photos ne se reverrouillent pas pour autant.
        viewModel.beforeLeavingApp()
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                title = { ScreenTitle(stringResource(R.string.profile_photos)) },
                actions = {
                    if (uiState.canCompare) {
                        IconButton(onClick = { onCompare(uiState.pose) }) {
                            Icon(painterResource(R.drawable.ic_compare), stringResource(R.string.photos_compare))
                        }
                    }
                    IconButton(onClick = pickImage) {
                        Icon(painterResource(R.drawable.ic_photo_library), stringResource(R.string.photos_import))
                    }
                    IconButton(onClick = onOpenLockSettings) {
                        Icon(painterResource(R.drawable.ic_lock), stringResource(R.string.lock_settings_title))
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.hasPhotos) {
                ExtendedFloatingActionButton(
                    onClick = { onTakePhoto(uiState.pose) },
                    icon = { Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null) },
                    text = { ButtonLabel(stringResource(R.string.photo_take)) }
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Unit
            !uiState.hasPhotos -> Column(modifier = Modifier.padding(padding)) {
                EmptyState(
                    iconRes = R.drawable.ic_photo_camera,
                    title = stringResource(R.string.photos_empty_title),
                    text = stringResource(R.string.photos_empty_text),
                    modifier = Modifier.weight(1f)
                )
                SignalButton(
                    text = stringResource(R.string.photo_take),
                    onClick = { onTakePhoto(null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    size = LiftButtonSize.Large
                )
                LiftOutlinedButton(
                    text = stringResource(R.string.photos_import),
                    onClick = pickImage,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    size = LiftButtonSize.Large
                )
            }
            else -> Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                PoseFilter(uiState.pose, viewModel::onPoseSelected)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(GRID_COLUMNS),
                    // Marge du bas : les dernières vignettes ne sont pas cachées par le bouton flottant.
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    uiState.months.forEach { group ->
                        item(key = group.month.toString(), span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                monthTitle(group.month),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp).semantics { heading() }
                            )
                        }
                        items(group.items, key = { it.id }) { photo ->
                            PhotoThumbnail(photo, viewModel.file(photo), onClick = { onOpenPhoto(photo.id) })
                        }
                    }
                }
            }
        }
    }

    viewModel.pendingImport?.let { draft ->
        ImportSheet(
            draft = draft,
            today = viewModel.today,
            isImporting = viewModel.isImporting,
            failed = viewModel.importFailed,
            onPoseChange = viewModel::onImportPoseChange,
            onDateChange = viewModel::onImportDateChange,
            onCancel = viewModel::cancelImport,
            onConfirm = viewModel::confirmImport
        )
    }
}

@Composable
private fun PoseFilter(selected: PhotoPose?, onSelect: (PhotoPose?) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        (listOf(null) + PhotoPose.entries).forEach { pose ->
            FilterChip(
                selected = pose == selected,
                onClick = { onSelect(pose) },
                label = { Text(stringResource(pose?.labelRes ?: R.string.photos_filter_all)) }
            )
        }
    }
}

// Aperçu de l'image choisie, avec sa pose et sa date (proposée depuis la photo elle-même).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportSheet(
    draft: PhotoImport,
    today: LocalDate,
    isImporting: Boolean,
    failed: Boolean,
    onPoseChange: (PhotoPose) -> Unit,
    onDateChange: (LocalDate?) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                stringResource(R.string.photo_import_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            AsyncImage(
                model = draft.uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(240.dp)
            )
            PoseSelector(draft.pose, onPoseChange)
            PastDateField(
                label = stringResource(R.string.measurement_date),
                date = draft.date,
                today = today,
                onChange = onDateChange
            )
            if (failed) {
                Text(
                    stringResource(R.string.photo_import_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LiftOutlinedButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    size = LiftButtonSize.Large
                )
                SignalButton(
                    text = stringResource(R.string.photo_import_confirm),
                    onClick = onConfirm,
                    enabled = !isImporting,
                    isLoading = isImporting,
                    size = LiftButtonSize.Large,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Face, profil ou dos : la silhouette fantôme et la comparaison se font toujours entre photos de la même pose.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoseSelector(selected: PhotoPose, onSelect: (PhotoPose) -> Unit, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Text(
            stringResource(R.string.photo_pose),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            PhotoPose.entries.forEachIndexed { index, pose ->
                SegmentedButton(
                    selected = pose == selected,
                    onClick = { onSelect(pose) },
                    shape = SegmentedButtonDefaults.itemShape(index, PhotoPose.entries.size),
                    label = { Text(stringResource(pose.labelRes)) }
                )
            }
        }
    }
}
