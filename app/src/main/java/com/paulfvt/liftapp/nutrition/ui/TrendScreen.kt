package com.paulfvt.liftapp.nutrition.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftCard
import com.paulfvt.liftapp.core.designsystem.component.LiftTextButton
import com.paulfvt.liftapp.core.designsystem.component.LiftTonalButton
import com.paulfvt.liftapp.core.designsystem.theme.LocalExtendedColors
import com.paulfvt.liftapp.core.domain.DailyValue
import com.paulfvt.liftapp.core.model.MeasurementType
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.nutrition.domain.CalorieWeek
import com.paulfvt.liftapp.nutrition.domain.ExpenditureEstimate
import com.paulfvt.liftapp.nutrition.domain.NutritionGoal
import com.paulfvt.liftapp.nutrition.domain.WeightTrend
import com.paulfvt.liftapp.profile.domain.toDisplay
import com.paulfvt.liftapp.profile.ui.changeText
import com.paulfvt.liftapp.profile.ui.measureText
import com.paulfvt.liftapp.profile.ui.rememberMeasureFormatter
import com.paulfvt.liftapp.progress.ui.ProgressChart
import com.paulfvt.liftapp.workout.domain.formatDecimal
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Marge au-dessus de la plus haute barre (ou de l'objectif), pour que rien ne touche le haut du graphique.
private const val BAR_HEADROOM = 1.1
private val BAR_GAP = 12.dp
private val CHART_HEIGHT = 120.dp

// Tendance de la nutrition (étape 6 de la phase 12, maquette « Tendance ») : la semaine de calories, le poids des
// 4 dernières semaines et la dépense réelle estimée, avec un nouvel objectif proposé.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendScreen(onBack: () -> Unit, onAddWeight: () -> Unit, viewModel: TrendViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nutrition_trend_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) return@Scaffold
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            uiState.week?.let { CaloriesCard(it) }
            if (!uiState.isWeightHidden) WeightCard(uiState.weight, onAddWeight)
            uiState.expenditure?.let { estimate ->
                ExpenditureCard(
                    estimate = estimate,
                    goal = uiState.goal,
                    proposedKcal = uiState.proposedKcal,
                    onApply = viewModel::applyProposal,
                    onLater = viewModel::dismissProposal
                )
            }
        }
    }
}

