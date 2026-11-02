package com.paulfvt.liftapp.workout.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.ButtonLabel
import com.paulfvt.liftapp.core.designsystem.component.hairlineBorder
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.parseWeightInput
import com.paulfvt.liftapp.core.ui.weightInput
import com.paulfvt.liftapp.workout.domain.DURATION_STEP_SECONDS
import com.paulfvt.liftapp.workout.domain.DistanceUnit
import com.paulfvt.liftapp.workout.domain.KeypadText
import com.paulfvt.liftapp.workout.domain.REPS_STEP
import com.paulfvt.liftapp.workout.domain.SetField
import com.paulfvt.liftapp.workout.domain.WorkoutExercise
import com.paulfvt.liftapp.workout.domain.distanceStepMeters
import com.paulfvt.liftapp.workout.domain.erased
import com.paulfvt.liftapp.workout.domain.filterDecimalInput
import com.paulfvt.liftapp.workout.domain.filterDurationInput
import com.paulfvt.liftapp.workout.domain.filterRepsInput
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.domain.formatDistance
import com.paulfvt.liftapp.workout.domain.formatDuration
import com.paulfvt.liftapp.workout.domain.keypadFields
import com.paulfvt.liftapp.workout.domain.moveAfter
import com.paulfvt.liftapp.workout.domain.parseDistance
import com.paulfvt.liftapp.workout.domain.parseDuration
import com.paulfvt.liftapp.workout.domain.parseReps
import com.paulfvt.liftapp.workout.domain.stepDistance
import com.paulfvt.liftapp.workout.domain.stepDuration
import com.paulfvt.liftapp.workout.domain.stepReps
import com.paulfvt.liftapp.workout.domain.stepWeight
import com.paulfvt.liftapp.workout.domain.typed

// Pavé numérique de saisie des séries, façon Strong (décision du 2 novembre 2026) : il remplace le clavier d'Android,
// trop grand et sans boutons − et +. Il s'affiche en bas de l'écran tant qu'une case de série est ouverte.

// Touche en bas à gauche : virgule pour un poids ou une distance, deux-points pour une durée, rien pour des reps.
enum class CornerKey(val char: Char?) {
    NONE(null),
    DECIMAL(','),
    COLON(':')
}

// Façon d'éditer une valeur de série : affichage, frappe autorisée, lecture, effet des touches − et +.
class ValueSpec<T>(
    val format: (T?) -> String,
    val filter: (String) -> String,
    val parse: (String) -> T?,
    val step: (T?, Int) -> T?,
    // Ce que fait un appui sur − ou +, lu par le lecteur d'écran (« Plus 2,5 kg »).
    val stepLabel: String,
    val cornerKey: CornerKey
)

// Poids en kilos, saisi dans l'unité choisie. Lest d'un exercice au poids du corps : − descend sous zéro
// (assistance), le pavé n'ayant pas de touche de signe.
@Composable
internal fun weightSpec(allowNegative: Boolean): ValueSpec<Double> {
    val unit = LocalUnits.current.weight
    val stepLabel =
        stringResource(R.string.value_with_unit, formatDecimal(unit.stepperStep), stringResource(unit.labelRes))
    return remember(unit, allowNegative, stepLabel) {
        ValueSpec(
            format = { weightInput(it, unit) },
            filter = { filterDecimalInput(it, allowNegative) },
            parse = { parseWeightInput(it, unit) },
            step = { kg, steps -> stepWeight(kg, steps, allowNegative, unit) },
            stepLabel = stepLabel,
            cornerKey = CornerKey.DECIMAL
        )
    }
}

@Composable
internal fun repsSpec(): ValueSpec<Int> {
    val stepLabel = pluralStringResource(R.plurals.workout_previous_reps, REPS_STEP, REPS_STEP)
    return remember(stepLabel) {
        ValueSpec(
            format = { it?.toString().orEmpty() },
            filter = ::filterRepsInput,
            parse = ::parseReps,
            step = ::stepReps,
            stepLabel = stepLabel,
            cornerKey = CornerKey.NONE
        )
    }
}

@Composable
internal fun durationSpec(): ValueSpec<Int> {
    val stepLabel = stringResource(R.string.duration_seconds, DURATION_STEP_SECONDS)
    return remember(stepLabel) {
        ValueSpec(
            format = ::formatDuration,
            filter = ::filterDurationInput,
            parse = ::parseDuration,
            step = ::stepDuration,
            stepLabel = stepLabel,
            cornerKey = CornerKey.COLON
        )
    }
}

@Composable
internal fun distanceSpec(unit: DistanceUnit): ValueSpec<Double> {
    val unitLabel = stringResource(if (unit.meters > 1.0) R.string.workout_col_km else R.string.workout_col_m)
    val stepLabel = formatDistance(distanceStepMeters(unit), unit) + " " + unitLabel
    return remember(unit, stepLabel) {
        ValueSpec(
            format = { formatDistance(it, unit) },
            filter = { filterDecimalInput(it, allowNegative = false) },
            parse = { parseDistance(it, unit) },
            step = { meters, steps -> stepDistance(meters, steps, unit) },
            stepLabel = stepLabel,
            cornerKey = CornerKey.DECIMAL
        )
    }
}

