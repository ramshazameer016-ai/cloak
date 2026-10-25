package com.example.ui.navigation

sealed class Screen(val route: String) {
  data object PinSetup : Screen("pin_setup")
  data object Keypad : Screen("keypad")
  data object Tracker : Screen("tracker")
  data object DemoSos : Screen("demo_sos")
  data object FakeSleep : Screen("fake_sleep")
  data object Shield : Screen("shield")
}
