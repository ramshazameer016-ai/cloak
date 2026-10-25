package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.AppNavHost
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val app by lazy { application as CloakApplication }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val navController = rememberNavController()
        AppNavHost(
          navController = navController,
          app = app
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Resume foreground shake detection
    app.shakeDetector.start()
  }

  override fun onPause() {
    super.onPause()
    // Pause sensor listener when app leaves foreground to conserve battery
    app.shakeDetector.stop()
  }
}
