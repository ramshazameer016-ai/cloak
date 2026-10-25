package com.example.core.demo

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages developer/demo configuration for hackathon presentations.
 * When enabled, duress PIN entry displays an honest visible SOS demonstration screen
 * before automatically transitioning to the black Fake-Sleep screen.
 *
 * Disabled by default in production.
 */
class DemoConfigManager(context: Context) {

  companion object {
    private const val PREFS_NAME = "cloak_demo_prefs"
    private const val KEY_DEMO_ENABLED = "demo_mode_enabled"
    private const val KEY_TRANSITION_DELAY_SECONDS = "demo_transition_delay_seconds"
    const val DEFAULT_DELAY_SECONDS = 8
    const val MIN_DELAY_SECONDS = 3
    const val MAX_DELAY_SECONDS = 30
  }

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _isDemoModeEnabled = MutableStateFlow(
    prefs.getBoolean(KEY_DEMO_ENABLED, false)
  )
  val isDemoModeEnabledFlow: StateFlow<Boolean> = _isDemoModeEnabled.asStateFlow()

  val isDemoModeEnabled: Boolean
    get() = _isDemoModeEnabled.value

  private val _transitionDelaySeconds = MutableStateFlow(
    prefs.getInt(KEY_TRANSITION_DELAY_SECONDS, DEFAULT_DELAY_SECONDS)
  )
  val transitionDelaySecondsFlow: StateFlow<Int> = _transitionDelaySeconds.asStateFlow()

  val transitionDelaySeconds: Int
    get() = _transitionDelaySeconds.value

  fun setDemoModeEnabled(enabled: Boolean) {
    _isDemoModeEnabled.value = enabled
    prefs.edit().putBoolean(KEY_DEMO_ENABLED, enabled).apply()
  }

  fun setTransitionDelaySeconds(seconds: Int) {
    val clamped = seconds.coerceIn(MIN_DELAY_SECONDS, MAX_DELAY_SECONDS)
    _transitionDelaySeconds.value = clamped
    prefs.edit().putInt(KEY_TRANSITION_DELAY_SECONDS, clamped).apply()
  }
}
