package com.paulfvt.liftapp.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.paulfvt.liftapp.core.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Redessine le widget. Un widget ne se met pas à jour tout seul : l'app le lui demande quand ses données changent.
@Singleton
class WidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope,
    private val source: WidgetDataSource
) {
    // Sans widget posé, updateAll ne fait rien : l'appel ne coûte presque rien.
    suspend fun update() {
        runCatching { LiftWidget().updateAll(context) }
            .onFailure { Log.e(TAG, "Mise à jour du widget impossible", it) }
    }

    // Lancé au démarrage de l'app : tout changement des données du widget (séance, objectif, templates, historique)
    // le redessine, tant que l'app tourne. Le reste du temps, la mise à jour périodique (toutes les 3 h) prend le relais.
    fun start() {
        scope.launch { source.observe().collectLatest { update() } }
    }

    private companion object {
        const val TAG = "WidgetUpdater"
    }
}
