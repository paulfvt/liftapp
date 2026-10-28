package com.paulfvt.liftapp.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.profile.domain.MeasurementSummary
import com.paulfvt.liftapp.profile.domain.ProgressPhoto
import com.paulfvt.liftapp.workout.domain.formatDecimal
import java.io.File
import java.util.Locale

// Onglet Profil : le profil fixe, puis les mesures, les photos, les réglages et « À propos ».
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onOpenMeasurement: (MeasurementType) -> Unit,
    onAddMeasurement: () -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { ScreenTitle(stringResource(R.string.nav_profile)) }) }) { padding ->
        if (uiState.isLoading) return@Scaffold
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            ProfileCard(uiState, onEditProfile, modifier = Modifier.padding(horizontal = 16.dp))
            MeasurementsSection(
                summaries = uiState.measurements,
                onOpen = onOpenMeasurement,
                onAdd = onAddMeasurement,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            PhotosSection(
                uiState = uiState,
                fileOf = viewModel::file,
                onOpen = onOpenPhotos,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_title)) },
                supportingContent = { Text(stringResource(R.string.settings_summary)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_settings), contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.clickable(role = Role.Button, onClick = onOpenSettings)
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_title)) },
                supportingContent = { Text(stringResource(R.string.profile_about_summary)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_info), contentDescription = null) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.clickable(role = Role.Button, onClick = onOpenAbout)
            )
        }
    }
}

// « Paul · 181 cm · 28 ans · Homme ». Un appui ouvre la modification.
@Composable
private fun ProfileCard(uiState: ProfileUiState, onEdit: () -> Unit, modifier: Modifier = Modifier) {
    val profile = uiState.profile
    val details = buildList {
        profile.heightCm?.let { add(stringResource(R.string.value_cm, formatDecimal(it))) }
        uiState.age?.let { add(pluralStringResource(R.plurals.profile_age, it, it)) }
        profile.sex?.let { add(stringResource(sexLabel(it))) }
    }
    ElevatedCard(onClick = onEdit, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(
                    56.dp
                ).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
            ) {
                val initial = profile.firstName?.first()?.toString()?.uppercase(Locale.FRENCH)
                if (initial != null) {
                    Text(
                        initial,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.ic_person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.firstName ?: stringResource(R.string.profile_title_empty),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    details.joinToString(" · ").ifEmpty { stringResource(R.string.profile_details_empty) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.profile_edit_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Dernière valeur de chaque mesure (« Poids 79,4 kg · −0,6 kg »), un appui ouvre sa courbe et son historique.
@Composable
private fun MeasurementsSection(
    summaries: List<MeasurementSummary>,
    onOpen: (MeasurementType) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        SectionTitle(stringResource(R.string.profile_measurements))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            if (summaries.isEmpty()) {
                Text(
                    stringResource(R.string.measurements_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
                )
            }
            summaries.forEach { summary ->
                val latest = summary.latest
                ListItem(
                    headlineContent = { Text(stringResource(latest.type.labelRes)) },
                    supportingContent = { Text(shortDay(latest.measuredAt)) },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                measureText(latest.type, latest.value),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            summary.change?.let {
                                Text(changeText(latest.type, it), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(role = Role.Button) { onOpen(latest.type) }
                )
            }
            TextButton(onClick = onAdd, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Icon(
                    painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Text(
                    stringResource(R.string.measurement_add),
                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
                )
            }
        }
    }
}

// Les dernières photos en vignettes ; un appui ouvre la timeline.
@Composable
private fun PhotosSection(
    uiState: ProfileUiState,
    fileOf: (ProgressPhoto) -> File,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        SectionTitle(stringResource(R.string.profile_photos))
        ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                if (uiState.photosLocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_lock),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(stringResource(R.string.photos_locked), style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (uiState.latestPhotos.isEmpty()) {
                    Text(
                        stringResource(R.string.photos_none_short),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.latestPhotos.forEach { photo ->
                            PhotoThumbnail(photo, fileOf(photo), onClick = onOpen, modifier = Modifier.weight(1f))
                        }
                        // Cases vides : les vignettes gardent la même taille avec une ou deux photos.
                        repeat(LATEST_PHOTO_SLOTS - uiState.latestPhotos.size) { Box(Modifier.weight(1f)) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sans photo, le texte « Aucune photo » au-dessus suffit : pas de compteur à zéro.
                    Text(
                        if (uiState.photoCount > 0) {
                            pluralStringResource(R.plurals.photos_count, uiState.photoCount, uiState.photoCount)
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.photos_see),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private const val LATEST_PHOTO_SLOTS = 3
