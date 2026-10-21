package com.paulfvt.liftapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.paulfvt.liftapp.core.designsystem.theme.LiftTheme
import com.paulfvt.liftapp.navigation.LiftApp
import dagger.hilt.android.AndroidEntryPoint

// Activité unique : tous les écrans sont des destinations Compose gérées par la navigation.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Augmente à chaque appui sur la notification de séance alors que l'app est déjà ouverte.
    private var openWorkoutRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dessine sous les barres système. Le Scaffold de LiftApp gère les marges (insets).
        enableEdgeToEdge()
        setContent {
            LiftTheme {
                LiftApp(openWorkoutRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_OPEN_WORKOUT) openWorkoutRequest++
    }

    companion object {
        const val ACTION_OPEN_WORKOUT = "com.paulfvt.liftapp.action.OPEN_WORKOUT"
    }
}
