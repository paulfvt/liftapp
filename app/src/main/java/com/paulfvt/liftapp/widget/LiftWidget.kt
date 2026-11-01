package com.paulfvt.liftapp.widget

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ButtonDefaults
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.paulfvt.liftapp.MainActivity
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.theme.DarkColors
import com.paulfvt.liftapp.core.designsystem.theme.DarkExtendedColors
import com.paulfvt.liftapp.core.designsystem.theme.LightColors
import com.paulfvt.liftapp.core.designsystem.theme.LightExtendedColors
import com.paulfvt.liftapp.core.domain.Units
import com.paulfvt.liftapp.core.domain.fromKg
import com.paulfvt.liftapp.core.ui.durationText
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.core.ui.signedPercent
import com.paulfvt.liftapp.core.ui.volumeText
import com.paulfvt.liftapp.core.ui.wholeNumber
import com.paulfvt.liftapp.home.data.Dashboard
import com.paulfvt.liftapp.progress.ui.formatMetric
import com.paulfvt.liftapp.workout.domain.RestPeriod
import com.paulfvt.liftapp.workout.notification.WorkoutActionReceiver
import com.paulfvt.liftapp.workout.notification.describeNextSet
import com.paulfvt.liftapp.workout.notification.describeSetPosition
import com.paulfvt.liftapp.workout.notification.describeSetValues
import dagger.hilt.android.EntryPointAccessors
import java.time.Duration
import java.time.Instant
import java.time.format.TextStyle as DayStyle
import java.util.Locale
import kotlinx.coroutines.flow.first

// Couleurs du thème ON AIR, en clair et en sombre selon le téléphone. Le vert de réussite n'existe pas dans
// Material 3 : il vient des couleurs étendues du thème.
private val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)
private val SuccessColor = DayNightColorProvider(day = LightExtendedColors.success, night = DarkExtendedColors.success)

private const val DAYS_IN_WEEK = 7L
private val STAT_TILE_HEIGHT = 60.dp

// Widget d'écran d'accueil, un seul qui change d'état tout seul : hors séance (anneau, streak, prochaine séance,
// dernier record) ou en séance (exercice, série, minuteur, « Série validée »). Trois tailles : petite, moyenne et
// grande ; la grande ajoute les jours et les tuiles de la semaine hors séance, la série à faire en grand et la
// progression en séance (décision du 1er novembre 2026).
class LiftWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    // Les données suivent la base tant que le widget est affiché ; sinon l'app demande une mise à jour (WidgetUpdater).
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).widgetData()
        val initial = source.observe().first()
        provideContent {
            val states = remember { source.observe() }
            val state by states.collectAsState(initial)
            GlanceTheme(colors = WidgetColors) {
                when (val current = state) {
                    is WidgetState.Idle -> IdleContent(current.dashboard, current.units)
                    is WidgetState.Live -> LiveContent(current.details, current.units)
                }
            }
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
        val LARGE = DpSize(250.dp, 200.dp)
    }
}

private fun isMedium(size: DpSize) = size.width >= LiftWidget.MEDIUM.width

private fun isLarge(size: DpSize) = size.width >= LiftWidget.LARGE.width && size.height >= LiftWidget.LARGE.height

// SINGLE_TOP : l'app déjà ouverte reçoit l'intention (onNewIntent) au lieu d'être ouverte une seconde fois.
private fun appIntent(context: Context, action: String? = null) = Intent(context, MainActivity::class.java)
    .setAction(action ?: Intent.ACTION_MAIN)
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

// « Démarrer » : l'app s'ouvre et lance la séance (décision du 28 octobre 2026).
private fun startAction(context: Context, templateId: Long?): Action = actionStartActivity(
    appIntent(context, MainActivity.ACTION_START_WORKOUT)
        .putExtra(MainActivity.EXTRA_TEMPLATE_ID, templateId ?: MainActivity.NO_TEMPLATE)
)

@Composable
private fun WidgetFrame(onClick: Action, content: @Composable () -> Unit) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(20.dp)
            .padding(12.dp)
            .clickable(onClick)
    ) { content() }
}

