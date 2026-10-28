package com.paulfvt.liftapp.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.paulfvt.liftapp.R

private const val RING_SIZE_PX = 192
private const val STROKE_RATIO = 12f / 128f
private const val START_ANGLE = -90f
private const val FULL_TURN = 360f

// Anneau hebdomadaire du widget. Glance ne sait pas tracer d'arc : l'anneau est dessiné dans une petite image, avec
// les couleurs du thème clair ou sombre en cours (res/values et res/values-night).
fun weeklyRingBitmap(context: Context, done: Int, goal: Int): Bitmap {
    val bitmap = createBitmap(RING_SIZE_PX, RING_SIZE_PX)
    val canvas = Canvas(bitmap)
    val stroke = RING_SIZE_PX * STROKE_RATIO
    val bounds = RectF(stroke, stroke, RING_SIZE_PX - stroke, RING_SIZE_PX - stroke)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }
    paint.color = ContextCompat.getColor(context, R.color.widget_ring_track)
    canvas.drawArc(bounds, 0f, FULL_TURN, false, paint)
    val fraction = if (goal > 0) (done.toFloat() / goal).coerceIn(0f, 1f) else 0f
    if (fraction > 0f) {
        paint.color = ContextCompat.getColor(context, R.color.widget_ring_bar)
        canvas.drawArc(bounds, START_ANGLE, FULL_TURN * fraction, false, paint)
    }
    return bitmap
}
