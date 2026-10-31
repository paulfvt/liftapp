package com.paulfvt.liftapp.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.SectionBlock
import com.paulfvt.liftapp.core.ui.dayAndTime
import com.paulfvt.liftapp.settings.domain.BackupInfo
import java.time.LocalDate

// Section « Données » des réglages : export CSV, sauvegarde et restauration. Les fichiers passent par le sélecteur
// de fichiers d'Android : l'utilisateur choisit l'emplacement, l'app n'a besoin d'aucune permission de stockage.
@Composable
fun DataSection(
    today: LocalDate,
    isWorking: Boolean,
    restoreCandidate: BackupInfo?,
    onExportHistory: (Uri) -> Unit,
    onExportMeasurements: (Uri) -> Unit,
    onCreateBackup: (Uri, Boolean) -> Unit,
    onInspectBackup: (Uri) -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit
) {
    var isBackupDialogOpen by rememberSaveable { mutableStateOf(false) }
    // Photos cochées par défaut (décision du 28 octobre 2026).
    var includePhotos by rememberSaveable { mutableStateOf(true) }
    val historyExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV_MIME_TYPE)) {
        it?.let(onExportHistory)
    }
    val measurementExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV_MIME_TYPE)) {
        it?.let(onExportMeasurements)
    }
    val backupTarget = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ZIP_MIME_TYPE)) {
        it?.let { uri -> onCreateBackup(uri, includePhotos) }
    }
    val backupSource = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let(onInspectBackup)
    }
    val date = today.toString()
    val historyFileName = stringResource(R.string.csv_history_file_name, date)
    val measurementFileName = stringResource(R.string.csv_measurements_file_name, date)
    val backupFileName = stringResource(R.string.backup_file_name, date)
    val workingDescription = stringResource(R.string.data_working)

    SectionBlock(stringResource(R.string.settings_data)) {
        SettingsCard {
            if (isWorking) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = workingDescription }
                )
            }
            ActionRow(
                label = stringResource(R.string.settings_export_history),
                supporting = stringResource(R.string.settings_export_history_summary),
                enabled = !isWorking,
                onClick = { historyExport.launch(historyFileName) }
            )
            ActionRow(
                label = stringResource(R.string.settings_export_measurements),
                supporting = stringResource(R.string.settings_export_measurements_summary),
                enabled = !isWorking,
                onClick = { measurementExport.launch(measurementFileName) }
            )
            ActionRow(
                label = stringResource(R.string.settings_backup),
                supporting = stringResource(R.string.settings_backup_summary),
                enabled = !isWorking,
                onClick = { isBackupDialogOpen = true }
            )
            ActionRow(
                label = stringResource(R.string.settings_restore),
                supporting = stringResource(R.string.settings_restore_summary),
                enabled = !isWorking,
                onClick = { backupSource.launch(BACKUP_MIME_TYPES) }
            )
        }
    }

    if (isBackupDialogOpen) {
        BackupDialog(
            includePhotos = includePhotos,
            onIncludePhotosChange = { includePhotos = it },
            onConfirm = {
                isBackupDialogOpen = false
                backupTarget.launch(backupFileName)
            },
            onDismiss = { isBackupDialogOpen = false }
        )
    }
    restoreCandidate?.let { RestoreDialog(it, onConfirm = onConfirmRestore, onDismiss = onCancelRestore) }
}

// Ligne qui lance une action : toute la ligne est un bouton, grisé pendant une autre opération.
@Composable
private fun ActionRow(label: String, supporting: String, enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(supporting) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            headlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
        ),
        modifier = Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    )
}

// Avant de choisir l'emplacement : ce que contient la sauvegarde, et l'avertissement sur les photos (le fichier n'est
// pas chiffré).
@Composable
private fun BackupDialog(
    includePhotos: Boolean,
    onIncludePhotosChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.backup_dialog_text))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(value = includePhotos, role = Role.Checkbox, onValueChange = onIncludePhotosChange)
                ) {
                    Checkbox(checked = includePhotos, onCheckedChange = null)
                    Text(stringResource(R.string.backup_include_photos), modifier = Modifier.padding(start = 12.dp))
                }
                if (includePhotos) {
                    Text(
                        stringResource(R.string.backup_photos_warning),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = { LiftTextButton(stringResource(R.string.backup_choose_file), onClick = onConfirm) },
        dismissButton = { LiftTextButton(stringResource(R.string.action_cancel), onClick = onDismiss) }
    )
}

// Confirmation de la restauration, une fois la sauvegarde vérifiée : ce qu'elle contient, et ce qui sera perdu.
@Composable
private fun RestoreDialog(info: BackupInfo, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restore_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val workouts = pluralStringResource(R.plurals.progress_sessions, info.workoutCount, info.workoutCount)
                val photos = if (info.includesPhotos) {
                    pluralStringResource(R.plurals.photos_count, info.photoCount, info.photoCount)
                } else {
                    stringResource(R.string.restore_no_photos)
                }
                Text(stringResource(R.string.restore_dialog_content, dayAndTime(info.createdAt), workouts, photos))
                Text(stringResource(R.string.restore_dialog_warning))
                if (!info.includesPhotos) {
                    Text(
                        stringResource(R.string.restore_dialog_photos_deleted),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            LiftTextButton(
                text = stringResource(R.string.restore_confirm),
                onClick = onConfirm,
                color = MaterialTheme.colorScheme.error
            )
        },
        dismissButton = { LiftTextButton(stringResource(R.string.action_cancel), onClick = onDismiss) }
    )
}

private const val CSV_MIME_TYPE = "text/csv"
private const val ZIP_MIME_TYPE = "application/zip"

// Selon l'appli de fichiers, un ZIP peut être annoncé sous l'un ou l'autre de ces types.
private val BACKUP_MIME_TYPES = arrayOf(ZIP_MIME_TYPE, "application/x-zip-compressed", "application/octet-stream")
private const val DISABLED_ALPHA = 0.38f
