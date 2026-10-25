package com.example.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detects deliberate shake gestures using gravity-free linear acceleration.
 * Requirements:
 * - 3 qualifying acceleration spikes >= threshold (default 18.0 m/s^2)
 * - Each spike separated by >= 250ms
 * - All 3 spikes within a 1.5-second (1500ms) sliding window
 * - Exclusively foreground operation
 */
class ShakeDetector(
  private val context: Context,
  var threshold: Float = DEFAULT_THRESHOLD,
  val minSpikeIntervalMs: Long = DEFAULT_MIN_SPIKE_INTERVAL_MS,
  val windowDurationMs: Long = DEFAULT_WINDOW_MS,
  val requiredSpikes: Int = DEFAULT_REQUIRED_SPIKES,
  private val onShakeTriggered: () -> Unit
) : SensorEventListener {

  companion object {
    const val DEFAULT_THRESHOLD = 18.0f // m/s^2
    const val DEFAULT_MIN_SPIKE_INTERVAL_MS = 250L
    const val DEFAULT_WINDOW_MS = 1500L
    const val DEFAULT_REQUIRED_SPIKES = 3
    private const val GRAVITY_ALPHA = 0.8f
    private const val COOLDOWN_MS = 2000L
  }

  private var sensorManager: SensorManager? = null
  private var accelerometer: Sensor? = null
  private var isListening = false

  // High-pass filter gravity components
  private val gravity = FloatArray(3)
  private val spikeTimestamps = mutableListOf<Long>()
  private var lastTriggerTimestamp = 0L

  fun start() {
    if (isListening) return
    sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    accelerometer?.let {
      sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
      isListening = true
    }
  }

  fun stop() {
    if (!isListening) return
    sensorManager?.unregisterListener(this)
    isListening = false
    spikeTimestamps.clear()
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

    val now = System.currentTimeMillis()

    // 1. Isolate gravity using low-pass filter
    gravity[0] = GRAVITY_ALPHA * gravity[0] + (1 - GRAVITY_ALPHA) * event.values[0]
    gravity[1] = GRAVITY_ALPHA * gravity[1] + (1 - GRAVITY_ALPHA) * event.values[1]
    gravity[2] = GRAVITY_ALPHA * gravity[2] + (1 - GRAVITY_ALPHA) * event.values[2]

    // 2. Remove gravity to get linear acceleration
    val linearX = event.values[0] - gravity[0]
    val linearY = event.values[1] - gravity[1]
    val linearZ = event.values[2] - gravity[2]

    val linearMagnitude = sqrt((linearX * linearX + linearY * linearY + linearZ * linearZ).toDouble()).toFloat()

    val triggered = processAccelerationSample(linearMagnitude, now)
    if (triggered) {
      onShakeTriggered()
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

  /**
   * Process a single acceleration magnitude sample against the window and interval rules.
   * Public for deterministic unit testing without requiring real hardware.
   */
  fun processAccelerationSample(magnitude: Float, timestampMs: Long): Boolean {
    // Check refractory cooldown
    if (lastTriggerTimestamp > 0 && (timestampMs - lastTriggerTimestamp < COOLDOWN_MS)) {
      return false
    }

    // Prune spikes outside the sliding window
    spikeTimestamps.removeAll { timestampMs - it > windowDurationMs }

    if (magnitude >= threshold) {
      val lastSpike = spikeTimestamps.lastOrNull()
      if (lastSpike == null || (timestampMs - lastSpike) >= minSpikeIntervalMs) {
        spikeTimestamps.add(timestampMs)

        if (spikeTimestamps.size >= requiredSpikes) {
          spikeTimestamps.clear()
          lastTriggerTimestamp = timestampMs
          return true
        }
      }
    }
    return false
  }
}