@Composable
private fun IdleContent(dashboard: Dashboard, units: Units) {
    val context = LocalContext.current
    val size = LocalSize.current
    val medium = isMedium(size)
    val template = dashboard.nextTemplate
    WidgetFrame(onClick = actionStartActivity(appIntent(context))) {
        if (isLarge(size)) {
            LargeIdle(dashboard, units)
        } else if (medium) {
            Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                WeekRing(dashboard, 64)
                Spacer(GlanceModifier.width(12.dp))
                Column(modifier = GlanceModifier.defaultWeight()) {
                    StreakLine(dashboard)
                    Text(
                        template?.name ?: context.getString(R.string.workout_start_empty),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    RecordLine(dashboard, units)
                    Spacer(GlanceModifier.height(6.dp))
                    StartButton(context, template?.id, context.getString(R.string.template_preview_start))
                }
            }
        } else {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeekRing(dashboard, 48)
                    Spacer(GlanceModifier.width(8.dp))
                    CompactStreak(dashboard)
                }
                Spacer(GlanceModifier.defaultWeight())
                // Petite taille : le nom de la prochaine séance sur le bouton, l'icône lecture tient lieu de « Démarrer ».
                StartButton(
                    context,
                    template?.id,
                    template?.name ?: context.getString(R.string.workout_start_empty)
                )
            }
        }
    }
}

// Grande taille hors séance : anneau, streak et jours de la semaine ; tuiles tonnage et durée ; « Démarrer » avec le
// nom de la prochaine séance.
@Composable
private fun LargeIdle(dashboard: Dashboard, units: Units) {
    val context = LocalContext.current
    val template = dashboard.nextTemplate
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeekRing(dashboard, 64)
            Spacer(GlanceModifier.width(12.dp))
            // defaultWeight : la colonne prend la place restante, sinon la largeur du texte coupe la rangée des jours.
            Column(modifier = GlanceModifier.defaultWeight()) {
                StreakLine(dashboard)
                Spacer(GlanceModifier.height(6.dp))
                WeekDays(dashboard)
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        WeekTiles(dashboard, units)
        Spacer(GlanceModifier.defaultWeight())
        StartButton(context, template?.id, template?.name ?: context.getString(R.string.workout_start_empty))
    }
}

