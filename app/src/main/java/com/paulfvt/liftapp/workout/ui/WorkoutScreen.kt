package com.paulfvt.liftapp.workout.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.DragHandle
import com.paulfvt.liftapp.core.designsystem.component.LiftButtonSize
import com.paulfvt.liftapp.core.designsystem.component.LiftOutlinedButton
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.MenuEntry
import com.paulfvt.liftapp.core.designsystem.component.NoteDialog
import com.paulfvt.liftapp.core.designsystem.component.OnAirBadge
import com.paulfvt.liftapp.core.designsystem.component.OverflowMenu
import com.paulfvt.liftapp.core.domain.moved
import com.paulfvt.liftapp.core.domain.workSetNumbers
import com.paulfvt.liftapp.core.model.Exercise
import com.paulfvt.liftapp.core.ui.durationLabel
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.recentDay
import com.paulfvt.liftapp.core.ui.rememberElapsedSeconds
import com.paulfvt.liftapp.core.ui.volumeText
import com.paulfvt.liftapp.exercise.ui.ExerciseThumbnail
import com.paulfvt.liftapp.template.domain.DEFAULT_REST_SECONDS
import com.paulfvt.liftapp.template.domain.REST_CHOICES_SECONDS
import com.paulfvt.liftapp.template.ui.ChoiceDialog
import com.paulfvt.liftapp.template.ui.NameDialog
import com.paulfvt.liftapp.workout.domain.NextSet
import com.paulfvt.liftapp.workout.domain.PreviousSet
import com.paulfvt.liftapp.workout.domain.RPE_CHOICES
import com.paulfvt.liftapp.workout.domain.Workout
import com.paulfvt.liftapp.workout.domain.WorkoutExercise
import com.paulfvt.liftapp.workout.domain.bestWorkSet
import com.paulfvt.liftapp.workout.domain.completedSetCount
import com.paulfvt.liftapp.workout.domain.formatDecimal
import com.paulfvt.liftapp.workout.domain.formatDuration
import com.paulfvt.liftapp.workout.domain.matchPrevious
import com.paulfvt.liftapp.workout.domain.nextSet
import com.paulfvt.liftapp.workout.domain.summarize
import java.time.Instant
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun WorkoutScreen(
    onClose: () -> Unit,
    onPickExercises: (excludedIds: List<Long>, replace: Boolean) -> Unit,
    viewModel: WorkoutViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel.isClosed) {
        if (viewModel.isClosed) onClose()
    }
    val finished = viewModel.finished
    val workout = uiState.workout
    when {
        finished != null -> WorkoutSummaryContent(
            finished = finished,
            isTemplateUpdated = viewModel.isTemplateUpdated,
            onUpdateTemplate = viewModel::onUpdateTemplate,
            onKeepTemplate = viewModel::onKeepTemplate,
            onClose = viewModel::onCloseSummary
        )
        uiState.isLoading || viewModel.isFinishing -> Unit
        // Plus de séance en cours (terminée ou abandonnée ailleurs) : rien à afficher.
        workout == null -> LaunchedEffect(Unit) { onClose() }
        else -> ActiveWorkout(
            workout,
            uiState.previous,
            uiState.previousDates,
            uiState.records,
            viewModel,
            onClose,
            onPickExercises
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveWorkout(
    workout: Workout,
    previous: Map<Long, List<PreviousSet>>,
    previousDates: Map<Long, Instant>,
    records: List<SessionRecord>,
    viewModel: WorkoutViewModel,
    onMinimize: () -> Unit,
    onPickExercises: (excludedIds: List<Long>, replace: Boolean) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageRes = viewModel.message
    val messageText = messageRes?.let { stringResource(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            viewModel.messageShown()
        }
    }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Au retour sur l'écran (réglages Android modifiés, app rouverte), on revérifie les permissions.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    WorkoutPermissionRequests(
        asked = settings.permissionsAsked,
        missingPermissions = viewModel::currentMissingPermissions,
        onFinished = viewModel::onPermissionsAsked
    )
    val elapsed = formatDuration(rememberElapsedSeconds(workout.startedAt))
    val next = workout.nextSet()
    val restTimer: @Composable (large: Boolean, modifier: Modifier) -> Unit = { large, modifier ->
        workout.rest?.let { rest ->
            RestTimer(
                rest = rest,
                onRemove = viewModel::onRemoveRest,
                onAdd = viewModel::onAddRest,
                onSkip = viewModel::onSkipRest,
                modifier = modifier,
                large = large,
                footer = {
                    viewModel.missingPermissions.firstOrNull()?.let { missing ->
                        LateAlertWarning(onEnable = { context.openAlertSettings(missing) })
                    }
                }
            )
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onMinimize) {
                        Icon(painterResource(R.drawable.ic_expand_more), stringResource(R.string.workout_minimize))
                    }
                },
                title = { OnAirBadge(stringResource(R.string.workout_live_with_time, elapsed)) },
                actions = {
                    OverflowMenu(
                        listOf(
                            MenuEntry(R.string.action_rename, R.drawable.ic_edit) {
                                viewModel.openDialog(WorkoutDialog.Rename)
                            },
                            MenuEntry(R.string.workout_note) { viewModel.openDialog(WorkoutDialog.Note) },
                            MenuEntry(
                                R.string.workout_gym_mode,
                                R.drawable.ic_fitness_center,
                                isChecked = settings.gymMode
                            ) { viewModel.onGymModeChange(!settings.gymMode) },
                            MenuEntry(R.string.workout_rest_sound, isChecked = settings.restSound) {
                                viewModel.onRestSoundChange(!settings.restSound)
                            },
                            MenuEntry(R.string.workout_rest_vibration, isChecked = settings.restVibration) {
                                viewModel.onRestVibrationChange(!settings.restVibration)
                            },
                            MenuEntry(R.string.workout_discard, R.drawable.ic_delete, isDestructive = true) {
                                viewModel.openDialog(WorkoutDialog.Discard)
                            }
                        )
                    )
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.navigationBarsPadding()) {
                    if (settings.gymMode) {
                        GymBottomBar(
                            hasNextSet = next != null,
                            onComplete = { next?.let { viewModel.onToggleCompleted(it.set.id) } },
                            onFinish = viewModel::onFinishClick
                        )
                    } else {
                        restTimer(false, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp))
                        FinishButton(viewModel::onFinishClick)
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // consumeWindowInsets : la barre du bas (minuteur, bouton de fin) est déjà décomptée ; le imePadding de la
        // liste n'ajoute que le reste du clavier, sans laisser de bloc vide au-dessus.
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            ActiveWorkoutContent(
                workout,
                previous,
                previousDates,
                settings.gymMode,
                next,
                viewModel,
                onPickExercises,
                restTimer
            )
            RecordCelebration(
                records = records,
                modifier = Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    WorkoutDialogs(workout, viewModel)
}

@Composable
private fun ActiveWorkoutContent(
    workout: Workout,
    previous: Map<Long, List<PreviousSet>>,
    previousDates: Map<Long, Instant>,
    isGymMode: Boolean,
    next: NextSet?,
    viewModel: WorkoutViewModel,
    onPickExercises: (excludedIds: List<Long>, replace: Boolean) -> Unit,
    restTimer: @Composable (large: Boolean, modifier: Modifier) -> Unit
) {
    if (isGymMode) {
        val gymActions = remember(viewModel) {
            GymActions(
                onStep = viewModel::onStepValues,
                onWeightChange = viewModel::onWeightChange,
                onRepsChange = viewModel::onRepsChange,
                onDurationChange = viewModel::onDurationChange,
                onDistanceChange = viewModel::onDistanceChange
            )
        }
        GymModeContent(
            next = next,
            previous = previous,
            actions = gymActions,
            onShowAll = { viewModel.onGymModeChange(false) },
            restTimer = { restTimer(true, Modifier) }
        )
    } else {
        WorkoutContent(workout, previous, previousDates, viewModel, onPickExercises)
    }
}

@Composable
// À contour (maquette ON AIR) : pendant la séance, l'action principale reste la validation des séries.
private fun FinishButton(onClick: () -> Unit) {
    LiftOutlinedButton(
        text = stringResource(R.string.workout_finish),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        size = LiftButtonSize.Large
    )
}

// Sous le minuteur, tant que l'alerte de fin de repos n'est pas garantie écran verrouillé.
@Composable
private fun LateAlertWarning(onEnable: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.rest_alert_late),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        LiftTextButton(
            text = stringResource(R.string.rest_alert_enable),
            onClick = onEnable,
            modifier = Modifier.heightIn(min = 48.dp)
        )
    }
}

@Composable
private fun WorkoutContent(
    workout: Workout,
    previous: Map<Long, List<PreviousSet>>,
    previousDates: Map<Long, Instant>,
    viewModel: WorkoutViewModel,
    onPickExercises: (excludedIds: List<Long>, replace: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    // Copie locale de l'ordre des exercices pendant un glisser-déposer ; la base est écrite au lâcher.
    var order by remember(workout.exercises.map { it.id }) { mutableStateOf(workout.exercises.map { it.id }) }
    val byId = workout.exercises.associateBy { it.id }
    val currentSetId = workout.nextSet()?.set?.id
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromIndex = order.indexOf(from.key as Long)
        val toIndex = order.indexOf(to.key as Long)
        if (fromIndex != -1 && toIndex != -1) order = order.moved(fromIndex, toIndex)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
    val setActions = remember(viewModel) {
        SetActions(
            onTypeChange = viewModel::onSetTypeChange,
            onWeightChange = viewModel::onWeightChange,
            onRepsChange = viewModel::onRepsChange,
            onDurationChange = viewModel::onDurationChange,
            onDistanceChange = viewModel::onDistanceChange,
            onToggleCompleted = viewModel::onToggleCompleted,
            onRpeClick = { set -> viewModel.openDialog(WorkoutDialog.Rpe(set.id, set.rpe)) },
            onRemove = viewModel::onRemoveSet
        )
    }
    LazyColumn(
        state = listState,
        // imePadding : le clavier ne cache pas la série en cours de saisie.
        modifier = modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            Column {
                Text(workout.name.uppercase(), style = MaterialTheme.typography.headlineLarge)
                WorkoutProgress(workout)
                workout.note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { viewModel.openDialog(WorkoutDialog.Note) }
                    )
                }
                if (workout.exercises.isEmpty()) {
                    Text(
                        stringResource(R.string.workout_no_exercise),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        itemsIndexed(order.mapNotNull { byId[it] }, key = { _, item -> item.id }) { index, item ->
            ReorderableItem(reorderState, key = item.id) { isDragging ->
                WorkoutExerciseCard(
                    item = item,
                    previous = previous[item.exercise.id].orEmpty(),
                    lastTime = previousDates[item.exercise.id],
                    currentSetId = currentSetId,
                    isDragging = isDragging,
                    canMoveUp = index > 0,
                    canMoveDown = index < order.lastIndex,
                    dragHandle = {
                        DragHandle(
                            Modifier.draggableHandle(
                                onDragStarted = {
                                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                },
                                onDragStopped = {
                                    viewModel.onReorderExercises(order)
                                    haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                }
                            )
                        )
                    },
                    onMoveBy = { offset ->
                        val from = order.indexOf(item.id)
                        if (from + offset in
                            order.indices
                        ) {
                            viewModel.onReorderExercises(order.moved(from, from + offset))
                        }
                    },
                    onReplace = {
                        viewModel.onReplaceExercise(item.id)
                        onPickExercises(order.mapNotNull { byId[it]?.exercise?.id }, true)
                    },
                    onRemove = { viewModel.onRemoveExercise(item.id) },
                    onEditNote = { viewModel.openDialog(WorkoutDialog.ExerciseNote(item.exercise)) },
                    onRestClick = {
                        viewModel.openDialog(WorkoutDialog.Rest(item.id, item.restSeconds ?: DEFAULT_REST_SECONDS))
                    },
                    onAddSet = { viewModel.onAddSet(item.id) },
                    setActions = setActions
                )
            }
        }
        item(key = "add") {
            LiftOutlinedButton(
                text = stringResource(R.string.workout_add_exercises),
                onClick = { onPickExercises(workout.exercises.map { it.exercise.id }, false) },
                modifier = Modifier.fillMaxWidth(),
                icon = R.drawable.ic_add
            )
        }
    }
}

@Composable
private fun WorkoutExerciseCard(
    item: WorkoutExercise,
    previous: List<PreviousSet>,
    lastTime: Instant?,
    // Prochaine série à valider de la séance, entourée de rouge.
    currentSetId: Long?,
    isDragging: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    dragHandle: @Composable () -> Unit,
    onMoveBy: (offset: Int) -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
    onEditNote: () -> Unit,
    onRestClick: () -> Unit,
    onAddSet: () -> Unit,
    setActions: SetActions
) {
    val elevation by animateDpAsState(if (isDragging) 8.dp else 1.dp, label = "elevation")
    val moveUpLabel = stringResource(R.string.action_move_up)
    val moveDownLabel = stringResource(R.string.action_move_down)
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        // Le glisser-déposer est inaccessible au lecteur d'écran : il propose « Monter » et « Descendre » à la place.
        modifier = Modifier.fillMaxWidth().semantics {
            customActions = listOfNotNull(
                CustomAccessibilityAction(moveUpLabel) {
                    onMoveBy(-1)
                    true
                }.takeIf { canMoveUp },
                CustomAccessibilityAction(moveDownLabel) {
                    onMoveBy(1)
                    true
                }.takeIf { canMoveDown }
            )
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            ExerciseThumbnail(item.exercise)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    item.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    exerciseSubtitle(item.exercise),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Note permanente de l'exercice (réglage de machine, placement…), rappelée à chaque séance.
                item.exercise.note?.let { note ->
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(onClick = onEditNote)
                    )
                }
            }
            OverflowMenu(
                listOf(
                    MenuEntry(R.string.workout_replace_exercise, R.drawable.ic_edit, onClick = onReplace),
                    MenuEntry(R.string.workout_exercise_note, onClick = onEditNote),
                    MenuEntry(
                        R.string.workout_remove_exercise,
                        R.drawable.ic_delete,
                        isDestructive = true,
                        onClick = onRemove
                    )
                )
            )
            dragHandle()
        }
        ExerciseChips(item, previous, lastTime, onRestClick)
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
        ) {
            SetHeaderRow(item.exercise)
            val types = item.sets.map { it.type }
            val workNumbers = workSetNumbers(types)
            val matched = matchPrevious(types, previous)
            item.sets.forEachIndexed { index, set ->
                // key : chaque ligne garde son propre texte saisi si une série au-dessus est retirée.
                key(set.id) {
                    WorkoutSetRow(
                        set = set,
                        workNumber = workNumbers[index],
                        previous = matched[index],
                        exercise = item.exercise,
                        canRemove = item.sets.size > 1,
                        actions = setActions,
                        isCurrent = set.id == currentSetId
                    )
                }
            }
        }
        LiftTextButton(
            text = stringResource(R.string.template_set_add),
            onClick = onAddSet,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            icon = R.drawable.ic_add
        )
    }
}

