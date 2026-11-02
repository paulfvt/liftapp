package com.paulfvt.liftapp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.core.designsystem.theme.LocalExtendedColors

val HairlineWidth = 1.dp

// Trait fin qui détache cartes, tuiles, cases et vignettes du fond : blanc en thème sombre (décision du
// 2 novembre 2026), gris en thème clair.
@Composable
fun hairlineBorder(): BorderStroke = BorderStroke(HairlineWidth, LocalExtendedColors.current.hairline)

// Carte du guide de style : la carte en relief de Material 3 (ElevatedCard), avec le trait fin. Cliquable si
// onClick est donné. border : un autre contour (le dernier record garde le sien, ambre).
@Composable
fun LiftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = CardDefaults.elevatedShape,
    colors: CardColors = CardDefaults.elevatedCardColors(),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    border: BorderStroke = hairlineBorder(),
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    } else {
        Card(modifier, shape = shape, colors = colors, elevation = elevation, border = border, content = content)
    }
}
