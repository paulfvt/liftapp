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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.LiftButtonSize
import com.paulfvt.liftapp.core.designsystem.component.SignalButton
import com.paulfvt.liftapp.core.designsystem.theme.LiftTheme
import com.paulfvt.liftapp.core.domain.LengthUnit
import com.paulfvt.liftapp.core.domain.Units
import com.paulfvt.liftapp.core.domain.WeightUnit
import com.paulfvt.liftapp.progress.domain.DEFAULT_WEEKLY_GOAL
import com.paulfvt.liftapp.progress.ui.WeeklyGoalStepper
import com.paulfvt.liftapp.settings.ui.UnitChoices

private class Feature(@param:DrawableRes val iconRes: Int, @param:StringRes val textRes: Int)

private val FEATURES = listOf(
    Feature(R.drawable.ic_fitness_center, R.string.welcome_feature_templates),
    Feature(R.drawable.ic_timer, R.string.welcome_feature_timer),
    Feature(R.drawable.ic_trending_up, R.string.welcome_feature_progress),
    Feature(R.drawable.ic_lock, R.string.welcome_feature_privacy)
)

// Écran affiché une seule fois, au premier lancement, avec le choix de l'objectif hebdomadaire et des unités. Le
// ViewModel racine (LiftAppViewModel) les enregistre et mémorise que l'écran a été vu ; les choix en cours survivent
// à une rotation (rememberSaveable).
@Composable
fun WelcomeScreen(onStart: (weeklyGoal: Int, units: Units) -> Unit) {
    var weeklyGoal by rememberSaveable { mutableIntStateOf(DEFAULT_WEEKLY_GOAL) }
    var weightUnit by rememberSaveable { mutableStateOf(WeightUnit.KG) }
    var lengthUnit by rememberSaveable { mutableStateOf(LengthUnit.CM) }
    Scaffold(
        bottomBar = {
            SignalButton(
                text = stringResource(R.string.welcome_start),
                onClick = { onStart(weeklyGoal, Units(weightUnit, lengthUnit)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                size = LiftButtonSize.Large
            )
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.weekly_goal_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    stringResource(R.string.weekly_goal_question),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WeeklyGoalStepper(weeklyGoal, onGoalChange = { weeklyGoal = it })
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.welcome_units_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
                UnitChoices(
                    units = Units(weightUnit, lengthUnit),
                    onWeightUnitChange = { weightUnit = it },
                    onLengthUnitChange = { lengthUnit = it }
                )
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
        WelcomeScreen(onStart = { _, _ -> })
    }
}
