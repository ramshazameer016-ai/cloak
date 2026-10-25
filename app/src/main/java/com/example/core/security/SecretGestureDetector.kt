package com.example.core.security

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlin.math.sqrt

object SecretGestureConfig {
  const val HOLD_DURATION_MS = 3000L
  const val MAX_MOVEMENT_DELTA_PX = 20.0f

  /**
   * Evaluates if a pointer offset drift exceeds the 20-pixel cancellation threshold.
   * Public for deterministic unit testing.
   */
  fun isDriftExceeded(start: Offset, current: Offset, maxDeltaPx: Float = MAX_MOVEMENT_DELTA_PX): Boolean {
    val dx = current.x - start.x
    val dy = current.y - start.y
    val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    return distance > maxDeltaPx
  }
}

/**
 * Modifier that triggers a secret callback only when the user holds
 * without moving more than 20 pixels for 3 full seconds.
 * Triggers a short haptic vibration pulse on success.
 */
fun Modifier.secretShieldHold(
  context: Context,
  isSosActive: Boolean,
  onActivated: () -> Unit
): Modifier = this.pointerInput(isSosActive) {
  if (isSosActive) return@pointerInput // Locked out while SOS is active

  awaitEachGesture {
    val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)
    val startPos = down.position
    var isCancelled = false
    val startTime = System.currentTimeMillis()

    while (!isCancelled) {
      val event = awaitPointerEvent()
      val currentPointer = event.changes.firstOrNull { it.id == down.id }

      if (currentPointer == null || !currentPointer.pressed) {
        // Released before 3000ms
        isCancelled = true
        break
      }

      // Check if finger moved more than 20 pixels
      if (SecretGestureConfig.isDriftExceeded(startPos, currentPointer.position)) {
        isCancelled = true
        break
      }

      // Check if 3 seconds elapsed
      if (System.currentTimeMillis() - startTime >= SecretGestureConfig.HOLD_DURATION_MS) {
        // Haptic feedback confirmation
        triggerHapticPulse(context)
        onActivated()
        break
      }
    }
  }
}

private fun triggerHapticPulse(context: Context) {
  try {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
      @Suppress("DEPRECATION")
      vibrator?.vibrate(60)
    }
  } catch (_: Exception) {}
}
