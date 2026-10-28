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
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.paulfvt.liftapp.MainActivity
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.theme.DarkColors
import com.paulfvt.liftapp.core.designsystem.theme.LightColors
import com.paulfvt.liftapp.home.data.Dashboard
import com.paulfvt.liftapp.progress.ui.formatMetric
import com.paulfvt.liftapp.workout.domain.RestPeriod
import com.paulfvt.liftapp.workout.domain.WorkoutNotice
import com.paulfvt.liftapp.workout.notification.WorkoutActionReceiver
import com.paulfvt.liftapp.workout.notification.describeNextSet
import dagger.hilt.android.EntryPointAccessors
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.first

// Couleurs du thème ON AIR, en clair et en sombre selon le téléphone.
private val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

// Widget d'écran d'accueil, un seul qui change d'état tout seul : hors séance (anneau, streak, prochaine séance,
// dernier record) ou en séance (exercice, série, minuteur, « Série validée »). Deux tailles : petite et moyenne.
class LiftWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    // Les données suivent la base tant que le widget est affiché ; sinon l'app demande une mise à jour (WidgetUpdater).
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val source = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).widgetData()
        val initial = source.observe().first()
        provideContent {
            val states = remember { source.observe() }
            val state by states.collectAsState(initial)
            GlanceTheme(colors = WidgetColors) {
                when (val current = state) {
                    is WidgetState.Idle -> IdleContent(current.dashboard)
                    is WidgetState.Live -> LiveContent(current.notice)
                }
            }
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
    }
}

private fun isMedium(size: DpSize) = size.width >= LiftWidget.MEDIUM.width

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
private fun IdleContent(dashboard: Dashboard) {
    val context = LocalContext.current
    val medium = isMedium(LocalSize.current)
    val template = dashboard.nextTemplate
    WidgetFrame(onClick = actionStartActivity(appIntent(context))) {
        if (medium) {
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
                    RecordLine(dashboard)
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
private fun RecordLine(dashboard: Dashboard) {
    val latest = dashboard.latestRecord ?: return
    val context = LocalContext.current
    val metric = latest.recordBreak.metric
    val value = formatMetric(context.resources, metric, latest.recordBreak.value, latest.exercise)
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
private fun LiveContent(notice: WorkoutNotice) {
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
                next?.let { describeNextSet(context, it) } ?: context.getString(R.string.notification_all_done),
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