// Jours de la semaine L M M J V S D : jour avec une séance en rouge signal, aujourd'hui en rouge sur fond gris.
@Composable
private fun WeekDays(dashboard: Dashboard) {
    val context = LocalContext.current
    val monday = dashboard.streak.currentWeek.monday
    val days = (0L until DAYS_IN_WEEK).map { monday.plusDays(it) }
    val trained = days.filter {
        it in dashboard.trainedDays
    }.map { it.dayOfWeek.getDisplayName(DayStyle.FULL, Locale.FRENCH) }
    val description = if (trained.isEmpty()) {
        context.getString(R.string.home_no_trained_day)
    } else {
        context.getString(R.string.home_trained_days, trained.joinToString(", "))
    }
    Row(modifier = GlanceModifier.semantics { contentDescription = description }) {
        // Un widget n'accepte que 10 éléments par rangée : l'écart entre les jours est une marge, pas un Spacer.
        days.forEachIndexed { index, day ->
            val isTrained = day in dashboard.trainedDays
            Box(modifier = GlanceModifier.padding(end = if (index < days.lastIndex) 3.dp else 0.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier
                        .size(18.dp)
                        .cornerRadius(4.dp)
                        .background(
                            if (isTrained) GlanceTheme.colors.primaryContainer else GlanceTheme.colors.surfaceVariant
                        )
                ) {
                    Text(
                        day.dayOfWeek.getDisplayName(DayStyle.NARROW, Locale.FRENCH),
                        style = TextStyle(
                            color = when {
                                isTrained -> GlanceTheme.colors.onPrimaryContainer
                                day == dashboard.today -> GlanceTheme.colors.primary
                                else -> GlanceTheme.colors.onSurfaceVariant
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

// Tuiles « Cette semaine » de l'accueil : tonnage avec l'écart sur la semaine précédente, durée et séances.
@Composable
private fun WeekTiles(dashboard: Dashboard, units: Units) {
    val context = LocalContext.current
    val stats = dashboard.weekStats
    val change = stats.tonnageChange
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        WidgetStat(
            label = context.getString(R.string.home_week_tonnage),
            value = wholeNumber(units.weight.fromKg(stats.current.tonnageKg)),
            unit = context.getString(units.weight.labelRes),
            detail = change?.let {
                context.getString(R.string.home_week_change, signedPercent(it), stats.previousWeekNumber)
            },
            detailColor = if ((change ?: 0.0) > 0) SuccessColor else GlanceTheme.colors.onSurfaceVariant,
            modifier = GlanceModifier.defaultWeight()
        )
        Spacer(GlanceModifier.width(8.dp))
        WidgetStat(
            label = context.getString(R.string.home_week_duration),
            value = durationText(context.resources, stats.current.duration),
            unit = null,
            detail = context.resources.getQuantityString(
                R.plurals.progress_sessions,
                stats.current.sessions,
                stats.current.sessions
            ),
            detailColor = GlanceTheme.colors.onSurfaceVariant,
            modifier = GlanceModifier.defaultWeight()
        )
    }
}

@Composable
private fun WidgetStat(
    label: String,
    value: String,
    unit: String?,
    detail: String?,
    detailColor: ColorProvider,
    modifier: GlanceModifier
) {
    // Hauteur fixe : les deux tuiles restent alignées même quand l'une n'a pas de ligne de précision.
    Column(
        modifier = modifier
            .height(STAT_TILE_HEIGHT)
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(12.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            label.uppercase(Locale.FRENCH),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Text(
            if (unit == null) value else "$value $unit",
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        detail?.let {
            Text(it, style = TextStyle(color = detailColor, fontSize = 10.sp), maxLines = 1)
        }
    }
}

// Anneau dessiné en image, avec « 2/3 » au centre. Description pour le lecteur d'écran.
@Composable
private fun WeekRing(dashboard: Dashboard, sizeDp: Int) {
    val context = LocalContext.current
    val week = dashboard.streak.currentWeek
    val description = context.resources.getQuantityString(
        R.plurals.weekly_ring_description,
        week.sessions,
        week.sessions,
        week.goal
    )
    Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.size(sizeDp.dp)) {
        Image(
            provider = ImageProvider(weeklyRingBitmap(context, week.sessions, week.goal)),
            contentDescription = description,
            modifier = GlanceModifier.size(sizeDp.dp)
        )
        Text(
            "${week.sessions}/${week.goal}",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = if (sizeDp >= 64) 16.sp else 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

// « Streak : 4 semaines », taille moyenne.
@Composable
private fun StreakLine(dashboard: Dashboard) {
    val context = LocalContext.current
    val weeks = dashboard.streak.weeks
    Text(
        context.getString(
            R.string.widget_streak,
            weeks.toString() + " " + context.resources.getQuantityString(R.plurals.streak_weeks_unit, weeks)
        ),
        style = TextStyle(color = GlanceTheme.colors.tertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
        maxLines = 1
    )
}

// Petite taille : le nombre de semaines en gros, « sem. » dessous (une trentaine de dp de large seulement).
@Composable
private fun CompactStreak(dashboard: Dashboard) {
    val context = LocalContext.current
    val weeks = dashboard.streak.weeks
    Column {
        Text(
            weeks.toString(),
            style = TextStyle(color = GlanceTheme.colors.tertiary, fontSize = 20.sp, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        Text(
            context.getString(R.string.widget_weeks_short),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
            maxLines = 1
        )
    }
}

// « Record : Squat à la barre, 135 kg », sur une ligne.
@Composable
private fun RecordLine(dashboard: Dashboard, units: Units) {
    val latest = dashboard.latestRecord ?: return
    val context = LocalContext.current
    val metric = latest.recordBreak.metric
    val value = formatMetric(context.resources, metric, latest.recordBreak.value, latest.exercise, units.weight)
    Text(
        context.getString(R.string.widget_record, latest.exercise.name, value),
        style = TextStyle(color = GlanceTheme.colors.tertiary, fontSize = 12.sp),
        maxLines = 1
    )
}

@Composable
private fun StartButton(context: Context, templateId: Long?, text: String) {
    FilledButton(
        text = text,
        onClick = startAction(context, templateId),
        icon = ImageProvider(R.drawable.ic_play_arrow),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = GlanceTheme.colors.primaryContainer,
            contentColor = GlanceTheme.colors.onPrimaryContainer
        ),
        modifier = GlanceModifier.fillMaxWidth().height(40.dp),
        maxLines = 1
    )
}

@Composable
private fun LiveContent(details: LiveDetails, units: Units) {
    if (isLarge(LocalSize.current)) {
        LargeLive(details, units)
        return
    }
    val notice = details.notice
    val context = LocalContext.current
    val medium = isMedium(LocalSize.current)
    val now = Instant.now()
    val rest = notice.rest?.takeIf { it.isRunning(now) }
    val next = notice.next
    WidgetFrame(onClick = actionStartActivity(appIntent(context, MainActivity.ACTION_OPEN_WORKOUT))) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                "● " + context.getString(R.string.workout_live).uppercase() +
                    if (medium) " · " + notice.workoutName else "",
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Text(
                next?.exercise?.name ?: notice.workoutName,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Text(
                next?.let { describeNextSet(context, it, units.weight) }
                    ?: context.getString(R.string.notification_all_done),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                maxLines = 1
            )
            Spacer(GlanceModifier.defaultWeight())
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        context.getString(
                            if (rest !=
                                null
                            ) {
                                R.string.notification_rest
                            } else {
                                R.string.widget_session_time
                            }
                        ),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
                        maxLines = 1
                    )
                    WorkoutChronometer(rest, notice.startedAt, now)
                }
                if (next != null && next.canComplete) {
                    CompleteSetButton(context, next.setId, medium)
                }
            }
        }
    }
}

// Grande taille en séance, pensée pour tenir en 4 × 2 : « EN DIRECT · Pull » et la durée de la séance, l'exercice,
// la série et la dernière fois, la série à faire en grand, la progression de la séance, puis le repos et « Série
// validée ».
@Composable
private fun LargeLive(details: LiveDetails, units: Units) {
    val context = LocalContext.current
    val notice = details.notice
    val now = Instant.now()
    val rest = notice.rest?.takeIf { it.isRunning(now) }
    val next = notice.next
    WidgetFrame(onClick = actionStartActivity(appIntent(context, MainActivity.ACTION_OPEN_WORKOUT))) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "● " + context.getString(R.string.workout_live).uppercase() + " · " + notice.workoutName,
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
                // Largeur fixe : sans elle, la vue du chronomètre prend toute la ligne et efface le texte.
                Box(contentAlignment = Alignment.CenterEnd, modifier = GlanceModifier.width(SESSION_TIME_WIDTH)) {
                    SessionChronometer(notice.startedAt, now)
                }
            }
            Text(
                next?.exercise?.name ?: notice.workoutName,
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Text(
                setLine(context, details, units),
                style = TextStyle(color = GlanceTheme.colors.tertiary, fontSize = 11.sp),
                maxLines = 1
            )
            next?.let {
                describeSetValues(context, it.values, it.exercise, units.weight)
            }?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    it,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
            Spacer(GlanceModifier.defaultWeight())
            Text(
                context.resources.getQuantityString(
                    R.plurals.workout_progress_sets,
                    details.totalSets,
                    details.doneSets,
                    details.totalSets
                ) + " · " + volumeText(context.resources, details.tonnageKg, units.weight),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
                maxLines = 1
            )
            Spacer(GlanceModifier.height(3.dp))
            LinearProgressIndicator(
                progress = if (details.totalSets > 0) details.doneSets.toFloat() / details.totalSets else 0f,
                color = GlanceTheme.colors.primary,
                backgroundColor = GlanceTheme.colors.secondaryContainer,
                modifier = GlanceModifier.fillMaxWidth().height(4.dp)
            )
            Spacer(GlanceModifier.height(6.dp))
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    if (rest != null) {
                        Text(
                            context.getString(R.string.notification_rest),
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp),
                            maxLines = 1
                        )
                        WorkoutChronometer(rest, notice.startedAt, now)
                    }
                }
                if (next != null && next.canComplete) {
                    CompleteSetButton(context, next.setId, medium = true)
                }
            }
        }
    }
}

// « Série 3 sur 4 · Dernière fois : 80 kg × 8 », ou « Séance terminée » quand tout est validé.
private fun setLine(context: Context, details: LiveDetails, units: Units): String {
    val next = details.notice.next ?: return context.getString(R.string.notification_all_done)
    val previous = details.previous?.let { describeSetValues(context, it.values, next.exercise, units.weight) }
        ?.takeIf { it.isNotEmpty() }
        ?.let { context.getString(R.string.widget_last_time, it) }
    return listOfNotNull(describeSetPosition(context, next), previous).joinToString(" · ")
}

private val SESSION_TIME_WIDTH = 64.dp

// Durée de la séance, petite, en tête du grand widget : animée par Android comme le minuteur.
@Composable
private fun SessionChronometer(startedAt: Instant, now: Instant) {
    val context = LocalContext.current
    val views = RemoteViews(context.packageName, R.layout.widget_session_chronometer_small).apply {
        setChronometer(
            R.id.widget_chronometer,
            SystemClock.elapsedRealtime() - Duration.between(startedAt, now).toMillis(),
            null,
            true
        )
    }
    AndroidRemoteViews(views)
}

// Compte à rebours du repos (ou durée de la séance hors repos) par le Chronometer d'Android : le lanceur l'anime
// lui-même, sans que l'app rafraîchisse le widget chaque seconde. La base est exprimée dans l'horloge du démarrage
// du téléphone (elapsedRealtime), celle qu'utilise le Chronometer.
@Composable
private fun WorkoutChronometer(rest: RestPeriod?, startedAt: Instant, now: Instant) {
    val context = LocalContext.current
    val elapsedNow = SystemClock.elapsedRealtime()
    val views = if (rest != null) {
        RemoteViews(context.packageName, R.layout.widget_rest_chronometer).apply {
            setChronometer(
                R.id.widget_chronometer,
                elapsedNow + Duration.between(now, rest.endsAt).toMillis(),
                null,
                true
            )
            setChronometerCountDown(R.id.widget_chronometer, true)
        }
    } else {
        RemoteViews(context.packageName, R.layout.widget_session_chronometer).apply {
            setChronometer(
                R.id.widget_chronometer,
                elapsedNow - Duration.between(startedAt, now).toMillis(),
                null,
                true
            )
            setChronometerCountDown(R.id.widget_chronometer, false)
        }
    }
    AndroidRemoteViews(views)
}

// « Série validée » passe par le même récepteur que l'action de la notification.
@Composable
private fun CompleteSetButton(context: Context, setId: Long, medium: Boolean) {
    val intent = Intent(context, WorkoutActionReceiver::class.java)
        .setAction(WorkoutActionReceiver.ACTION_COMPLETE_SET)
        .putExtra(WorkoutActionReceiver.EXTRA_SET_ID, setId)
    val label = context.getString(R.string.notification_action_complete)
    val colors = ButtonDefaults.buttonColors(
        backgroundColor = GlanceTheme.colors.primaryContainer,
        contentColor = GlanceTheme.colors.onPrimaryContainer
    )
    if (medium) {
        FilledButton(
            text = label,
            onClick = actionSendBroadcast(intent),
            icon = ImageProvider(R.drawable.ic_check),
            colors = colors,
            modifier = GlanceModifier.height(40.dp),
            maxLines = 1
        )
    } else {
        // Bouton rond fait main : celui des composants Glance affiche une coche trop petite pour être lisible.
        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .size(48.dp)
                .cornerRadius(24.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .clickable(actionSendBroadcast(intent))
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_check),
                contentDescription = label,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                modifier = GlanceModifier.size(28.dp)
            )
        }
    }
}
