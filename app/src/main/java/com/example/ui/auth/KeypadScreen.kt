package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.security.PinStorage
import com.example.core.security.PinVerificationResult
import com.example.core.sos.SosController

@Composable
fun KeypadScreen(
  pinStorage: PinStorage,
  sosController: SosController,
  onUnlockSuccess: (isDuress: Boolean) -> Unit,
  viewModel: AuthViewModel = remember(pinStorage, sosController) {
    AuthViewModel(pinStorage, sosController)
  }
) {
  val enteredPin by viewModel.enteredPin.collectAsState()
  val errorMessage by viewModel.errorMessage.collectAsState()

  LaunchedEffect(viewModel) {
    viewModel.unlockEvent.collect { isDuress ->
      onUnlockSuccess(isDuress)
    }
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("keypad_screen"),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)
        .widthIn(max = 480.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Header Section
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 40.dp)
      ) {
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Petal Health Logo",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Petal",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        )

        Text(
          text = "Enter your 4-digit health PIN",
          style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 4 PIN Dots
        Row(
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          for (i in 0 until 4) {
            val isFilled = i < enteredPin.length
            Box(
              modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                  if (isFilled) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                  width = 1.5.dp,
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                  shape = CircleShape
                )
            )
          }
        }

        AnimatedVisibility(visible = errorMessage != null) {
          Text(
            text = errorMessage ?: "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 16.dp),
            textAlign = TextAlign.Center
          )
        }
      }

      // 3x4 Keypad Grid
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        val rows = listOf(
          listOf("1", "2", "3"),
          listOf("4", "5", "6"),
          listOf("7", "8", "9"),
          listOf("CLEAR", "0", "BACK")
        )

        for (row in rows) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            for (key in row) {
              KeypadButton(
                key = key,
                onClick = {
                  viewModel.onKeyClicked(key)
                }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun KeypadButton(
  key: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(76.dp)
      .clip(CircleShape)
      .background(
        if (key == "CLEAR" || key == "BACK") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else MaterialTheme.colorScheme.surfaceContainerHigh
      )
      .clickable { onClick() }
      .testTag("keypad_btn_$key"),
    contentAlignment = Alignment.Center
  ) {
    when (key) {
      "BACK" -> {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Backspace,
          contentDescription = "Backspace",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(24.dp)
        )
      }
      "CLEAR" -> {
        Text(
          text = "C",
          fontSize = 18.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      else -> {
        Text(
          text = key,
          fontSize = 28.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}
