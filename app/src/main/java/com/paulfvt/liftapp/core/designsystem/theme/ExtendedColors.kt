package com.paulfvt.liftapp.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Couleurs du guide de style qui n'existent pas dans Material 3 : « success » marque les séries validées, « wash »
// est le halo rouge signal en haut de l'accueil (--wash). Elles passent par un CompositionLocal, comme MaterialTheme
// le fait pour ses propres couleurs.
@Immutable
data class ExtendedColors(val success: Color, val onSuccess: Color, val wash: Color)

// Halo : rouge signal à 18 % en sombre, 8 % en clair.
internal val DarkExtendedColors =
    ExtendedColors(success = Color(0xFF5BD98A), onSuccess = Color(0xFF00391B), wash = Color(0x2ED9241B))

internal val LightExtendedColors =
    ExtendedColors(success = Color(0xFF196B3B), onSuccess = Color(0xFFFFFFFF), wash = Color(0x14D9241B))

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
