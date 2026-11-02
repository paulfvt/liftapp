package com.paulfvt.liftapp.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.MenuEntry
import com.paulfvt.liftapp.core.designsystem.theme.LocalExtendedColors
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.core.ui.SetTypeButton
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.setName
import com.paulfvt.liftapp.core.ui.weightInput
import com.paulfvt.liftapp.workout.domain.FieldSlot
import com.paulfvt.liftapp.workout.domain.PreviousSet
import com.paulfvt.liftapp.workout.domain.SetField
import com.paulfvt.liftapp.workout.domain.WorkoutSet
import com.paulfvt.liftapp.workout.domain.distanceUnitFor
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.domain.formatDistance
import com.paulfvt.liftapp.workout.domain.formatDuration
import java.util.Locale

// Largeurs des colonnes d'une ligne de série, partagées avec l'en-tête pour que tout reste aligné.
private val TypeColumn = 48.dp
private val FirstFieldColumn = 72.dp
private val SecondFieldColumn = 60.dp
private val CompleteColumn = 48.dp
private val ColumnGap = 6.dp

// Actions sur une série, regroupées pour ne pas faire passer dix paramètres à chaque ligne.
class SetActions(
    val onTypeChange: (setId: Long, SetType) -> Unit,
    val onWeightChange: (setId: Long, Double?) -> Unit,
    val onRepsChange: (setId: Long, Int?) -> Unit,
    val onDurationChange: (setId: Long, Int?) -> Unit,
    val onDistanceChange: (setId: Long, Double?) -> Unit,
    val onToggleCompleted: (setId: Long) -> Unit,
    val onRpeClick: (WorkoutSet) -> Unit,
    val onRemove: (setId: Long) -> Unit
)

// Séance en cours (LIVE) : colonne « Précédent » et case de validation. Séance passée modifiée (EDIT) : ni l'une
// ni l'autre, toutes ses séries sont validées.
enum class SetRowMode {
    LIVE,
    EDIT
}

@Composable
fun SetHeaderRow(exercise: Exercise, mode: SetRowMode = SetRowMode.LIVE) {
    val (first, second) = columnTitles(exercise)
    Row(
        horizontalArrangement = Arrangement.spacedBy(ColumnGap),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
        ColumnTitle(stringResource(R.string.template_set_header), Modifier.width(TypeColumn))
        val previousTitle = if (mode == SetRowMode.LIVE) stringResource(R.string.workout_col_previous) else ""
        ColumnTitle(previousTitle, Modifier.weight(1f), TextAlign.Start)
        if (first != null) ColumnTitle(first, Modifier.width(FirstFieldColumn))
        ColumnTitle(
            second,
            Modifier.width(
                if (first ==
                    null
                ) {
                    FirstFieldColumn + ColumnGap + SecondFieldColumn
                } else {
                    SecondFieldColumn
                }
            )
        )
        Spacer(Modifier.width(CompleteColumn))
    }
}

// Titres des colonnes de saisie selon le type de suivi. Null : pas de première colonne (durée seule).
@Composable
internal fun columnTitles(exercise: Exercise): Pair<String?, String> = when (exercise.trackingType) {
    TrackingType.WEIGHT_REPS ->
        stringResource(LocalUnits.current.weight.labelRes) to stringResource(R.string.workout_col_reps)
    TrackingType.REPS -> stringResource(
        R.string.workout_col_added,
        stringResource(LocalUnits.current.weight.labelRes)
    ) to
        stringResource(R.string.workout_col_reps)
    TrackingType.DURATION -> null to stringResource(R.string.workout_col_time)
    TrackingType.DISTANCE -> {
        val unit = if (distanceUnitFor(exercise.category).meters >
            1.0
        ) {
            R.string.workout_col_km
        } else {
            R.string.workout_col_m
        }
        stringResource(unit) to stringResource(R.string.workout_col_time)
    }
}

