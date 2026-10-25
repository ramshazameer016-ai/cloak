package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.security.PinStorage
import com.example.core.security.PinVerificationResult
import com.example.core.sos.SosController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing custom keypad interaction, 4-digit accumulation,
 * and cryptographic verification.
 * Dispatches Normal PIN vs Duress PIN decisions to the SOS controller.
 */
class AuthViewModel(
  private val pinStorage: PinStorage,
  private val sosController: SosController
) : ViewModel() {

  private val _enteredPin = MutableStateFlow("")
  val enteredPin: StateFlow<String> = _enteredPin.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  // Single-shot event emitted when either PIN successfully validates (true = duress, false = normal)
  private val _unlockEvent = MutableSharedFlow<Boolean>(replay = 0, extraBufferCapacity = 1)
  val unlockEvent: SharedFlow<Boolean> = _unlockEvent.asSharedFlow()

  fun onKeyClicked(key: String) {
    _errorMessage.value = null
    when (key) {
      "CLEAR" -> _enteredPin.value = ""
      "BACK" -> {
        if (_enteredPin.value.isNotEmpty()) {
          _enteredPin.value = _enteredPin.value.dropLast(1)
        }
      }
      else -> {
        if (_enteredPin.value.length < 4 && key.all { it.isDigit() }) {
          val newPin = _enteredPin.value + key
          _enteredPin.value = newPin
          if (newPin.length == 4) {
            verifyPin(newPin)
          }
        }
      }
    }
  }

  fun verifyPin(pin: String) {
    val result = pinStorage.verify(pin)
    when (result) {
      is PinVerificationResult.NormalPinSuccess -> {
        // Normal PIN: stops any ongoing SOS session and proceeds to disguise
        if (sosController.isSosActive) {
          sosController.stopSos()
        }
        _unlockEvent.tryEmit(false)
      }
      is PinVerificationResult.DuressPinSuccess -> {
        // Duress PIN: silently activates the central idempotent SOS workflow
        // Invokes startSos with trigger source "duress_pin"
        sosController.startSos(triggerSource = "duress_pin")
        _unlockEvent.tryEmit(true)
      }
      is PinVerificationResult.InvalidPin -> {
        _errorMessage.value = "Incorrect passcode. Please try again."
        _enteredPin.value = ""
      }
    }
  }

  fun clearPin() {
    _enteredPin.value = ""
    _errorMessage.value = null
  }
}
