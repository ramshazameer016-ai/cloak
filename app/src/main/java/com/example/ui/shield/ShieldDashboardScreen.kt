
package com.example.ui.shield

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.core.shield.DigitalLockdown
import com.example.core.shield.PhotoSanitizer
import com.example.core.shield.SanitizationResult
import com.example.core.telephony.WhatsAppRelay
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldDashboardScreen(
    contactRepository: ContactRepository? = null,
    onDismiss: () -> Unit = {},
    onCloseShield: () -> Unit = onDismiss
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedSubTool by remember { mutableIntStateOf(0) }

    val handleClose = {
        onCloseShield()
        onDismiss()
    }

    // Close Shield when the app moves into the background.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                handleClose()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("shield_dashboard_screen"),
        color = Color(0xFF141923)
    ) {
        Scaffold(
            containerColor = Color(0xFF141923),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(22.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Digital Wellbeing & Digital Shield",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = handleClose,
                            modifier = Modifier.testTag("close_shield_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Shield",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF1B2232)
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B2232))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Protect your personal data and review device controls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF90CAF9)
                    )
                }

                // Only the two remaining tools are displayed.
                TabRow(
                    selectedTabIndex = selectedSubTool,
                    containerColor = Color(0xFF1B2232),
                    contentColor = Color(0xFF64B5F6)
                ) {
                    Tab(
                        selected = selectedSubTool == 0,
                        onClick = { selectedSubTool = 0 },
                        text = { Text("Sanitizer", fontSize = 13.sp) },
                        icon = {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )

                    Tab(
                        selected = selectedSubTool == 1,
                        onClick = { selectedSubTool = 1 },
                        text = { Text("Lockdown", fontSize = 13.sp) },
                        icon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when (selectedSubTool) {
                        0 -> PhotoSanitizerSection()
                        1 -> DigitalLockdownSection(contactRepository)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. PHOTO SANITIZER SECTION
// -------------------------------------------------------------
@Composable
private fun PhotoSanitizerSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var sanitizationResult by remember {
        mutableStateOf<SanitizationResult?>(null)
    }
    var isProcessing by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
        sanitizationResult = null
        downloadMessage = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Photo EXIF Sanitizer",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Text(
            text = "Creates a cleaned photo copy intended to remove embedded location and device metadata.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E2638)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Selected Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A3650)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_photo_button")
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (selectedImageUri == null) {
                            "Select Photo from Gallery"
                        } else {
                            "Choose Different Photo"
                        },
                        color = Color.White
                    )
                }
            }
        }

        if (selectedImageUri != null) {
            Button(
                onClick = {
                    val uriToSanitize = selectedImageUri ?: return@Button

                    isProcessing = true
                    sanitizationResult = null
                    downloadMessage = null

                    scope.launch {
                        sanitizationResult = try {
                            withContext(Dispatchers.IO) {
                                PhotoSanitizer.sanitizePhoto(
                                    context,
                                    uriToSanitize
                                )
                            }
                        } catch (e: Exception) {
                            null
                        }

                        isProcessing = false
                    }
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF43A047)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sanitize_photo_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = Color.White
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        "Strip EXIF & Save Clean Copy",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        sanitizationResult?.let { res ->
            val canDownload = res.isSuccess &&
                res.verifiedClean &&
                res.outputFile?.isFile == true

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (canDownload) {
                        Color(0xFF163824)
                    } else {
                        Color(0xFF381B1E)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (canDownload) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Close
                            },
                            contentDescription = null,
                            tint = if (canDownload) {
                                Color(0xFF81C784)
                            } else {
                                Color(0xFFE57373)
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (canDownload) {
                                "Sanitization Verified: Clean"
                            } else {
                                "Sanitization Unverified"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (canDownload) {
                            "The cleaned copy passed the sanitizer's verification. The original photo is not overwritten."
                        } else {
                            "The cleaned copy could not be verified. Download is disabled for safety."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    res.outputFile?.let { outputFile ->
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Output: ${outputFile.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val outputFile = res.outputFile

                            if (
                                res.isSuccess &&
                                res.verifiedClean &&
                                outputFile != null &&
                                outputFile.isFile
                            ) {
                                isDownloading = true
                                downloadMessage = null

                                scope.launch {
                                    downloadMessage = try {
                                        withContext(Dispatchers.IO) {
                                            saveSanitizedPhotoToDevice(
                                                context = context,
                                                outputFile = outputFile
                                            )
                                        }
                                        "Safe photo saved to your device."
                                    } catch (e: Exception) {
                                        "Could not save the photo: ${
                                            e.message ?: "Unknown error"
                                        }"
                                    }

                                    isDownloading = false
                                }
                            }
                        },
                        enabled = canDownload && !isDownloading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0),
                            disabledContainerColor = Color(0xFF424957)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_safe_photo_button")
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Download Safe Photo",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    downloadMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = message,
                            color = if (message.startsWith("Safe photo saved")) {
                                Color(0xFF81C784)
                            } else {
                                Color(0xFFFFAB91)
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// Saves the sanitized image through Android's MediaStore.
// Android 10+ saves it in Downloads/Cloak.
// Older Android versions use the Pictures collection.

private fun saveSanitizedPhotoToDevice(
    context: android.content.Context,
    outputFile: File
): Uri {
    require(outputFile.isFile) {
        "The sanitized photo file does not exist."
    }

    val resolver = context.contentResolver
    val fileName = "Cloak_Safe_${System.currentTimeMillis()}.jpg"

    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${android.os.Environment.DIRECTORY_PICTURES}/Cloak"
            )
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    val destinationUri = resolver.insert(collection, values)
        ?: throw IOException("Android could not create the image file.")

    try {
        resolver.openOutputStream(destinationUri)?.use { destination ->
            outputFile.inputStream().use { source ->
                source.copyTo(destination)
            }
        } ?: throw IOException("Could not open the destination file.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val completedValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }

            resolver.update(destinationUri, completedValues, null, null)
        }

        return destinationUri
    } catch (e: Exception) {
        resolver.delete(destinationUri, null, null)
        throw e
    }
}


// -------------------------------------------------------------
// 2. DIGITAL LOCKDOWN SECTION
// -------------------------------------------------------------
@Composable
private fun DigitalLockdownSection(
    contactRepository: ContactRepository?
) {
    val context = LocalContext.current

    val contacts by (
        contactRepository?.contactsFlow
            ?: kotlinx.coroutines.flow.flowOf(emptyList())
        ).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Digital Lockdown",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Text(
            text = "Open Android system panels to manage device radios and location settings.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )

        LockdownActionButton(
            title = "Wi-Fi Panel",
            subtitle = "Manage wireless network connections",
            icon = Icons.Default.Wifi,
            onClick = { DigitalLockdown.openWifiSettings(context) }
        )

        LockdownActionButton(
            title = "Bluetooth Settings",
            subtitle = "Manage Bluetooth connections",
            icon = Icons.Default.Bluetooth,
            onClick = { DigitalLockdown.openBluetoothSettings(context) }
        )

        LockdownActionButton(
            title = "Location Services",
            subtitle = "Manage device location settings",
            icon = Icons.Default.LocationOn,
            onClick = { DigitalLockdown.openLocationSettings(context) }
        )

        LockdownActionButton(
            title = "Airplane Mode",
            subtitle = "Open airplane mode settings",
            icon = Icons.Default.AirplanemodeActive,
            onClick = { DigitalLockdown.openAirplaneModeSettings(context) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (contacts.isNotEmpty()) {
            Text(
                text = "Emergency WhatsApp Relay",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Text(
                text = "Open WhatsApp with a prefilled emergency alert for a Support Circle contact.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )

            contacts.forEach { contact ->
                Button(
                    onClick = {
                        WhatsAppRelay.openWhatsAppEmergencyChat(
                            context = context,
                            phoneNumber = contact.phoneNumber,
                            mapsUrl = null
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = null,
                        tint = Color.White
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        "Message ${contact.name} via WhatsApp",
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun LockdownActionButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2638)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A3650)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )

                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF64B5F6)
                )
            ) {
                Text("Open", fontSize = 12.sp)
            }
        }
    }
}
