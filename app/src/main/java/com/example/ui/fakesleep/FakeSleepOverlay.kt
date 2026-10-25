package com.example.ui.fakesleep

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.security.PinStorage
import com.example.core.sos.SosController
import com.example.ui.auth.KeypadScreen
import kotlinx.coroutines.delay

@Composable
fun FakeSleepOverlay(
  isSosActive: Boolean,
  pinStorage: PinStorage,
  sosController: SosController,
  inactivityThresholdMs: Long = 15_000L,
  onNormalPinUnlocked: () -> Unit,
  onDuressPinUnlocked: () -> Unit
) {
  var lastUserInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var isFakeSleepVisible by remember { mutableStateOf(false) }
  var isHoldInitiated by remember { mutableStateOf(false) }
  var showUnlockKeypad by remember { mutableStateOf(false) }

  // 15-second Inactivity Timer during active SOS
  LaunchedEffect(isSosActive, lastUserInteractionTime, isFakeSleepVisible) {
    if (isSosActive && !isFakeSleepVisible) {
      while (true) {
        val elapsed = System.currentTimeMillis() - lastUserInteractionTime
        if (elapsed >= inactivityThresholdMs) {
          isFakeSleepVisible = true
          showUnlockKeypad = false
          break
        }
        delay(1000L)
      }
    } else if (!isSosActive) {
      isFakeSleepVisible = false
      showUnlockKeypad = false
    }
  }

  // 3-second hold detector for Fake-Sleep exit
  LaunchedEffect(isHoldInitiated) {
    if (isHoldInitiated) {
      delay(3000L)
      if (isHoldInitiated) {
        showUnlockKeypad = true
      }
    }
  }

  if (isFakeSleepVisible) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .testTag("fake_sleep_overlay")
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
      // Completely black screen simulating a locked device
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
            .background(Color(0xFF1B1112))
        ) {
          KeypadScreen(
            pinStorage = pinStorage,
            sosController = sosController,
            onUnlockSuccess = { isDuress ->
              isFakeSleepVisible = false
              showUnlockKeypad = false
              lastUserInteractionTime = System.currentTimeMillis()
              if (!isDuress) {
                onNormalPinUnlocked()
              } else {
                onDuressPinUnlocked()
              }
            }
          )
        }
      }
    }
  }
}
