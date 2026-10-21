package com.paulfvt.liftapp.onboarding.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.theme.LiftTheme

private class Feature(@param:DrawableRes val iconRes: Int, @param:StringRes val textRes: Int)

private val FEATURES = listOf(
    Feature(R.drawable.ic_fitness_center, R.string.welcome_feature_templates),
    Feature(R.drawable.ic_timer, R.string.welcome_feature_timer),
    Feature(R.drawable.ic_trending_up, R.string.welcome_feature_progress),
    Feature(R.drawable.ic_lock, R.string.welcome_feature_privacy)
)

// Écran affiché une seule fois, au premier lancement. Les unités (phase 10) et l'objectif hebdomadaire (phase 8)
// viendront s'y ajouter. Écran sans état : le ViewModel racine (LiftAppViewModel) mémorise qu'il a été vu.
@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    Scaffold(
        bottomBar = {
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .heightIn(min = 56.dp)
            ) {
                Text(stringResource(R.string.welcome_start), style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { padding ->
        // Défilement pour les petits écrans et les grandes tailles de police.
        Column(
            verticalArrangement = Arrangement.spacedBy(40.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Header()
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                FEATURES.forEach { FeatureRow(it) }
            }
        }
    }
}

@Composable
private fun Header() {
    val appName = stringResource(R.string.app_name)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Nom en capitales façon enseigne ; le lecteur d'écran reçoit le nom d'origine pour ne pas l'épeler.
        Text(
            appName.uppercase(),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clearAndSetSemantics {
                text = AnnotatedString(appName)
                heading()
            }
        )
        Text(stringResource(R.string.welcome_tagline), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun FeatureRow(feature: Feature) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
        ) {
            Icon(painterResource(feature.iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            stringResource(feature.textRes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    LiftTheme {
        WelcomeScreen(onStart = {})
    }
}
