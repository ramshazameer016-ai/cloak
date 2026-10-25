package com.example.ui.fakesleep

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.security.PinStorage
import com.example.core.sos.SosController
import com.example.ui.auth.KeypadScreen
import kotlinx.coroutines.delay

/**
 * Minimal black screen simulating a powered-down / locked phone (Fake-Sleep).
 * The SOS controller and background services remain fully active independently of this UI.
 * A 3-second hold gesture reveals the keypad:
 * - Normal PIN: stops SOS and returns to Petal tracker.
 * - Duress PIN: leaves SOS active and returns to Petal tracker.
 */
@Composable
fun FakeSleepFullScreen(
  pinStorage: PinStorage,
  sosController: SosController,
  onExitFakeSleep: () -> Unit
) {
  var isHoldInitiated by remember { mutableStateOf(false) }
  var showUnlockKeypad by remember { mutableStateOf(false) }

  // 3-second hold detector for Fake-Sleep exit
  LaunchedEffect(isHoldInitiated) {
    if (isHoldInitiated) {
      delay(3000L)
      if (isHoldInitiated) {
        showUnlockKeypad = true
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .testTag("fake_sleep_fullscreen")
      .pointerInput(Unit) {
        detectTapGestures(
          onPress = {
            isHoldInitiated = true
            tryAwaitRelease()
            isHoldInitiated = false
          }
        )
      }
  ) {
    // When 3-second hold succeeds, reveal PIN entry keypad
    AnimatedVisibility(
      visible = showUnlockKeypad,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize()
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color(0xFF140D0F))
      ) {
        KeypadScreen(
          pinStorage = pinStorage,
          sosController = sosController,
          onUnlockSuccess = { isDuress ->
            showUnlockKeypad = false
            if (!isDuress) {
              // Normal PIN stops SOS
              if (sosController.isSosActive) {
                sosController.stopSos()
              }
            }
            onExitFakeSleep()
          }
        )
      }
    }
  }
}
