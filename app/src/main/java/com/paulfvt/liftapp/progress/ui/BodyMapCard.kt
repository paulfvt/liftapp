package com.paulfvt.liftapp.progress.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.model.Muscle
import com.paulfvt.liftapp.core.ui.labelRes
import com.paulfvt.liftapp.progress.domain.BodyDiagram
import com.paulfvt.liftapp.progress.domain.BodyView
import com.paulfvt.liftapp.progress.domain.HeatLevel
import com.paulfvt.liftapp.progress.domain.heatLevel
import com.paulfvt.liftapp.workout.domain.formatDecimal

// Silhouette prête à dessiner : les tracés SVG sont convertis une seule fois en formes Compose (Path).
@Immutable
class BodyFigure(
    val view: BodyView,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val outline: List<Path>,
    val zones: List<Pair<Muscle, Path>>
)

// PathParser (fourni par Compose) lit le langage des tracés SVG (« M10 20 L… ») : pas de bibliothèque à ajouter.
// La conversion prend quelques millisecondes : le ViewModel l'appelle hors du fil principal.
fun BodyDiagram.toFigure(): BodyFigure = BodyFigure(
    view = view,
    left = bounds[0],
    top = bounds[1],
    width = bounds[2],
    height = bounds[3],
    outline = outline.map { PathParser().parsePathString(it).toPath() },
    zones = zones.map { it.muscle to PathParser().parsePathString(it.path).toPath() }
)

private const val ZONE_SEPARATOR_WIDTH = 3f

// Carte du corps : face et dos colorés selon les séries effectives des 7 derniers jours, légende, puis la liste des
// muscles en texte (lue par le lecteur d'écran, et seul endroit où le cou apparaît, faute de zone dessinée).
@Composable
fun BodyMapCard(muscleSets: Map<Muscle, Double>, figures: List<BodyFigure>, modifier: Modifier = Modifier) {
    val colors = heatColors()
    val worked = muscleSets.filterValues { it > 0 }.entries.sortedByDescending { it.value }
    val summary = pluralStringResource(R.plurals.body_map_description, worked.size, worked.size)
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.clearAndSetSemantics { contentDescription = summary }
            ) {
                // Cadre commun aux deux silhouettes : même échelle, et « Face » et « Dos » alignés.
                val frameWidth = figures.maxOf { it.width }
                val frameHeight = figures.maxOf { it.height }
                figures.forEach { figure ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        BodyFigureCanvas(figure, frameWidth, frameHeight, muscleSets, colors)
                        Text(
                            stringResource(
                                if (figure.view == BodyView.FRONT) R.string.body_map_front else R.string.body_map_back
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HeatLegend(colors)
            if (worked.isEmpty()) {
                Text(
                    stringResource(R.string.body_map_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    worked.forEach { (muscle, sets) -> MuscleChip(muscle, sets, colors.getValue(heatLevel(sets))) }
                }
            }
        }
    }
}

@Composable
private fun BodyFigureCanvas(
    figure: BodyFigure,
    frameWidth: Float,
    frameHeight: Float,
    muscleSets: Map<Muscle, Double>,
    colors: Map<HeatLevel, Color>
) {
    val body = MaterialTheme.colorScheme.surfaceContainerHighest
    val separator = MaterialTheme.colorScheme.surfaceContainerLow
    Canvas(Modifier.fillMaxWidth().aspectRatio(frameWidth / frameHeight)) {
        val ratio = size.width / frameWidth
        // Mise à l'échelle depuis le coin haut gauche, puis décalage pour centrer le corps dans le cadre commun.
        scale(ratio, ratio, pivot = Offset.Zero) {
            translate(
                -figure.left + (frameWidth - figure.width) / 2,
                -figure.top + (frameHeight - figure.height) / 2
            ) {
                figure.outline.forEach { drawPath(it, body) }
                figure.zones.forEach { (muscle, path) ->
                    drawPath(path, colors.getValue(heatLevel(muscleSets[muscle] ?: 0.0)))
                    drawPath(path, separator, style = Stroke(ZONE_SEPARATOR_WIDTH))
                }
            }
        }
    }
}

// Du gris de la silhouette vers le rouge signal : plus un muscle a de séries, plus il est rouge.
@Composable
private fun heatColors(): Map<HeatLevel, Color> {
    val none = lerp(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, 0.2f)
    val signal = MaterialTheme.colorScheme.primaryContainer
    return mapOf(
        HeatLevel.NONE to none,
        HeatLevel.LOW to lerp(none, signal, 0.35f),
        HeatLevel.MEDIUM to lerp(none, signal, 0.6f),
        HeatLevel.HIGH to lerp(none, signal, 0.85f),
        HeatLevel.VERY_HIGH to signal
    )
}

@Composable
private fun HeatLegend(colors: Map<HeatLevel, Color>) {
    val labels = listOf(
        HeatLevel.NONE to "0",
        HeatLevel.LOW to "<3",
        HeatLevel.MEDIUM to "3–5",
        HeatLevel.HIGH to "6–9",
        HeatLevel.VERY_HIGH to "10+"
    )
    val description = stringResource(R.string.body_map_legend_description)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description }
    ) {
        Text(
            stringResource(R.string.body_map_legend),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        labels.forEach { (level, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Swatch(colors.getValue(level))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// « Pectoraux 9 » : séries de la semaine, avec la couleur de la carte.
@Composable
private fun MuscleChip(muscle: Muscle, sets: Double, color: Color) {
    val name = stringResource(muscle.labelRes)
    val count = formatDecimal(sets)
    val description = stringResource(R.string.body_map_muscle_description, name, count)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description }
    ) {
        Swatch(color)
        Text("$name $count", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Swatch(color: Color) {
    Box(Modifier.size(10.dp).background(color, MaterialTheme.shapes.extraSmall))
}
