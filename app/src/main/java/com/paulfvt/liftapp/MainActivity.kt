package com.paulfvt.liftapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.paulfvt.liftapp.core.designsystem.theme.LiftTheme
import com.paulfvt.liftapp.navigation.LiftApp
import dagger.hilt.android.AndroidEntryPoint

// Activité unique : tous les écrans sont des destinations Compose gérées par la navigation.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dessine sous les barres système. Le Scaffold de LiftApp gère les marges (insets).
        enableEdgeToEdge()
        setContent {
            LiftTheme {
                LiftApp()
            }
        }
    }
}
