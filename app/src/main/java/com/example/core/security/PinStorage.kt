package com.example.core.security

import android.content.Context
import android.content.SharedPreferences

sealed class PinVerificationResult {
  data object NormalPinSuccess : PinVerificationResult()
  data object DuressPinSuccess : PinVerificationResult()
  data object InvalidPin : PinVerificationResult()
}

/**
 * Manages persisted PIN hashes and provides distinct verification channels
 * for Normal and Duress credentials.
 */
class PinStorage(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  companion object {
    private const val PREFS_NAME = "cloak_secure_pin_store"
    private const val KEY_NORMAL_SALT = "normal_pin_salt"
    private const val KEY_NORMAL_HASH = "normal_pin_hash"
    private const val KEY_DURESS_SALT = "duress_pin_salt"
    private const val KEY_DURESS_HASH = "duress_pin_hash"
    private const val KEY_IS_SETUP_DONE = "is_pin_setup_completed"
  }

  fun isSetupComplete(): Boolean {
    return prefs.getBoolean(KEY_IS_SETUP_DONE, false) &&
      !prefs.getString(KEY_NORMAL_HASH, null).isNullOrBlank() &&
      !prefs.getString(KEY_DURESS_HASH, null).isNullOrBlank()
  }

  /**
   * Sets up both PINs atomically.
   * Requires:
   * - Both PINs must be exactly 4 numeric digits.
   * - Normal PIN and Duress PIN must NOT be identical.
   */
  fun setupPins(normalPin: String, duressPin: String): Result<Unit> {
    if (normalPin.length != 4 || !normalPin.all { it.isDigit() }) {
      return Result.failure(IllegalArgumentException("Normal PIN must be exactly 4 digits."))
    }
    if (duressPin.length != 4 || !duressPin.all { it.isDigit() }) {
      return Result.failure(IllegalArgumentException("Duress PIN must be exactly 4 digits."))
    }
    if (normalPin == duressPin) {
      return Result.failure(IllegalArgumentException("Normal PIN and Duress PIN cannot be identical."))
    }

    val normalHashed = CryptoManager.hashPin(normalPin)
    val duressHashed = CryptoManager.hashPin(duressPin)

    prefs.edit()
      .putString(KEY_NORMAL_SALT, normalHashed.saltHex)
      .putString(KEY_NORMAL_HASH, normalHashed.hashHex)
      .putString(KEY_DURESS_SALT, duressHashed.saltHex)
      .putString(KEY_DURESS_HASH, duressHashed.hashHex)
      .putBoolean(KEY_IS_SETUP_DONE, true)
      .apply()

    return Result.success(Unit)
  }

  fun verify(pin: String): PinVerificationResult {
    if (pin.length != 4) return PinVerificationResult.InvalidPin

    val normalSalt = prefs.getString(KEY_NORMAL_SALT, "") ?: ""
    val normalHash = prefs.getString(KEY_NORMAL_HASH, "") ?: ""
    if (CryptoManager.verifyPin(pin, normalSalt, normalHash)) {
      return PinVerificationResult.NormalPinSuccess
    }

    val duressSalt = prefs.getString(KEY_DURESS_SALT, "") ?: ""
    val duressHash = prefs.getString(KEY_DURESS_HASH, "") ?: ""
    if (CryptoManager.verifyPin(pin, duressSalt, duressHash)) {
      return PinVerificationResult.DuressPinSuccess
    }

    return PinVerificationResult.InvalidPin
  }

  fun clearAll() {
    prefs.edit().clear().apply()
  }
}
