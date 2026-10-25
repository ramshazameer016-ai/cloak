package com.example.core.sos

data class ContactSmsStatus(
  val name: String,
  val phoneNumber: String,
  val deliveryStatus: String // "Getting location", "Waiting for permission", "Sending SMS", "SMS submitted", "Failed"
)

sealed class SosState {
  data object Idle : SosState()

  data class Active(
    val sessionId: String,
    val triggerSource: String,
    val startTimeMs: Long,
    val accessToken: String = "",
    val audioAccessUrl: String = "",
    val lastMapsUrl: String? = null,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val locationTimestampMs: Long? = null,
    val updateCount: Int = 0,
    val statusMessage: String = "SOS active",
    val contactStatuses: List<ContactSmsStatus> = emptyList(),
    val audioRecordingStatus: String = "Starting recording",
    val chunkUploadStatus: String = "Idle",
    val hasLocationPermission: Boolean = true,
    val hasSmsPermission: Boolean = true,
    val hasAudioPermission: Boolean = true
  ) : SosState()
}
