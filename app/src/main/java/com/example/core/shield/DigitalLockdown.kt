package com.example.core.shield

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object DigitalLockdown {

  fun openWifiSettings(context: Context): Boolean {
    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(panelIntent)
      } else {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
      }
      true
    } catch (_: Exception) {
      try {
        val fallback = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallback)
        true
      } catch (_: Exception) {
        false
      }
    }
  }

  fun openBluetoothSettings(context: Context): Boolean {
    return try {
      val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      false
    }
  }

  fun openLocationSettings(context: Context): Boolean {
    return try {
      val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      false
    }
  }

  fun openAirplaneModeSettings(context: Context): Boolean {
    return try {
      val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      false
    }
  }
}
