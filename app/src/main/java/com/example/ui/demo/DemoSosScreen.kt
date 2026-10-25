package com.example.ui.demo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.demo.DemoConfigManager
import com.example.core.sos.SosController
import com.example.core.sos.SosState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DemoSosScreen(
  sosController: SosController,
  demoConfigManager: DemoConfigManager,
  onTransitionToFakeSleep: () -> Unit,
  onNavigateToShield: () -> Unit
) {
  val sosState by sosController.state.collectAsState()
  val transitionDelay = demoConfigManager.transitionDelaySeconds
  var secondsRemaining by remember { mutableIntStateOf(transitionDelay) }
  var elapsedMs by remember { mutableLongStateOf(0L) }

  // Live elapsed timer and auto-transition countdown
  LaunchedEffect(transitionDelay) {
    secondsRemaining = transitionDelay
    val startTick = System.currentTimeMillis()
    while (secondsRemaining > 0) {
      delay(1000L)
      secondsRemaining--
      elapsedMs = System.currentTimeMillis() - startTick
    }
    onTransitionToFakeSleep()
  }

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("demo_sos_screen"),
    color = Color(0xFF140D0F) // High-contrast stealth emergency theme
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(20.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Top Demo Badge & Transition Progress
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFFFFB74D))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = "DEMO MODE",
              color = Color.Black,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
          }

          Text(
            text = "Fake-Sleep in ${secondsRemaining}s",
            color = Color(0xFFFFB74D),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        val progress = 1f - (secondsRemaining.toFloat() / transitionDelay.coerceAtLeast(1).toFloat())
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
          color = Color(0xFFFFB74D),
          trackColor = Color(0xFF2C1E22)
        )
      }

      // Heading Section
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xFFE53935).copy(alpha = 0.2f))
            .border(1.5.dp, Color(0xFFE53935), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = Color(0xFFE53935),
            modifier = Modifier.size(26.dp)
          )
        }

        Column {
          Text(
            text = "Emergency SOS Active",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          )
          val elapsedSeconds = elapsedMs / 1000
          val timeFormatted = String.format(Locale.US, "%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
          Text(
            text = "Triggered via Duress PIN • Elapsed: $timeFormatted",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE0E0E0)
          )
        }
      }

      // --- ROW 1: LOCATION SHARING ---
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1417)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF332025)))
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("1. Emergency Contacts & SMS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }

          val active = sosState as? SosState.Active
          val contacts = active?.contactStatuses ?: emptyList()

          if (contacts.isEmpty()) {
            Text(
              text = "No contacts configured in Support Circle.",
              color = Color(0xFFB0BEC5),
              fontSize = 13.sp
            )
          } else {
            contacts.take(3).forEach { contact ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF261A1E), RoundedCornerShape(8.dp))
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(contact.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                  Text(contact.phoneNumber, color = Color(0xFF9E9E9E), fontSize = 11.sp)
                }

                StatusBadge(status = contact.deliveryStatus)
              }
            }
          }

          // Location Coordinates & Link
          if (active?.lastMapsUrl != null) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2A1C21), RoundedCornerShape(8.dp))
                .padding(10.dp)
            ) {
              Text(
                text = "GPS Coordinates: ${active.lastLatitude ?: 0.0}, ${active.lastLongitude ?: 0.0}",
                color = Color(0xFF81C784),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "Maps URL: ${active.lastMapsUrl}",
                color = Color(0xFF64B5F6),
                fontSize = 11.sp,
                maxLines = 1
              )
              if (active.locationTimestampMs != null) {
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(active.locationTimestampMs))
                Text(
                  text = "Acquired at: $timeStr",
                  color = Color(0xFF9E9E9E),
                  fontSize = 10.sp
                )
              }
            }
          } else {
            Text(
              text = "Acquiring GPS fix (Last known / Fused Provider)...",
              color = Color(0xFFFFB74D),
              fontSize = 12.sp
            )
          }
        }
      }

      // --- ROW 2: VOICE RECORDING & STORAGE ---
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1417)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF332025)))
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("2. Audio Capture (~5s Chunks)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }

          val active = sosState as? SosState.Active
          val recStatus = active?.audioRecordingStatus ?: "Idle"
          val chunkStatus = active?.chunkUploadStatus ?: "Idle"

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Recorder State:", color = Color(0xFFE0E0E0), fontSize = 13.sp)
            StatusBadge(status = recStatus)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Supabase Upload:", color = Color(0xFFE0E0E0), fontSize = 13.sp)
            StatusBadge(status = chunkStatus)
          }

          Text(
            text = "Private storage path: sos_audio/{sessionId}/chunk_*.m4a",
            color = Color(0xFF757575),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // --- ROW 3: SOS CONTROLLER & SERVICE ---
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1417)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF332025)))
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("3. Service & Controller Architecture", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }

          val active = sosState as? SosState.Active
          Text(
            text = "Central Controller: ${if (sosController.isSosActive) "ACTIVE (Idempotent)" else "IDLE"}",
            color = if (sosController.isSosActive) Color(0xFF81C784) else Color(0xFFB0BEC5),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Session ID: ${active?.sessionId ?: "N/A"}",
            color = Color(0xFF9E9E9E),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "Foreground Service: Running independently of UI. Will persist across screen changes and Fake-Sleep.",
            color = Color(0xFFB0BEC5),
            fontSize = 11.sp
          )
        }
      }

      // Actions Section
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = { onTransitionToFakeSleep() },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("demo_transition_fake_sleep_btn")
        ) {
          Text("Transition to Fake-Sleep (${secondsRemaining}s)", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = { onNavigateToShield() },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB74D)),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("demo_open_shield_btn")
        ) {
          Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Open Digital Shield Dashboard")
        }
      }
    }
  }
}

@Composable
private fun StatusBadge(status: String) {
  val (bgColor, textColor) = when {
    status.contains("submitted", ignoreCase = true) || status.contains("Uploaded", ignoreCase = true) || status.equals("Recording", ignoreCase = true) ->
      Pair(Color(0xFF1B5E20), Color(0xFF81C784))
    status.contains("Getting", ignoreCase = true) || status.contains("Sending", ignoreCase = true) || status.contains("Uploading", ignoreCase = true) || status.contains("Starting", ignoreCase = true) ->
      Pair(Color(0xFFE65100), Color(0xFFFFB74D))
    status.contains("Failed", ignoreCase = true) || status.contains("Permission", ignoreCase = true) || status.contains("denied", ignoreCase = true) ->
      Pair(Color(0xFFB71C1C), Color(0xFFFF8A80))
    else ->
      Pair(Color(0xFF37474F), Color(0xFFCFD8DC))
  }

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(4.dp))
      .background(bgColor)
      .padding(horizontal = 6.dp, vertical = 2.dp)
  ) {
    Text(
      text = status,
      color = textColor,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
