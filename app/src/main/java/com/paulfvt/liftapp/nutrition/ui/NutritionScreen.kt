package com.paulfvt.liftapp.nutrition.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.component.EmptyState
import com.paulfvt.liftapp.core.designsystem.component.ScreenTitle

// Onglet Nutrition : le journal du jour arrive à l'étape 4 de la phase 12.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen() {
    Scaffold(topBar = { TopAppBar(title = { ScreenTitle(stringResource(R.string.nav_nutrition)) }) }) { padding ->
        EmptyState(
            iconRes = R.drawable.ic_restaurant,
            title = stringResource(R.string.nutrition_soon_title),
            text = stringResource(R.string.nutrition_soon_text),
            modifier = Modifier.padding(padding)
        )
    }
}
