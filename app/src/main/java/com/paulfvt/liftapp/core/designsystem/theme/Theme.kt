package com.paulfvt.liftapp.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

// Thème « ON AIR » (voir design-system/). Pas de couleurs dynamiques (Material You) : l'app garde une identité
// visuelle fixe. Le paramètre darkTheme permettra au réglage « thème clair ou sombre » (phase 10) d'ignorer le système.
@Composable
fun LiftTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalExtendedColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = LiftTypography,
            shapes = LiftShapes,
            content = content
        )
    }
}