@Composable
private fun TrendCard(title: String, isAccent: Boolean = false, content: @Composable () -> Unit) {
    LiftCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(16.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isAccent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

// Grand chiffre et sa légende sur une ligne : « 2 980 kcal par jour en moyenne ».
@Composable
private fun BigValue(value: String, caption: String) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(value, style = MaterialTheme.typography.headlineLarge)
        Text(
            caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

@Composable
private fun CaloriesCard(week: CalorieWeek) {
    TrendCard(stringResource(R.string.nutrition_trend_calories_title)) {
        val average = week.averageKcal
        if (average == null) {
            Text(
                stringResource(R.string.nutrition_trend_no_day),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            BigValue(
                formatDecimal(average.toDouble()),
                week.goalKcal?.let {
                    stringResource(R.string.nutrition_trend_average_goal, formatDecimal(it.toDouble()))
                }
                    ?: stringResource(R.string.nutrition_trend_average)
            )
        }
        CalorieBars(week)
        if (week.loggedDays > 0 && week.goalKcal != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = LocalExtendedColors.current.success,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    pluralStringResource(
                        R.plurals.nutrition_trend_protein,
                        week.proteinReachedDays,
                        week.proteinReachedDays,
                        week.loggedDays
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Une barre par jour, la ligne pointillée de l'objectif, le jour en cours en plus pâle (il n'est pas fini).
@Composable
private fun CalorieBars(week: CalorieWeek) {
    val bar = MaterialTheme.colorScheme.primary
    val today = MaterialTheme.colorScheme.secondaryContainer
    val goalLine = MaterialTheme.colorScheme.outline
    // L'interface n'existe qu'en français : jours de la semaine en français, quelle que soit la langue du téléphone.
    val locale = Locale.FRENCH
    val description = stringResource(
        R.string.nutrition_trend_chart_description,
        week.days.joinToString(", ") { day ->
            day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale) + " " +
                formatDecimal(day.kcal.roundToInt().toDouble())
        }
    )
    val top = maxOf(week.goalKcal?.toDouble() ?: 0.0, week.days.maxOf { it.kcal }).takeIf { it > 0 } ?: 1.0
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics {
            contentDescription =
                description
        }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
            val gap = BAR_GAP.toPx()
            val width = (size.width - gap * (week.days.size - 1)) / week.days.size
            val scale = size.height / (top * BAR_HEADROOM)
            val radius = CornerRadius(4.dp.toPx())
            week.days.forEachIndexed { index, day ->
                val height = (day.kcal * scale).toFloat()
                drawRoundRect(
                    color = if (day.isToday) today else bar,
                    topLeft = Offset(index * (width + gap), size.height - height),
                    size = Size(width, height),
                    cornerRadius = radius
                )
            }
            week.goalKcal?.let { goal ->
                val y = size.height - (goal * scale).toFloat()
                drawLine(
                    color = goalLine,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                )
            }
        }
        val dayColor = MaterialTheme.colorScheme.onSurfaceVariant
        Row(horizontalArrangement = Arrangement.spacedBy(BAR_GAP)) {
            week.days.forEach { day ->
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (day.isToday) bar else dayColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WeightCard(trend: WeightTrend?, onAddWeight: () -> Unit) {
    TrendCard(stringResource(R.string.nutrition_trend_weight_title)) {
        if (trend == null) {
            Text(
                stringResource(R.string.nutrition_trend_weight_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LiftTonalButton(stringResource(R.string.nutrition_needs_add_weight), onClick = onAddWeight)
            return@TrendCard
        }
        val type = MeasurementType.BODY_WEIGHT
        BigValue(
            measureText(type, trend.latestKg),
            stringResource(
                R.string.nutrition_trend_weight_change,
                changeText(type, trend.changeKg),
                changeText(type, trend.weeklyChangeKg)
            )
        )
        // Pesées du jour en points, droite de tendance en ligne (de la première à la dernière pesée).
        val units = LocalUnits.current
        val display = trend.days.map { DailyValue(it.date, type.toDisplay(it.value, units)) }
        val slopePerDay = trend.weeklyChangeKg / DAYS_PER_WEEK
        val last = trend.days.last()
        val line = listOf(trend.days.first(), last).map { day ->
            val kg = trend.latestKg - slopePerDay * ChronoUnit.DAYS.between(day.date, last.date)
            DailyValue(day.date, type.toDisplay(kg, units))
        }
        ProgressChart(
            values = display,
            valueText = rememberMeasureFormatter(type),
            description = stringResource(
                R.string.nutrition_trend_weight_description,
                measureText(type, trend.days.first().value),
                measureText(type, trend.latestKg)
            ),
            color = MaterialTheme.colorScheme.primary,
            trend = line,
            trendLabel = stringResource(R.string.nutrition_trend_line)
        )
    }
}

@Composable
private fun ExpenditureCard(
    estimate: ExpenditureEstimate,
    goal: NutritionGoal?,
    proposedKcal: Int?,
    onApply: () -> Unit,
    onLater: () -> Unit
) {
    TrendCard(stringResource(R.string.nutrition_trend_expenditure_title), isAccent = true) {
        when (estimate) {
            is ExpenditureEstimate.NotEnoughData -> Text(
                stringResource(
                    R.string.nutrition_trend_not_enough,
                    pluralStringResource(
                        R.plurals.nutrition_trend_logged_days,
                        estimate.loggedDays,
                        estimate.loggedDays
                    ),
                    pluralStringResource(R.plurals.nutrition_trend_weigh_ins, estimate.weighIns, estimate.weighIns)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            is ExpenditureEstimate.Ready -> {
                BigValue(
                    formatDecimal(estimate.kcalPerDay.toDouble()),
                    goal?.let {
                        stringResource(
                            R.string.nutrition_trend_formula,
                            formatDecimal(it.impliedExpenditure.toDouble())
                        )
                    } ?: stringResource(R.string.nutrition_trend_per_day)
                )
                Text(
                    stringResource(R.string.nutrition_trend_basis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (proposedKcal != null && goal != null) {
                    val offset = goal.settings.objective.kcalOffset
                    val kcal = formatDecimal(proposedKcal.toDouble())
                    Text(
                        if (offset == 0) {
                            stringResource(R.string.nutrition_trend_proposal_maintain, kcal)
                        } else {
                            stringResource(R.string.nutrition_trend_proposal_offset, signedKcal(offset), kcal)
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiftTonalButton(stringResource(R.string.nutrition_trend_apply, kcal), onClick = onApply)
                        LiftTextButton(stringResource(R.string.nutrition_trend_later), onClick = onLater)
                    }
                }
            }
        }
    }
}

// « +300 », « −500 » (vrai signe moins).
private fun signedKcal(kcal: Int): String = if (kcal > 0) "+$kcal" else "−${abs(kcal)}"

private const val DAYS_PER_WEEK = 7.0
