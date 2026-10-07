package com.paulfvt.liftapp.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Palette « ON AIR » (enseigne de studio), reprise de design-system/styles.css où les contrastes sont vérifiés.
// primaryContainer est le rouge signal : fonds pleins (FAB, indicateur de navigation, boutons d'action principaux)
// avec texte blanc. primary sert au texte, aux icônes et aux contours. tertiary (ambre) est réservé aux records
// et au minuteur. Tous les rôles sont définis : un rôle oublié retomberait sur le violet par défaut de Material 3.

private val SignalRed = Color(0xFFD9241B)

internal val DarkColors =
    darkColorScheme(
        primary = Color(0xFFFF7468),
        onPrimary = Color(0xFF3B0805),
        primaryContainer = SignalRed,
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFFE7BDB6),
        onSecondary = Color(0xFF442925),
        secondaryContainer = Color(0xFF5D3F3B),
        onSecondaryContainer = Color(0xFFFFDAD5),
        tertiary = Color(0xFFFFB547),
        onTertiary = Color(0xFF432C00),
        tertiaryContainer = Color(0xFF6B4500),
        onTertiaryContainer = Color(0xFFFFDDB0),
        error = Color(0xFFFF8FAB),
        onError = Color(0xFF5C0F27),
        errorContainer = Color(0xFF8C1D3E),
        onErrorContainer = Color(0xFFFFD9E0),
        background = Color(0xFF0D0A0A),
        onBackground = Color(0xFFF3EAE8),
        surface = Color(0xFF0D0A0A),
        onSurface = Color(0xFFF3EAE8),
        surfaceVariant = Color(0xFF3B3132),
        onSurfaceVariant = Color(0xFFD2C2BF),
        outline = Color(0xFF9C8A87),
        outlineVariant = Color(0xFF4C403F),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFF3EAE8),
        inverseOnSurface = Color(0xFF2B2223),
        inversePrimary = Color(0xFFB3141B),
        surfaceDim = Color(0xFF0D0A0A),
        surfaceBright = Color(0xFF352D2E),
        surfaceContainerLowest = Color(0xFF080606),
        surfaceContainerLow = Color(0xFF151011),
        surfaceContainer = Color(0xFF1B1516),
        surfaceContainerHigh = Color(0xFF251E1F),
        surfaceContainerHighest = Color(0xFF302829)
    )

internal val LightColors =
    lightColorScheme(
        primary = Color(0xFFB8121A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = SignalRed,
        onPrimaryContainer = Color(0xFFFFFFFF),
        secondary = Color(0xFF775652),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFDAD5),
        onSecondaryContainer = Color(0xFF2C1512),
        tertiary = Color(0xFF7E5000),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFDDB0),
        onTertiaryContainer = Color(0xFF291800),
        error = Color(0xFFA8174A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFD9E0),
        onErrorContainer = Color(0xFF3E0016),
        background = Color(0xFFFFF8F6),
        onBackground = Color(0xFF221919),
        surface = Color(0xFFFFF8F6),
        onSurface = Color(0xFF221919),
        surfaceVariant = Color(0xFFF4DDD9),
        onSurfaceVariant = Color(0xFF554240),
        outline = Color(0xFF86716E),
        outlineVariant = Color(0xFFD9C1BD),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF382E2D),
        inverseOnSurface = Color(0xFFFFEDEA),
        inversePrimary = Color(0xFFFF7468),
        surfaceDim = Color(0xFFE4D6D4),
        surfaceBright = Color(0xFFFFF8F6),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFCF0EE),
        surfaceContainer = Color(0xFFF7EAE8),
        surfaceContainerHigh = Color(0xFFF1E4E2),
        surfaceContainerHighest = Color(0xFFEBDEDB)
    )
