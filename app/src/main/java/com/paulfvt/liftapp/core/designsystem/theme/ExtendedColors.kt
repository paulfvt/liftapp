package com.paulfvt.liftapp.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Couleurs du guide de style qui n'existent pas dans Material 3 : « success » marque les séries validées.
// Elles passent par un CompositionLocal, comme MaterialTheme le fait pour ses propres couleurs.
@Immutable
data class ExtendedColors(val success: Color, val onSuccess: Color)

internal val DarkExtendedColors = ExtendedColors(success = Color(0xFF5BD98A), onSuccess = Color(0xFF00391B))

internal val LightExtendedColors = ExtendedColors(success = Color(0xFF196B3B), onSuccess = Color(0xFFFFFFFF))

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