@Composable
// En-têtes du tableau en petites capitales (maquette ON AIR).
private fun ColumnTitle(text: String, modifier: Modifier, textAlign: TextAlign = TextAlign.Center) {
    Text(
        text.uppercase(Locale.FRENCH),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.08.em),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
fun WorkoutSetRow(
    set: WorkoutSet,
    workNumber: Int?,
    previous: PreviousSet?,
    exercise: Exercise,
    canRemove: Boolean,
    actions: SetActions,
    keypad: SetKeypadState,
    mode: SetRowMode = SetRowMode.LIVE,
    isCurrent: Boolean = false
) {
    val name = setName(set.type, workNumber)
    val isLive = mode == SetRowMode.LIVE
    val successColors = LocalExtendedColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ColumnGap),
        modifier = Modifier
            .fillMaxWidth()
            // Série validée : fond vert discret, comme dans le guide de style.
            .background(
                if (isLive && set.isCompleted) successColors.success.copy(alpha = 0.14f) else Color.Transparent,
                MaterialTheme.shapes.small
            )
            // Prochaine série à valider : entourée de rouge (.setrow.current).
            .border(
                1.dp,
                if (isLive && isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                MaterialTheme.shapes.small
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Box(Modifier.width(TypeColumn), contentAlignment = Alignment.Center) {
            SetTypeButton(
                type = set.type,
                workNumber = workNumber,
                onTypeChange = { actions.onTypeChange(set.id, it) },
                extraEntries = listOf(
                    MenuEntry(R.string.workout_rpe_menu) { actions.onRpeClick(set) },
                    MenuEntry(
                        R.string.action_delete,
                        R.drawable.ic_delete,
                        isDestructive = true,
                        isEnabled = canRemove
                    ) { actions.onRemove(set.id) }
                )
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (isLive) {
                Text(
                    previousLabel(previous, exercise),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val details = listOfNotNull(
                set.targetReps?.let { stringResource(R.string.workout_target, it) },
                set.rpe?.let { stringResource(R.string.workout_rpe, formatDecimal(it)) }
            )
            if (details.isNotEmpty()) {
                Text(
                    details.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1
                )
            }
        }
        ValueFields(set, exercise, name, actions, keypad)
        if (isLive) {
            CompleteButton(
                completed = set.isCompleted,
                name = name,
                onToggle = { actions.onToggleCompleted(set.id) }
            )
        } else {
            Spacer(Modifier.width(CompleteColumn))
        }
    }
}

@Composable
private fun ValueFields(
    set: WorkoutSet,
    exercise: Exercise,
    name: String,
    actions: SetActions,
    keypad: SetKeypadState
) {
    val values = set.values
    val (firstTitle, secondTitle) = columnTitles(exercise)
    val first = SetField(set.id, FieldSlot.FIRST)
    val second = SetField(set.id, FieldSlot.SECOND)
    val firstDescription = stringResource(R.string.workout_field, name, firstTitle.orEmpty())
    val secondDescription = stringResource(R.string.workout_field, name, secondTitle)
    when (exercise.trackingType) {
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
            // Pour le poids du corps, le premier champ est le lest : négatif pour une assistance.
            SetValueField(
                value = values.weightKg,
                spec = weightSpec(allowNegative = exercise.trackingType == TrackingType.REPS),
                onValueChange = { actions.onWeightChange(set.id, it) },
                field = first,
                keypad = keypad,
                description = firstDescription,
                width = FirstFieldColumn
            )
            SetValueField(
                value = values.reps,
                spec = repsSpec(),
                onValueChange = { actions.onRepsChange(set.id, it) },
                field = second,
                keypad = keypad,
                description = secondDescription,
                width = SecondFieldColumn
            )
        }
        TrackingType.DURATION -> SetValueField(
            value = values.durationSec,
            spec = durationSpec(),
            onValueChange = { actions.onDurationChange(set.id, it) },
            field = second,
            keypad = keypad,
            description = secondDescription,
            width = FirstFieldColumn + ColumnGap + SecondFieldColumn
        )
        TrackingType.DISTANCE -> {
            SetValueField(
                value = values.distanceM,
                spec = distanceSpec(distanceUnitFor(exercise.category)),
                onValueChange = { actions.onDistanceChange(set.id, it) },
                field = first,
                keypad = keypad,
                description = firstDescription,
                width = FirstFieldColumn
            )
            SetValueField(
                value = values.durationSec,
                spec = durationSpec(),
                onValueChange = { actions.onDurationChange(set.id, it) },
                field = second,
                keypad = keypad,
                description = secondDescription,
                width = SecondFieldColumn
            )
        }
    }
}

// Case de validation de 48 dp. Le lecteur d'écran l'annonce comme une case à cocher nommée d'après la série.
@Composable
private fun CompleteButton(completed: Boolean, name: String, onToggle: () -> Unit) {
    val colors = LocalExtendedColors.current
    val shape = MaterialTheme.shapes.small
    val description =
        stringResource(if (completed) R.string.workout_uncomplete_set else R.string.workout_complete_set, name)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(CompleteColumn)
            .toggleable(value = completed, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = description }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = if (completed) {
                Modifier.size(40.dp).background(colors.success, shape)
            } else {
                Modifier.size(40.dp).border(2.dp, MaterialTheme.colorScheme.outline, shape)
            }
        ) {
            if (completed) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = colors.onSuccess
                )
            }
        }
    }
}

// « 80 × 8 », « +10 × 8 », « 12 reps », « 1:30 », « 5,2 · 25:00 » ou « — » si l'exercice n'a jamais été fait.
@Composable
internal fun previousLabel(previous: PreviousSet?, exercise: Exercise): String {
    val none = stringResource(R.string.workout_previous_none)
    val unit = LocalUnits.current.weight
    val values = previous?.values ?: return none
    return when (exercise.trackingType) {
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
            val reps = values.reps ?: return none
            val weight = values.weightKg
            when {
                weight == null -> pluralStringResource(R.plurals.workout_previous_reps, reps, reps)
                exercise.trackingType == TrackingType.REPS && weight > 0 ->
                    stringResource(R.string.workout_previous_weight_reps, "+" + weightInput(weight, unit), reps)
                else -> stringResource(R.string.workout_previous_weight_reps, weightInput(weight, unit), reps)
            }
        }
        TrackingType.DURATION -> formatDuration(values.durationSec).ifEmpty { none }
        TrackingType.DISTANCE -> {
            val distance = formatDistance(values.distanceM, distanceUnitFor(exercise.category))
            val duration = formatDuration(values.durationSec)
            listOf(distance, duration).filter { it.isNotEmpty() }.joinToString(" · ").ifEmpty { none }
        }
    }
}
