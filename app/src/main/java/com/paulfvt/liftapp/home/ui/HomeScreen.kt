package com.paulfvt.liftapp.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import com.paulfvt.liftapp.core.designsystem.component.LiftCard
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.OnAirBadge
import com.paulfvt.liftapp.core.designsystem.component.SectionBlock
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.designsystem.component.SignalButton
import com.paulfvt.liftapp.core.designsystem.component.StatTile
import com.paulfvt.liftapp.core.designsystem.theme.LocalExtendedColors
import com.paulfvt.liftapp.core.domain.fromKg
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.core.ui.dayTitle
import com.paulfvt.liftapp.core.ui.durationText
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.shortDay
import com.paulfvt.liftapp.core.ui.signedPercent
import com.paulfvt.liftapp.core.ui.wholeNumber
import com.paulfvt.liftapp.home.domain.LatestRecord
import com.paulfvt.liftapp.home.domain.WeekStats
import com.paulfvt.liftapp.home.domain.weekNumber
import com.paulfvt.liftapp.progress.ui.FlashbackCard
import com.paulfvt.liftapp.progress.ui.formatMetric
import com.paulfvt.liftapp.progress.ui.recordLabelRes
import com.paulfvt.liftapp.template.domain.TemplateSummary
import java.time.Duration
import java.time.LocalDate
import java.util.Locale

// Accueil (tableau de bord) : la semaine, la prochaine séance avec « Démarrer », le dernier record et le souvenir.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenWorkout: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
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
                NextSessionCard(
                    liveWorkoutName = uiState.liveWorkoutName,
                    template = dashboard.nextTemplate,
                    folderName = dashboard.nextFolderName,
                    lastDuration = dashboard.nextLastDuration,
                    onStart = { templateId -> viewModel.start(templateId, emptyName) }
                )
            }
            item(key = "weekStats") {
                WeekStatsSection(dashboard.weekStats, onSeeAll = onOpenHistory)
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

// Carte de la prochaine séance (maquette ON AIR) : libellé rouge et dossier, nom en grand, « 5 exercices · 18 séries
// · ~ 1 h 05 », puces des premiers exercices, puis « DÉMARRER ». Une séance en cours se reprend à la place ; sans
// template, une séance vide est proposée. onStart(null) lance ou reprend une séance vide.
@Composable
private fun NextSessionCard(
    liveWorkoutName: String?,
    template: TemplateSummary?,
    folderName: String?,
    lastDuration: Duration?,
    onStart: (Long?) -> Unit
) {
    LiftCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            when {
                liveWorkoutName != null -> {
                    CardLabel(stringResource(R.string.home_live_workout))
                    CardTitle(liveWorkoutName)
                    PrimaryButton(stringResource(R.string.workout_resume), onClick = { onStart(null) })
                }
                template != null -> {
                    CardLabel(stringResource(R.string.home_next_session), trailing = folderName)
                    CardTitle(template.name)
                    Text(
                        nextSessionSummary(template, lastDuration),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    ExerciseChips(template.exerciseNames)
                    PrimaryButton(stringResource(R.string.template_preview_start), onClick = { onStart(template.id) })
                }
                else -> {
                    CardLabel(stringResource(R.string.home_next_session))
                    Text(
                        stringResource(R.string.home_no_template),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    PrimaryButton(stringResource(R.string.workout_start_empty), onClick = { onStart(null) })
                }
            }
        }
    }
}

// Libellé en petites capitales rouges, avec à droite une précision (le dossier du template).
@Composable
private fun CardLabel(text: String, trailing: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(Locale.FRENCH),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.08.em),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        trailing?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// Nom en capitales condensées, lu dans sa casse d'origine.
@Composable
private fun CardTitle(text: String) {
    Text(
        text.uppercase(Locale.FRENCH),
        style = MaterialTheme.typography.headlineMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 6.dp).semantics { contentDescription = text }
    )
}

