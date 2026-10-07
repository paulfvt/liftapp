package com.paulfvt.liftapp.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Échelle de formes du guide de style : angles plutôt nets, comme un panneau lumineux.
// Seul extraLarge diffère de Material 3 (24 dp au lieu de 28) : feuilles du bas et dialogues moins arrondis.
internal val LiftShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )
