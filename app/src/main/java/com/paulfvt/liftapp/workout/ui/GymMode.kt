package com.paulfvt.liftapp.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftButtonSize
import com.paulfvt.liftapp.core.designsystem.component.LiftOutlinedButton
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.SignalButton
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.model.SetType
import com.paulfvt.liftapp.core.model.TrackingType
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.workout.domain.FieldSlot
import com.paulfvt.liftapp.workout.domain.NextSet
import com.paulfvt.liftapp.workout.domain.PreviousSet
import com.paulfvt.liftapp.workout.domain.SetField
import com.paulfvt.liftapp.workout.domain.SetValues
import com.paulfvt.liftapp.workout.domain.distanceUnitFor
import com.paulfvt.liftapp.workout.domain.matchPrevious
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
// taper. Les valeurs restent modifiables au pavé en touchant le chiffre.
@Composable
fun GymModeContent(
    next: NextSet?,
    previous: Map<Long, List<PreviousSet>>,
    actions: GymActions,
    keypad: SetKeypadState,
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
            key(next.set.id) { NextSetPanel(next, previous[next.exercise.exercise.id].orEmpty(), actions, keypad) }
        }
        LiftTextButton(
            text = stringResource(R.string.gym_show_all),
            onClick = onShowAll,
            modifier = Modifier.heightIn(min = 48.dp)
        )
    }
}

@Composable
private fun NextSetPanel(next: NextSet, previous: List<PreviousSet>, actions: GymActions, keypad: SetKeypadState) {
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
    ValueSteppers(set.id, set.values, exercise, actions, keypad)
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
private fun ValueSteppers(
    setId: Long,
    values: SetValues,
    exercise: Exercise,
    actions: GymActions,
    keypad: SetKeypadState
) {
    val (firstTitle, secondTitle) = columnTitles(exercise)
    val first = SetField(setId, FieldSlot.FIRST)
    val second = SetField(setId, FieldSlot.SECOND)
    val durationStepper = @Composable {
        Stepper(
            title = secondTitle,
            value = values.durationSec,
            spec = durationSpec(),
            field = second,
            keypad = keypad,
            onStep = { steps -> actions.onStep(setId) { it.copy(durationSec = stepDuration(it.durationSec, steps)) } },
            onValueChange = { actions.onDurationChange(setId, it) }
        )
    }
    when (exercise.trackingType) {
        TrackingType.WEIGHT_REPS, TrackingType.REPS -> {
            val allowNegative = exercise.trackingType == TrackingType.REPS
            val unit = LocalUnits.current.weight
            Stepper(
                title = firstTitle.orEmpty(),
                value = values.weightKg,
                spec = weightSpec(allowNegative),
                field = first,
                keypad = keypad,
                onStep = { steps ->
                    actions.onStep(setId) { it.copy(weightKg = stepWeight(it.weightKg, steps, allowNegative, unit)) }
                },
                onValueChange = { actions.onWeightChange(setId, it) }
            )
            Stepper(
                title = secondTitle,
                value = values.reps,
                spec = repsSpec(),
                field = second,
                keypad = keypad,
                onStep = { steps -> actions.onStep(setId) { it.copy(reps = stepReps(it.reps, steps)) } },
                onValueChange = { actions.onRepsChange(setId, it) }
            )
        }
        TrackingType.DURATION -> durationStepper()
        TrackingType.DISTANCE -> {
            val unit = distanceUnitFor(exercise.category)
            Stepper(
                title = firstTitle.orEmpty(),
                value = values.distanceM,
                spec = distanceSpec(unit),
                field = first,
                keypad = keypad,
                onStep = { steps ->
                    actions.onStep(setId) { it.copy(distanceM = stepDistance(it.distanceM, steps, unit)) }
                },
                onValueChange = { actions.onDistanceChange(setId, it) }
            )
            durationStepper()
        }
    }
}

// Valeur encadrée de deux gros boutons. Case ouverte au pavé : − et + passent par lui, pour que la case affiche la
// nouvelle valeur (sinon elle gardait le texte tapé et les boutons semblaient sans effet).
@Composable
private fun <T> Stepper(
    title: String,
    value: T?,
    spec: ValueSpec<T>,
    field: SetField,
    keypad: SetKeypadState,
    onStep: (steps: Int) -> Unit,
    onValueChange: (T?) -> Unit
) {
    val step = { steps: Int -> if (keypad.isEditing(field)) keypad.step(steps) else onStep(steps) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepButton(R.drawable.ic_remove, stringResource(R.string.gym_decrease, spec.stepLabel)) { step(-1) }
            SetValueField(
                value = value,
                spec = spec,
                onValueChange = onValueChange,
                field = field,
                keypad = keypad,
                description = title,
                width = BigFieldWidth,
                height = BigFieldHeight,
                textStyle = MaterialTheme.typography.displayMedium
            )
            StepButton(R.drawable.ic_add, stringResource(R.string.gym_increase, spec.stepLabel)) { step(1) }
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (hasNextSet) {
            LiftOutlinedButton(
                text = stringResource(R.string.gym_finish),
                onClick = onFinish,
                modifier = Modifier.weight(1f).heightIn(min = BigButtonHeight),
                size = LiftButtonSize.Large,
                // Bouton étroit (un tiers de la largeur) : marges serrées pour que « TERMINER » tienne sur une ligne.
                contentPadding = PaddingValues(horizontal = 8.dp)
            )
            SignalButton(
                text = stringResource(R.string.gym_complete),
                onClick = onComplete,
                modifier = Modifier.weight(2f),
                size = LiftButtonSize.Gym
            )
        } else {
            SignalButton(
                text = stringResource(R.string.workout_finish),
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
                size = LiftButtonSize.Gym
            )
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
