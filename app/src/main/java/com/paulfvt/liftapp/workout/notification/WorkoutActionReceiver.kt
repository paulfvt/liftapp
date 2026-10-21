package com.paulfvt.liftapp.workout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.paulfvt.liftapp.core.di.ApplicationScope
import com.paulfvt.liftapp.workout.data.WorkoutRepository
import com.paulfvt.liftapp.workout.domain.REST_ADD_SECONDS
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Actions de la notification de séance, utilisables écran verrouillé sans ouvrir l'app.
@AndroidEntryPoint
class WorkoutActionReceiver : BroadcastReceiver() {
    @Inject lateinit var workouts: WorkoutRepository

    @Inject lateinit var sync: WorkoutSessionSync

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_COMPLETE_SET -> {
                        val setId = intent.getLongExtra(EXTRA_SET_ID, NO_SET)
                        if (setId != NO_SET) workouts.completeSet(setId)
                    }
                    ACTION_ADD_REST -> workouts.adjustRest(REST_ADD_SECONDS)
                }
                // Mise à jour immédiate, sans attendre l'observation de la base : Android peut remettre l'app en
                // veille dès la fin du récepteur.
                sync.refresh()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_SET = "com.paulfvt.liftapp.action.COMPLETE_SET"
        const val ACTION_ADD_REST = "com.paulfvt.liftapp.action.ADD_REST"
        const val EXTRA_SET_ID = "set_id"
        private const val NO_SET = -1L
    }
}