// Ce que la case ouverte confie au pavé : comment filtrer, enregistrer et ajuster son texte.
class FieldEditor(
    val filter: (String) -> String,
    val save: (String) -> Unit,
    val step: (text: String, steps: Int) -> String,
    val stepLabel: String,
    val cornerKey: CornerKey
)

private fun <T> ValueSpec<T>.editor(onValueChange: (T?) -> Unit) = FieldEditor(
    filter = filter,
    save = { onValueChange(parse(it)) },
    step = { text, steps -> format(step(parse(text), steps)) },
    stepLabel = stepLabel,
    cornerKey = cornerKey
)

// État du pavé, partagé par l'écran (qui l'affiche) et les cases (qui l'ouvrent). Chaque touche enregistre aussitôt
// la valeur, comme le faisait le clavier : la séance en cours survit ainsi à un arrêt de l'app.
@Stable
class SetKeypadState {
    // Case ouverte ; le pavé est affiché tant qu'elle n'est pas nulle.
    var openField: SetField? by mutableStateOf(null)
        private set

    var input: KeypadText by mutableStateOf(KeypadText(""))
        private set

    // Hauteur de la barre du bas (pavé, minuteur), donnée par l'écran : quand elle change (un repos démarre après
    // « Suivant »), la case ouverte se remet en vue au lieu de rester cachée derrière.
    var bottomBarHeight by mutableIntStateOf(0)

    // La case ouverte se « branche » une fois composée (bind) : jusque-là, les touches ne font rien. Utile quand
    // « Suivant » ouvre une case encore hors de l'écran.
    private var bound: SetField? by mutableStateOf(null)
    private var editor: FieldEditor? by mutableStateOf(null)

    val isOpen: Boolean get() = openField != null
    val cornerKey: CornerKey get() = editor?.cornerKey ?: CornerKey.NONE
    val stepLabel: String get() = editor?.stepLabel.orEmpty()

    fun isEditing(target: SetField): Boolean = openField == target && bound == target

    // Toucher la case déjà ouverte resélectionne sa valeur, comme dans Strong.
    fun open(target: SetField) {
        if (isEditing(target)) {
            input = KeypadText(input.text)
            return
        }
        openField = target
        bound = null
        editor = null
    }

    fun close() {
        openField = null
        bound = null
        editor = null
    }

    fun bind(target: SetField, text: String, editor: FieldEditor) {
        if (openField != target || bound == target) return
        input = KeypadText(text)
        this.editor = editor
        bound = target
    }

    fun type(key: Char) = edit { text, editor -> text.typed(key, editor.filter) }

    fun erase() = edit { text, _ -> text.erased() }

    // − et + : la nouvelle valeur reste sélectionnée, la touche suivante la remplace.
    fun step(steps: Int) = edit { text, editor -> KeypadText(editor.step(text.text, steps)) }

    private fun edit(change: (KeypadText, FieldEditor) -> KeypadText) {
        val editor = editor ?: return
        input = change(input, editor)
        editor.save(input.text)
    }
}

// « Suivant » : ouvre la case suivante de la séance (ou ferme le pavé après la dernière) et renvoie la série quittée,
// que l'écran de la séance valide.
fun SetKeypadState.moveNext(exercises: List<WorkoutExercise>): Long? {
    val current = openField ?: return null
    val move = exercises.keypadFields().moveAfter(current)
    move.next?.let(::open) ?: close()
    return move.finishedSetId
}

// Suit la case ouverte dans la liste des exercices : fait défiler jusqu'à sa carte quand elle est hors de l'écran
// (« Suivant » vers l'exercice d'après), ferme le pavé si sa série a été retirée. firstIndex : place du premier
// exercice dans la liste.
@Composable
internal fun FollowOpenField(
    keypad: SetKeypadState,
    exercises: List<WorkoutExercise>,
    listState: LazyListState,
    firstIndex: Int
) {
    val setId = keypad.openField?.setId ?: return
    val index = exercises.indexOfFirst { item -> item.sets.any { it.id == setId } }
    if (index == -1) {
        SideEffect { keypad.close() }
        return
    }
    val key = exercises[index].id
    LaunchedEffect(setId) {
        if (listState.layoutInfo.visibleItemsInfo.none { it.key == key }) {
            listState.animateScrollToItem(firstIndex + index)
        }
    }
}

