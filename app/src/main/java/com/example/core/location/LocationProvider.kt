package com.example.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class LocationProvider(private val context: Context) {

  private val fusedClient by lazy {
    LocationServices.getFusedLocationProviderClient(context)
  }

  fun hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  /**
   * Retrieves the last known location if cached.
   */
  @SuppressLint("MissingPermission")
  suspend fun getLastKnownLocation(): Location? {
    if (!hasLocationPermission()) return null
    return try {
      fusedClient.lastLocation.await()
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Requests a fresh high-accuracy location fix.
   */
  @SuppressLint("MissingPermission")
  suspend fun getFreshLocation(): Location? {
    if (!hasLocationPermission()) return null
    return try {
      val cts = CancellationTokenSource()
      fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Formats a Google Maps URL matching the master specification.
   */
  fun formatGoogleMapsUrl(latitude: Double, longitude: Double): String {
    return "https://maps.google.com/?q=$latitude,$longitude"
  }
}
