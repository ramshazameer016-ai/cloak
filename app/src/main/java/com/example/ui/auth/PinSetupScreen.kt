package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.security.PinStorage

@Composable
fun PinSetupScreen(
  pinStorage: PinStorage,
  onSetupComplete: () -> Unit
) {
  var normalPin by remember { mutableStateOf("") }
  var confirmNormalPin by remember { mutableStateOf("") }
  var duressPin by remember { mutableStateOf("") }
  var confirmDuressPin by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("pin_setup_screen"),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(24.dp)
        .widthIn(max = 500.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      Box(
        modifier = Modifier
          .size(60.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = "Setup Security",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(32.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Security Setup",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      )

      Text(
        text = "Set your dual-access passcodes for Petal.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
      )

      // Normal PIN Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "1. Normal PIN (Daily Access)",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(start = 8.dp)
            )
          }

          Text(
            text = "Opens your cycle tracker normally. Cancels any active SOS.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
          )

          OutlinedTextField(
            value = normalPin,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) normalPin = it },
            label = { Text("4-digit Normal PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("normal_pin_input"),
            singleLine = true
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = confirmNormalPin,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmNormalPin = it },
            label = { Text("Confirm Normal PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("confirm_normal_pin_input"),
            singleLine = true
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Duress PIN Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "2. Duress PIN (Silent SOS)",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(start = 8.dp)
            )
          }

          Text(
            text = "Opens the exact same tracker UI, but silently triggers background emergency alerts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
          )

          OutlinedTextField(
            value = duressPin,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) duressPin = it },
            label = { Text("4-digit Duress PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("duress_pin_input"),
            singleLine = true
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = confirmDuressPin,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmDuressPin = it },
            label = { Text("Confirm Duress PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("confirm_duress_pin_input"),
            singleLine = true
          )
        }
      }

      AnimatedVisibility(visible = errorMessage != null) {
        Text(
          text = errorMessage ?: "",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(top = 12.dp)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = {
          errorMessage = null
          if (normalPin.length != 4 || duressPin.length != 4) {
            errorMessage = "Both PINs must be exactly 4 digits."
            return@Button
          }
          if (normalPin != confirmNormalPin) {
            errorMessage = "Normal PIN confirmation does not match."
            return@Button
          }
          if (duressPin != confirmDuressPin) {
            errorMessage = "Duress PIN confirmation does not match."
            return@Button
          }
          if (normalPin == duressPin) {
            errorMessage = "Normal PIN and Duress PIN cannot be identical."
            return@Button
          }

          val result = pinStorage.setupPins(normalPin, duressPin)
          result.onSuccess {
            onSetupComplete()
          }.onFailure {
            errorMessage = it.message ?: "Failed to save passcodes."
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_pins_button")
      ) {
        Text("Save & Activate Protection")
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Convenience demo loader button for testing architecture defaults (1234 / 9999)
      OutlinedButton(
        onClick = {
          normalPin = "1234"
          confirmNormalPin = "1234"
          duressPin = "9999"
          confirmDuressPin = "9999"
          errorMessage = null
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("load_demo_pins_button")
      ) {
        Text("Load Architecture Demo Values (1234 / 9999)")
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
