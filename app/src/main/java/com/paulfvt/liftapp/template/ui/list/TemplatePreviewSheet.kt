package com.paulfvt.liftapp.template.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftButtonSize
import com.paulfvt.liftapp.core.designsystem.component.LiftOutlinedButton
import com.paulfvt.liftapp.core.designsystem.component.SignalButton
import com.paulfvt.liftapp.exercise.ui.ExerciseThumbnail
import com.paulfvt.liftapp.template.domain.TemplateExerciseDraft

// Aperçu d'un template : ses exercices et leurs séries, avec « Démarrer » et « Modifier ».
// « Démarrer » doit être visible dès l'ouverture, quelle que soit la longueur du template.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatePreviewSheet(preview: TemplatePreview, onStart: () -> Unit, onEdit: () -> Unit, onDismiss: () -> Unit) {
    // Par défaut, Material 3 ouvre une feuille haute à mi-écran, ce qui cachait les boutons :
    // elle s'ouvre ici directement à la hauteur de son contenu (au plus, la hauteur de l'écran).
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                preview.draft.name.uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() }
            )
            // Seule la liste défile : elle prend la place restante (sans s'étirer si elle est courte),
            // et les boutons restent toujours sous elle, à l'écran.
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
            ) {
                if (preview.draft.exercises.isEmpty()) {
                    Text(
                        stringResource(R.string.template_no_exercise),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                preview.draft.exercises.forEach { PreviewExerciseRow(it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                LiftOutlinedButton(
                    text = stringResource(R.string.template_preview_edit),
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    size = LiftButtonSize.Large
                )
                SignalButton(
                    text = stringResource(R.string.template_preview_start),
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    size = LiftButtonSize.Large,
                    icon = R.drawable.ic_play_arrow
                )
            }
        }
    }
}

// « Développé couché · 4 séries · 12-10-8-8 reps » : les reps cibles n'apparaissent que si au moins une est fixée.
@Composable
private fun PreviewExerciseRow(item: TemplateExerciseDraft) {
    val count = item.sets.size
    val targets = item.sets.map { it.targetReps }
    val details = buildList {
        add(pluralStringResource(R.plurals.template_preview_sets, count, count))
        if (targets.any { it != null }) {
            add(stringResource(R.string.template_preview_targets, targets.joinToString("-") { it?.toString() ?: "—" }))
        }
    }.joinToString(" · ")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ExerciseThumbnail(item.exercise)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