// Case de valeur d'une série. Un appui ouvre le pavé ; fermée, elle affiche la valeur enregistrée.
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <T> SetValueField(
    value: T?,
    spec: ValueSpec<T>,
    onValueChange: (T?) -> Unit,
    field: SetField,
    keypad: SetKeypadState,
    description: String,
    width: Dp,
    height: Dp = 44.dp,
    textStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp)
) {
    val isOpen = keypad.openField == field
    val isEditing = keypad.isEditing(field)
    if (isOpen && !isEditing) SideEffect { keypad.bind(field, spec.format(value), spec.editor(onValueChange)) }
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(isOpen, keypad.bottomBarHeight) {
        if (isOpen) {
            // Le pavé vient d'apparaître ou la barre a changé : on attend que la liste ait pris sa nouvelle hauteur.
            repeat(2) { withFrameNanos { } }
            requester.bringIntoView()
        }
    }
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    val text = if (isEditing) keypad.input.text else spec.format(value)
    val isSelected = isEditing && keypad.input.selected && text.isNotEmpty()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(width)
            .height(height)
            .bringIntoViewRequester(requester)
            .background(colors.surfaceContainerHighest, shape)
            // Ouverte : contour rouge épais ; sinon le trait fin blanc des cases (décision du 2 novembre 2026).
            .border(if (isOpen) BorderStroke(2.dp, colors.primary) else hairlineBorder(), shape)
            .clickable(role = Role.Button) {
                // Un champ de texte ouvert ailleurs (nom, note) rend la main : le clavier d'Android se ferme.
                focusManager.clearFocus()
                keypad.open(field)
            }
            .semantics { contentDescription = description }
    ) {
        Text(
            text,
            style = textStyle.copy(textAlign = TextAlign.Center),
            color = colors.onSurface,
            maxLines = 1,
            // Valeur sélectionnée : surlignée, la prochaine touche la remplace.
            modifier = if (isSelected) {
                Modifier.background(colors.primary.copy(alpha = SELECTION_ALPHA), RoundedCornerShape(4.dp))
                    .padding(horizontal = 2.dp)
            } else {
                Modifier
            }
        )
    }
}

private const val SELECTION_ALPHA = 0.35f
private val KeyHeight = 52.dp
private val KeyGap = 6.dp

// Quatre rangées comme Strong : chiffres à gauche ; fermer, − +, RPE et SUIVANT à droite. onRpe null : pas de RPE.
@Composable
fun SetKeypad(state: SetKeypadState, onNext: () -> Unit, onRpe: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(KeyGap),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        KeyRow {
            DigitKeys("123", state)
            IconKey(R.drawable.ic_keyboard_hide, stringResource(R.string.keypad_close), onClick = state::close)
        }
        KeyRow {
            DigitKeys("456", state)
            StepKeys(state)
        }
        KeyRow {
            DigitKeys("789", state)
            SideKey(onClick = { onRpe?.invoke() }, enabled = onRpe != null) {
                ButtonLabel(
                    stringResource(R.string.keypad_rpe),
                    description = stringResource(R.string.workout_rpe_menu)
                )
            }
        }
        KeyRow {
            val corner = state.cornerKey.char
            Key(onClick = { corner?.let(state::type) }, enabled = corner != null) {
                Text(corner?.toString().orEmpty(), style = MaterialTheme.typography.headlineSmall)
            }
            DigitKeys("0", state)
            Key(onClick = state::erase) {
                Icon(painterResource(R.drawable.ic_backspace), stringResource(R.string.keypad_erase))
            }
            SideKey(onClick = onNext, color = MaterialTheme.colorScheme.primaryContainer) {
                ButtonLabel(stringResource(R.string.keypad_next), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KeyGap),
        modifier = Modifier.fillMaxWidth().height(KeyHeight),
        content = content
    )
}

@Composable
private fun RowScope.DigitKeys(digits: String, state: SetKeypadState) {
    digits.forEach { digit ->
        Key(onClick = { state.type(digit) }) {
            Text(digit.toString(), style = MaterialTheme.typography.headlineSmall)
        }
    }
}

// Touche de chiffre : sans fond, comme dans Strong, pour que la colonne de droite ressorte.
@Composable
private fun RowScope.Key(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) { content() }
}

// Colonne de droite, un peu plus large : − et + y tiennent côte à côte avec 48 dp chacun.
@Composable
private fun RowScope.SideKey(
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(SIDE_WEIGHT)
            .fillMaxHeight()
            .background(color, MaterialTheme.shapes.small)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) { content() }
}

@Composable
private fun RowScope.IconKey(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    SideKey(onClick = onClick) { Icon(painterResource(icon), description) }
}

@Composable
private fun RowScope.StepKeys(state: SetKeypadState) {
    Row(
        modifier = Modifier
            .weight(SIDE_WEIGHT)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
    ) {
        Key(onClick = { state.step(-1) }) {
            Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.gym_decrease, state.stepLabel))
        }
        Key(onClick = { state.step(1) }) {
            Icon(painterResource(R.drawable.ic_add), stringResource(R.string.gym_increase, state.stepLabel))
        }
    }
}

private const val SIDE_WEIGHT = 1.2f
