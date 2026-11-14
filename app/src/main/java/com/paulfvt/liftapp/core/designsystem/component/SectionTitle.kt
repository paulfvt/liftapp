package com.paulfvt.liftapp.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Titre de section en petites capitales grises (« RECORDS », « CRÉDITS »), annoncé comme titre au lecteur d'écran.
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = modifier.semantics { heading() }
    )
}

// Titre de section suivi de son contenu (accueil, Progrès). isAccent : titre rouge, réservé aux blocs du jour de
// l'accueil, la prochaine séance et la nutrition (12 novembre 2026).
@Composable
fun SectionBlock(title: String, isAccent: Boolean = false, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(
            title,
            color = if (isAccent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}
