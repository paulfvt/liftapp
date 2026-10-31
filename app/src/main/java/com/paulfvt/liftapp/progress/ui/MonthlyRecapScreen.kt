package com.paulfvt.liftapp.progress.ui

import androidx.annotation.PluralsRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle
import com.paulfvt.liftapp.core.designsystem.component.SectionTitle
import com.paulfvt.liftapp.core.designsystem.component.StatTile
import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.core.ui.LocalUnits
import com.paulfvt.liftapp.core.ui.monthTitle
import com.paulfvt.liftapp.core.ui.volumeText
import com.paulfvt.liftapp.progress.domain.MonthlyRecap
import com.paulfvt.liftapp.progress.domain.TonnageObject
import com.paulfvt.liftapp.workout.domain.formatDecimal
import java.time.YearMonth
import kotlin.math.roundToLong
import kotlinx.coroutines.launch

private const val MAX_RECORD_LINES = 5
private const val KG_PER_TON = 1_000.0

// Récap du mois, partageable en image : la carte est dessinée une seconde fois dans un calque graphique
// (GraphicsLayer), que le bouton Partager convertit en image sans capture d'écran ni permission.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyRecapScreen(onBack: () -> Unit, viewModel: MonthlyRecapViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recap = uiState.recap
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recapLayer = rememberGraphicsLayer()
    val chooserTitle = stringResource(R.string.recap_share_chooser)
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
                title = { ScreenTitle(stringResource(R.string.recap_title)) },
                actions = {
                    IconButton(
                        enabled = recap != null && recap.workoutCount > 0,
                        onClick = {
                            val month = recap?.month ?: return@IconButton
                            scope.launch {
                                val image = recapLayer.toImageBitmap().asAndroidBitmap()
                                shareImage(context, image, "recap-$month.png", chooserTitle)
                            }
                        }
                    ) {
                        Icon(painterResource(R.drawable.ic_share), stringResource(R.string.recap_share))
                    }
                }
            )
        }
    ) { padding ->
        if (recap == null) return@Scaffold
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            MonthSwitcher(recap.month, uiState.previousMonth, uiState.nextMonth, viewModel::onMonthSelected)
            Box(
                Modifier.drawWithContent {
                    recapLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(recapLayer)
                }
            ) {
                RecapCard(recap)
            }
        }
    }
}

@Composable
private fun MonthSwitcher(month: YearMonth, previous: YearMonth?, next: YearMonth?, onSelect: (YearMonth) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(enabled = previous != null, onClick = { previous?.let(onSelect) }) {
            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.recap_previous_month))
        }
        Text(
            monthTitle(month),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        IconButton(enabled = next != null, onClick = { next?.let(onSelect) }) {
            Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.recap_next_month))
        }
    }
}

// Carte partagée en image : fond plein et angles droits, pour qu'elle reste nette une fois enregistrée.
@Composable
private fun RecapCard(recap: MonthlyRecap) {
    val resources = LocalResources.current
    val unit = LocalUnits.current.weight
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(24.dp)
    ) {
        Column {
            Text(
                stringResource(R.string.app_name).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.recap_heading, monthTitle(recap.month)).uppercase(),
                style = MaterialTheme.typography.headlineMedium
            )
        }
        if (recap.workoutCount == 0) {
            Text(
                stringResource(R.string.recap_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                pluralStringResource(R.plurals.recap_sessions_label, recap.workoutCount),
                recap.workoutCount.toString(),
                Modifier.weight(1f)
            )
            StatTile(
                stringResource(R.string.workout_summary_tonnage),
                tonnageText(recap.tonnageKg),
                Modifier.weight(1f)
            )
        }
        recap.equivalence?.let { equivalence ->
            val count = equivalence.count
            val thing = pluralStringResource(equivalence.thing.pluralRes(), count.toInt())
            Text(
                stringResource(R.string.recap_equivalence, formatDecimal(count), thing),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (recap.records.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(pluralStringResource(R.plurals.recap_records, recap.records.size, recap.records.size))
                recap.records.take(MAX_RECORD_LINES).forEach { record ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) {}
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_emoji_events),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            stringResource(
                                R.string.record_new_detail,
                                record.exercise.name,
                                stringResource(record.metric.recordLabelRes(record.exercise.trackingType)),
                                formatMetric(resources, record.metric, record.value, record.exercise, unit)
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                val hidden = recap.records.size - MAX_RECORD_LINES
                if (hidden > 0) {
                    Text(
                        pluralStringResource(R.plurals.record_new_others, hidden, hidden),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        recap.favorite?.let { favorite ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle(stringResource(R.string.recap_favorite))
                Text(
                    favorite.exercise.name + " · " +
                        pluralStringResource(R.plurals.progress_sessions, favorite.sessions, favorite.sessions),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

// En kilos : « 850 kg » ou, à partir d'une tonne, « 48,2 t ». En livres : « 106 263 lb ».
@Composable
fun tonnageText(kg: Double): String = if (LocalUnits.current.weight == WeightUnit.KG && kg >= KG_PER_TON) {
    stringResource(R.string.recap_tons, formatDecimal((kg / KG_PER_TON * 10).roundToLong() / 10.0))
} else {
    volumeText(kg)
}

@PluralsRes
private fun TonnageObject.pluralRes(): Int = when (this) {
    TonnageObject.CEMENT_BAG -> R.plurals.tonnage_cement_bag
    TonnageObject.UPRIGHT_PIANO -> R.plurals.tonnage_piano
    TonnageObject.CITY_CAR -> R.plurals.tonnage_car
    TonnageObject.ELEPHANT -> R.plurals.tonnage_elephant
    TonnageObject.BLUE_WHALE -> R.plurals.tonnage_blue_whale
}