// « 5 exercices · 18 séries · ~ 1 h 05 », la durée seulement si le template a déjà été fait.
@Composable
private fun nextSessionSummary(template: TemplateSummary, lastDuration: Duration?): String = buildList {
    val exercises = template.exerciseNames.size
    add(pluralStringResource(R.plurals.home_next_exercises, exercises, exercises))
    if (template.setCount > 0) add(pluralStringResource(R.plurals.history_sets, template.setCount, template.setCount))
    lastDuration?.let { add(stringResource(R.string.home_next_duration, durationText(it))) }
}.joinToString(" · ")

// Les deux premiers exercices en puces, puis « +3 » pour les autres.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseChips(names: List<String>) {
    if (names.isEmpty()) return
    val shown = names.take(SHOWN_EXERCISES)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 12.dp)
    ) {
        shown.forEach { InfoChip(it) }
        if (names.size > shown.size) InfoChip(stringResource(R.string.home_next_more, names.size - shown.size))
    }
}

private const val SHOWN_EXERCISES = 2

// Puce d'information, non cliquable (.chip-suggest du guide de style).
@Composable
private fun InfoChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

// « CETTE SEMAINE · TOUT VOIR » puis deux tuiles : tonnage avec l'écart sur la semaine précédente au même moment,
// durée cumulée avec le nombre de séances. « Tout voir » ouvre l'Historique.
@Composable
private fun WeekStatsSection(stats: WeekStats, onSeeAll: () -> Unit) {
    val units = LocalUnits.current
    val change = stats.tonnageChange
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.week_title), modifier = Modifier.weight(1f))
            LiftTextButton(text = stringResource(R.string.home_see_all), onClick = onSeeAll)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            StatTile(
                label = stringResource(R.string.home_week_tonnage),
                value = wholeNumber(units.weight.fromKg(stats.current.tonnageKg)),
                unit = stringResource(units.weight.labelRes),
                detail = change?.let {
                    stringResource(R.string.home_week_change, signedPercent(it), stats.previousWeekNumber)
                },
                detailColor = if ((change ?: 0.0) > 0) LocalExtendedColors.current.success else Color.Unspecified,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            StatTile(
                label = stringResource(R.string.home_week_duration),
                value = durationText(stats.current.duration),
                detail = pluralStringResource(
                    R.plurals.progress_sessions,
                    stats.current.sessions,
                    stats.current.sessions
                ),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    }
}

// Action principale de l'écran, en rouge signal (guide de style ON AIR).
@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    SignalButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        icon = R.drawable.ic_play_arrow,
        size = LiftButtonSize.Large
    )
}

// Dernier record battu, entouré de la lueur ambre (un moment fort, .card.is-signal) : l'exercice, « 1RM estimé ·
// 28 oct. 2026 » et la valeur en grand. Un appui ouvre la progression de l'exercice.
@Composable
private fun LatestRecordCard(latest: LatestRecord, onClick: () -> Unit) {
    val resources = LocalResources.current
    val (exercise, recordBreak) = latest
    val metric = recordBreak.metric
    val amber = MaterialTheme.colorScheme.tertiary
    val shape = MaterialTheme.shapes.medium
    val value = formatMetric(resources, metric, recordBreak.value, exercise, LocalUnits.current.weight)
    // « 120,5 kg » : le nombre en grand, l'unité à côté en petit.
    val number = value.substringBeforeLast(' ')
    val unit = if (' ' in value) value.substringAfterLast(' ') else ""
    LiftCard(
        onClick = onClick,
        shape = shape,
        border = BorderStroke(1.dp, amber),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = shape, ambientColor = amber, spotColor = amber)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(16.dp).semantics(mergeDescendants = true) {}
        ) {
            Icon(
                painterResource(R.drawable.ic_emoji_events),
                contentDescription = null,
                tint = amber,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Text(
                    stringResource(metric.recordLabelRes(exercise.trackingType)) + " · " +
                        shortDay(recordBreak.achievedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(number, style = MaterialTheme.typography.displaySmall)
                if (unit.isNotEmpty()) {
                    Text(
                        " $unit",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}
