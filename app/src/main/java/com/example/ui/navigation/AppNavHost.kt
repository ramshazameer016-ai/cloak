package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.CloakApplication
import com.example.ui.auth.KeypadScreen
import com.example.ui.auth.PinSetupScreen
import com.example.ui.demo.DemoSosScreen
import com.example.ui.fakesleep.FakeSleepFullScreen
import com.example.ui.shield.ShieldDashboardScreen
import com.example.ui.tracker.TrackerMainScreen

@Composable
fun AppNavHost(
  navController: NavHostController,
  app: CloakApplication
) {
  val startDestination = if (app.pinStorage.isSetupComplete()) {
    Screen.Keypad.route
  } else {
    Screen.PinSetup.route
  }

  NavHost(
    navController = navController,
    startDestination = startDestination
  ) {
    composable(Screen.PinSetup.route) {
      PinSetupScreen(
        pinStorage = app.pinStorage,
        onSetupComplete = {
          navController.navigate(Screen.Tracker.route) {
            popUpTo(Screen.PinSetup.route) { inclusive = true }
          }
        }
      )
    }

    composable(Screen.Keypad.route) {
      BackHandler {
        // Prevent back gesture from bypassing PIN lock
      }

      KeypadScreen(
        pinStorage = app.pinStorage,
        sosController = app.sosController,
        onUnlockSuccess = { isDuress ->
          if (isDuress && app.demoConfigManager.isDemoModeEnabled) {
            navController.navigate(Screen.DemoSos.route) {
              popUpTo(Screen.Keypad.route) { inclusive = true }
            }
          } else {
            navController.navigate(Screen.Tracker.route) {
              popUpTo(Screen.Keypad.route) { inclusive = true }
            }
          }
        }
      )
    }

    composable(Screen.Tracker.route) {
      BackHandler {
        // Pressing back from tracker locks back to Keypad
        navController.navigate(Screen.Keypad.route) {
          popUpTo(Screen.Tracker.route) { inclusive = true }
        }
      }

      TrackerMainScreen(
        pinStorage = app.pinStorage,
        sosController = app.sosController,
        contactRepository = app.contactRepository,
        periodRepository = app.periodRepository,
        demoConfigManager = app.demoConfigManager,
        onNavigateToShield = {
          navController.navigate(Screen.Shield.route)
        },
        onLockApp = {
          navController.navigate(Screen.Keypad.route) {
            popUpTo(Screen.Tracker.route) { inclusive = true }
          }
        },
        onResetPins = {
          navController.navigate(Screen.PinSetup.route) {
            popUpTo(0) { inclusive = true }
          }
        }
      )
    }

    composable(Screen.DemoSos.route) {
      BackHandler {
        // Keep demo active during presentation
      }

      DemoSosScreen(
        sosController = app.sosController,
        demoConfigManager = app.demoConfigManager,
        onTransitionToFakeSleep = {
          navController.navigate(Screen.FakeSleep.route) {
            popUpTo(Screen.DemoSos.route) { inclusive = true }
          }
        },
        onNavigateToShield = {
          navController.navigate(Screen.Shield.route)
        }
      )
    }

    composable(Screen.FakeSleep.route) {
      BackHandler {
        // Prevent back from skipping Fake-Sleep
      }

      FakeSleepFullScreen(
        pinStorage = app.pinStorage,
        sosController = app.sosController,
        onExitFakeSleep = {
          navController.navigate(Screen.Tracker.route) {
            popUpTo(Screen.FakeSleep.route) { inclusive = true }
          }
        }
      )
    }

    composable(Screen.Shield.route) {
      BackHandler {
        navController.popBackStack()
      }

      ShieldDashboardScreen(
        onDismiss = {
          navController.popBackStack()
        }
      )
    }
  }
}
