package com.paulfvt.liftapp.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.paulfvt.liftapp.R

// Polices embarquées dans l'app (res/font, licence OFL) plutôt que téléchargées : elles s'affichent hors connexion
// dès le premier lancement. Barlow Condensed pour les titres et les gros chiffres, Barlow pour le texte.
private val Barlow =
    FontFamily(
        Font(R.font.barlow_regular, FontWeight.Normal),
        Font(R.font.barlow_semibold, FontWeight.SemiBold),
        Font(R.font.barlow_bold, FontWeight.Bold)
    )

private val BarlowCondensed = FontFamily(Font(R.font.barlow_condensed_bold, FontWeight.Bold))

// Chiffres à chasse fixe : un poids ou un chrono qui change ne fait pas bouger le texte autour.
private const val TABULAR_NUMBERS = "tnum"

private fun display(size: Int, lineHeight: Int, letterSpacing: Double) = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.Bold,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.em,
    fontFeatureSettings = TABULAR_NUMBERS
)

private fun text(weight: FontWeight, size: Int, lineHeight: Int, letterSpacing: Double) = TextStyle(
    fontFamily = Barlow,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.em
)

// Échelle Material 3 (tailles du guide de style). Compose ne sait pas mettre un style en capitales :
// les titres en capitales du design passent par ScreenTitle.
internal val LiftTypography =
    Typography(
        displayLarge = display(size = 57, lineHeight = 60, letterSpacing = -0.01),
        displayMedium = display(size = 45, lineHeight = 48, letterSpacing = -0.005),
        displaySmall = display(size = 36, lineHeight = 40, letterSpacing = 0.0),
        headlineLarge = display(size = 32, lineHeight = 36, letterSpacing = 0.01),
        headlineMedium = display(size = 28, lineHeight = 32, letterSpacing = 0.015),
        headlineSmall = display(size = 24, lineHeight = 28, letterSpacing = 0.02),
        titleLarge = text(FontWeight.SemiBold, size = 22, lineHeight = 28, letterSpacing = 0.0),
        titleMedium = text(FontWeight.SemiBold, size = 16, lineHeight = 24, letterSpacing = 0.01),
        titleSmall = text(FontWeight.SemiBold, size = 14, lineHeight = 20, letterSpacing = 0.01),
        bodyLarge = text(FontWeight.Normal, size = 16, lineHeight = 24, letterSpacing = 0.01),
        bodyMedium = text(FontWeight.Normal, size = 14, lineHeight = 20, letterSpacing = 0.015),
        bodySmall = text(FontWeight.Normal, size = 12, lineHeight = 16, letterSpacing = 0.025),
        labelLarge = text(FontWeight.SemiBold, size = 14, lineHeight = 20, letterSpacing = 0.02),
        labelMedium = text(FontWeight.SemiBold, size = 12, lineHeight = 16, letterSpacing = 0.04),
        labelSmall = text(FontWeight.SemiBold, size = 11, lineHeight = 16, letterSpacing = 0.04)
    )
