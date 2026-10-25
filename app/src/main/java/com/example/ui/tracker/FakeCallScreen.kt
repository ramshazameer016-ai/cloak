package com.example.ui.tracker

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FakeCallScreen(
  callerName: String,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var isCallAnswered by remember { mutableStateOf(false) }
  var callDurationSeconds by remember { mutableIntStateOf(0) }
  var ringtone: Ringtone? by remember { mutableStateOf(null) }

  // Sound and vibration management
  DisposableEffect(Unit) {
    try {
      val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val rt = RingtoneManager.getRingtone(context, notificationUri)
      rt?.play()
      ringtone = rt
    } catch (_: Exception) {}

    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val pattern = longArrayOf(0, 800, 1000, 800, 1000)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 1))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(longArrayOf(0, 800, 1000, 800, 1000), 1)
      }
    } catch (_: Exception) {}

    onDispose {
      try {
        ringtone?.stop()
        vibrator?.cancel()
      } catch (_: Exception) {}
    }
  }

  // Active call timer
  LaunchedEffect(isCallAnswered) {
    if (isCallAnswered) {
      ringtone?.stop()
      val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      vibrator?.cancel()

      while (true) {
        delay(1000L)
        callDurationSeconds++
      }
    }
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("fake_call_screen"),
    color = Color(0xFF10141D) // Deep phone dark mode
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp, vertical = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Caller Header
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 40.dp)
      ) {
        Box(
          modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color(0xFF2B3242)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(56.dp)
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = callerName,
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = if (isCallAnswered) {
            val minutes = callDurationSeconds / 60
            val seconds = callDurationSeconds % 60
            String.format("%02d:%02d", minutes, seconds)
          } else {
            "Incoming call..."
          },
          fontSize = 16.sp,
          color = Color.White.copy(alpha = 0.7f)
        )
      }

      // Mid-Call Controls (shown after answering)
      AnimatedVisibility(visible = isCallAnswered) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          IconButton(onClick = {}) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.Mic, contentDescription = "Mute", tint = Color.White)
              Text("Mute", color = Color.White, fontSize = 12.sp)
            }
          }
          IconButton(onClick = {}) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.VolumeUp, contentDescription = "Speaker", tint = Color.White)
              Text("Speaker", color = Color.White, fontSize = 12.sp)
            }
          }
        }
      }

      // Call Actions (Accept / Decline)
      if (!isCallAnswered) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Decline Button
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFE53935))
                .clickable { onDismiss() }
                .testTag("decline_call_button"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "Decline Call",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Decline", color = Color.White, fontSize = 14.sp)
          }

          // Accept Button
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF43A047))
                .clickable { isCallAnswered = true }
                .testTag("accept_call_button"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Accept Call",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Accept", color = Color.White, fontSize = 14.sp)
          }
        }
      } else {
        // End Call Button
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(bottom = 32.dp)
        ) {
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(Color(0xFFE53935))
              .clickable { onDismiss() }
              .testTag("end_call_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.CallEnd,
              contentDescription = "End Call",
              tint = Color.White,
              modifier = Modifier.size(34.dp)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text("End Call", color = Color.White, fontSize = 14.sp)
        }
      }
    }
  }
}
