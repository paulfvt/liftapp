package com.paulfvt.liftapp.progress.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayerPadding
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.VicoTheme
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.paulfvt.liftapp.core.domain.DailyValue
import com.paulfvt.liftapp.progress.domain.chartRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val AXIS_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.FRENCH)
private const val BOTTOM_LABEL_COUNT = 4
private const val AREA_ALPHA = 0.3f
private val POINT_SIZE = 8.dp
private val TREND_POINT_SIZE = 6.dp

// Courbe d'une valeur par jour, par défaut en ambre (couleur réservée aux records et au minuteur). La courbe tient
// toujours dans la largeur (ni défilement ni zoom), et un appui affiche la valeur. Avec une tendance (moyenne mobile
// du poids), les valeurs du jour deviennent de simples points et la tendance est la ligne. description : résumé lu
// par le lecteur d'écran, qui ne peut pas parcourir les points d'un graphique.
@Composable
fun ProgressChart(
    values: List<DailyValue>,
    valueText: (Double) -> String,
    description: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.tertiary,
    trend: List<DailyValue> = emptyList(),
    trendLabel: String = ""
) {
    val byDay = remember(values) { values.associate { it.date.toEpochDay() to it.value }.toSortedMap() }
    val trendByDay = remember(trend) { trend.associate { it.date.toEpochDay() to it.value }.toSortedMap() }
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(byDay, trendByDay) {
        modelProducer.runTransaction {
            lineModel {
                series(x = byDay.keys.toList(), y = byDay.values.toList())
                if (trendByDay.isNotEmpty()) series(x = trendByDay.keys.toList(), y = trendByDay.values.toList())
            }
        }
    }
    if (byDay.isEmpty()) return
    val hasTrend = trendByDay.isNotEmpty()
    val span = byDay.lastKey() - byDay.firstKey()
    val range = remember(byDay, trendByDay) { chartRange(byDay.values + trendByDay.values) }
    val labelSpacing = (span / BOTTOM_LABEL_COUNT).toInt().coerceAtLeast(1)

    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val theme = MaterialTheme.colorScheme.run {
        VicoTheme(
            candlestickCartesianLayerColors = VicoTheme.CandlestickCartesianLayerColors(primary, outline, error),
            columnCartesianLayerColors = listOf(color),
            lineColor = outlineVariant,
            textColor = onSurfaceVariant
        )
    }
    val dateFormatter = remember {
        CartesianValueFormatter { _, value, _ -> AXIS_DATE.format(LocalDate.ofEpochDay(value.toLong())) }
    }
    val valueFormatter = remember(valueText) { CartesianValueFormatter { _, value, _ -> valueText(value) } }
    // Étiquette au toucher : « 14 oct. · 116,5 kg », suivie de la tendance s'il y en a une.
    val markerFormatter = remember(valueText, trendLabel) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            targets.filterIsInstance<LineCartesianLayerMarkerTarget>().joinToString { target ->
                val day = AXIS_DATE.format(LocalDate.ofEpochDay(target.x.toLong()))
                val parts = target.points.map { valueText(it.entry.y) }
                if (parts.size > 1) "$day · ${parts[0]} · $trendLabel ${parts[1]}" else "$day · ${parts.first()}"
            }
        }
    }
    val area = LineCartesianLayer.AreaFill.single(
        Fill(Brush.verticalGradient(listOf(color.copy(alpha = AREA_ALPHA), Color.Transparent)))
    )
    // Valeurs du jour : ligne et points dans la couleur choisie, ou points discrets sous une tendance.
    val valuesLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(if (hasTrend) Color.Transparent else color)),
        areaFill = if (hasTrend) null else area,
        pointProvider = LineCartesianLayer.PointProvider.single(
            LineCartesianLayer.Point(
                ShapeComponent(Fill(if (hasTrend) muted else color), CircleShape),
                size = if (hasTrend) TREND_POINT_SIZE else POINT_SIZE
            )
        )
    )
    val trendLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(color)),
        areaFill = area
    )
    ProvideVicoTheme(theme) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = if (hasTrend) {
                        LineCartesianLayer.LineProvider.series(valuesLine, trendLine)
                    } else {
                        LineCartesianLayer.LineProvider.series(valuesLine)
                    },
                    rangeProvider = remember(range) {
                        CartesianLayerRangeProvider.fixed(minY = range.min, maxY = range.max)
                    }
                ),
                startAxis = VerticalAxis.rememberStart(
                    valueFormatter = valueFormatter,
                    itemPlacer = remember(range) { VerticalAxis.ItemPlacer.step({ range.step }) }
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = dateFormatter,
                    itemPlacer = remember(labelSpacing) {
                        HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing })
                    }
                ),
                marker = rememberDefaultCartesianMarker(
                    label = rememberTextComponent(),
                    valueFormatter = markerFormatter
                ),
                // Marge à droite : la dernière date de l'axe n'est pas coupée par le bord.
                layerPadding = { CartesianLayerPadding(unscalableStart = 8.dp, unscalableEnd = 24.dp) },
                getXStep = { _, _, _ -> 1.0 }
            ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp)
                .clearAndSetSemantics { contentDescription = description }
        )
    }
}