// « 7 / 16 séries » et le tonnage validé, au-dessus d'une barre de progression (.lin du guide de style).
@Composable
private fun WorkoutProgress(workout: Workout) {
    val total = workout.exercises.sumOf { it.sets.size }
    if (total == 0) return
    val done = workout.completedSetCount()
    val fraction = done.toFloat() / total
    Column(
        modifier = Modifier.padding(top = 8.dp).semantics(mergeDescendants = true) {
            progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
        }
    ) {
        Row {
            Text(
                pluralStringResource(R.plurals.workout_progress_sets, total, done, total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                volumeText(summarize(workout, workout.startedAt).tonnageKg),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

// « Pectoraux · Barre » : muscles principaux et matériel.
@Composable
private fun exerciseSubtitle(exercise: Exercise): String {
    val muscles = exercise.primaryMuscles.map { stringResource(it.labelRes) }.joinToString(", ")
    return listOf(muscles, stringResource(exercise.equipment.labelRes)).filter { it.isNotEmpty() }.joinToString(" · ")
}

// Puces sous l'exercice : son repos (un appui le change) et sa meilleure série de travail la dernière fois.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseChips(
    item: WorkoutExercise,
    previous: List<PreviousSet>,
    lastTime: Instant?,
    onRestClick: () -> Unit
) {
    val best = bestWorkSet(previous, item.exercise.trackingType)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 12.dp)
    ) {
        AssistChip(
            onClick = onRestClick,
            label = {
                Text(stringResource(R.string.template_rest, durationLabel(item.restSeconds ?: DEFAULT_REST_SECONDS)))
            },
            leadingIcon = {
                Icon(
                    painterResource(R.drawable.ic_timer),
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            }
        )
        if (best != null && lastTime != null) {
            InfoChip(
                icon = R.drawable.ic_history,
                text = stringResource(
                    R.string.workout_last_time,
                    recentDay(lastTime),
                    previousLabel(best, item.exercise)
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

// Puce d'information, non cliquable, du même dessin qu'une puce d'assistance.
@Composable
private fun InfoChip(icon: Int, text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .heightIn(min = 32.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(AssistChipDefaults.IconSize)
        )
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WorkoutDialogs(workout: Workout, viewModel: WorkoutViewModel) {
    when (val dialog = viewModel.dialog) {
        null -> Unit
        WorkoutDialog.Rename -> NameDialog(
            title = stringResource(R.string.workout_rename),
            initialName = workout.name,
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = viewModel::onRename,
            onDismiss = viewModel::dismissDialog
        )
        WorkoutDialog.Note -> NoteDialog(
            title = stringResource(R.string.workout_note),
            hint = stringResource(R.string.workout_note_hint),
            initialNote = workout.note.orEmpty(),
            onDismiss = viewModel::dismissDialog,
            onSave = viewModel::onNoteSave
        )
        is WorkoutDialog.ExerciseNote -> NoteDialog(
            title = stringResource(R.string.exercise_note),
            hint = stringResource(R.string.exercise_note_hint),
            initialNote = dialog.exercise.note.orEmpty(),
            onDismiss = viewModel::dismissDialog,
            onSave = { viewModel.onExerciseNoteSave(dialog.exercise.id, it) }
        )
        is WorkoutDialog.Rest -> ChoiceDialog(
            title = stringResource(R.string.template_rest_title),
            options = REST_CHOICES_SECONDS,
            selected = dialog.current,
            label = { durationLabel(it) },
            onSelect = { viewModel.onRestSelected(dialog.workoutExerciseId, it) },
            onDismiss = viewModel::dismissDialog
        )
        is WorkoutDialog.Rpe -> {
            val none = stringResource(R.string.workout_rpe_none)
            ChoiceDialog(
                title = stringResource(R.string.workout_rpe_title),
                options = listOf<Double?>(null) + RPE_CHOICES,
                selected = dialog.current,
                label = { rpe -> rpe?.let(::formatDecimal) ?: none },
                onSelect = { viewModel.onRpeSelected(dialog.setId, it) },
                onDismiss = viewModel::dismissDialog
            )
        }
        WorkoutDialog.Discard -> ConfirmDialog(
            title = stringResource(R.string.workout_discard_title),
            text = stringResource(R.string.workout_discard_text),
            confirmLabel = stringResource(R.string.workout_discard_confirm),
            isDestructive = true,
            onConfirm = viewModel::discard,
            onDismiss = viewModel::dismissDialog
        )
        WorkoutDialog.FinishEmpty -> ConfirmDialog(
            title = stringResource(R.string.workout_finish_empty_title),
            text = stringResource(R.string.workout_finish_empty_text),
            confirmLabel = stringResource(R.string.workout_discard_confirm),
            isDestructive = true,
            onConfirm = viewModel::discard,
            onDismiss = viewModel::dismissDialog
        )
        is WorkoutDialog.FinishIncomplete -> ConfirmDialog(
            title = stringResource(R.string.workout_finish_title),
            text = pluralStringResource(
                R.plurals.workout_finish_incomplete,
                dialog.incompleteSets,
                dialog.incompleteSets
            ),
            confirmLabel = stringResource(R.string.workout_finish_confirm),
            isDestructive = false,
            onConfirm = viewModel::finish,
            onDismiss = viewModel::dismissDialog
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    isDestructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            LiftTextButton(
                text = confirmLabel,
                onClick = onConfirm,
                color = if (isDestructive) MaterialTheme.colorScheme.error else Color.Unspecified
            )
        },
        dismissButton = { LiftTextButton(stringResource(R.string.workout_keep), onClick = onDismiss) }
    )
}
