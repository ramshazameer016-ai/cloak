package com.example.ui.tracker

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.core.security.PinStorage
import com.example.core.sos.SosController
import com.example.core.sos.SosState
import com.example.data.local.entity.EmergencyContact
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerShellScreen(
  pinStorage: PinStorage,
  sosController: SosController,
  contactRepository: ContactRepository,
  onLockApp: () -> Unit,
  onResetPins: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val sosState by sosController.state.collectAsState()
  val contacts by contactRepository.contactsFlow.collectAsState(initial = emptyList())

  var showAddContactDialog by remember { mutableStateOf(false) }
  var newContactName by remember { mutableStateOf("") }
  var newContactPhone by remember { mutableStateOf("") }
  var newContactRel by remember { mutableStateOf("") }
  var contactError by remember { mutableStateOf<String?>(null) }

  // Permission Launchers
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { _ ->
    // Permissions updated
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Favorite,
              contentDescription = "Petal Health",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Petal",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            )
          }
        },
        actions = {
          IconButton(
            onClick = onLockApp,
            modifier = Modifier.testTag("lock_app_button")
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Lock App",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 20.dp)
        .testTag("tracker_shell_content"),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      item { Spacer(modifier = Modifier.height(4.dp)) }

      // 1. Health Disguise Hero Card
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          shape = RoundedCornerShape(20.dp)
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Cycle Day 14",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "Fertile Window",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Next period predicted in 14 days (Average cycle: 28 days).",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "Daily Symptom Log: Normal • Mood: Calm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
        }
      }

      // 2. Support Circle (Emergency Contacts)
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(20.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Phone,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Support Circle (${contacts.size}/3)",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              if (contacts.size < 3) {
                OutlinedButton(
                  onClick = {
                    contactError = null
                    newContactName = ""
                    newContactPhone = ""
                    newContactRel = ""
                    showAddContactDialog = true
                  },
                  modifier = Modifier.testTag("add_contact_button")
                ) {
                  Icon(imageVector = Icons.Default.Add, contentDescription = "Add Contact", modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Add")
                }
              }
            }

            Text(
              text = "Trusted contacts who will receive emergency SMS alerts with your live location during SOS.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            if (contacts.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                  .padding(16.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "No emergency contacts yet. Tap '+ Add' to configure up to 3 contacts.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                contacts.forEach { contact ->
                  ContactItemRow(
                    contact = contact,
                    onDelete = {
                      scope.launch { contactRepository.deleteContact(contact.id) }
                    }
                  )
                }
              }
            }
          }
        }
      }

      // 3. Central SOS Controller Status & Test Controls (Stage 1 Core Safety Verification)
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
          colors = CardDefaults.cardColors(
            containerColor = if (sosState is SosState.Active)
              MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
          ),
          shape = RoundedCornerShape(20.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (sosState is SosState.Active) Icons.Default.NotificationsActive else Icons.Default.Security,
                  contentDescription = null,
                  tint = if (sosState is SosState.Active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "SOS Safety Engine",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(
                    if (sosState is SosState.Active) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.surfaceVariant
                  )
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = if (sosState is SosState.Active) "ACTIVE" else "IDLE",
                  color = if (sosState is SosState.Active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (val current = sosState) {
              is SosState.Idle -> {
                Text(
                  text = "Guardian is standing by in foreground. Ready for Duress PIN or Shake gesture (3 spikes >= 18 m/s²).",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              is SosState.Active -> {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = "Trigger Source: ${current.triggerSource}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                  )
                  Text(
                    text = "Status: ${current.statusMessage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  if (current.lastMapsUrl != null) {
                    Text(
                      text = "Maps URL: ${current.lastMapsUrl}",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(current.lastMapsUrl))
                        context.startActivity(intent)
                      }
                    )
                  }
                  Text(
                    text = "Location updates sent: ${current.updateCount} (every ~2 min)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons for Verification
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  sosController.startSos(triggerSource = "shake_simulated")
                },
                modifier = Modifier
                  .weight(1f)
                  .testTag("test_shake_trigger_button")
              ) {
                Icon(imageVector = Icons.Default.Vibration, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Simulate Shake", fontSize = 12.sp)
              }

              if (sosState is SosState.Active) {
                Button(
                  onClick = {
                    sosController.stopSos()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("stop_sos_button")
                ) {
                  Text("Stop SOS", fontSize = 12.sp)
                }
              } else {
                OutlinedButton(
                  onClick = {
                    permissionLauncher.launch(
                      arrayOf(
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                      )
                    )
                  },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("request_permissions_button")
                ) {
                  Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Permissions", fontSize = 12.sp)
                }
              }
            }
          }
        }
      }

      // 4. Testing & Maintenance Section
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(16.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Verification Tools",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Reset passcodes to rerun first-launch onboarding.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            OutlinedButton(
              onClick = {
                pinStorage.clearAll()
                onResetPins()
              },
              modifier = Modifier.testTag("reset_passcodes_button")
            ) {
              Text("Reset PINs", fontSize = 12.sp)
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }

  // Add Contact Dialog
  if (showAddContactDialog) {
    AlertDialog(
      onDismissRequest = { showAddContactDialog = false },
      title = { Text("Add Emergency Contact") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = newContactName,
            onValueChange = { newContactName = it },
            label = { Text("Name") },
            placeholder = { Text("e.g. Sister, Mom, Friend") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("contact_name_input")
          )
          OutlinedTextField(
            value = newContactPhone,
            onValueChange = { newContactPhone = it },
            label = { Text("Phone Number") },
            placeholder = { Text("e.g. +1234567890") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("contact_phone_input")
          )
          OutlinedTextField(
            value = newContactRel,
            onValueChange = { newContactRel = it },
            label = { Text("Relationship (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          contactError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            contactError = null
            if (newContactPhone.isBlank()) {
              contactError = "Phone number is required."
              return@Button
            }
            scope.launch {
              val res = contactRepository.addContact(
                name = newContactName,
                phoneNumber = newContactPhone,
                relationship = newContactRel
              )
              res.onSuccess {
                showAddContactDialog = false
              }.onFailure {
                contactError = it.message ?: "Failed to add contact."
              }
            }
          },
          modifier = Modifier.testTag("confirm_add_contact_button")
        ) {
          Text("Save Contact")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showAddContactDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun ContactItemRow(
  contact: EmergencyContact,
  onDelete: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .padding(horizontal = 14.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = contact.name.firstOrNull()?.uppercase() ?: "C",
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column {
        Text(
          text = contact.name,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = contact.phoneNumber + if (contact.relationship.isNotBlank()) " (${contact.relationship})" else "",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    IconButton(
      onClick = onDelete,
      modifier = Modifier.testTag("delete_contact_${contact.id}")
    ) {
      Icon(
        imageVector = Icons.Default.Delete,
        contentDescription = "Delete Contact",
        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
