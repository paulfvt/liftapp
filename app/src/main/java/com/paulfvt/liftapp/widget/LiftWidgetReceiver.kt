package com.paulfvt.liftapp.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

// Récepteur déclaré dans le manifeste : Android passe par lui pour créer, mettre à jour et redimensionner le widget.
class LiftWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiftWidget()
}
