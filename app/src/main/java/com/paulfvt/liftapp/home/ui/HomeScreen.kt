package com.paulfvt.liftapp.home.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftButtonSize
import com.paulfvt.liftapp.core.designsystem.component.OnAirBadge
import com.paulfvt.liftapp.core.designsystem.component.SectionBlock
import com.paulfvt.liftapp.core.designsystem.component.SignalButton
import com.paulfvt.liftapp.core.designsystem.theme.LocalExtendedColors
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.core.ui.dayTitle
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.home.domain.LatestRecord
import com.paulfvt.liftapp.home.domain.weekNumber
import com.paulfvt.liftapp.progress.ui.FlashbackCard
import com.paulfvt.liftapp.progress.ui.formatMetric
import com.paulfvt.liftapp.progress.ui.recordLabelRes
import com.paulfvt.liftapp.template.domain.TemplateSummary
import java.time.LocalDate
import java.util.Locale

// Accueil (tableau de bord) : la semaine, la prochaine séance avec « Démarrer », le dernier record et le souvenir.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenWorkout: () -> Unit, onOpenExercise: (Long) -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isGoalDialogOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel.isWorkoutReady) {
        if (viewModel.isWorkoutReady) {
            onOpenWorkout()
            viewModel.onWorkoutOpened()
        }
    }
    val emptyName = stringResource(R.string.workout_empty_name)
    Scaffold { padding ->
        // Halo rouge signal en haut de l'écran (--wash du guide de style), sous le contenu qui défile.
        SignalWash(Modifier.fillMaxSize())
        val dashboard = uiState.dashboard ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                HomeHeader(
                    today = dashboard.today,
                    isLive = uiState.liveWorkoutName != null,
                    onOpenWorkout = { viewModel.start(null, emptyName) }
                )
            }
            item(key = "week") {
                WeekHero(
                    streak = dashboard.streak,
                    today = dashboard.today,
                    trainedDays = dashboard.trainedDays,
                    onEditGoal = { isGoalDialogOpen = true }
                )
            }
            item(key = "next") {
                SectionBlock(stringResource(R.string.home_next_session)) {
                    NextSessionCard(
                        liveWorkoutName = uiState.liveWorkoutName,
                        template = dashboard.nextTemplate,
                        onStart = { templateId -> viewModel.start(templateId, emptyName) }
                    )
                }
            }
            dashboard.latestRecord?.let { latest ->
                item(key = "record") {
                    SectionBlock(stringResource(R.string.home_latest_record)) {
                        LatestRecordCard(latest, onClick = { onOpenExercise(latest.exercise.id) })
                    }
                }
            }
            dashboard.flashback?.let { flashback ->
                item(key = "flashback") {
                    SectionBlock(stringResource(R.string.flashback_title)) {
                        FlashbackCard(flashback, onClick = { onOpenExercise(flashback.exercise.id) })
                    }
                }
            }
        }
    }
    val streak = uiState.dashboard?.streak
    if (isGoalDialogOpen && streak != null) {
        WeeklyGoalDialog(
            currentGoal = streak.currentWeek.goal,
            onConfirm = { goal ->
                viewModel.onWeeklyGoalChange(goal)
                isGoalDialogOpen = false
            },
            onDismiss = { isGoalDialogOpen = false }
        )
    }
}

// « DIMANCHE 1 NOVEMBRE / SEMAINE 44 » et l'enseigne : « EN DIRECT » pendant une séance (un appui l'ouvre),
// « HORS ANTENNE » sinon.
@Composable
private fun HomeHeader(today: LocalDate, isLive: Boolean, onOpenWorkout: () -> Unit) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                dayTitle(today).uppercase(Locale.FRENCH),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.08.em),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val title = stringResource(R.string.home_week_number, weekNumber(today))
            Text(
                title.uppercase(Locale.FRENCH),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 2.dp).clearAndSetSemantics {
                    text = AnnotatedString(title)
                    heading()
                }
            )
        }
        if (isLive) {
            // Zone tactile de 48 dp autour de l'enseigne de 28 dp.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(role = Role.Button, onClick = onOpenWorkout)
            ) {
                OnAirBadge(
                    text = stringResource(R.string.workout_live),
                    description = stringResource(R.string.home_live_description)
                )
            }
        } else {
            OnAirBadge(
                text = stringResource(R.string.home_off_air),
                isLive = false,
                description = stringResource(R.string.home_off_air_description),
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

// Halo du guide de style (--wash) : ellipse rouge signal centrée au-dessus de l'écran, 120 % de la largeur sur 70 %
// de la hauteur, transparente à 60 % de son rayon.
@Composable
private fun SignalWash(modifier: Modifier = Modifier) {
    val wash = LocalExtendedColors.current.wash
    Box(
        modifier.drawBehind {
            val center = Offset(size.width / 2, -WASH_TOP_OFFSET * size.height)
            val radiusX = WASH_WIDTH * size.width
            val radiusY = WASH_HEIGHT * size.height
            scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = center) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to wash,
                        WASH_FADE to Color.Transparent,
                        center = center,
                        radius = radiusX
                    ),
                    radius = radiusX,
                    center = center
                )
            }
        }
    )
}

private const val WASH_WIDTH = 1.2f
private const val WASH_HEIGHT = 0.7f
private const val WASH_TOP_OFFSET = 0.1f
private const val WASH_FADE = 0.6f

// Séance en cours à reprendre, sinon la prochaine séance proposée, sinon une séance vide (aucun template).
// onStart(null) lance une séance vide.
@Composable
private fun NextSessionCard(liveWorkoutName: String?, template: TemplateSummary?, onStart: (Long?) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when {
                liveWorkoutName != null -> {
                    Text(stringResource(R.string.home_live_workout), style = MaterialTheme.typography.labelLarge)
                    Text(liveWorkoutName, style = MaterialTheme.typography.titleLarge)
                    PrimaryButton(stringResource(R.string.workout_resume), onClick = { onStart(null) })
                }
                template != null -> {
                    Text(template.name, style = MaterialTheme.typography.titleLarge)
                    if (template.exerciseNames.isNotEmpty()) {
                        Text(
                            template.exerciseNames.joinToString(", "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PrimaryButton(stringResource(R.string.template_preview_start), onClick = { onStart(template.id) })
                }
                else -> {
                    Text(
                        stringResource(R.string.home_no_template),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrimaryButton(stringResource(R.string.workout_start_empty), onClick = { onStart(null) })
                }
            }
        }
    }
}

// Action principale de l'écran, en rouge signal (guide de style ON AIR).
@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    SignalButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        icon = R.drawable.ic_play_arrow,
        size = LiftButtonSize.Large
    )
}

// « Squat à la barre · 1RM estimé : 135 kg, le 28 oct. 2026 ». Un appui ouvre la progression de l'exercice.
@Composable
private fun LatestRecordCard(latest: LatestRecord, onClick: () -> Unit) {
    val resources = LocalResources.current
    val (exercise, recordBreak) = latest
    val metric = recordBreak.metric
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp).semantics(mergeDescendants = true) {}
        ) {
            Icon(
                painterResource(R.drawable.ic_emoji_events),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(metric.recordLabelRes(exercise.trackingType)) + " : " +
                        formatMetric(resources, metric, recordBreak.value, exercise, LocalUnits.current.weight),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    stringResource(R.string.progress_record_date, shortDay(recordBreak.achievedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
