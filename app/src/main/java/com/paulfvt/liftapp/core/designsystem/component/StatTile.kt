package com.paulfvt.liftapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import java.util.Locale

// Tuile de statistique du guide de style (.stat) : libellé en petites capitales, valeur en gros chiffres condensés,
// unité en petit à côté, et une précision dessous (« +6 % vs S43 »). Lue d'un seul tenant par le lecteur d'écran
// (« Tonnage, 9 840 kg »).
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    detail: String? = null,
    detailColor: Color = Color.Unspecified
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(
            label.uppercase(Locale.FRENCH),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.em),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            Text(value, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.alignByBaseline())
            unit?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline().padding(start = 2.dp)
                )
            }
        }
        detail?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = detailColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant }
            )
        }
    }
}
