package com.paulfvt.liftapp.nutrition.data

import com.google.mlkit.vision.text.Text
import com.paulfvt.liftapp.nutrition.domain.TextLine
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

// Lignes reconnues par ML Kit, pour la lecture du tableau (photo ou scan en direct).
fun Text.toTextLines(): List<TextLine> = textBlocks.flatMap { it.lines }.mapNotNull { it.toTextLine() }

// Rectangle de la ligne sans son inclinaison, d'après ses quatre coins (haut gauche, haut droit, bas droit, bas
// gauche) : sur une photo penchée, le cadre droit que donne ML Kit est plus haut que le texte. À défaut, ce cadre.
private fun Text.Line.toTextLine(): TextLine? {
    val corners = cornerPoints
    if (corners == null || corners.size < CORNER_COUNT) {
        return boundingBox?.let { TextLine(text, it.left, it.top, it.right, it.bottom) }
    }
    val (topLeft, topRight, _, bottomLeft) = corners
    val centerX = corners.sumOf { it.x } / CORNER_COUNT.toDouble()
    val centerY = corners.sumOf { it.y } / CORNER_COUNT.toDouble()
    val dx = (topRight.x - topLeft.x).toDouble()
    val dy = (topRight.y - topLeft.y).toDouble()
    val width = hypot(dx, dy)
    val height = hypot((bottomLeft.x - topLeft.x).toDouble(), (bottomLeft.y - topLeft.y).toDouble())
    return TextLine(
        text = text,
        left = (centerX - width / 2).roundToInt(),
        top = (centerY - height / 2).roundToInt(),
        right = (centerX + width / 2).roundToInt(),
        bottom = (centerY + height / 2).roundToInt(),
        angle = Math.toDegrees(atan2(dy, dx)).toFloat()
    )
}

private const val CORNER_COUNT = 4
