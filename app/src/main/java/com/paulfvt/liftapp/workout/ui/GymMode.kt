package com.paulfvt.liftapp.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.parseWeightInput
import com.paulfvt.liftapp.core.ui.weightInput
import com.paulfvt.liftapp.workout.domain.DURATION_STEP_SECONDS
import com.paulfvt.liftapp.workout.domain.NextSet
import com.paulfvt.liftapp.workout.domain.PreviousSet
import com.paulfvt.liftapp.workout.domain.REPS_STEP
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.distanceStepMeters
import com.paulfvt.liftapp.workout.domain.distanceUnitFor
import com.paulfvt.liftapp.workout.domain.filterDecimalInput
import com.paulfvt.liftapp.workout.domain.filterDurationInput
import com.paulfvt.liftapp.workout.domain.filterRepsInput
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.domain.formatDistance
import com.paulfvt.liftapp.workout.domain.formatDuration
import com.paulfvt.liftapp.workout.domain.matchPrevious
import com.paulfvt.liftapp.workout.domain.parseDistance
import com.paulfvt.liftapp.workout.domain.parseDuration
import com.paulfvt.liftapp.workout.domain.parseReps
import com.paulfvt.liftapp.workout.domain.stepDistance
import com.paulfvt.liftapp.workout.domain.stepDuration
import com.paulfvt.liftapp.workout.domain.stepReps
import com.paulfvt.liftapp.workout.domain.stepWeight

private val StepButtonSize = 72.dp
private val BigFieldWidth = 132.dp
private val BigFieldHeight = 72.dp
private val BigButtonHeight = 72.dp

// Actions de saisie du mode salle. onStep applique un changement aux valeurs en base (boutons − et +).
class GymActions(
    val onStep: (setId: Long, change: (SetValues) -> SetValues) -> Unit,
    val onWeightChange: (setId: Long, Double?) -> Unit,
    val onRepsChange: (setId: Long, Int?) -> Unit,
    val onDurationChange: (setId: Long, Int?) -> Unit,
    val onDistanceChange: (setId: Long, Double?) -> Unit
)

// Mode salle : seule la série à faire est affichée, en grand, avec des boutons − et + de 72 dp pour ne pas avoir à
// taper au clavier. Les valeurs restent modifiables au clavier en touchant le chiffre.
@Composable
fun GymModeContent(
    next: NextSet?,
    previous: Map<Long, List<PreviousSet>>,
    actions: GymActions,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier,
    restTimer: @Composable () -> Unit
) {
    KeepScreenOn()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        restTimer()
        if (next == null) {
            Text(
                stringResource(R.string.gym_all_done),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 32.dp)
            )
        } else {
            // key : en passant à la série suivante, les champs repartent de ses valeurs.
            key(next.set.id) { NextSetPanel(next, previous[next.exercise.exercise.id].orEmpty(), actions) }
        }
        TextButton(onClick = onShowAll, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.gym_show_all))
        }
    }
}

@Composable
private fun NextSetPanel(next: NextSet, previous: List<PreviousSet>, actions: GymActions) {
    val exercise = next.exercise.exercise
    val set = next.set
    val types = next.exercise.sets.map { it.type }
    val matched = matchPrevious(types, previous)[next.position - 1]
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            exercise.name.uppercase(),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Text(
            setPosition(next),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val details = listOfNotNull(
            stringResource(R.string.gym_previous, previousLabel(matched, exercise)),
            set.targetReps?.let { pluralStringResource(R.plurals.gym_target, it, it) }
        )
        Text(
            details.joinToString(" · "),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
    ValueSteppers(set.id, set.values, exercise, actions)
}

// « Série 3 sur 4 », « Échauffement · 1 sur 4 ».
@Composable
private fun setPosition(next: NextSet): String {
    val position = stringResource(R.string.notification_set_position, next.position, next.count)
    return when (next.set.type) {
        SetType.WORK -> position
        SetType.WARMUP -> stringResource(R.string.set_type_warmup) + " · " + position
        SetType.DROP -> stringResource(R.string.set_type_drop) + " · " + position
    }
}

@Composable
private fun ValueSteppers(setId: Long, values: SetValues, exercise: Exercise, actions: GymActions) {
    val (firstTitle, secondTitle) = columnTitles(exercise)
    when (exercise.trackingType) {
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
            val allowNegative = exercise.trackingType == TrackingType.REPS
            val unit = LocalUnits.current.weight
            Stepper(
                title = firstTitle.orEmpty(),
                step = stringResource(
                    R.string.value_with_unit,
                    formatDecimal(unit.stepperStep),
                    stringResource(unit.labelRes)
                ),
                onStep = { steps ->
                    actions.onStep(setId) { it.copy(weightKg = stepWeight(it.weightKg, steps, allowNegative, unit)) }
                }
            ) {
                SetValueField(
                    value = values.weightKg,
                    format = { weightInput(it, unit) },
                    filter = { filterDecimalInput(it, allowNegative) },
                    parse = { parseWeightInput(it, unit) },
                    onValueChange = { actions.onWeightChange(setId, it) },
                    description = firstTitle.orEmpty(),
                    keyboardType = if (allowNegative) KeyboardType.Text else KeyboardType.Decimal,
                    width = BigFieldWidth,
                    height = BigFieldHeight,
                    textStyle = MaterialTheme.typography.displayMedium
                )
            }
            RepsStepper(setId, values, secondTitle, actions)
        }
        TrackingType.DURATION -> DurationStepper(setId, values, secondTitle, actions)
        TrackingType.DISTANCE -> {
            val unit = distanceUnitFor(exercise.category)
            val stepLabel = formatDistance(distanceStepMeters(unit), unit) + " " + firstTitle.orEmpty()
            Stepper(
                title = firstTitle.orEmpty(),
                step = stepLabel,
                onStep = { steps ->
                    actions.onStep(setId) { it.copy(distanceM = stepDistance(it.distanceM, steps, unit)) }
                }
            ) {
                SetValueField(
                    value = values.distanceM,
                    format = { formatDistance(it, unit) },
                    filter = { filterDecimalInput(it, allowNegative = false) },
                    parse = { parseDistance(it, unit) },
                    onValueChange = { actions.onDistanceChange(setId, it) },
                    description = firstTitle.orEmpty(),
                    keyboardType = KeyboardType.Decimal,
                    width = BigFieldWidth,
                    height = BigFieldHeight,
                    textStyle = MaterialTheme.typography.displayMedium
                )
            }
            DurationStepper(setId, values, secondTitle, actions)
        }
    }
}

@Composable
private fun RepsStepper(setId: Long, values: SetValues, title: String, actions: GymActions) {
    Stepper(
        title = title,
        step = pluralStringResource(R.plurals.workout_previous_reps, REPS_STEP, REPS_STEP),
        onStep = { steps -> actions.onStep(setId) { it.copy(reps = stepReps(it.reps, steps)) } }
    ) {
        SetValueField(
            value = values.reps,
            format = { it?.toString().orEmpty() },
            filter = ::filterRepsInput,
            parse = ::parseReps,
            onValueChange = { actions.onRepsChange(setId, it) },
            description = title,
            keyboardType = KeyboardType.Number,
            width = BigFieldWidth,
            height = BigFieldHeight,
            textStyle = MaterialTheme.typography.displayMedium
        )
    }
}

@Composable
private fun DurationStepper(setId: Long, values: SetValues, title: String, actions: GymActions) {
    Stepper(
        title = title,
        step = stringResource(R.string.duration_seconds, DURATION_STEP_SECONDS),
        onStep = { steps -> actions.onStep(setId) { it.copy(durationSec = stepDuration(it.durationSec, steps)) } }
    ) {
        SetValueField(
            value = values.durationSec,
            format = ::formatDuration,
            filter = ::filterDurationInput,
            parse = ::parseDuration,
            onValueChange = { actions.onDurationChange(setId, it) },
            description = title,
            keyboardType = KeyboardType.Text,
            width = BigFieldWidth,
            height = BigFieldHeight,
            textStyle = MaterialTheme.typography.displayMedium
        )
    }
}

// Valeur encadrée de deux gros boutons. step : ce que fait un appui, lu par le lecteur d'écran (« Plus 2,5 kg »).
@Composable
private fun Stepper(title: String, step: String, onStep: (steps: Int) -> Unit, field: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepButton(R.drawable.ic_remove, stringResource(R.string.gym_decrease, step)) { onStep(-1) }
            field()
            StepButton(R.drawable.ic_add, stringResource(R.string.gym_increase, step)) { onStep(1) }
        }
    }
}

@Composable
private fun StepButton(icon: Int, description: String, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(StepButtonSize)) {
        Icon(painterResource(icon), contentDescription = description, modifier = Modifier.size(32.dp))
    }
}

// Barre du bas du mode salle : « Valider la série » en grand sous le pouce, « Terminer » à côté.
// Quand tout est validé, il ne reste que « Terminer la séance ».
@Composable
fun GymBottomBar(hasNextSet: Boolean, onComplete: () -> Unit, onFinish: () -> Unit) {
    val signal = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (hasNextSet) {
            OutlinedButton(onClick = onFinish, modifier = Modifier.weight(1f).heightIn(min = BigButtonHeight)) {
                Text(stringResource(R.string.gym_finish), style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = onComplete,
                colors = signal,
                modifier = Modifier.weight(2f).heightIn(min = BigButtonHeight)
            ) {
                Text(stringResource(R.string.gym_complete), style = MaterialTheme.typography.titleLarge)
            }
        } else {
            Button(
                onClick = onFinish,
                colors = signal,
                modifier = Modifier.fillMaxWidth().heightIn(min = BigButtonHeight)
            ) {
                Text(stringResource(R.string.workout_finish), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

// Garde l'écran allumé tant que ce composant est affiché, et seulement lui : en quittant la séance ou le mode salle,
// le téléphone reprend sa mise en veille normale.
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
