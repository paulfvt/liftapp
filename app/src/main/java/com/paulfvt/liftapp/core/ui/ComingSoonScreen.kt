package com.paulfvt.liftapp.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.paulfvt.liftapp.R
import com.paulfvt.liftapp.core.designsystem.theme.LiftTheme

// Écran temporaire affiché par chaque onglet tant que sa fonctionnalité n'est pas développée.
@Composable
fun ComingSoonScreen(@StringRes titleRes: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.coming_soon),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ComingSoonScreenPreview() {
    LiftTheme {
        ComingSoonScreen(titleRes = R.string.nav_profile)
    }
}
